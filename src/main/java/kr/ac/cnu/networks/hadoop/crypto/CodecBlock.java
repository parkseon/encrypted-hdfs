package kr.ac.cnu.networks.hadoop.crypto;

public class CodecBlock {
    private byte[] buffer;
    private int blkOff = 0;


    public CodecBlock(int size) {
        buffer = new byte[size];
    }

    public void put(byte[] b) {
        put(b, 0, b.length);
    }

    public void put(byte[] b, int off, int len) {
        System.arraycopy(b, off, buffer, blkOff, len);
        blkOff += len;
    }

    public void put(int b) {
        put(Utils.intToByte(b));
    }

    public int remaining() {
        return buffer.length - blkOff;
    }

    public boolean isFull() {
        return remaining() == 0;
    }

    public boolean isEmpty() {
        return getBlockLength() == 0;
    }

    public byte[] get() {
        return buffer;
    }

    public byte[] get(int n) {
        return buffer;
    }

    public int getBlockLength() {
        return blkOff;
    }

    public void clear() {
        blkOff = 0;
    }
}
