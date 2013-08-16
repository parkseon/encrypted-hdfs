package kr.ac.cnu.networks.hadoop.crypto;

import java.nio.ByteBuffer;

public class Utils {
    private static final ByteBuffer converter = ByteBuffer.allocate(4); // int to byte array(4)

    public static final byte[] intToByte(int i) {
        converter.clear();
        converter.putInt(i);
        byte[] ret = converter.array();
        return ret;
    }

    public static final int byteToInt(byte[] b) {
        converter.clear();
        converter.put(b);
        converter.flip();
        int ret = converter.getInt();
        return ret;
    }

    public static final byte getByte(byte[] b, int index) {
        converter.clear();
        converter.put(b);
        return converter.get(index);
    }
}
