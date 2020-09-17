package com.sds.mobile.servicebrokerLib;

import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class StringCryptoUtil {
	
	public String encryptAES(String s, String key) throws Exception {
        String encrypted = null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
             
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
             
            encrypted = byteArrayToHex(cipher.doFinal(s.getBytes()));
            return encrypted;
        } catch (NoSuchAlgorithmException e) {
        	LogUtil.errorLog(e);
            throw e;
        } catch (Exception e) {
            LogUtil.errorLog(e);
            throw e;
        }
    }
     
    // key�� 16 ����Ʈ�� ���� �Ǿ�� �Ѵ�.
	public String decryptAES(String s, String key) throws Exception {
        String decrypted = null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
             
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            decrypted = new String(cipher.doFinal(hexToByteArray(s)));
            return decrypted;
        } catch (NoSuchAlgorithmException e) {
        	LogUtil.errorLog(e);
            throw e;
        } catch (Exception e) {
        	LogUtil.errorLog(e);
            throw e;
        }
    }
     
    private byte[] hexToByteArray(String s) {
        byte[] retValue = null;
        if (s != null && s.length() != 0) {
            retValue = new byte[s.length() / 2];
            for (int i = 0; i < retValue.length; i++) {
                retValue[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
            }
        }
        return retValue;
    }
     
    private String byteArrayToHex(byte buf[]) {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        for (int i = 0; i < buf.length; i++) {
            if (((int) buf[i] & 0xff) < 0x10) {
                strbuf.append("0");
            }
            strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
        }
         
        return strbuf.toString();
    }
}
