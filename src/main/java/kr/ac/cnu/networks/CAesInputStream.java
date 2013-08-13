package kr.ac.cnu.networks;

import org.apache.hadoop.io.compress.SplittableCompressionCodec.READ_MODE;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CAesInputStream extends InputStream implements AesConstants {

    public static final long BLOCK_DELIMITER = 0X314159265359L;// start of block
    public static final long EOS_DELIMITER = 0X177245385090L;// end of Aes

    private static final int DELIMITER_BIT_LENGTH = 48;
    READ_MODE readMode = READ_MODE.BYBLOCK;
    private long reportedBytesReadFromCompressedStream = 0L;
    private long bytesReadFromCompressedStream = 0L;
    private boolean lazyInitialization = false;
    private byte array[] = new byte[1];

    private long bsBuff;
    private long bsLive;

    private BufferedInputStream in;

    private byte[] plain;
    private int plainOffs;
    private int plainLen;

    private byte[] crypt;
    private int cryptLen;
    private int cryptStatus;

    private AesDecryptor dec;

    public enum STATE {
        EOF, START_BLOCK_STATE, NO_PROCESS_STATE
    }

    ;

    private STATE currentState = STATE.START_BLOCK_STATE;

    private boolean skipResult = false;
    private static boolean skipDecompression = false;


    public long getProcessedByteCount() {
        return reportedBytesReadFromCompressedStream;
    }

    protected void updateProcessedByteCount(int count) {
        this.bytesReadFromCompressedStream += count;
    }

    public void updateReportedByteCount(int count) {
        this.reportedBytesReadFromCompressedStream += count;
        this.updateProcessedByteCount(count);
    }

    private int readAByte(InputStream inStream) throws IOException {
        int read = inStream.read();
        if (read >= 0) {
            this.updateProcessedByteCount(1);
        }
        return read;
    }

    public boolean skipToNextMarker(long marker, int markerBitLength)
            throws IOException, IllegalArgumentException {
        try {
            if (markerBitLength > 63) {
                throw new IllegalArgumentException("skipToNextMarker can not find patterns greater than 63 bits");
            }

            long bytes = 0;
            bytes = this.bsR(markerBitLength);
            if (bytes == -1) {
                return false;
            }
            while (true) {
                if (bytes == marker) {
                    return true;

                } else {
                    bytes = bytes << 1;
                    bytes = bytes & ((1L << markerBitLength) - 1);
                    int oneByte = (int) this.bsR(1);
                    if (oneByte != -1) {
                        bytes = bytes | oneByte;
                    } else
                        return false;
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    public CAesInputStream(final InputStream in, READ_MODE readMode)
            throws IOException {

        super();
        this.plain = new byte[baseBlockSize * 2];
        this.crypt = new byte[baseBlockSize * 2];
        this.dec = new AesDecryptor("2b7e151628aed2a6abf7158809cf4f3c");
        this.in = new BufferedInputStream(in, 1024 * 9);// >1 MB buffer
        readMode = READ_MODE.BYBLOCK;
        this.readMode = readMode;
        if (readMode == READ_MODE.CONTINUOUS) {
            currentState = STATE.START_BLOCK_STATE;
            lazyInitialization = (in.available() == 0) ? true : false;
            if (!lazyInitialization) {
                init();
            }
        } else if (readMode == READ_MODE.BYBLOCK) {
            this.currentState = STATE.NO_PROCESS_STATE;
            skipResult = this.skipToNextMarker(CAesInputStream.BLOCK_DELIMITER,
                    DELIMITER_BIT_LENGTH);
            this.reportedBytesReadFromCompressedStream = this.bytesReadFromCompressedStream;

            if (!skipDecompression) {
                changeStateToProcessABlock();
            }
        }
    }

    public static long numberOfBytesTillNextMarker(final InputStream in)
            throws IOException {
        CAesInputStream.skipDecompression = true;
        CAesInputStream anObject = null;

        anObject = new CAesInputStream(in, READ_MODE.BYBLOCK);

        return anObject.getProcessedByteCount();
    }

    public CAesInputStream(final InputStream in) throws IOException {
        this(in, READ_MODE.CONTINUOUS);
    }

    private void changeStateToProcessABlock() throws IOException {
        if (skipResult == true) {
            initBlock();
            setupBlock();
        } else {
            this.currentState = STATE.EOF;
        }
    }

    public int read() throws IOException {
        if (this.in != null) {
            int result = this.read(array, 0, 1);
            int value = 0XFF & array[0];
            return (result > 0 ? value : result);

        } else {
            throw new IOException("stream closed");
        }
    }

    public int read(final byte[] dest, final int offs, final int len)
            throws IOException {
        if (offs < 0) {
            throw new IndexOutOfBoundsException("offs(" + offs + ") < 0.");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len(" + len + ") < 0.");
        }
        if (offs + len > dest.length) {
            throw new IndexOutOfBoundsException("offs(" + offs + ") + len("
                    + len + ") > dest.length(" + dest.length + ").");
        }
        if (this.in == null) {
            throw new IOException("stream closed");
        }

        if (lazyInitialization) {
            this.init();
            this.lazyInitialization = false;
        }

        if (skipDecompression) {
            changeStateToProcessABlock();
            CAesInputStream.skipDecompression = false;
        }

        int destOffs = offs;

        int copyBytes = Math.min(len, this.plainLen - this.plainOffs);

        if (this.plainOffs < this.plainLen) {
            System.arraycopy(this.plain, this.plainOffs, dest, 0, copyBytes);
            this.plainOffs += copyBytes;
            destOffs += copyBytes;
        }

        int result = destOffs - offs;
        if (result == 0) {
            result = currentStats();

            skipResult = this.skipToNextMarker(CAesInputStream.BLOCK_DELIMITER,
                    DELIMITER_BIT_LENGTH);

            this.reportedBytesReadFromCompressedStream = this.bytesReadFromCompressedStream;

            changeStateToProcessABlock();
        }
        return result;
    }

    private int currentStats() throws IOException {
        switch (this.currentState) {
            case EOF:
                return END_OF_STREAM;
            case NO_PROCESS_STATE:
                return END_OF_BLOCK;
            case START_BLOCK_STATE:
                throw new IllegalStateException();
            default:
                throw new IllegalStateException();
        }
    }

    private void init() throws IOException {
        int magic = this.readAByte(in);
        if (magic != '1') {
            throw new IOException("Stream is not Aes formatted: expected 'h'"
                    + " as first byte but got '" + (char) magic + "'");
        }

        magic = this.readAByte(in);
        if (magic != '2') {
            throw new IOException("Stream is not Aes formatted: expected 'h'"
                    + " as first byte but got '" + (char) magic + "'");
        }

        magic = this.readAByte(in);
        if (magic != '8') {
            throw new IOException("Stream is not Aes formatted: expected 'h'"
                    + " as first byte but got '" + (char) magic + "'");
        }

        initBlock();
        setupBlock();
    }

    private void initBlock() throws IOException {
        if (this.readMode == READ_MODE.BYBLOCK) {
            this.currentState = STATE.START_BLOCK_STATE;

            this.cryptLen = 0;
            getAndMoveToFrontDecode();

            return;
        }
    }

    public void close() throws IOException {
        InputStream inShadow = this.in;
        if (inShadow != null) {
            try {
                if (inShadow != System.in) {
                    inShadow.close();
                }
            } finally {
                this.in = null;
            }
        }
    }

    private long bsR(final long n) throws IOException {
        long bsLiveShadow = this.bsLive;
        long bsBuffShadow = this.bsBuff;

        if (bsLiveShadow < n) {
            final InputStream inShadow = this.in;
            do {
                int thech = readAByte(inShadow);

                if (thech < 0) {
                    throw new IOException("unexpected end of stream");
                }

                bsBuffShadow = (bsBuffShadow << 8) | thech;
                bsLiveShadow += 8;
            } while (bsLiveShadow < n);

            this.bsBuff = bsBuffShadow;
        }

        this.bsLive = bsLiveShadow - n;
        return (bsBuffShadow >> (bsLiveShadow - n)) & ((1L << n) - 1);
    }

    private void getAndMoveToFrontDecode() throws IOException {
        do {
            this.cryptStatus = in.read(this.crypt, this.cryptLen,
                    baseBlockSize - this.cryptLen);
            if (this.cryptStatus > 0)
                this.cryptLen += this.cryptStatus;
        } while (this.cryptStatus >= 0 && this.cryptLen < baseBlockSize);
        this.updateProcessedByteCount(this.cryptLen);
        this.plainLen = this.dec.decrypt(this.crypt, 0, this.cryptLen,
                this.plain, 0);
        this.plainOffs = 0;
    }

    private void setupBlock() throws IOException {
        this.currentState = STATE.NO_PROCESS_STATE;
    }
}
