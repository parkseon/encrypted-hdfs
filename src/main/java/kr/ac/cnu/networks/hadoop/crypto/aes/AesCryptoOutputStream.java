package kr.ac.cnu.networks.hadoop.crypto.aes;

import org.apache.hadoop.io.compress.CompressionOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class AesCryptoOutputStream extends CompressionOutputStream {
    private boolean needsReset;
    private AesEncryptor encryptor;

    protected AesCryptoOutputStream(OutputStream out) {
        super(out);
        needsReset = true;
    }

    private void internalReset() throws IOException {
        if(needsReset) {
            needsReset = false;
            encryptor = new AesEncryptor(super.out);
        }

    }

    @Override
    public void write(int b) throws IOException {
        if(needsReset)
            internalReset();

        if (encryptor == null) {
            throw new IOException("stream closed");
        }

        encryptor.write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        if(needsReset)
            internalReset();

        if (off < 0) {
            throw new IndexOutOfBoundsException("off(" + off + ") < 0.");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len(" + len + ") < 0.");
        }
        if (off + len > buf.length) {
            throw new IndexOutOfBoundsException("off(" + off + ") + len("
                    + len + ") > buf.length(" + buf.length + ").");
        }
        if (encryptor == null) {
            throw new IOException("stream closed");
        }

        encryptor.write(buf, off, len);
    }

    @Override
    public void finish() throws IOException {
        if(needsReset)
            internalReset();

        if (encryptor != null) {
            encryptor = null;
        }
        needsReset = true;
    }

    @Override
    public void close() throws IOException {
        if(needsReset)
            internalReset();

        AesEncryptor encShadow = encryptor;
        if (encShadow != null) {
            encShadow.flush();
            finish();
            encShadow.close();
        }

        needsReset = true;
    }

    @Override
    public void resetState() throws IOException {
        needsReset = true;
    }
}
