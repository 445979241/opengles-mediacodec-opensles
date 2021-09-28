package com.darren.livepush;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private LivePush mLivePush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLivePush = new LivePush("rtmp://148.70.96.230/myapp/mystream");
        mLivePush.setOnConnectListener(new LivePush.ConnectListener() {
            @Override
            public void connectError(int errorCode, String errorMsg) {
                Log.e("TAG", "errorCode:" + errorCode);
                Log.e("TAG", "errorMsg:" + errorMsg);
            }

            @Override
            public void connectSuccess() {
                Log.e("TAG", "connectSuccess:可以推流了");
            }
        });
        mLivePush.initConnect();
    }
}
