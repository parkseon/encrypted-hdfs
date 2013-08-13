package kr.ac.cnu.networks;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

public class AesEncryptor {
    private SecretKeySpec sKeySpec = null;
    private AlgorithmParameterSpec paramSpec = null;
    private Cipher cipher = null;

    public AesEncryptor(String hexKey) {

        final byte[] symKeyData = DatatypeConverter.parseHexBinary(hexKey);
        this.sKeySpec = new SecretKeySpec(symKeyData, "AES");
        try {
            this.cipher = Cipher.getInstance("AES/ECB/NoPadding");
            this.cipher.init(Cipher.ENCRYPT_MODE, this.sKeySpec);
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

    public byte[] encrypt(byte[] b) {
        return cipher.update(b);
    }

    public byte[] encrypt(byte[] b, int offs, int len) {
        return cipher.update(b, offs, len);
    }

    public byte[] encryptFinal() {
        byte[] ret = null;

        try {
            ret = cipher.doFinal();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (BadPaddingException e) {
            e.printStackTrace();
            System.exit(0);
        }

        return ret;
    }
}
