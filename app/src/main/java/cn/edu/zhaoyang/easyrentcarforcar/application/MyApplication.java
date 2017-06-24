package cn.edu.zhaoyang.easyrentcarforcar.application;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

/**
 * Created by ZhaoYang on 2017/3/14.
 */

public class MyApplication extends Application {
    private String carId;
    private String key;

    public String getCarId() {
        return carId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //魅蓝 川A Y0001  送的手机 川A Y0002
        if (BluetoothAdapter.getDefaultAdapter().getAddress().toUpperCase().equals("00:27:15:08:43:31")) {
            carId = "川A Y0002";
        } else {
            carId = "川A Y0001";
        }
    }
}
