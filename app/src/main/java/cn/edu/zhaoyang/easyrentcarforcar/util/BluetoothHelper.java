package cn.edu.zhaoyang.easyrentcarforcar.util;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by ZhaoYang on 2017/3/8.
 * 本类使用反射取得蓝牙API里面隐藏的方法
 */

public class BluetoothHelper {
    private static final String TAG = "bluetooth";

    /**
     * 设置蓝牙可检测性为永远可见
     *
     * @param bluetoothAdapter 默认蓝牙设备
     * @return 设置成功返回true，否则返回false
     */
    public static boolean setScanMode(BluetoothAdapter bluetoothAdapter) {
        //设置可检测性
        Method setScanMode = null;
        try {
            setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        try {
            return (boolean) setScanMode.invoke(bluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 300);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 接收到信息后，用AES解密，验证SHA1值是否正确，然后取出时戳验证，然后得到message信息。<br/>
     * 然后生成JSON对象并添加相应信息和时戳，并在末尾附加SHA1值，然后用虚拟钥匙加密后发送到目标蓝牙设备。<br/>
     *
     * @param keyStr AES虚拟钥匙
     * @param in     socket输入流
     * @param out    socket输出流
     * @return 返回接收到的message信息，若出错则返回0
     */
    public static int receiveAndSendMessage(String keyStr, InputStream in, OutputStream out) {
        int message = 0;
        try {
            byte[] AESKey = keyStr.getBytes(Constants.CHARSET_NAME_AESKEY);
            int len = 0;
            byte[] buffer = null;
            //根据输入流的大小接收数据
            try {
                while (len == 0) {
                    len = in.available();
                }
                buffer = new byte[len];
                in.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] rcvByte = AESCoder.decrypt(buffer, AESKey);
            // 截取解密后字节数组除后二十个字节以外的字节，即JSON对象的byte数组表示
            byte[] rcvJsonByte = Arrays.copyOfRange(rcvByte, 0, rcvByte.length - 20);
            // 截取解密后字节数组的后二十个字节，即SHA1值
            byte[] rcvSHA1 = Arrays.copyOfRange(rcvByte, rcvByte.length - 20, rcvByte.length);
            // 对jsonByte进行SHA1值计算
            byte[] calculatedSHA1 = SHA1Coder.encodeBySHA1(rcvJsonByte);
            // 比较接收的和计算的SHA1值是否相等
            if (!Arrays.equals(rcvSHA1, calculatedSHA1)) {
                System.out.println("SHA1验证未通过！");
                return 0;
            }
            JSONObject rcvJsonObj = new JSONObject(new String(rcvJsonByte, Constants.CHARSET_NAME));
            // 验证时戳
            if (Math.abs(System.currentTimeMillis() - Long.parseLong(rcvJsonObj.getString("timestamp"))) > Constants.TIME_OUT) {
                System.out.println("时戳验证未通过!");
                return 0;
            }
            //得到JSON里的信息
            message = rcvJsonObj.getInt("message");
            JSONObject sndJsonObj = new JSONObject();
            //添加信息
            sndJsonObj.put("status", true);
            //添加时戳
            sndJsonObj.put("timestamp", String.valueOf(System.currentTimeMillis()));
            byte[] sndJsonByte = sndJsonObj.toString().getBytes(Constants.CHARSET_NAME);
            byte[] sndSHA1 = SHA1Coder.encodeBySHA1(sndJsonByte);
            byte[] sndByte = new byte[sndJsonByte.length + sndSHA1.length];
            // 合并jsonByte和SHA1两个字节数组到sndByte中
            System.arraycopy(sndJsonByte, 0, sndByte, 0, sndJsonByte.length);
            System.arraycopy(sndSHA1, 0, sndByte, sndJsonByte.length, sndSHA1.length);
            // 使用AES密钥加密数据
            byte[] sndEncryptByte = AESCoder.encrypt(sndByte, AESKey);
            try {
                out.write(sndEncryptByte);
            } catch (IOException e) {
                Log.d(TAG, "写数据失败");
                return 0;
            }
        } catch (UnsupportedEncodingException e) {
            //永远不会发生
            e.printStackTrace();
        } catch (JSONException e) {
            Log.d("ERROR", "JSON格式错误！");
        }
        return message;
    }
}
