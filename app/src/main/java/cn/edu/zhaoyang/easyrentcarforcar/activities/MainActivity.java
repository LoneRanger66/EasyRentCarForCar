package cn.edu.zhaoyang.easyrentcarforcar.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.zhaoyang.easyrentcarforcar.R;
import cn.edu.zhaoyang.easyrentcarforcar.application.MyApplication;
import cn.edu.zhaoyang.easyrentcarforcar.util.BluetoothHelper;
import cn.edu.zhaoyang.easyrentcarforcar.util.NetworkHelper;

public class MainActivity extends AppCompatActivity {
    private BluetoothReceiver bluetoothReceiver;
    private TextView bluetoothStatusTextView;
    private LinearLayout carStatusLL;
    private TextView carStatusTextView;
    private LinearLayout linearLayout;
    private ImageView orderImageView;
    private TextView orderTextView;
    private LinearLayout setting;

    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private OutputStream out;
    private InputStream in;
    private boolean userConnectFlag = false;
    private boolean serverThreadFlag = true;
    private BluetoothAdapter bluetoothAdapter;

    private static final String BLUETOOTH_STATE_ON = "已开启";
    private static final String BLUETOOTH_STATE_OFF = "未开启";
    private static final String WAIT_CONNECT = "等待用户连接";
    private static final String CONNECTED = "用户已连接";
    private static final int[] orderIV = {R.drawable.car, R.drawable.lock, R.drawable.unlock, R.drawable.open_trunk};
    private static final String[] orderTV = {"无效命令", "锁车", "开锁", "打开后备箱"};
    private static final String TAG = "CarBluetooth";
    private static final UUID MY_UUID = UUID.fromString("b1d0699c-b38e-4b73-88ab-f2991e714354");
    private Handler handler = new Handler();
    private ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initLocation();
    }

    private void init() {
        bluetoothStatusTextView = (TextView) findViewById(R.id.bluetoothStatusTextView);
        carStatusLL = (LinearLayout) findViewById(R.id.carStatusLL);
        carStatusTextView = (TextView) findViewById(R.id.carStatusTextView);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        orderImageView = (ImageView) findViewById(R.id.orderImageView);
        orderTextView = (TextView) findViewById(R.id.orderTextView);
        setting = (LinearLayout) findViewById(R.id.setting);
        setting.setOnClickListener(new SettingOnClickListener());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initToolbar("易租车汽车版");
        bluetoothReceiver = new BluetoothReceiver();
        IntentFilter intentFilter = new IntentFilter();
        //蓝牙状态改变监听
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(bluetoothReceiver, intentFilter);
        serverThreadFlag = true;
        startBluetooth();
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    //开启蓝牙设备
    private void startBluetooth() {
        //因为汽车端必须支持蓝牙功能，所以永远不会发生不支持蓝牙的情况
        if (bluetoothAdapter == null) {
            Log.d(TAG, "此设备不支持蓝牙");
            return;
        }
        Log.d(TAG, "此设备支持蓝牙");
        switch (bluetoothAdapter.getState()) {
            //如果蓝牙没有打开，打开蓝牙
            case BluetoothAdapter.STATE_OFF:
                bluetoothStatusTextView.setText(BLUETOOTH_STATE_OFF);
                carStatusLL.setVisibility(View.INVISIBLE);
                bluetoothAdapter.enable();
                Log.d(TAG, "正在打开蓝牙");
                break;
            //如果蓝牙已开启，设置可检测性为永远可见，取消搜索进程，等待客户端连接
            case BluetoothAdapter.STATE_ON:
                //设置可检测性为永远可见
                Log.d(TAG, "设置蓝牙可见性");
                bluetoothStatusTextView.setText(BLUETOOTH_STATE_ON);
                carStatusLL.setVisibility(View.VISIBLE);
                BluetoothHelper.setScanMode(bluetoothAdapter);
                //防止其他程序开启了蓝牙搜索进程
                bluetoothAdapter.cancelDiscovery();
                threadPool.execute(new ServerRunnable());
                break;
            //其他的两种情况，等待蓝牙状态改变，由广播状态改变BluetoothAdapter.ACTION_STATE_CHANGED来处理
            default:
                break;
        }
    }

    /**
     * 汽车端运行的服务器线程，用来创建蓝牙服务器并响应客户端的请求
     */
    class ServerRunnable implements Runnable {

        @Override
        public void run() {
            while (serverThreadFlag) {
                if (serverSocket == null) {
                    try {
                        serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("server", MY_UUID);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                carStatusTextView.setText(WAIT_CONNECT);
                            }
                        });
                    } catch (IOException e) {
                        Log.d(TAG, "serverSocket建立失败");
                        return;
                    }
                }
                Log.d(TAG, "正在等待客户端连接...");
                if (socket == null) {
                    try {
                        socket = serverSocket.accept();
                        userConnectFlag = true;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                carStatusTextView.setText(CONNECTED);
                                linearLayout.setVisibility(View.VISIBLE);
                            }
                        });
                        Log.d(TAG, "客户端已连接");
                    } catch (IOException e) {
                        Log.d(TAG, "线程关闭");
                        try {
                            serverSocket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        serverSocket = null;
                        return;
                    }
                }
                // 防止serverSocket关闭的时候，socket为空
                if (socket == null) {
                    return;
                }
                //向服务器请求虚拟钥匙
                boolean status = requestKeyToServer();
                if (!status) {
                    Log.d(TAG, "汽车请求虚拟钥匙失败！");
                    return;
                }
                //获取socket的输入输出流
                try {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                MyApplication app = (MyApplication) getApplication();
                //读取客户端发来的数据，并作出响应
                while (userConnectFlag) {
                    int message = BluetoothHelper.receiveAndSendMessage(app.getKey(), in, out);
                    //汽车端处理客户端发来的命令
                    if (message >= 0 && message <= 3) {
                        final int finalMessage = message;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                orderTextView.setText(orderTV[finalMessage]);
                                orderImageView.setImageResource(orderIV[finalMessage]);
                            }
                        });
                    }
                    if (message == 1) {
                        userConnectFlag = false;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                carStatusTextView.setText(WAIT_CONNECT);
                                orderTextView.setText("");
                                orderImageView.setImageResource(R.drawable.lock);
                                linearLayout.setVisibility(View.GONE);
                            }
                        });
                        try {
                            in.close();
                            out.close();
                            socket.close();
                            socket = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //用户结束用车之后，自动定位汽车位置
                        startLocation();
                    }
                }
            }
        }
    }

    /*
    向服务器请求虚拟钥匙，并设置虚拟钥匙
     */
    private boolean requestKeyToServer() {
        MyApplication app = (MyApplication) getApplication();
        String carId = app.getCarId();
        String servletName = "DistributeKeyForCar";
        JSONObject sndJsonObj = new JSONObject();
        try {
            sndJsonObj.put("carId", carId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
        if (rcvJsonObj == null) {
            Log.d(TAG, "接收到的JSON格式错误！");
            return false;
        }
        boolean status = false;
        try {
            status = rcvJsonObj.getBoolean("status");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (!status) {
            Log.d(TAG, "请求密钥失败！");
            return false;
        }
        try {
            String AESKeyStr = rcvJsonObj.getString("key");
            app.setKey(AESKeyStr);
        } catch (JSONException e) {
            Log.d(TAG, "接收到的JSON格式错误！");
            return false;
        }
        return true;
    }

    /**
     * 蓝牙监听接收器
     * 如果收到了蓝牙配对的信息，取消确认配对框
     * 如果收到了蓝牙状态的改变信息，则检查蓝牙是否开启，开启的话就设置蓝牙可见，并取消搜索进程
     */
    class BluetoothReceiver extends BroadcastReceiver {
        private static final String TAG = "BluetoothReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //如果收到了蓝牙状态的改变信息，则检查蓝牙是否开启，开启的话就设置蓝牙可见，并取消搜索进程
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    //设置可检测性为永远可见
                    Log.d(TAG, "设置蓝牙可见性");
                    BluetoothHelper.setScanMode(bluetoothAdapter);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            bluetoothStatusTextView.setText(BLUETOOTH_STATE_ON);
                            carStatusLL.setVisibility(View.VISIBLE);
                        }
                    });
                    //防止其他程序开启了蓝牙搜索进程
                    bluetoothAdapter.cancelDiscovery();
                    threadPool.execute(new ServerRunnable());
                } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            bluetoothStatusTextView.setText(BLUETOOTH_STATE_OFF);
                            carStatusLL.setVisibility(View.INVISIBLE);
                        }
                    });
                    bluetoothAdapter.enable();
                }
            }
        }
    }

    class SettingOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, Setting.class);
            startActivity(intent);
        }
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        //初始化client
        locationClient = new AMapLocationClient(this.getApplicationContext());
        locationOption = getDefaultOption();
        //设置定位参数
        locationClient.setLocationOption(locationOption);
        // 设置定位监听
        locationClient.setLocationListener(locationListener);
    }

    /**
     * 设置默认的定位参数
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setHttpTimeOut(10000);//设置网络请求超时时间为10秒
        mOption.setNeedAddress(false);//设置不返回逆地理地址信息
        mOption.setOnceLocation(true);//设置单次定位
        mOption.setOnceLocationLatest(true);//设置等待wifi刷新
        mOption.setSensorEnable(false);//设置不使用传感器
        mOption.setWifiScan(true); //设置开启wifi扫描
        mOption.setLocationCacheEnable(true); //设置使用缓存定位
        return mOption;
    }

    /**
     * 定位监听
     */
    AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(final AMapLocation location) {
            if (location != null) {
                //errCode等于0代表定位成功，其他的为定位失败
                if (location.getErrorCode() == 0) {
                    final double longitude = location.getLongitude();
                    final double latitude = location.getLatitude();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            MyApplication app = (MyApplication) getApplication();
                            String carId = app.getCarId();
                            String servletName = "CarPosition";
                            JSONObject sndJsonObj = new JSONObject();
                            try {
                                sndJsonObj.put("carId", carId);
                                sndJsonObj.put("longitude", longitude);
                                sndJsonObj.put("latitude", latitude);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            JSONObject rcvJsonObj = NetworkHelper.sendAndReceiveMessage(sndJsonObj, servletName);
                            if (rcvJsonObj == null) {
                                Log.d(TAG, "接收到的JSON格式错误！");
                                return;
                            }
                            boolean status = false;
                            try {
                                status = rcvJsonObj.getBoolean("status");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (!status) {
                                Log.d(TAG, "更新汽车位置失败！");
                                return;
                            }
                            Log.d(TAG, "经度:" + longitude + " 纬度: " + latitude);
                        }
                    }).start();
                } else {
                    Log.d(TAG, "定位失败");
                }
            } else {
                Log.d(TAG, "定位失败");
            }
        }
    };

    /**
     * 开始定位
     */
    private void startLocation() {
        // 设置定位参数
        locationClient.setLocationOption(locationOption);
        // 启动定位
        locationClient.startLocation();
    }

    /**
     * 停止定位
     */
    private void stopLocation() {
        // 停止定位
        locationClient.stopLocation();
    }

    /**
     * 销毁定位
     */
    private void destroyLocation() {
        if (locationClient != null) {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            locationClient.onDestroy();
            locationClient = null;
            locationOption = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消注册的BluetoothReceiver
        this.unregisterReceiver(bluetoothReceiver);
        destroyLocation();
        threadPool.shutdown();
        //设置线程停止标志
        serverThreadFlag = false;
        //设置用户断开连接标志
        userConnectFlag = false;
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(MainActivity.this).setTitle("提示").setMessage("确定退出易租车汽车版？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(0);
                }
            }).setNegativeButton("取消", null).show();
        }
        return super.onKeyDown(keyCode, event);
    }
}
