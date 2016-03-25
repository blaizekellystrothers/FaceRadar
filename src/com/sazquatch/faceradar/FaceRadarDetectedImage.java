/*
 *  FaceRadar
 *  Copyright (C) 2015 Blaize Strothers
 *  
 *  Derived from SmutDetectCategorisedImage by Rajmund Witt
 */
package com.sazquatch.faceradar;

public class FaceRadarDetectedImage {

    private boolean hasFace_ = false;

    public FaceRadarDetectedImage() {}

    public boolean getHasFace() {
        return hasFace_;
    }

    public void setHasFace(boolean hasFace) {
        hasFace_ = hasFace;
    }

    /**
     * @return the DetectedImage as a string for report creation
     */
    @Override
    public String toString() {

        StringBuilder string = new StringBuilder();
        string.append("Contains face: ").append(getHasFace());

        return string.toString();
    }

}
