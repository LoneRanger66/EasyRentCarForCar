package cn.edu.zhaoyang.easyrentcarforcar.activities;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.zhaoyang.easyrentcarforcar.R;
import cn.edu.zhaoyang.easyrentcarforcar.util.Constants;

public class Setting extends AppCompatActivity {

    private TextView oldAddressTextView;
    private EditText newAddressEditText;
    private Button confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        init();
    }

    private void init() {
        initToolbar("设置");
        oldAddressTextView = (TextView) findViewById(R.id.oldAddressTextView);
        newAddressEditText = (EditText) findViewById(R.id.newAddressEditText);
        confirmButton = (Button) findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(new ConfirmButtonOnClickListener());
        refresh();
    }

    private void initToolbar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setTitle("");
        toolbar_title.setText(title);
        setSupportActionBar(toolbar);
    }

    //显示当前的服务器地址
    private void refresh() {
        String ip = Constants.SERVER_ADDRESS.substring(7, Constants.SERVER_ADDRESS.indexOf(':', 7));
        oldAddressTextView.setText(ip);
        newAddressEditText.setText("");
    }

    class ConfirmButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            String newAddress = newAddressEditText.getText().toString();
            //匹配合法ip地址的正则表达式
            String regex = "^([1-9]?\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.([1-9]?\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.([1-9]?\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.([1-9]?\\d|1\\d\\d|2[0-4]\\d|25[0-5]$)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(newAddress);
            if (matcher.matches()) {
                Constants.SERVER_ADDRESS = "http://" + newAddress + ":8080/EasyRentCarServer/servlet/";
                refresh();
                new AlertDialog.Builder(Setting.this).setTitle("提示").setMessage("服务器ip地址修改成功！").setPositiveButton("确定", null).show();
            } else {
                new AlertDialog.Builder(Setting.this).setTitle("提示").setMessage("请输入合法的ip地址！").setPositiveButton("确定", null).show();
            }
        }
    }

    /**
     * 点击返回按钮，回到上一级
     *
     * @param item 点击的项目
     * @return 点击的是toolbar的图标则返回true，否则交由上级处理
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
