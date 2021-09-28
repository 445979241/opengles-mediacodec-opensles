package com.darren.livepush;

/**
 * Created by hcDarren on 2019/7/7.
 */

public class LivePush {
    static {
        System.loadLibrary("live-push");
    }

    private String mLiveUrl;

    public LivePush(String liveUrl) {
        this.mLiveUrl = liveUrl;
    }

    private ConnectListener mConnectListener;

    public void setOnConnectListener(ConnectListener connectListener) {
        this.mConnectListener = connectListener;
    }

    public interface ConnectListener{
        void connectError(int errorCode, String errorMsg);
        void connectSuccess();
    }

    /**
     * 初始化連接
     */
    public void initConnect(){
        nInitConnect(mLiveUrl);
    }

    private native void nInitConnect(String liveUrl);

    // 連接的回調
    // called from jni
    private void onConnectError(int errorCode, String errorMsg){
        if(mConnectListener != null){
            mConnectListener.connectError(errorCode,errorMsg);
        }
    }

    // 連接的回調
    // called from jni
    private void onConnectSuccess(){
        if(mConnectListener != null){
            mConnectListener.connectSuccess();
        }
    }
}
