package com.darren.livepush.mrecord;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.darren.livepush.R;
import com.darren.livepush.camera.widget.CameraFocusView;
import com.darren.livepush.camera.widget.RecordProgressButton;
import com.darren.livepush.mcamera.widget.MyCameraView;

public class RecordActivity extends AppCompatActivity {

    private MyCameraView mCameraView;
    private DefaultVideoRecorder mVideoRecorder;
    private CameraFocusView mFocusView;
    private RecordProgressButton mRecordButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mycamera_render);

        mCameraView = findViewById(R.id._mysurface_view);
        mFocusView = findViewById(R.id.camera_focus_view);
        mRecordButton = findViewById(R.id.record);
        mRecordButton.setMaxProgress(60 * 1000);//最大60秒

        mRecordButton.setOnRecordListener(new RecordProgressButton.RecordListener() {
            @Override
            public void onStart() {

                String outPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/live_pusher.mp4";
                String audioPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/01.mp4";

                mVideoRecorder = new DefaultVideoRecorder(RecordActivity.this,mCameraView.getEglContext(),mCameraView.getTextureId());
                mVideoRecorder.initVideoParams(audioPath,outPath,720,1280);
                mVideoRecorder.setRecordInfoListener(new BaseVideoRecorder.RecordInfoListener() {
                    @Override
                    public void onTime(long times) {
                        mRecordButton.setCurrentProgress((int)times);
                    }
                });
                mVideoRecorder.startRecord();
            }

            @Override
            public void onEnd() {
                mVideoRecorder.stopRecord();
            }
        });

        mCameraView.setOnFocusListener(new MyCameraView.FocusListener() {
            @Override
            public void beginFocus(int x, int y) {
                mFocusView.beginFocus(x, y);
            }

            @Override
            public void endFocus() {
                    mFocusView.endFocus(true);
            }
        }
        );
    }
}
