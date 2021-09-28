package com.darren.livepush.picture;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.darren.livepush.R;

public class PictureActivity extends AppCompatActivity {

    PictureView pictureView;
    @Override
    protected void onResume() {
        super.onResume();
        pictureView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pictureView.onPause();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_picture);
        pictureView = findViewById(R.id.v_picture);
    }
}
