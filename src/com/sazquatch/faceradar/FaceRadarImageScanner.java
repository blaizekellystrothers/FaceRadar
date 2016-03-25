/*
 *  FaceRadar
 *  Copyright (C) 2015 Blaize Strothers
 *  
 *  Derived from SmutDetectImageScanner by Rajmund Witt
 */
package com.sazquatch.faceradar;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.ReadContentInputStream;

//Checks an image file from Autopsy for human faces
public abstract class FaceRadarImageScanner {

    private static final Logger logger_ = Logger.getLogger(FaceRadarImageScanner.class.getName());

    public static FaceRadarDetectedImage scanImage(AbstractFile file) {
        InputStream inputStream;
        FaceRadarDetectedImage detectedImage = null;
        BufferedImage bufferedImage;

        try {
            // get the file from Autopsy
            inputStream = new ReadContentInputStream(file);
            // load the file as a BufferedImage
            bufferedImage = ImageIO.read(inputStream);

            // check the results of the load operation and copy the results to a FaceRadarDetectedImage
            if (bufferedImage != null) {
                detectedImage = new FaceRadarDetectedImage();
                
                // scan the image for faces
                FaceRadarFaceDetector f = new FaceRadarFaceDetector();
                
                //works to return images
                //return detectedImage;
                
                detectedImage.setHasFace(f.detectFaces(bufferedImage));

                if (detectedImage.getHasFace()) 
                    return detectedImage;
                else
                    return null;
                   
            } 
            else return null;
            
        } catch (Exception e) {
            logger_.log(Level.WARNING,
                    "Error scanning image for faces: " + e.toString());
        }

        return null;
    }
   
}
