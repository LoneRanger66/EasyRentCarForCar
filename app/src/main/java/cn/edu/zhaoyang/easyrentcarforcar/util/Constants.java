package cn.edu.zhaoyang.easyrentcarforcar.util;

/**
 * Created by ZhaoYang on 2017/3/6.
 * 常量类，不允许被继承
 */

public final class Constants {
    // 时戳允许误差时间（单位：毫秒）
    public static final long TIME_OUT = 100 * 1000;
    // 服务器地址
    public static String SERVER_ADDRESS = "http://192.168.1.110:8080/EasyRentCarServer/servlet/";
    // 字符编码
    public static final String CHARSET_NAME = "UTF-8";
    // AES key用字符串保存时的编码
    public static final String CHARSET_NAME_AESKEY = "ISO-8859-1";

}
