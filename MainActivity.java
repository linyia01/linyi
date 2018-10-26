package com.linyi.www.tf_mnist;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.musicg.wave.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import static android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    static {//导入libhello.so 文件。 这里面只写hello就可以
        System.loadLibrary("hello-lib");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI(String str);
    public native void  nsProcess(short [] buffer, int sampleRate, int samplesCount,int level);
    public native void  nsMfccFeature(byte [] inbuffer, int sampleRate, int samplesCount, int level, float voiseAdd, float[] outbuffer);

    //    private static final String MODEL_FILE = "file:///android_asset/model.pb_124469";
    private Executor executor = Executors.newSingleThreadExecutor();
    private List<TensorFlowImageClassifier> classifier =new ArrayList<TensorFlowImageClassifier>();
    private List<String> modelfiles=new ArrayList<String>();
    private static int currentModelIndex=0;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isExit = false;
    private int recBufSize;
    private int playBufSize;
    private static final int srcfre = 16000;//44100;//22050;
    private static final int downFre=16000;//44100;//22050;
    //    private static final int audioMinBuff=srcfre*1*5;
    private static final int datatimes = 5;
    private static final boolean useSSRC=false;
    private static final  int minDecibel=1;
    private static final  int maxDecibel=200;
    private static float voiseAdd=0.5f;
    private static int READ_SIZE=srcfre/10;
    //    private static float normalDecibel=40;
    private static float minNormalDecibel=10;
    private static float maxNormalDecibel=200;
    private static double  voiceDecibel=0;
    DecimalFormat decimalFormat=new DecimalFormat(".000");
    //    private byte[] audioParserBuf = new byte[srcfre*datatimes*2];
    private List<recognizeResult> listRecongnize=new ArrayList<recognizeResult>();


    private static boolean isrecord=false;
    private static boolean isstop=false;
    private static boolean isplay=false;
    private  File mSavefile = null;
    private static  int DELEVEL=2;
    float[] changeToSampleData=new float[srcfre*datatimes];
    short[] changeToSampleStort=new short[srcfre*datatimes];

    private class recognizeResult{
        Integer type;
        Long time;
    }
//    private static final float oneReadtimes=5f;

//    private static final int INPUT_SIZE = times*frequency;

    //    static final int channelConfiguration= AudioFormat.CHANNEL_CONFIGURATION_MONO;
//    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private MediaRecorder mMediaRecorder;

    private static void scanFile(Context context,File filname){
//        MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/"}, null, null);
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(filname));
        context.sendBroadcast(scanIntent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String back="";

        initTensorFlowAndLoadModel();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView mytext=(TextView) findViewById(R.id.mytext);
        final TextView nextModel=(TextView) findViewById(R.id.nextModel);
        final TextView beforeModel=(TextView) findViewById(R.id.beforModel);
        final TextView addVol=(TextView) findViewById(R.id.addVol);
        final TextView descVol=(TextView) findViewById(R.id.descVol);
        final TextView recordVoice=(TextView) findViewById(R.id.recordvoice);
        final TextView stopVoice=(TextView) findViewById(R.id.stopvoice);
        final TextView playVoice=(TextView) findViewById(R.id.playvoice);
        final TextView denoiseLevel=(TextView) findViewById(R.id.denoiseLevel);


        recordVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isrecord==false){
                    setSoundRecordFlag();
                    if (recordVoice.getText().equals("录音")){
                        stopVoice.setText("停止");
                        recordVoice.setText("录音ok");
                    }
                    Log.i("voice","开始录音");
                    isrecord=true;
                }
            }
        });
        stopVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isrecord=false;
                if (stopVoice.getText().equals("停止")){
                    stopVoice.setText("停止ok");
                    recordVoice.setText("录音");
                }

                Log.i("voice","结束录音");
                scanFile(getApplicationContext(),mSavefile);
            }
        });
        playVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isrecord=false;
                Log.i("voice","开始播放");
                playRecord();
            }
        });

        denoiseLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DELEVEL=(DELEVEL+1)%5;
                denoiseLevel.setText("去噪等级"+(DELEVEL-1));
            }
        });

