package com.darren.livepush.mrecord;

import android.content.Context;

import com.darren.livepush.camera.CameraRender;

import javax.microedition.khronos.egl.EGLContext;

public class DefaultVideoRecorder extends BaseVideoRecorder{

    public DefaultVideoRecorder(Context context, EGLContext eglContext, int textureId){
        super(context,eglContext);
        setRender(new RecorderRenderer(context,textureId));
    }
}
