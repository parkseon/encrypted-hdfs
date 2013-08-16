package kr.ac.cnu.networks.hadoop.crypto.aes;

import kr.ac.cnu.networks.hadoop.crypto.CipherBuilder;
import kr.ac.cnu.networks.hadoop.crypto.CodecBlock;
import kr.ac.cnu.networks.hadoop.crypto.Utils;

import javax.crypto.CipherOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class AesEncryptor {
    private CipherOutputStream cipherOut;
    private OutputStream out;
    private long writtenBytes = 0L;
    private CodecBlock codecBlk = new CodecBlock(AesConstant.CODEC_BLOCK_SIZE);

    public AesEncryptor(OutputStream out) {
        cipherOut = new CipherOutputStream(out, CipherBuilder.getCipherInstance(CipherBuilder.CIPHER_AES_ENC));
        this.out = out;
    }

    public void write(int b) throws IOException {
        encrypt(b);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        encrypt(buf, off, len);
    }

    public void flush() throws IOException {
        cipherOut.flush();
    }

    public void close() throws IOException {
        // padding
        byte paddingSize = (byte) (16 - (writtenBytes % 16));
        if (paddingSize != 16) {
            byte[] padding = new byte[paddingSize];
            Arrays.fill(padding, paddingSize);
            encrypt(padding);
        }

        // write the rest of codec block
        writeLastBlock();

        // write the end of file
        writeEOF();

        // closing...
        flush();
        cipherOut.close();
    }

    public void write(byte[] buf) throws IOException {
        encrypt(buf);
    }


    private void encrypt(byte[] buf) throws IOException {
        encrypt(buf, 0, buf.length);
    }

    private void encrypt(byte[] buf, int off, int len) throws IOException {
        if (len == 0) return;

        int bufLen = len;
        int bufOff = off;
        int writeBytes;
        while((writeBytes = Math.min(codecBlk.remaining(), bufLen)) > 0) {
            codecBlk.put(buf, bufOff, writeBytes);
            bufOff += writeBytes;
            bufLen -= writeBytes;

            if(codecBlk.isFull()) {
                writtenBytes += codecBlk.getBlockLength();
                writeStartBlock(codecBlk.getBlockLength());
                cipherOut.write(codecBlk.get());
                codecBlk.clear();
            }
        }
    }

    private void encrypt(int b) throws IOException {
        encrypt(Utils.intToByte(b), 0, 4);
    }

    private void writeStartBlock(int blkLength) throws IOException {
        out.write(AesConstant.BLOCK_DELIMITER_BYTE);
        out.write(Utils.intToByte(blkLength));
    }

    private void writeLastBlock() throws IOException {
        if(codecBlk.getBlockLength() > 0) {
            writtenBytes += codecBlk.getBlockLength();
            writeStartBlock(codecBlk.getBlockLength());
            cipherOut.write(codecBlk.get(), 0, codecBlk.getBlockLength());
            codecBlk.clear();
        }
    }

    private void writeEOF() throws IOException {
        out.write(AesConstant.EOS_DELIMITER_BYTE);
    }
}