//        playBufSize = AudioTrack.getMinBufferSize(frequency,  AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        //④ 调用write写数据
        try{
            initTensorFlowAndLoadModel();
            Thread.sleep(1000);
            beforeModel.setText("上一个模型："+modelfiles.get(currentModelIndex));
            nextModel.setText("下一个模型："+modelfiles.get(currentModelIndex));
            addVol.setText("+ : "+decimalFormat.format(voiseAdd));
            descVol.setText("- : "+decimalFormat.format(voiseAdd));

        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //从MIC存储到缓存区
                while(!isExit){
                    try{
                        int  preTimes=1000;
                        while(true){
                            if(ContextCompat.checkSelfPermission(MainActivity.this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                if(preTimes>5*10){
                                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{RECORD_AUDIO}, 1);
                                    preTimes=0;
                                }
                            }else{
                                break;
                            }
                            preTimes++;
                            Thread.sleep(100);
                        }
                        if(ContextCompat.checkSelfPermission(MainActivity.this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{WRITE_EXTERNAL_STORAGE}, 1);
                        }
                        if(ContextCompat.checkSelfPermission(MainActivity.this, MOUNT_UNMOUNT_FILESYSTEMS) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{MOUNT_UNMOUNT_FILESYSTEMS}, 1);
                        }
                        if(audioTrack==null){
                            int bufsize = AudioTrack.getMinBufferSize(downFre, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
                            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, downFre, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, bufsize, AudioTrack.MODE_STREAM);
                            audioTrack.play() ;
                        }
                        if(audioRecord == null){
                            recBufSize = AudioRecord.getMinBufferSize(srcfre,  AudioFormat.CHANNEL_IN_MONO , AudioFormat.ENCODING_PCM_16BIT);
                            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,srcfre, AudioFormat.CHANNEL_IN_MONO ,AudioFormat.ENCODING_PCM_16BIT,recBufSize*5);
                            Thread.sleep(5*1000);
                        }
                        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                            recBufSize = AudioRecord.getMinBufferSize(srcfre,  AudioFormat.CHANNEL_IN_MONO , AudioFormat.ENCODING_PCM_16BIT);
                            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,srcfre, AudioFormat.CHANNEL_IN_MONO ,AudioFormat.ENCODING_PCM_16BIT,recBufSize*5);
                            Log.e("linyi","not ready");
                        }
                        audioRecord.startRecording();
                        Thread.sleep(5*1000);
                        byte[] audioBuf = new byte[READ_SIZE];
                        int curTotalLen=0;
                        byte[] oneReadSrcBuf = new byte[(int)(srcfre*datatimes*2)];
                        while(!isExit) {
                            int readLen = audioRecord.read(audioBuf, 0, READ_SIZE);
                            if(readLen!=READ_SIZE){
                                Log.e("read linyi","read data less is : "+readLen);
                                continue;
                            }
                            System.arraycopy(audioBuf, 0, oneReadSrcBuf, curTotalLen, readLen);
                            curTotalLen += readLen;
                            if (curTotalLen == oneReadSrcBuf.length) {
                                Message msg = new Message();
//                                msg.obtain(mHander,1,oneReadSrcBuf).sendToTarget();
                                msg.obtain(mHander,2,oneReadSrcBuf).sendToTarget();
                                System.arraycopy(oneReadSrcBuf, srcfre*(datatimes-1)*2, oneReadSrcBuf, 0, srcfre*1*2);
                                curTotalLen=srcfre*1*2;
                            }else if (curTotalLen > oneReadSrcBuf.length){
                                curTotalLen=0;
                            }
                        }
                    }catch (Exception e){
                        audioRecord=null;
                        Log.e("liner",e.getLocalizedMessage());
                    }
                }
            }
        }).start();

        addVol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    String  back=stringFromJNI("linyi");
                    System.out.print(back);
                }catch (Exception e){
                    System.out.println(e.getLocalizedMessage());
                }
                upVoiceDecibal();
            }
        });

        descVol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downVoiceDecibal();
            }
        });

        beforeModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentModelIndex --;
                if(currentModelIndex<0){
                    currentModelIndex = modelfiles.size()-1;
                }
                if(currentModelIndex-1<0){
                    beforeModel.setText("上一个模型："+modelfiles.get(modelfiles.size()-1));
                }else{
                    beforeModel.setText("上一个模型："+modelfiles.get(currentModelIndex-1));
                }
                if(currentModelIndex+1>=modelfiles.size()){
                    nextModel.setText("下一个模型："+modelfiles.get(0));
                }else{
                    nextModel.setText("下一个模型："+modelfiles.get(currentModelIndex+1));
                }
                mytext.setText("当前模型："+modelfiles.get(currentModelIndex));
            }
        });

        nextModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentModelIndex++;
                if(currentModelIndex>=modelfiles.size()){
                    currentModelIndex=currentModelIndex%(modelfiles.size());
                }
                if(currentModelIndex-1<0){
                    beforeModel.setText("上一个模型："+modelfiles.get(modelfiles.size()-1));
                }else{
                    beforeModel.setText("上一个模型："+modelfiles.get(currentModelIndex-1));
                }
                if(currentModelIndex+1>=modelfiles.size()){
                    nextModel.setText("下一个模型："+modelfiles.get(0));
                }else{
                    nextModel.setText("下一个模型："+modelfiles.get(currentModelIndex+1));
                }
                if(modelfiles.get(currentModelIndex).equals("pb0905_9290")){
                    voiseAdd=2.0f;
                    DELEVEL=1;
                }
                if(modelfiles.get(currentModelIndex).equals("pb0825_21200")){
                    voiseAdd=0.5f;
                    DELEVEL=2;
                }if(modelfiles.get(currentModelIndex).contains("pb0918")){
                    voiseAdd=1f;
                    DELEVEL=2;
                }
                mytext.setText("当前模型："+modelfiles.get(currentModelIndex));
            }
        });
    }


    private void upVoiceDecibal(){
        final TextView addVol=(TextView) findViewById(R.id.addVol);
        final TextView descVol=(TextView) findViewById(R.id.descVol);
        if(voiseAdd>10){
            voiseAdd += 1f;
        }else if (voiseAdd>1){
            voiseAdd += 0.1f;
        }else if(voiseAdd>0.1) {
            voiseAdd += 0.01f;
        }else if(voiseAdd>0.01){
            voiseAdd += 0.001f;
        }
        addVol.setText("+ : "+decimalFormat.format(voiseAdd));
        descVol.setText("- : "+decimalFormat.format(voiseAdd));
    }

    private void downVoiceDecibal(){
        final TextView addVol=(TextView) findViewById(R.id.addVol);
        final TextView descVol=(TextView) findViewById(R.id.descVol);
        if(voiseAdd>10){
            voiseAdd -= 1f;
        }else if (voiseAdd>1){
            voiseAdd -= 0.1f;
        }else if(voiseAdd>0.1) {
            voiseAdd -= 0.01f;
        }else if(voiseAdd>0.01){
            voiseAdd -= 0.001f;
        }
        addVol.setText("+ : "+decimalFormat.format(voiseAdd));
        descVol.setText("- : "+decimalFormat.format(voiseAdd));
    }


    public float[] interpolate(int oldSampleRate, int newSampleRate, float[] samples) {

        if (oldSampleRate==newSampleRate){
            return samples;
        }

        int newLength=(int)Math.round(((float)samples.length/oldSampleRate*newSampleRate));
        float lengthMultiplier=(float)newLength/samples.length;
        float[] interpolatedSamples = new float[newLength];

        // interpolate the value by the linear equation y=mx+c
        for (int i = 0; i < newLength; i++){

            // get the nearest positions for the interpolated point
            float currentPosition = i / lengthMultiplier;
            int nearestLeftPosition = (int)currentPosition;
            int nearestRightPosition = nearestLeftPosition + 1;
            if (nearestRightPosition>=samples.length){
                nearestRightPosition=samples.length-1;
            }

            float slope=samples[nearestRightPosition]-samples[nearestLeftPosition]; // delta x is 1
            float positionFromLeft = currentPosition - nearestLeftPosition;

            interpolatedSamples[i] = (short)(slope*positionFromLeft+samples[nearestLeftPosition]); // y=mx+c
        }

        return interpolatedSamples;
    }





    public  static short getShort(byte[] b, int index) {
        return (short) (((b[index + 1] << 8) | b[index + 0] & 0xff));
    }
    public static void putShort(byte b[], short s, int index) {
        b[index + 1] = (byte) (s >> 8);
        b[index + 0] = (byte) (s >> 0);
    }
    private float [] bytesbytesToTensorInputMfccData(byte [] audioBuff){
        float [] result= new  float[208*24];
        try{
//            ByteArrayInputStream srcStream = new ByteArrayInputStream(audioBuff);
//            int currentCount=0;
//            byte [] s2byte=new byte[2];
//            while(srcStream.available()>0&&currentCount<changeToSampleStort.length){
//                srcStream.read(s2byte);
//                changeToSampleStort[currentCount]=getShort(s2byte,0 );
//                currentCount++;
//            }
            nsMfccFeature(audioBuff,srcfre,srcfre*datatimes,DELEVEL-1,voiseAdd,result);
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return result;
    }


    private   float[]  bytesToTensorInputData(byte [] audioBuff){
        try{
            ByteArrayInputStream srcStream = new ByteArrayInputStream(audioBuff);
            int currentCount=0;
            byte [] s2byte=new byte[2];
            while(srcStream.available()>0&&currentCount<changeToSampleStort.length){
                srcStream.read(s2byte);
                changeToSampleStort[currentCount]=(short)(getShort(s2byte,0 )* voiseAdd);
                currentCount++;
            }
            if (DELEVEL >0){
                nsProcess(changeToSampleStort,srcfre,srcfre*datatimes,DELEVEL-1);
            }
            if (isrecord && mSavefile != null) {
                try {
                    OutputStream os =new FileOutputStream(mSavefile,true);
                    BufferedOutputStream bos = new BufferedOutputStream(os);
                    DataOutputStream dos =    new DataOutputStream(bos);
                    if (dos != null) {
                        try {
                            byte [] src_audio=new byte[2];
                            for(int i=0;i<changeToSampleStort.length;++i){
                                putShort(src_audio, changeToSampleStort[i],0);
                                dos.write(src_audio, 0, src_audio.length);
                            }
                            dos.flush();
                            dos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Log.i("TOMLEE","开始音频分析 2222  5555  ");
            }
            for (int i=0;i<changeToSampleStort.length;++i){
                changeToSampleData[i]=(float)changeToSampleStort[i];
            }
            this.voiceDecibel=getVoiceDecibel( changeToSampleData);
            if(this.voiceDecibel<minDecibel||this.voiceDecibel>maxDecibel){
                return  new float[2];
            }
            if(this.voiceDecibel < minNormalDecibel ){
                Log.d(TAG,"upVoiceDecibal "+this.voiceDecibel );
                upVoiceDecibal();
            }else if (this.voiceDecibel > maxNormalDecibel ){
                Log.d(TAG,"downVoiceDecibal "+this.voiceDecibel);
                downVoiceDecibal();
            }
            return (float[])  changeToSampleData;

        }catch (Exception e){
            Log.e("linyi",e.getLocalizedMessage());
        }
        return null;
    }

    private float[] voicefft(double [] indata){
        int le=(int)Math.ceil (indata.length / 2.0) ;
        float[] result2 = new float[le];
        RealDoubleFFT spectrumAmpFFT   = new RealDoubleFFT(indata.length);
        spectrumAmpFFT.ft(indata);
        result2[0]=(float)(indata[0]*2.0/indata.length);
        for(int i=1,j=1;j+1<indata.length;++i,j+=2){
            result2[i]=(float)(Math.sqrt(indata[j]*indata[j]+indata[j+1]*indata[j+1])*2.0/indata.length);
        }
        return result2;
    }
    private  float[] fftpower(float [] data ){
        int  index=0;
        int  eachlen=441;
        int  sampleRate=22050;
        double  srate=eachlen/((eachlen-1)*1.0/sampleRate);
        if(data.length!=5*sampleRate){
            return null;
        }
        int fftcount=(int)( Math.ceil(data.length/(eachlen*1.0)) );
        float[] fftpowers=new float[ fftcount * 159];
        int indexfft=0;
        while (index < data.length){
            float [] datafft=null;
            double [] indata =null;
            if(index+eachlen<data.length){
                indata =new double[eachlen];
            }else{
                indata =new double[data.length-index];
            }
            for(int i=0;i<indata.length;++i,index++){
                indata[i]=data[index];
            }
            datafft=voicefft(indata);
            int end=0,begin=0;
            int ifft=0;
            for(int j=1;j<160;++j){
                if(end <datafft.length){
                    begin=(int)( ((j - 1) * 50*eachlen*1.0)/srate +0.5 );
                    end =( int)( (j*50*eachlen*1.0)/srate + 0.5 );
                    float power=0.0f;
                    while(begin<end){
                        power+=datafft[begin];
                        begin++;
                    }
                    fftpowers[indexfft*159+j-1]=power;
                }else{
                    fftpowers[indexfft*159+j-1]=0;
                }
            }
            indexfft++;
        }
        return fftpowers;
    }

    private double getVoiceDecibel(float [] changeToSampleData){
        double vdata = .0;
        for (int i = 0; i < changeToSampleData.length; i++) {
            vdata += Math.abs(changeToSampleData[i]) ;
        }
        double mean=vdata/(changeToSampleData.length/2);
        return (int)(20 * Math.log10(mean));
    }



    private class voicePool{
//        private int pool_size=downFre*15;
//        private float [] voice_pool=new float[pool_size];
//        private int pool_front = 0;
//        private int pool_tail = 0;
//        private boolean front_behind_tail=false;

//        public float [] getVoice(int count){
//            if(front_behind_tail==false){
//                if(pool_tail+count<pool_front){
//                    float [] data=new float [count];
//                    for(int i=0;i<count;++i){
//                        data[i]=voice_pool[i+pool_tail];
//                    }
//                    return data;
//                }
//            }else{
//                if(pool_tail+count<pool_size){
//                    float [] data=new float [count];
//                    for(int i=0;i<count;++i){
//                        data[i]=voice_pool[i+pool_tail];
//                    }
//                    return data;
//                }else if( (pool_tail+count)%pool_size<pool_front ){
//                    float [] data=new float [count];
//                    for(int i=0;i<count;++i){
//                        data[i]=voice_pool[(i+pool_tail)%pool_size];
//                    }
//                    return data;
//                }
//            }
//            return null;
//        }
//
//        public boolean addVoice(Float item){
//            return add_and_move(item,null);
//        }
//
//
//        private synchronized boolean add_and_move(Float item,Integer count){
//            if(item !=null){
//                if( (front_behind_tail==true && ( pool_front + 1 ) % pool_size < pool_tail )||(front_behind_tail==false ) ){
//                    voice_pool[pool_front]= item;
//                    pool_front++;
//                    if(pool_front>=pool_size){
//                        pool_front = pool_front % pool_size;
//                        changeMoveDirection(true);
//                    }
//                    return true;
//                }
//            }else{
//                pool_tail=pool_tail+count;
//                if(pool_tail>=pool_size){
//                    pool_tail=pool_tail%pool_size;
//                    changeMoveDirection(false);
//                }
//            }
//            return false;
//        }
//
//        public void changeTailIndex(int count){
//            add_and_move(null,count);
//        }
//        private synchronized  void changeMoveDirection(boolean direc){
//            this.front_behind_tail=direc;
//        }

        private  float standardDiviation(float[] x) {
            int m=x.length;
            double sum=0;
            for(int i=0;i<m;i++){
                sum+=x[i];
            }
            double dAve=sum/m;
            double dVar=0;
            for(int i=0;i<m;i++){
                dVar+=(x[i]-dAve)*(x[i]-dAve);
            }
            return (float)Math.sqrt(dVar/m);
        }

        private int minVoiceWindowsSize=(int)(0.5f*downFre);
        private int checkVoiceSize=5*downFre;
        private float minVar=120;
        private float minMult=2.5f;
        private float minVoiceMult=2.5f;

        private float breathMinSize=(int)(2.3f*downFre);
        private float breathMinFloat=(int)(0.2f*downFre);//正常鼾声间隔浮动自我调整时间
        private final float breathMinBase=(int)(2.3f*downFre);

        private float breathMaxSize=(int)(4.5f*downFre);
        private float breathMaxFloat=(int)(0.3f*downFre);//正常鼾声间隔浮动自我调整时间
        private final  float breathMaxBase=(int)(4.5f*downFre);

        private int beforeLastCount=0;
        private int beforeLastPost=-1;
        private int lastCount=0;
        private int lastPos=-1;


        @SuppressLint("NewApi")
        private int  checkVoiceCount(float []  srcdata){
//            float baseLine=baseLine(srcdata);
            int windowSize=minVoiceWindowsSize;
            Map<Integer ,Float> windowsVar =new HashMap<Integer,Float>();
            float [] windowData =new float[windowSize];
            for (int i=0,j=0;i<srcdata.length;++j) {
                windowData[j]=srcdata[i];
                if((++i)%windowSize==0){
                    j=-1;
                    float sdv=standardDiviation(windowData);
                    windowsVar.put(i,sdv);
                    System.out.print(" i: "+sdv);
                }
            }

            List<Map.Entry<Integer ,Float>> list = new ArrayList<>();

            for(Map.Entry<Integer ,Float> entry : windowsVar.entrySet()){
                list.add(entry);
            }

            list.sort(new Comparator<Map.Entry<Integer ,Float>>(){
                @Override
                public int compare(Map.Entry<Integer ,Float> o1, Map.Entry<Integer ,Float> o2) {
                    if(o2.getValue()>o1.getValue()){
                        return 1;
                    }else{
                        return -1;
                    }
                }
            });

            if(list.size()<3||list.get(0).getValue()<minVar){
                return 0;
            }

            float baseVoice=0;
            baseVoice+=list.get(list.size()-1).getValue();
            baseVoice+=list.get(list.size()-2).getValue();
            baseVoice+=list.get(list.size()-3).getValue();
            baseVoice/=3;
            int currentCount = -1,maxCount = 0;
            int currentPos1 = 0,currentPos2 = 0;

            if(lastCount>1 && beforeLastCount>1){
                maxCount=1;
            }else{
                maxCount=2;
            }

            int forAdjustParamCount = 0;
            int forAdjustParamPos = 0;

            for(int m=0;m< list.size();++m) {
                if(forAdjustParamCount>maxCount){
                    if(Math.abs(breathMinSize+0.01*downFre-breathMinBase)<breathMinFloat){
                        breathMinSize=(int)(breathMinSize+0.01*downFre);
                    }
                    if(Math.abs(breathMaxSize-0.01*downFre-breathMaxBase)<breathMaxFloat){
                        breathMaxSize=(int)(breathMaxSize-0.01*downFre);
                    }
                    break;
                }
                if(list.get(m).getValue()>baseVoice*minMult){
                    if(forAdjustParamCount==0){
                        if(list.get(m).getKey()+lastPos>breathMinSize&& list.get(m).getKey()+lastPos<breathMaxSize){
                            forAdjustParamCount++;
                            forAdjustParamPos=list.get(m).getKey();
                        }
                    }else{
                        if(list.get(m).getKey()+lastPos>breathMinSize && list.get(m).getKey()+lastPos<breathMaxSize &&
                                Math.abs(list.get(m).getKey()-forAdjustParamPos)>breathMinSize && Math.abs(list.get(m).getKey()-forAdjustParamPos)< breathMaxSize){
                            forAdjustParamCount++;
                        }
                    }
                }
            }

            if (forAdjustParamCount==0){
                if(Math.abs(breathMinSize-0.01*downFre-breathMinBase)<breathMinFloat){
                    breathMinSize=(int)(breathMinSize-0.01*downFre);
                }
                if(Math.abs(breathMaxSize+0.01*downFre-breathMaxBase)<breathMaxFloat){
                    breathMaxSize=(int)(breathMaxSize+0.01*downFre);
                }
            }

            if ( baseVoice*minVoiceMult<list.get(0).getValue() && ( lastPos == -1 ||
                    ( lastPos != -1 && list.get(0).getKey()+lastPos > breathMinSize &&
                            list.get(0).getKey()+lastPos < breathMaxSize ) ) ){
                currentCount=1;
                currentPos1=list.get(0).getKey();
            }

            for(int m=1;m< list.size();++m) {
                if(currentCount>=maxCount){
                    break;
                }
                if(list.get(m).getValue()>baseVoice*minMult){
                    if(currentCount==-1){
                        if(list.get(m).getKey()+lastPos>breathMinSize&& list.get(m).getKey()+lastPos<breathMaxSize) {
                            currentCount = 1 ;
                            currentPos1=list.get(m).getKey();
                        }
                    } else {
                        if(list.get(m).getKey()+lastPos>breathMinSize && list.get(m).getKey()+lastPos<breathMaxSize &&
                                Math.abs(list.get(m).getKey()-currentPos1)>breathMinSize && Math.abs(list.get(m).getKey()-currentPos1)< breathMaxSize){
                            currentCount++;
                            currentPos2=list.get(m).getKey();
                            break;
                        }
                    }
                }
            }

            beforeLastCount=lastCount;
            lastCount=currentCount;
            if(currentCount==0){
                beforeLastPost=-1;
                lastPos=-1;
            }
            else if(currentCount==1){
                beforeLastPost=lastPos;
                lastPos=currentPos1;
            }else{
                if(currentPos1<currentPos2){
                    lastPos=checkVoiceSize-currentPos2;
                    beforeLastPost=checkVoiceSize-currentPos1;
                }else{
                    lastPos=checkVoiceSize-currentPos1;
                    beforeLastPost=checkVoiceSize-currentPos2;
                }
            }
            if(currentCount==0){
                System.out.println(" ");
            }
            System.out.println("currentCount is : "+ currentCount);
            return currentCount;
        }
    }
    public void setSoundRecordFlag(){
        audioTrack.stop();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = format.format(new Date());
        String fileName=Environment.getExternalStorageDirectory().getAbsolutePath() + "/soundRecord_" + timeStr + ".pcm";
        //生成PCM文件
        mSavefile = new File(fileName);
        Log.i(TAG, "生成文件");
        //如果存在，就先删除再创建
        if (mSavefile.exists())
            mSavefile.delete();
        Log.i(TAG, "删除文件");
        try {
            mSavefile.createNewFile();
//            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
            MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/"}, null, null);
            Log.i(TAG, "创建文件");
        } catch (IOException e) {
            Log.i(TAG, "未能创建");
            throw new IllegalStateException("未能创建" + mSavefile.toString());
        }
    }
    //播放文件
    public void playRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //读取文件
                int musicLength = (int) (mSavefile.length());
                byte[] music = new byte[musicLength];
                try {
                    InputStream is = new FileInputStream(mSavefile);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    DataInputStream dis = new DataInputStream(bis);
                    dis.read(music,0,musicLength);
                    dis.close();
                    audioTrack.play();
                    audioTrack.write(music, 0, musicLength);
                    audioTrack.stop();
                } catch (Throwable t) {
                    Log.e(TAG, "播放失败");
                }
            }
        }).start();
    }


    @SuppressLint("HandlerLeak")
    private  Handler mHander = new Handler(){
        @Override
        public  void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    try{
                        byte []  src_audio = (byte [] ) msg.obj;
                    }catch (Exception e){
                        Log.i("linyi",e.getLocalizedMessage());
                    }
                    break;
                case 2:
                    try{
                        long beginRecordTimes=System.currentTimeMillis();
                        byte [] audioBuff = (byte[]) msg.obj;
                        if(srcfre*datatimes*2!=audioBuff.length){
                            Log.e("linyi","read data error");
                            break;
                        }

                        String retStr = null;
                        beginRecordTimes=System.currentTimeMillis();
                        final TextView mytext=(TextView) findViewById(R.id.mytext);
                        mytext.setText("当前模型："+modelfiles.get(currentModelIndex));
                        long results=0;
                        if (modelfiles.get(currentModelIndex).equals("pb0905_9290")) {
                            float[] changeToSampleData =bytesToTensorInputData(audioBuff);
                            if(changeToSampleData.length==2){
                                retStr = "其它声音";
                            }else{
                                results = classifier.get(currentModelIndex).recognizeImage(changeToSampleData);
                                if(results==1){
                                    retStr = "其它声音";
                                }else if(results==0){
                                    retStr = "发现咳嗽声";
                                }
                            }
                        }else if(modelfiles.get(currentModelIndex).contains("unsleepping")){
                            float[] changeToSampleData =bytesToTensorInputData(audioBuff);
                            if(changeToSampleData.length==2){
                                retStr = "其它声音";
                            }else{
                                results = classifier.get(currentModelIndex).recognizeImage(changeToSampleData);
                                if(results==0){
                                    retStr = "其它声音";
                                }else if(results==1){
                                    retStr = "发现咳嗽声";
                                }
                            }
                        }
                        else if(modelfiles.get(currentModelIndex).equals("pb0825_21200")||
                                modelfiles.get(currentModelIndex).contains("sleepping") ){
                            float[] changeToSampleData =bytesToTensorInputData(audioBuff);
                            results = classifier.get(currentModelIndex).recognizeImage(changeToSampleData);
                            if (results == 0) {
                                retStr = "发现鼾声";
                            } else if (results == 1) {
                                retStr = "发现咳嗽声";
                            } else {
                                retStr = "其它声音";
                            }
                        }else if(modelfiles.get(currentModelIndex).contains("pb0918")){
                            retStr = "其它声音";
//                            float[] changeToSampleData =bytesbytesToTensorInputMfccData(audioBuff);
//                            Log.i("Tomlee:", "Preprocessing time is "+(System.currentTimeMillis()-beginRecordTimes)/1000.0);
//                            results = classifier.get(currentModelIndex).recognizeImage(changeToSampleData);
//                            if(results==0){
//                                retStr = "其它声音";
//                            }else if(results==1){
//                                retStr = "发现咳嗽声";
//                            }
                        }
                        Log.d(TAG, "分贝值:" + voiceDecibel);
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                        Log.i("Tomlee:", sdf.format(new Date())+"cur audio track status "+"("+ results +") " + retStr +" ; "+(System.currentTimeMillis()-beginRecordTimes)/1000.0);
                        LinearLayout linel = (LinearLayout) findViewById(R.id.lay);
                        linel.setHorizontalScrollBarEnabled(true);
                        TextView tv = new TextView(MainActivity.this);
                        tv.setText(modelfiles.get(currentModelIndex).split("_")[1]+" "+sdf.format(new Date())+" : "+retStr +"  分贝 ：" +  voiceDecibel);
                        tv.setTextSize(12);
                        if(linel.getChildCount()>12) {
                            linel.removeViewAt(0);
                        }
                        linel.addView(tv);
                    }catch (Exception e){
                        Log.e("linyi",e.getLocalizedMessage());
                    }
                    break;
                default:
                    break;
            }
        }
    };


    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String []  files = getAssets().list("");
                    for (String f : files) {
                        if(f.startsWith("pb")||f.contains("unsleepping")||f.contains("sleepping")){
                            modelfiles.add(f);
                            classifier.add( TensorFlowImageClassifier.create(getAssets(),"file:///android_asset/"+f ));
                        }
                    }
                    Log.d(TAG, "Load Success");
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }
}
