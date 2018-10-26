/*
 *    Copyright (C) 2017 MINDORKS NEXTGEN PRIVATE LIMITED
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.linyi.www.tf_mnist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import android.content.res.AssetManager;
import android.support.v4.os.TraceCompat;


public class TensorFlowImageClassifier  {

    private static final String TAG = "TFVoviceClassifier";
    private String inputName;
    private String outputName;
    private int inputSize;
    private String[] outputNames;
    private TensorFlowInferenceInterface inferenceInterface;
    private boolean runStats = false;
    private String modelName;
    private float [][] valdatas;
    private int [] vallbs;
    private static  int sampleCount=300;
    private static int sampleLe=66;

    public static TensorFlowImageClassifier create(
            AssetManager assetManager,
            String modelFilename
    )
            throws IOException {
        TensorFlowImageClassifier c = new TensorFlowImageClassifier();
        c.inputName = "input";
        c.outputName = "output";
        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
        c.inputSize = 5*16000;
        c.outputNames = new String[]{"output"};
        c.modelName=modelFilename;
        c.valdatas=readdata(assetManager);
        c.vallbs=readlbs(assetManager);
        return c;
    }
    public static float byte2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }
    public  static short getShort(byte[] b, int index) {
        return (short) (((b[index + 1] << 8) | b[index + 0] & 0xff));
    }
    private static float [][] readdata(AssetManager assetManager){
        try{
            InputStream in =(InputStream)assetManager.open("b2.data");
            byte[] s4byte=new byte[4];
            int count=0;
            float [][] inpt=new float[sampleCount][sampleLe];
            while(count<sampleCount*sampleLe && in.available()>0){
                in.read(s4byte);
                inpt[count/sampleLe][count%sampleLe]=byte2float(s4byte,0);
                count++;
            }
            in.close();
            return inpt;
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return null;
    }
    private static int [] readlbs(AssetManager assetManager){
        try{
            InputStream in =(InputStream)assetManager.open("b2.lbs");
            int count=0;
            int [] inpt=new int[sampleCount];
            while(count<sampleCount && in.available()>0){
                int lbs=in.read();
                inpt[count]=lbs;
                count++;
            }
            in.close();
            return inpt;
        }catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }
        return null;
    }

    private float caldistance(float [] data1,float [] data2){
        float distance=0.0f;
        for(int i=0;i<66;++i){
            distance+= (data1[i]-data2[i])*(data1[i]-data2[i]);
        }
        return distance;
    }
    private int getemblbs(float [] data){
        float mindis=100f;
        int lbs=0;
        for(int i=0;i<vallbs.length;++i){
           float tmpdis= caldistance(valdatas[i],data);
            System.out.print(tmpdis+" ");
           if(tmpdis<mindis){
               lbs=vallbs[i];
               mindis=tmpdis;
           }
        }
        System.out.println();
        System.out.println( lbs+":"+mindis);
        return lbs;
    }

    public long recognizeImage(final float[] pixels) {
//        FloatBuffer das;
        long [] lbs=new long[1];
        float [] ys=new float[66];
        TraceCompat.beginSection("recognizeImage");
        inferenceInterface.feed(inputName, pixels, 1,pixels.length);
        inferenceInterface.run(outputNames, true);
        inferenceInterface.fetch(outputName, lbs);
        TraceCompat.endSection();
        return lbs[0];
    }



    public void close() {
        inferenceInterface.close();
    }
}


