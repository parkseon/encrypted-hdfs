package kr.ac.cnu.networks;

import org.apache.hadoop.io.compress.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AesSerialCodec implements CompressionCodec {

    private static final String HEADER = "AES128";
    private static final int HEADER_LEN = HEADER.length();

    public AesSerialCodec() {
    }

    public CompressionOutputStream createOutputStream(OutputStream out)
            throws IOException {
        return new AesCompressionOutputStream(out);
    }

    public CompressionOutputStream createOutputStream(OutputStream out,
                                                      Compressor compressor) throws IOException {
        return createOutputStream(out);
    }

    public Class<? extends org.apache.hadoop.io.compress.Compressor> getCompressorType() {
        return AesDummyCompressor.class;
    }

    public Compressor createCompressor() {
        return new AesDummyCompressor();
    }

    public CompressionInputStream createInputStream(InputStream in)
            throws IOException {
        return new AesCompressionInputStream(in);
    }

    public CompressionInputStream createInputStream(InputStream in,
                                                    Decompressor decompressor) throws IOException {
        return createInputStream(in);
    }

    public Class<? extends org.apache.hadoop.io.compress.Decompressor> getDecompressorType() {
        return AesDummyDecompressor.class;
    }

    public Decompressor createDecompressor() {
        return new AesDummyDecompressor();
    }

    public String getDefaultExtension() {
        return ".saes";
    }

    private static class AesCompressionOutputStream extends CompressionOutputStream {

        private CAesOutputStream output;
        private boolean needsReset;

        public AesCompressionOutputStream(OutputStream out) throws IOException {
            super(out);
            needsReset = true;
        }

        private void writeStreamHeader() throws IOException {
            if (super.out != null) {
                out.write(HEADER.getBytes());
            }
        }

        public void finish() throws IOException {
            if (needsReset) {
                internalReset();
            }
            this.output.finish();
            needsReset = true;
        }

        private void internalReset() throws IOException {
            if (needsReset) {
                needsReset = false;
                writeStreamHeader();
                this.output = new CAesOutputStream(out);
            }
        }

        public void resetState() throws IOException {
            needsReset = true;
        }

        public void write(int b) throws IOException {
            if (needsReset) {
                internalReset();
            }
            this.output.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (needsReset) {
                internalReset();
            }
            this.output.write(b, off, len);
        }

        public void close() throws IOException {
            if (needsReset) {
                internalReset();
            }
            this.output.flush();
            this.output.close();
            needsReset = true;
        }

    }

    private static class AesCompressionInputStream extends
            CompressionInputStream {

        private CAesInputStream input;
        boolean needsReset;
        private BufferedInputStream bufferedIn;

        public AesCompressionInputStream(InputStream in) throws IOException {
            super(in);
            needsReset = true;
        }

        private BufferedInputStream readStreamHeader() throws IOException {
            if (super.in != null) {
                bufferedIn = new BufferedInputStream(super.in);
                bufferedIn.mark(HEADER_LEN);
                byte[] headerBytes = new byte[HEADER_LEN];
                int actualRead = bufferedIn.read(headerBytes, 0, HEADER_LEN);
                if (actualRead != -1) {
                    String header = new String(headerBytes);
                    if (header.compareTo(HEADER) != 0) {
                        bufferedIn.reset();
                    }
                }
            }

            if (bufferedIn == null) {
                throw new IOException("Failed to read Aes stream.");
            }

            return bufferedIn;

        }

        public void close() throws IOException {
            if (!needsReset) {
                input.close();
                needsReset = true;
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (needsReset) {
                internalReset();
            }

            int result = this.input.read(b, off, len);

            if (result == AesConstants.END_OF_BLOCK) {
                result = this.input.read(b, off, len);
            }

            return result;
        }

        public int read() throws IOException {
            byte b[] = new byte[1];
            int result = this.read(b, 0, 1);
            return (result < 0) ? result : (b[0] & 0xff);
        }

        private void internalReset() throws IOException {
            if (needsReset) {
                needsReset = false;
                BufferedInputStream bufferedIn = readStreamHeader();
                input = new CAesInputStream(bufferedIn);
            }
        }

        public void resetState() throws IOException {
            needsReset = true;
        }
    }
}
