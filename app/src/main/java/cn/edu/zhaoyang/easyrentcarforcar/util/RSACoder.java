package cn.edu.zhaoyang.easyrentcarforcar.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by ZhaoYang on 2016/12/19.
 * RSA解密模块
 */
public class RSACoder {
    //客户端的RSA公钥
    private static final byte[] PUBLIC_KEY = {48, 92, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, 75, 0,
            48, 72, 2, 65, 0, -111, -79, 67, -85, 108, 3, -32, -93, 61, -66, -28, -13, -119, -44, 16, -40, -33, 39, -35,
            -38, -103, -26, -94, 122, -83, 87, -2, 14, 99, 102, -6, 55, 114, -12, -82, 50, 48, -45, -113, -75, -68, 121,
            12, 81, -108, -128, -109, 11, 25, 67, 124, -112, 16, -128, 111, 60, 118, -32, 108, -19, 4, -77, 79, 115, 2,
            3, 1, 0, 1};

    //512位密钥最大解密512/8-11=53位数据
    private static final int MAX_ENCRYPT_BLOCK = 53;

    public static byte[] encryptByPublicKey(byte[] input) {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(PUBLIC_KEY);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        PublicKey publicKey = null;
        try {
            publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        //处理数据多余最大数据块的情况
        int length = input.length;
        int offset = 0;
        byte[] buffer = null;
        int i = 0;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // 有数据需要处理作为循环判断的条件
        while (length - offset > 0) {
            if (length - offset > MAX_ENCRYPT_BLOCK) {
                try {
                    buffer = cipher.doFinal(input, offset, MAX_ENCRYPT_BLOCK);
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    buffer = cipher.doFinal(input, offset, length - offset);
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }
            }
            try {
                byteArrayOutputStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
            offset = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] result = byteArrayOutputStream.toByteArray();
        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
