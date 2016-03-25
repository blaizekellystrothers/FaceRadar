/*
 *  FaceRadar
 *  Copyright (C) 2015 Blaize Strothers
 *  
 *  Derived from OpenCV tutorial
 */
package com.sazquatch.faceradar;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

public class FaceRadarFaceDetector {

    public boolean detectFaces(BufferedImage bufferedImage) {
        boolean hasFace = false;
        //ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);

        // Create a face detector from the cascade file in the resources directory
        // By copying the face detection xml resource to a temporary local file
        // Because we can't load resources directly from the JAR
        File tempCascadefile = null;
        String resource = "haarcascade_frontalface_alt.xml";
        //"haarcascade_frontalface_alt.xml"
        //"lbpcascade_frontalface.xml"
        URL resourceURL = getClass().getResource("haarcascade_frontalface_alt.xml");
        if (resourceURL.toString().startsWith("jar:")) {
            try {
                InputStream input = getClass().getResourceAsStream(resource);
                tempCascadefile = File.createTempFile(new Date().getTime() + "", ".xml");
                OutputStream out = new FileOutputStream(tempCascadefile);
                int read;
                byte[] bytes = new byte[1024];

                while ((read = input.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                out.flush();
                out.close();
                input.close();
                tempCascadefile.deleteOnExit();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // Use the temporary local file to create the face detection CascadeClassifier
        CascadeClassifier faceDetector = new CascadeClassifier(tempCascadefile.getPath());

        // Convert the Java BufferedImage to OpenCV Mat
        byte[] bufferedImagePixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

        Mat bufferedImageAsMat = new Mat(bufferedImage.getHeight(), bufferedImage.getWidth(), CvType.CV_8UC3);
        //Mat bufferedImageAsMat = new Mat(480, 640, CvType.CV_8UC3);
        bufferedImageAsMat.put(0, 0, bufferedImagePixels);

        // Detect faces in the image
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(bufferedImageAsMat, faceDetections);
        //assert !faceDetections.empty();
        //System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
        // Draw a bounding box around each face
        //for (Rect rect : faceDetections.toArray()) {
        //    Imgproc.rectangle(img, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
        //}
        Rect[] faceDetectionsArray = faceDetections.toArray();
        if (faceDetectionsArray.length == 0) {
            hasFace = false;
        } else {
            hasFace = true;
        }

        return hasFace;
    }

}
