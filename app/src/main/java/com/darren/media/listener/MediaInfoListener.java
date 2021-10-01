package com.darren.media.listener;

public interface MediaInfoListener {

    public void musicInfo(int sampleRate, int channel);

    public void callBackPcm(byte[] pcmData, int size);
}
