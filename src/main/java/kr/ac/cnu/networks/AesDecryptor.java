package kr.ac.cnu.networks;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

public class AesDecryptor {
    private SecretKeySpec sKeySpec = null;
    private AlgorithmParameterSpec paramSpec = null;
    private Cipher cipher = null;

    public AesDecryptor(String hexKey) {

        final byte[] symKeyData = DatatypeConverter.parseHexBinary(hexKey);
        this.sKeySpec = new SecretKeySpec(symKeyData, "AES");
        try {
            this.cipher = Cipher.getInstance("AES/ECB/NoPadding");
            this.cipher.init(Cipher.DECRYPT_MODE, this.sKeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public boolean ready() {
        if (sKeySpec != null && paramSpec != null && cipher != null)
            return true;
        return false;
    }

    public byte[] decrypt(byte[] b) {
        return cipher.update(b);
    }

    public byte[] decrypt(byte[] b, int off, int len) {
        return cipher.update(b, off, len);
    }

    public int decrypt(byte[] b, int off, int len, byte[] dest, int destOff) {
        try {
            return cipher.update(b, off, len, dest, destOff);
        } catch (ShortBufferException e) {
            System.err.println("ShortBufferException occurs");
            return -1;
        }
    }

    public int decryptFinal(byte[] dest, int destOff) {
        int ret = -1;

        try {
            ret = cipher.doFinal(dest, destOff);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return -1;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return -1;
        } catch (ShortBufferException e) {
            e.printStackTrace();
            return -1;
        }

        return ret;
    }

    public byte[] decryptFinal() {
        byte[] ret = null;

        try {
            ret = cipher.doFinal();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return null;
        }

        return ret;
    }

    public int decryptFinal(byte[] b, int off, int len, byte[] dest) {
        int ret = 0;

        try {
            ret = cipher.doFinal(b, off, len, dest);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return -1;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return -1;
        } catch (ShortBufferException e) {
            e.printStackTrace();
            return -1;
        }

        return ret;
    }
}
