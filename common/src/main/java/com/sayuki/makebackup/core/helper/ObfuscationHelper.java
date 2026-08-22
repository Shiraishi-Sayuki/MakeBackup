/*
 * SayukiUtils
 * Copyright (C) 2026 Shiraishi Sayuki. <sayukishiraishi@gmail.com>
 */

 package com.sayuki.makebackup.core.helper;

import com.sayuki.makebackup.MakeBackup;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

// 難読化ヘルパークラス - 文字列の暗号化と復号をする
public class ObfuscationHelper {

    private static final byte[] keyValue = "lPXrMBtylzEUn422hzPqNN25".getBytes();

    // 暗号化する
    public static String encrypt(String valueToEnc) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance("AES");
        c.init(Cipher.ENCRYPT_MODE, key);

        byte[] encValue = c.doFinal(valueToEnc.getBytes());
        byte[] encryptedValue = Base64.getEncoder().encode(encValue);

        return new String(encryptedValue);
    }

    // 復号する
    public static String decrypt(String encryptedValue) {
        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);

            byte[] decodedValue = Base64.getDecoder().decode(encryptedValue.getBytes());
            byte[] decryptedVal = c.doFinal(decodedValue);

            return new String(decryptedVal);
        } catch (Exception e) {

            MakeBackup.getInstance().getLogManager().warn("Failed to decrypt String");
            MakeBackup.getInstance().getLogManager().warn(e);
            return null;
        }
    }

    // 鍵を生成する
    private static Key generateKey() {
        return new SecretKeySpec(keyValue, "AES");
    }
}
