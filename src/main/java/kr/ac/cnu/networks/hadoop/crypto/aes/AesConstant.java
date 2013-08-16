package kr.ac.cnu.networks.hadoop.crypto.aes;

import javax.xml.bind.DatatypeConverter;

public class AesConstant {
    public static final int MARKET_BIT_LENGTH = 6 * 8;   // 48bit
    public static final int CODEC_BLOCK_SIZE = 1048576; // 1MB
    public static final long BLOCK_DELIMITER = 0x314159265359L;
    public static final long EOS_DELIMITER = 0x177245385090L;
    public static final byte[] BLOCK_DELIMITER_BYTE = DatatypeConverter.parseHexBinary("314159265359");
    public static final byte[] EOS_DELIMITER_BYTE = DatatypeConverter.parseHexBinary("177245385090");
}
