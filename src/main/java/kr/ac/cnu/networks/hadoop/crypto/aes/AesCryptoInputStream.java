package kr.ac.cnu.networks.hadoop.crypto.aes;

import kr.ac.cnu.networks.hadoop.crypto.CryptoConstants;
import org.apache.hadoop.io.compress.SplitCompressionInputStream;
import org.apache.hadoop.io.compress.SplittableCompressionCodec.READ_MODE;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AesCryptoInputStream extends SplitCompressionInputStream {
    private AesDecryptor decryptor;
    private BufferedInputStream bufferedIn;
    private boolean needsReset;
    private READ_MODE readMode;
    private long startPosition;
    private long decryptedPosition;

    private enum POS_ADVERTISEMENT_STATE_MACHINE {
        HOLD, ADVERTISE
    }

    POS_ADVERTISEMENT_STATE_MACHINE posSM = POS_ADVERTISEMENT_STATE_MACHINE.HOLD;

    public AesCryptoInputStream(InputStream in) throws IOException {
        this(in, 0L, Long.MAX_VALUE, READ_MODE.BYBLOCK);
    }

    public AesCryptoInputStream(InputStream in, long start, long end, READ_MODE readMode) throws IOException {
        super(in, start, end);
        needsReset = false;
        bufferedIn = new BufferedInputStream(super.in);
        decryptor = new AesDecryptor(in, READ_MODE.BYBLOCK);  // currently only support READ_MODE.BYBLOCK
        this.readMode = READ_MODE.BYBLOCK;  // currently only support READ_MODE.BYBLOCK
        startPosition = start;
        updatePos(false);
    }

    @Override
    public long getPos() {
        return decryptedPosition;
    }

    @Override
    public int read() throws IOException {
        byte b[] = new byte[1];
        int result = this.read(b, 0, 1);
        return (result < 0) ? result : (b[0] & 0xff);
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (needsReset) {
            internalReset();
        }

        int result = 0;
        result = read0(buf, off, len);
        if (result == CryptoConstants.END_OF_BLOCK) {
            this.posSM = POS_ADVERTISEMENT_STATE_MACHINE.ADVERTISE;
        }

        if (this.posSM == POS_ADVERTISEMENT_STATE_MACHINE.ADVERTISE) {
            result = read0(buf, off, off + 1);
            this.updatePos(true);
            this.posSM = POS_ADVERTISEMENT_STATE_MACHINE.HOLD;
        }
        return result;
    }

    private int read0(byte[] buf, int off, int len) throws IOException {
        if (off < 0) {
            throw new IndexOutOfBoundsException("offs(" + off + ") < 0.");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len(" + len + ") < 0.");
        }
        if (off + len > buf.length) {
            throw new IndexOutOfBoundsException("offs(" + off + ") + len("
                    + len + ") > dest.length(" + buf.length + ").");
        }

        if (this.in == null) {
            throw new IOException("stream closed");
        }

        return decryptor.read(buf, off, len);
    }

    @Override
    public void resetState() throws IOException {
        needsReset = true;
    }

    private void internalReset() throws IOException {
        if (needsReset) {
            needsReset = false;
            decryptor = new AesDecryptor(bufferedIn, readMode);
        }
    }

    @Override
    public void close() throws IOException {
        if (!needsReset) {
            AesDecryptor decShadow = decryptor;
            if (decShadow != null) {
                decShadow.close();
                decryptor = null;
            }
            needsReset = true;
        }
    }

    private void updatePos(boolean shouldAddOn) {
        int addOn = shouldAddOn ? 1 : 0;
        decryptedPosition = startPosition + decryptor.getProcessedByteCount() + addOn;
    }
}
