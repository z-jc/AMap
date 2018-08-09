package com.zjc.amap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.fence.GeoFence;
import com.amap.api.fence.GeoFenceClient;
import com.amap.api.fence.GeoFenceListener;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.CoordinateConverter;
import com.amap.api.location.DPoint;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //定义接收广播的action字符串
    public static final String GEOFENCE_BROADCAST_ACTION = "com.location.apis.geofencedemo.broadcast";
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationClientOption;
    private GeoFenceClient mGeoFenceClient;
    private String address;
    private boolean isInFence; // 是否进入围栏区域
    private boolean isInit;
    private TextView mTvLongitudeAndLatitude;
    private EditText mEdLong;
    private EditText mEdLati;
    private EditText mEdKeyword;
    private EditText mEdRedius;
    private Button mBtnAddpen;
    private TextView mTvLog;

    private StringBuilder stringBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        init();
        initGeoFenceReceiver();
    }

    /**
     * 增加地理围栏
     */
    private void addGeoFence(double lati, double lon, int center, String address) {
        /**
         * 创建一个中心点坐标
         * */
        DPoint point = new DPoint();
        point.setLatitude(lati);
        point.setLongitude(lon);
        /**
         * 实例化地理围栏客户端
         * */
        mGeoFenceClient = new GeoFenceClient(getApplicationContext());
        mGeoFenceClient.setActivateAction(GeoFenceClient.GEOFENCE_IN | GeoFenceClient.GEOFENCE_OUT |
                GeoFenceClient.GEOFENCE_STAYED);//设置希望侦测的围栏触发行为，默认只侦测用户进入围栏的行为
        mGeoFenceClient.addGeoFence(point, center, address);//创建围栏
        mGeoFenceClient.setGeoFenceListener(mGeoFenceListener);
        mGeoFenceClient.createPendingIntent(GEOFENCE_BROADCAST_ACTION);
    }

    /**
     * 初始化围栏接收
     */
    private void initGeoFenceReceiver() {
        IntentFilter fliter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        fliter.addAction(GEOFENCE_BROADCAST_ACTION);
        registerReceiver(mGeoFenceReceiver, fliter);
    }

    /**
     * 初始化定位
     */
    private void init() {
        if (!isInit) {
            isInit = true;
            mLocationClient = new AMapLocationClient(getApplicationContext());
            mLocationClient.setLocationListener(mLocationListener);
            mLocationClientOption = new AMapLocationClientOption();
            mLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationClient.setLocationOption(mLocationClientOption);
        }
        mLocationClient.startLocation();
    }

    /**
     * 定位信息
     */
    private AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    //可在其中解析amapLocation获取相应内容。
                    double latitude = aMapLocation.getLatitude();//获取纬度
                    double longitude = aMapLocation.getLongitude();//获取经度
                    if (!isInFence) {
                        // 不在地理围栏范围内，则使用获取到的公共地址
                        address = aMapLocation.getAddress();//获取地址
                    }
                    mLocationClient.stopLocation();
                    Log.e("TAG", "latitude:" + latitude + ",longitude:" + longitude + "," + "address:" + address);
                    stringBuilder.append("当前位置 经度:" + longitude + ",维度:" + latitude + "\n");
                    mTvLongitudeAndLatitude.setText("当前位置 经度:" + longitude + ",维度:" + latitude);
                    CoordinateConverter converter = new CoordinateConverter(MainActivity.this);
                    DPoint start = new DPoint();
                    start.setLatitude(latitude);
                    start.setLongitude(longitude);
                    DPoint end = new DPoint();
                    end.setLatitude(latitude);
                    end.setLongitude(longitude);
                    float distance = converter.calculateLineDistance(start, end);
                    Log.e("TAG", "distance:" + distance);
                } else {
                    Log.e("TAG", "location Error, ErrCode:"
                            + aMapLocation.getErrorCode() + ", errInfo:"
                            + aMapLocation.getErrorInfo());
                    stringBuilder.append("定位失败\n");
                    mTvLongitudeAndLatitude.setText("定位失败!");
                }
                mTvLog.setText(stringBuilder.toString());
            }
        }
    };

    /**
     * 创建围栏回调
     */
    GeoFenceListener mGeoFenceListener = new GeoFenceListener() {
        @Override
        public void onGeoFenceCreateFinished(List<GeoFence> list, int errorCode, String s) {
            if (errorCode == GeoFence.ADDGEOFENCE_SUCCESS) {//判断围栏是否创建成功
                Log.e("TAG", "添加围栏成功!!");
                stringBuilder.append("添加围栏成功!!!\n");
            } else {
                Log.e("TAG", "添加围栏失败!!");
                stringBuilder.append("添加围栏失败!!!\n");
            }
            mTvLog.setText(stringBuilder.toString());
        }
    };

    /**
     * 清除所有围栏
     */
    @Override
    protected void onDestroy() {
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
            mGeoFenceClient.removeGeoFence();
        }
        super.onDestroy();
    }

    private BroadcastReceiver mGeoFenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GEOFENCE_BROADCAST_ACTION)) {
                Log.e("TAG", "进入地理围栏");
                stringBuilder.append("进入地理围栏\n");
                //进入地理围栏
                isInFence = true;
                //获取Bundle
                Bundle bundle = intent.getExtras();
                // 在地理围栏范围内，则使用地理围栏地址
                address = bundle.getString(GeoFence.BUNDLE_KEY_CUSTOMID);
            } else {
                // 不在围栏
                isInFence = false;
                Log.e("TAG", "不在地理围栏");
                stringBuilder.append("离开地理围栏\n");
            }
            mTvLog.setText(stringBuilder.toString());
        }
    };

    private void initView() {
        mTvLongitudeAndLatitude = (TextView) findViewById(R.id.tv_longitude_and_latitude);
        mEdLong = (EditText) findViewById(R.id.ed_long);
        mEdLati = (EditText) findViewById(R.id.ed_lati);
        mEdKeyword = (EditText) findViewById(R.id.ed_keyword);
        mEdRedius = (EditText) findViewById(R.id.ed_redius);
        mBtnAddpen = (Button) findViewById(R.id.btn_addpen);
        mTvLog = (TextView) findViewById(R.id.tv_log);

        mBtnAddpen.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_addpen:
                submit();
                break;
        }
    }

    private void submit() {
        // validate
        String lon = mEdLong.getText().toString().trim();
        if (TextUtils.isEmpty(lon)) {
            Toast.makeText(this, "中心点经度", Toast.LENGTH_SHORT).show();
            return;
        }

        String lati = mEdLati.getText().toString().trim();
        if (TextUtils.isEmpty(lati)) {
            Toast.makeText(this, "中心点维度", Toast.LENGTH_SHORT).show();
            return;
        }

        String keyword = mEdKeyword.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, "关键字", Toast.LENGTH_SHORT).show();
            return;
        }

        String redius = mEdRedius.getText().toString().trim();
        if (TextUtils.isEmpty(redius)) {
            Toast.makeText(this, "围栏半径", Toast.LENGTH_SHORT).show();
            return;
        }
        addGeoFence(Double.valueOf(lati), Double.valueOf(lon), Integer.parseInt(redius), keyword);
    }
}