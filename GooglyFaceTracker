/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.googlyeyes;

import android.graphics.PointF;

import com.google.android.gms.samples.vision.face.googlyeyes.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Tracks the eye positions and state over time, managing an underlying graphic which renders googly
 * eyes over the source video.<p>
 *
 * To improve eye tracking performance, it also helps to keep track of the previous landmark
 * proportions relative to the detected face and to interpolate landmark positions for future
 * updates if the landmarks are missing.  This helps to compensate for intermediate frames where the
 * face was detected but one or both of the eyes were not detected.  Missing landmarks can happen
 * during quick movements due to camera image blurring.
 */
class GooglyFaceTracker extends Tracker<Face> {
    private static final float EYE_CLOSED_THRESHOLD = 0.35f;
    private static final float EYE_OPEN_THRESHOLD=0.65f;
    private static final int minChangeTime=2000;

    private GraphicOverlay mOverlay;
    private GooglyEyesGraphic mEyesGraphic;

    // Record the previously seen proportions of the landmark locations relative to the bounding box
    // of the face.  These proportions can be used to approximate where the landmarks are within the
    // face bounding box if the eye landmark is missing in a future update.
    private Map<Integer, PointF> mPreviousProportions = new HashMap<>();

    // Similarly, keep track of the previous eye open state so that it can be reused for
    // intermediate frames which lack eye landmarks and corresponding eye state.
    private boolean mPreviousIsLeftOpen = true;
    private boolean mPreviousIsRightOpen = true;


    //==============================================================================================
    // Methods
    //==============================================================================================

    GooglyFaceTracker(GraphicOverlay overlay) {
        mOverlay = overlay;
    }

    /**
     * Resets the underlying googly eyes graphic and associated physics state.
     */
    @Override
    public void onNewItem(int id, Face face) {
        mEyesGraphic = new GooglyEyesGraphic(mOverlay);
    }

    /**
     * Updates the positions and state of eyes to the underlying graphic, according to the most
     * recent face detection results.  The graphic will render the eyes and simulate the motion of
     * the iris based upon these changes over time.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        mOverlay.add(mEyesGraphic);
        updatePreviousProportions(face);
        PointF leftPosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
        PointF rightPosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);
        float isSmile=face.getIsSmilingProbability();
        face.getLandmarks();
        float leftOpenScore = face.getIsLeftEyeOpenProbability();
        float rightOpenScore = face.getIsRightEyeOpenProbability();
        long leftChageTime=0;
        if (leftOpenScore != Face.UNCOMPUTED_PROBABILITY) {
            if (leftOpenScore>EYE_OPEN_THRESHOLD && mPreviousIsLeftOpen==false){
                leftChageTime=System.currentTimeMillis();
            }else if (leftOpenScore<EYE_CLOSED_THRESHOLD && mPreviousIsLeftOpen==true){
                leftChageTime=System.currentTimeMillis();
            }
        }
        System.out.println("left:"+leftOpenScore+" -right:"+rightOpenScore +" -smile:"+isSmile+" "+mPreviousIsLeftOpen+" "+mPreviousIsRightOpen);
        boolean isRightOpen=mPreviousIsRightOpen;
        if (rightOpenScore != Face.UNCOMPUTED_PROBABILITY) {
            if(rightOpenScore>EYE_OPEN_THRESHOLD && mPreviousIsRightOpen==false && System.currentTimeMillis()- leftChageTime<minChangeTime){
                mPreviousIsRightOpen = true;
                isRightOpen=true;
            }else if (rightOpenScore<EYE_CLOSED_THRESHOLD && mPreviousIsRightOpen==true && System.currentTimeMillis()- leftChageTime<minChangeTime){
//                System.out.println("close right :" +rightOpenScore);
                mPreviousIsRightOpen = false;
                isRightOpen=false;
            }
            mPreviousIsLeftOpen=mPreviousIsRightOpen;
            mEyesGraphic.updateEyes(leftPosition, isRightOpen, rightPosition, isRightOpen, isSmile>0.2 ? true:false);
        }
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily (e.g., if the face was momentarily blocked from
     * view).
     */
    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        mOverlay.remove(mEyesGraphic);
    }

    /**
     * Called when the face is assumed to be gone for good. Remove the googly eyes graphic from
     * the overlay.
     */
    @Override
    public void onDone() {
        mOverlay.remove(mEyesGraphic);
    }

    //==============================================================================================
    // Private
    //==============================================================================================

    private void updatePreviousProportions(Face face) {
        for (Landmark landmark : face.getLandmarks()) {
            PointF position = landmark.getPosition();
            float xProp = (position.x - face.getPosition().x) / face.getWidth();
            float yProp = (position.y - face.getPosition().y) / face.getHeight();
            mPreviousProportions.put(landmark.getType(), new PointF(xProp, yProp));
        }
    }

    /**
     * Finds a specific landmark position, or approximates the position based on past observations
     * if it is not present.
     */
    private PointF getLandmarkPosition(Face face, int landmarkId) {
        for (Landmark landmark : face.getLandmarks()) {
            if (landmark.getType() == landmarkId) {
                return landmark.getPosition();
            }
        }

        PointF prop = mPreviousProportions.get(landmarkId);
        if (prop == null) {
            return null;
        }

        float x = face.getPosition().x + (prop.x * face.getWidth());
        float y = face.getPosition().y + (prop.y * face.getHeight());
        return new PointF(x, y);
    }
}
