package kr.ac.cnu.networks;

import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.compress.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AesCodec implements SplittableCompressionCodec {

    private static final String HEADER = "AES";
    private static final int HEADER_LEN = HEADER.length();
    private static final String SUB_HEADER = "128";
    private static final int SUB_HEADER_LEN = SUB_HEADER.length();

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

    public SplitCompressionInputStream createInputStream(InputStream seekableIn,
                                                         Decompressor decompressor, long start, long end, READ_MODE readMode)
            throws IOException {

        if (!(seekableIn instanceof Seekable)) {
            throw new IOException("seekableIn must be an instance of " +
                    Seekable.class.getName());
        }

        ((Seekable) seekableIn).seek(0);

        final long FIRST_Aes_BLOCK_MARKER_POSITION =
                CAesInputStream.numberOfBytesTillNextMarker(seekableIn);
        long adjStart = Math.max(0L, start - FIRST_Aes_BLOCK_MARKER_POSITION);

        ((Seekable) seekableIn).seek(adjStart);
        SplitCompressionInputStream in =
                new AesCompressionInputStream(seekableIn, adjStart, end, readMode);

        if (in.getPos() <= start) {
            ((Seekable) seekableIn).seek(start);
            in = new AesCompressionInputStream(seekableIn, start, end, readMode);
        }

        return in;
    }

    public Class<? extends org.apache.hadoop.io.compress.Decompressor> getDecompressorType() {
        return AesDummyDecompressor.class;
    }

    public Decompressor createDecompressor() {
        return new AesDummyDecompressor();
    }

    public String getDefaultExtension() {
        return ".aes";
    }

    private static class AesCompressionOutputStream extends
            CompressionOutputStream {

        private CAesOutputStream output;
        private boolean needsReset;

        public AesCompressionOutputStream(OutputStream out)
                throws IOException {
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
            SplitCompressionInputStream {

        private CAesInputStream input;
        boolean needsReset;
        private BufferedInputStream bufferedIn;
        private boolean isHeaderStripped = false;
        private boolean isSubHeaderStripped = false;
        private READ_MODE readMode = READ_MODE.BYBLOCK;
        private long startingPos = 0L;

        private enum POS_ADVERTISEMENT_STATE_MACHINE {
            HOLD, ADVERTISE
        }

        ;

        POS_ADVERTISEMENT_STATE_MACHINE posSM = POS_ADVERTISEMENT_STATE_MACHINE.HOLD;
        long compressedStreamPosition = 0;

        public AesCompressionInputStream(InputStream in) throws IOException {
            this(in, 0L, Long.MAX_VALUE, READ_MODE.BYBLOCK);
        }

        public AesCompressionInputStream(InputStream in, long start, long end,
                                         READ_MODE readMode) throws IOException {
            super(in, start, end);
            needsReset = false;
            bufferedIn = new BufferedInputStream(super.in);
            this.startingPos = super.getPos();
            this.readMode = readMode;
            if (this.startingPos == 0) {
                bufferedIn = readStreamHeader();
            }
            input = new CAesInputStream(bufferedIn, readMode);
            if (this.isHeaderStripped) {
                input.updateReportedByteCount(HEADER_LEN);
            }

            if (this.isSubHeaderStripped) {
                input.updateReportedByteCount(SUB_HEADER_LEN);
            }

            this.updatePos(false);
        }

        private BufferedInputStream readStreamHeader() throws IOException {
            if (super.in != null) {
                bufferedIn.mark(HEADER_LEN);
                byte[] headerBytes = new byte[HEADER_LEN];
                int actualRead = bufferedIn.read(headerBytes, 0, HEADER_LEN);
                if (actualRead != -1) {
                    String header = new String(headerBytes);
                    if (header.compareTo(HEADER) != 0) {
                        bufferedIn.reset();
                    } else {
                        this.isHeaderStripped = true;
                        if (this.readMode == READ_MODE.BYBLOCK) {
                            actualRead = bufferedIn.read(headerBytes, 0,
                                    SUB_HEADER_LEN);
                            if (actualRead != -1) {
                                this.isSubHeaderStripped = true;
                            }
                        }
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

            int result = 0;
            result = this.input.read(b, off, len);

            if (result == AesConstants.END_OF_BLOCK) {
                this.posSM = POS_ADVERTISEMENT_STATE_MACHINE.ADVERTISE;
            }

            if (this.posSM == POS_ADVERTISEMENT_STATE_MACHINE.ADVERTISE) {
                result = this.input.read(b, off, off + 1);

                this.updatePos(true);
                this.posSM = POS_ADVERTISEMENT_STATE_MACHINE.HOLD;
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
                input = new CAesInputStream(bufferedIn, this.readMode);
            }
        }

        public void resetState() throws IOException {
            needsReset = true;
        }

        public long getPos() {
            return this.compressedStreamPosition;
        }

        private void updatePos(boolean shouldAddOn) {
            int addOn = shouldAddOn ? 1 : 0;
            this.compressedStreamPosition = this.startingPos
                    + this.input.getProcessedByteCount() + addOn;
        }
    }
}
