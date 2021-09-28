package com.darren.livepush.mrecord;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLSurfaceView;
import android.view.Surface;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.CyclicBarrier;

import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public abstract class BaseVideoRecorder {
    private Context mContext;
    private EGLContext mEglContext;

    private WeakReference<BaseVideoRecorder> videoRecorderReference = new WeakReference<>(this);

    private Surface mSurface;
    private GLSurfaceView.Renderer mRender;

    VideoRenderThread videoRenderThread;
    VideoEncoderThread videoEncoderThread;
    MediaCodec mMediaCodec;
    MediaMuxer mMediaMuxer;

    CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

    public BaseVideoRecorder(Context context,EGLContext eglContext) {
        this.mContext = context;
        this.mEglContext = eglContext;
    }

    public void setRender(GLSurfaceView.Renderer mRender) {
        this.mRender = mRender;
        videoRenderThread = new VideoRenderThread(videoRecorderReference);
    }

    public void initVideoParams(String autioPath, String outPath, int videoWidth, int videoHeight) {

        try {
            mMediaMuxer = new MediaMuxer(outPath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoRenderThread.setSize(videoWidth,videoHeight);

            initVideoCodec(videoWidth,videoHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

private void initVideoCodec(int width, int height) {
    try {
        // https://developer.android.google.cn/reference/android/media/MediaCodec mediacodec官方介绍
        // 比方MediaCodec的几种状态
        // avc即h264编码
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,width,height);
        // 设置颜色格式
        // 本地原始视频格式（native raw video format）：这种格式通过COLOR_FormatSurface标记，并可以与输入或输出Surface一起使用
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 设置码率，通常码率越高，视频越清晰，但是对应的视频也越大
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,width * height * 4);

        // 设置帧率 三星s21手机camera预览时，支持的帧率为10-30
        // 通常这个值越高，视频会显得越流畅，一般默认设置成30，你最低可以设置成24，不要低于这个值，低于24会明显卡顿
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,30);
        // 设置 I 帧间隔的时间
        // 通常的方案是设置为 1s，对于图片电影等等特殊情况，这里可以设置为 0，表示希望每一帧都是 KeyFrame
        // IFRAME_INTERVAL是指的帧间隔，这是个很有意思的值，它指的是，关键帧的间隔时间。通常情况下，你设置成多少问题都不大。
        // 比如你设置成10，那就是10秒一个关键帧。但是，如果你有需求要做视频的预览，那你最好设置成1
        // 因为如果你设置成10，那你会发现，10秒内的预览都是一个截图
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,5);

        // 创建编码器
        // https://www.codercto.com/a/41316.html MediaCodec 退坑指南
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mMediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

        // 相机的像素数据绘制到该 surface 上面
        mSurface = mMediaCodec.createInputSurface();

        videoEncoderThread = new VideoEncoderThread(videoRecorderReference);
    } catch (Exception e) {
        e.printStackTrace();
    }

}

    public void startRecord(){
        videoRenderThread.start();
        videoEncoderThread.start();
    }

    public void stopRecord(){
        videoRenderThread.requestExit();
        videoEncoderThread.requestExit();
    }


    RecordInfoListener recordInfoListener;
    public void setRecordInfoListener(RecordInfoListener recordInfoListener) {
        this.recordInfoListener = recordInfoListener;
    }
    public interface RecordInfoListener {
        void onTime(long times);
    }


//    /**
//     * 释放资源
//     */
//    private void release() {
//
//        if (mMediaMuxer != null){
//            mMediaMuxer.stop();
//            mMediaMuxer.release();
//            mMediaMuxer = null;
//        }
//        if (mMediaCodec != null){
//            mMediaCodec.stop();
//            mMediaCodec.release();
//            mMediaCodec = null;
//        }
//    }



    private class VideoEncoderThread extends Thread{

        WeakReference<BaseVideoRecorder> videoRecorderWf;
        private boolean shouldExit =false;

        private MediaCodec mMediaCodec;
        private MediaMuxer mMediaMuxer;
        MediaCodec.BufferInfo bufferInfo;

        long videoPts = 0;

        /**
         * 视频轨道
         */
        private int mVideoTrackIndex = -1;

        public VideoEncoderThread(WeakReference<BaseVideoRecorder> videoRecorderWf){
            this.videoRecorderWf = videoRecorderWf;
            this.mMediaCodec = videoRecorderWf.get().mMediaCodec;
            this.mMediaMuxer = videoRecorderWf.get().mMediaMuxer;

            bufferInfo = new MediaCodec.BufferInfo();
        }

        @Override
        public void run() {
            try {
                mMediaCodec.start();

                while (true){
                    try {
                        if(shouldExit){
                            onDestroy();
                            return;
                        }

                        // 返回有效数据填充的输出缓冲区的索引
                        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,0);
                        if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                            // 将mMediaCodec的指定的格式的数据轨道，设置到mMediaMuxer上
                            mVideoTrackIndex = mMediaMuxer.addTrack(mMediaCodec.getOutputFormat());
                            mMediaMuxer.start();
                        }else{

                            while (outputBufferIndex >= 0){

                                // 获取数据
                                ByteBuffer outBuffer = mMediaCodec.getOutputBuffers()[outputBufferIndex];

                                outBuffer.position(bufferInfo.offset);
                                outBuffer.limit(bufferInfo.offset+bufferInfo.size);

                                // 修改视频的 pts,基准时间戳
                                if(videoPts ==0)
                                    videoPts = bufferInfo.presentationTimeUs;
                                bufferInfo.presentationTimeUs -= videoPts;

//                                System.out.println(bufferInfo.presentationTimeUs);
                                // 写入数据
                                System.out.println("writeSampleData:"+System.currentTimeMillis());
                                mMediaMuxer.writeSampleData(mVideoTrackIndex,outBuffer,bufferInfo);

                                if(videoRecorderWf.get().recordInfoListener != null){
                                    // us，需要除以1000转为 ms
                                    videoRecorderWf.get().recordInfoListener.onTime( bufferInfo.presentationTimeUs / 1000);
                                }

                                // 释放 outBuffer
                                mMediaCodec.releaseOutputBuffer(outputBufferIndex,false);
                                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,0);
                            }
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onDestroy();
            }
        }

        private void onDestroy() {
//            videoRecorderWf.get().release();
            if (mMediaCodec != null){
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
            if (mMediaMuxer != null){
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMediaMuxer = null;
            }
        }

        public void requestExit() {
            shouldExit = true;
        }
    }

    private long drawTime;

    private class VideoRenderThread extends Thread{

        private WeakReference<BaseVideoRecorder> mVideoRecorderWf;

        boolean mShouldExit;
        com.darren.livepush.opengl.EglHelper mEglHelper;
        boolean hasCreateEglContext =false;
        boolean hasSurfaceCreated =false;
        boolean hasSurfaceChanged =false;
        boolean hasDrawFrame =false;
        private int mWidth;
        private int mHeight;
        GL10 egl ;


        public VideoRenderThread(WeakReference<BaseVideoRecorder> mVideoRecorderWf){
            this.mVideoRecorderWf = mVideoRecorderWf;
            mEglHelper = new com.darren.livepush.opengl.EglHelper();
        }

        private void requestExit(){
            mShouldExit = true;
        }

        public void run(){

                while (true){
                    // 按下结束时能退出
                    if (mShouldExit){
                        onDestroy();
                        return;
                    }

                    BaseVideoRecorder baseVideoRecorder = mVideoRecorderWf.get();

                    // 根据GLSurfaceView源码中的循环绘制流程
                    // GLSurfaceView绘制源码解析：https://www.jianshu.com/p/369d5694c8ca
                    if(!hasCreateEglContext){
                        mEglHelper.initCreateEgl(baseVideoRecorder.mSurface,baseVideoRecorder.mEglContext);
                        hasCreateEglContext = true;
                    }
                    egl = (GL10) mEglHelper.getEglContext().getGL();

                    if(!hasSurfaceCreated){
                        // 调用mRender的onSurfaceCreated，做参数和纹理等的初始化
                        baseVideoRecorder.mRender.onSurfaceCreated(egl,mEglHelper.getEGLConfig());
                        hasSurfaceCreated = true;
                    }

                    if(!hasSurfaceChanged){
                        // 调用mRender的onSurfaceChanged，做窗口的初始化，和变换
                        baseVideoRecorder.mRender.onSurfaceChanged(egl,mWidth,mHeight);
                        hasSurfaceChanged = true;
                    }

                    drawTime = System.currentTimeMillis();
                    System.out.println("onDrawFrame:"+drawTime);
                    baseVideoRecorder.mRender.onDrawFrame(egl);

                    // 绘制到 MediaCodec 的 Surface 上面去
                    mEglHelper.swapBuffers();

                    try {
                        //休眠33毫秒，30fps，一秒需要30帧
                        Thread.sleep(16);
                    }catch (Exception r){
                        r.printStackTrace();
                    }
                }
        }

        private void onDestroy() {
            mEglHelper.destroy();
//            mVideoRecorderWf.get().release();
        }

        public void setSize(int mWidth, int mHeight) {
            this.mWidth = mWidth;
            this.mHeight = mHeight;
        }
    }

}
