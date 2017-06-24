package cn.edu.zhaoyang.easyrentcarforcar.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by ZhaoYang on 2016/12/24.
 * 消息完整性验证模块（产生SHA1值）
 */

public class SHA1Coder {
    /**
     * 产生数据的SHA1值
     *
     * @param input 需要产生SHA1值的字节数组
     * @return SHA1值，用字节数组表示
     */
    public static byte[] encodeBySHA1(byte[] input) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            //永远不会发生
            e.printStackTrace();
        }
        return messageDigest.digest(input);
    }
}
