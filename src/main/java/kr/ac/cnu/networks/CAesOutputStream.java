package kr.ac.cnu.networks;

import java.io.IOException;
import java.io.OutputStream;

public class CAesOutputStream extends OutputStream {
    private int blockOffs;

    private int allowableBlockSize;

    private OutputStream out;

    private AesEncryptor enc;

    private boolean needsInit;

    public CAesOutputStream(final OutputStream out) throws IOException {
        super();

        this.out = out;
        this.enc = new AesEncryptor("2b7e151628aed2a6abf7158809cf4f3c");
        this.needsInit = false;
        init();
    }

    public void write(final int b) throws IOException {
        if (this.out != null) {

        } else {
            throw new IOException("closed");
        }
    }

    protected void finalize() throws Throwable {
        finish();
        super.finalize();
    }

    public void finish() throws IOException {
        if (out != null) {
            this.out = null;
        }
    }

    public void close() throws IOException {
        if (out != null) {
            OutputStream outShadow = this.out;
            finish();
            outShadow.close();
        }
    }

    public void flush() throws IOException {
        OutputStream outShadow = this.out;
        if (outShadow != null) {
            outShadow.flush();
        }
    }

    private void init() throws IOException {
        bsPutUByte('1');
        bsPutUByte('2');
        bsPutUByte('8');

        initBlock();
    }

    private void initBlock() throws IOException {
        this.blockOffs = 0;

        this.allowableBlockSize = AesConstants.baseBlockSize;

        bsPutUByte(0x31);
        bsPutUByte(0x41);
        bsPutUByte(0x59);
        bsPutUByte(0x26);
        bsPutUByte(0x53);
        bsPutUByte(0x59);
    }

    public void write(final byte[] buf, int offs, final int len)
            throws IOException {
        if (offs < 0) {
            throw new IndexOutOfBoundsException("offs(" + offs + ") < 0.");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len(" + len + ") < 0.");
        }
        if (offs + len > buf.length) {
            throw new IndexOutOfBoundsException("offs(" + offs + ") + len("
                    + len + ") > buf.length(" + buf.length + ").");
        }
        if (this.out == null) {
            throw new IOException("stream closed");
        }

        byte[] cryptBuffer = this.enc.encrypt(buf, offs, len);
        int cryptBufLength = cryptBuffer.length;

        int cryptBufOffs = 0;

        if (cryptBuffer != null && cryptBufLength > 0) {
            while (cryptBufOffs < cryptBufLength) {
                if (this.needsInit) {
                    initBlock();
                    this.needsInit = false;
                }

                int wlength = cryptBufLength - cryptBufOffs;
                int remainlength = this.allowableBlockSize - this.blockOffs;
                if (wlength < remainlength) {
                    this.out.write(cryptBuffer, cryptBufOffs, wlength);
                    blockOffs += wlength;
                    cryptBufOffs += wlength;
                } else {
                    this.out.write(cryptBuffer, cryptBufOffs, remainlength);
                    cryptBufOffs += remainlength;
                    blockOffs += remainlength;
                    this.needsInit = true;
                }
            }
        }
    }

    private void bsW(final int n, final int v) throws IOException {
        final OutputStream outShadow = this.out;
        int bsLiveShadow = n;
        int bsBuffShadow = v;

        while (bsLiveShadow >= 8) {
            outShadow.write(bsBuffShadow & (0xff << (v - bsBuffShadow)));
            bsBuffShadow <<= 8;
            bsLiveShadow -= 8;
        }
    }

    private void bsPutUByte(final int c) throws IOException {
        bsW(8, c);
    }
}
