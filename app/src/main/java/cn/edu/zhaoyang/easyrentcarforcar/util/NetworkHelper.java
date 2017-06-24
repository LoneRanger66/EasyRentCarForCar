package cn.edu.zhaoyang.easyrentcarforcar.util;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by ZhaoYang on 2017/3/6.
 * 本类提供网络连接方面的工具方法。
 */

public class NetworkHelper {
    private static final String TAG = "network";

    /**
     * 向要发送的JSON对象添加AESKey和时戳，并在末尾附加SHA1值，然后用RSA公钥加密、Base64编码后发送到指定地址。<br/>
     * 接收到信息后，先用Base64解码、AES解密后，验证SHA1值是否正确，然后取出时戳验证，通过则返回收到的JSON对象，否则返回null。
     *
     * @param sndJsonObj  要发送的JSON对象
     * @param servletName 服务器的servlet名字
     * @return 接收到的JSON对象；如果发生错误则返回NULL
     */
    public static JSONObject sendAndReceiveMessage(JSONObject sndJsonObj, String servletName) {
        try {
            //生成AES密钥
            byte[] AESKey = AESCoder.initKey();
            //将生成的AES密钥用Constants.CHARSET_NAME_AESKEY存储
            String AESKeyStr = new String(AESKey, Constants.CHARSET_NAME_AESKEY);
            //添加AES密钥
            sndJsonObj.put("AESKey", AESKeyStr);
            //添加时戳
            sndJsonObj.put("timestamp", String.valueOf(System.currentTimeMillis()));
            byte[] sndJsonByte = sndJsonObj.toString().getBytes(Constants.CHARSET_NAME);
            byte[] sndSHA1 = SHA1Coder.encodeBySHA1(sndJsonByte);
            byte[] sndByte = new byte[sndJsonByte.length + sndSHA1.length];
            // 合并jsonByte和SHA1两个字节数组到sndByte中
            System.arraycopy(sndJsonByte, 0, sndByte, 0, sndJsonByte.length);
            System.arraycopy(sndSHA1, 0, sndByte, sndJsonByte.length, sndSHA1.length);
            // 使用RSA加密数据
            byte[] sndEncryptByte = RSACoder.encryptByPublicKey(sndByte);
            // 使用Base64编码数据
            byte[] sndEncodeByte = Base64.encode(sndEncryptByte, Base64.NO_WRAP);
            //准备建立http连接
            URL url = new URL(Constants.SERVER_ADDRESS + servletName);
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            conn.setDoInput(true);
            conn.setDoOutput(true);
            try {
                conn.setRequestMethod("POST");
            } catch (ProtocolException e) {
                //永远不会发生
                e.printStackTrace();
            }
            conn.setRequestProperty("Charset", Constants.CHARSET_NAME);
            conn.setConnectTimeout(10000);
            OutputStream out;
            try {
                out = conn.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "得到输出流失败");
                return null;
            }
            try {
                out.write(sndEncodeByte);
            } catch (IOException e) {
                Log.d(TAG, "发送信息失败");
                return null;
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream in;
            try {
                in = conn.getInputStream();
            } catch (IOException e) {
                Log.d(TAG, "获取输入流失败");
                return null;
            }
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            try {
                while ((len = in.read(buffer)) != -1) {
                    bout.write(buffer, 0, len);
                }
            } catch (IOException e) {
                Log.d(TAG, "输入流读取失败");
                return null;
            }
            byte[] rcvEncodeByte = bout.toByteArray();
            try {
                bout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] rcvEncryptByte = Base64.decode(rcvEncodeByte, Base64.NO_WRAP);
            byte[] rcvByte = AESCoder.decrypt(rcvEncryptByte, AESKey);
            // 截取解密后字节数组除后二十个字节以外的字节，即JSON对象的byte数组表示
            byte[] rcvJsonByte = Arrays.copyOfRange(rcvByte, 0, rcvByte.length - 20);
            // 截取解密后字节数组的后二十个字节，即SHA1值
            byte[] rcvSHA1 = Arrays.copyOfRange(rcvByte, rcvByte.length - 20, rcvByte.length);
            // 对jsonByte进行SHA1值计算
            byte[] calculatedSHA1 = SHA1Coder.encodeBySHA1(rcvJsonByte);
            // 比较接收的和计算的SHA1值是否相等
            if (!Arrays.equals(rcvSHA1, calculatedSHA1)) {
                System.out.println("SHA1验证未通过！");
                return null;
            }
            JSONObject rcvJsonObj = new JSONObject(new String(rcvJsonByte, Constants.CHARSET_NAME));
            // 验证时戳
            if (Math.abs(System.currentTimeMillis() - Long.parseLong(rcvJsonObj.getString("timestamp"))) > Constants.TIME_OUT) {
                System.out.println("时戳验证未通过!");
                return null;
            }
            return rcvJsonObj;
        } catch (JSONException e) {
            Log.d("ERROR", "JSON格式错误！");
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            //永远不会发生
            e.printStackTrace();
        }
        return null;
    }
}
