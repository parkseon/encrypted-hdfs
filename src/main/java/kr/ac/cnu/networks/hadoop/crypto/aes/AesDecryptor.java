package kr.ac.cnu.networks.hadoop.crypto.aes;

import kr.ac.cnu.networks.hadoop.crypto.CipherBuilder;
import kr.ac.cnu.networks.hadoop.crypto.CryptoConstants;
import kr.ac.cnu.networks.hadoop.crypto.Utils;
import org.apache.hadoop.io.compress.SplittableCompressionCodec.READ_MODE;

import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;

public class AesDecryptor {
    private READ_MODE readMode;
    private Cipher cipher;
    private InputStream in;
    private long bytesReadFromCompressedStream = 0L;
    private long reportedBytesReadFromCompressedStream = 0L;
    private byte[] codecBlk = new byte[AesConstant.CODEC_BLOCK_SIZE];
    private int codecBlkLen = 0;
    private int codecBlkOff = 0;
    private int readCodecBlkLen = 0;
    private byte[] decryptedCodecBlk;
    private int decryptedCodecBlkOff = 0;
    private int decryptedCodecBlkLen = 0;
    private enum STATE { EOS, READY, DECRYPT, EOB }
    private STATE currentState = STATE.READY;

    public AesDecryptor(InputStream in, READ_MODE readMode) {
        cipher = CipherBuilder.getCipherInstance(CipherBuilder.CIPHER_AES_DEC);
        this.in = in;
        this.readMode = readMode;
    }

    public void close() throws IOException {
        InputStream inShadow = in;
        if (inShadow != null) {
            try {
                inShadow.close();
            } finally {
                this.in = null;
            }
        }
    }



    public int read(byte[] buf, int off, int len) throws IOException {
        int returnStatus = 0;
        int readStatus;

        if(currentState == STATE.READY) {
            readStatus = findSyncMark();
            if(readStatus < 0) {
                reportedBytesReadFromCompressedStream = bytesReadFromCompressedStream;
                return CryptoConstants.END_OF_STREAM;
            }
            currentState = STATE.DECRYPT;
            readCodecBlkLen = getCodecBlkLen();
        } else if(currentState == STATE.EOB) {
            currentState = STATE.READY;
            codecBlkOff = 0;
            codecBlkLen = 0;
            reportedBytesReadFromCompressedStream = bytesReadFromCompressedStream;
            return CryptoConstants.END_OF_BLOCK;
        }

        if (decryptedCodecBlk == null) {
            int readBytes;
            while((readBytes = readCodecBlkLen - codecBlkLen) > 0) {
                readStatus = in.read(codecBlk, codecBlkLen, readBytes);
                codecBlkLen += readStatus;
            }
            updateProcessedByteCount(codecBlkLen);
            decryptedCodecBlk = cipher.update(codecBlk, codecBlkOff, codecBlkLen);
            decryptedCodecBlkOff = 0;
            decryptedCodecBlkLen = decryptedCodecBlk.length;
        }

        int copyBytes = Math.min(decryptedCodecBlkLen, len);
        System.arraycopy(decryptedCodecBlk, decryptedCodecBlkOff, buf, off, copyBytes);
        returnStatus = copyBytes;
        decryptedCodecBlkOff += copyBytes;
        decryptedCodecBlkLen -= copyBytes;
        if(decryptedCodecBlkLen == 0) {
            codecBlkOff += codecBlkLen;
            codecBlkLen = 0;
            decryptedCodecBlk = null;
        }

        if (codecBlkOff == readCodecBlkLen) {
            currentState = STATE.EOB;
        }

        return returnStatus;
    }

    private int findSyncMark() throws IOException {
        long syncMarker;
        int readBytes = 0;

        syncMarker = getNBytes(6);
        readBytes += 6;
        if(syncMarker == AesConstant.EOS_DELIMITER) return -1;
        while(syncMarker != AesConstant.BLOCK_DELIMITER) {
            int read = readAByte();
            readBytes++;
            syncMarker = ((syncMarker << 8) | (read & 0xFF)) & ((1L << AesConstant.MARKET_BIT_LENGTH) - 1);
            if(syncMarker == AesConstant.EOS_DELIMITER) return -1;
        }

        return readBytes;
    }

    private int getCodecBlkLen() throws IOException {
        byte[] blk = new byte[4];
        int off = 0;
        do {
            int status = in.read(blk, off, 4 - off);

            off += status;
        } while(off != 4);
        updateProcessedByteCount(4);
        return Utils.byteToInt(blk);
    }
    private long getNBytes(int n) throws IOException {
        long readBytesValue = 0L;
        for(int i = 0;i < n;i++) {
            int read = readAByte();
            readBytesValue = read | (readBytesValue << 8);
        }
        return readBytesValue & ((1L << AesConstant.MARKET_BIT_LENGTH) - 1);
    }

    private int readAByte() throws IOException {
        int read = in.read();
        if (read >= 0) {
            this.updateProcessedByteCount(1);
        } else {
            throw new IOException("unexpected end of stream");
        }

        return read;
    }

    private void updateProcessedByteCount(int count) {
        bytesReadFromCompressedStream += count;
    }

    public long getProcessedByteCount() {
        return reportedBytesReadFromCompressedStream;
    }
}
