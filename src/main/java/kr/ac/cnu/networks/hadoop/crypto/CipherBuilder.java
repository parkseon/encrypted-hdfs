package kr.ac.cnu.networks.hadoop.crypto;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class CipherBuilder {
    public static final String CIPHER_AES_ENC = "aes-enc";
    public static final String CIPHER_AES_DEC = "aes-dec";
    private static final String aesParam = "AES/ECB/NoPadding";
    private static final KeyStore keyStore = new KeyStore();

    public static final Cipher getCipherInstance(String type) {
        try {
            if (type == CIPHER_AES_ENC)
                return getAesEncInstance();
            else if (type == CIPHER_AES_DEC)
                return getAesDecInstance();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static final Cipher getAesEncInstance() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Cipher c = Cipher.getInstance(aesParam);
        c.init(Cipher.ENCRYPT_MODE, keyStore.getAesKey());
        return c;
    }

    private static final Cipher getAesDecInstance() throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher c = Cipher.getInstance(aesParam);
        c.init(Cipher.DECRYPT_MODE, keyStore.getAesKey());
        return c;
    }
}
