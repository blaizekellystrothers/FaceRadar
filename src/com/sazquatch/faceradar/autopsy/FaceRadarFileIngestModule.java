/*
 *  FaceRadar
 *  Copyright (C) 2015 Blaize Strothers
 * 
 *  Derived from Sample Module provided with Autopsy 3.1 and SmutDetect4Autopsy by Rajmund Witt
 * 
 *  This is free and unencumbered software released into the public domain.
 *  
 *  Anyone is free to copy, modify, publish, use, compile, sell, or
 *  distribute this software, either in source code form or as a compiled
 *  binary, for any purpose, commercial or non-commercial, and by any
 *  means.
 *  
 *  In jurisdictions that recognize copyright laws, the author or authors
 *  of this software dedicate any and all copyright interest in the
 *  software to the public domain. We make this dedication for the benefit
 *  of the public at large and to the detriment of our heirs and
 *  successors. We intend this dedication to be an overt act of
 *  relinquishment in perpetuity of all present and future rights to this
 *  software under copyright law.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 *  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE. 
 */
package com.sazquatch.faceradar.autopsy;

import com.sazquatch.faceradar.FaceRadarDetectedImage;
import com.sazquatch.faceradar.FaceRadarImageScanner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import org.opencv.core.Core;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.ingest.FileIngestModule;
import org.sleuthkit.autopsy.ingest.IngestModule;
import org.sleuthkit.autopsy.ingest.IngestJobContext;
import org.sleuthkit.autopsy.ingest.IngestMessage;
import org.sleuthkit.autopsy.ingest.IngestServices;
import org.sleuthkit.autopsy.ingest.ModuleDataEvent;
import org.sleuthkit.autopsy.ingest.IngestModuleReferenceCounter;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.TskData;

public class FaceRadarFileIngestModule implements FileIngestModule {

    private static final HashMap<Long, Long> artifactCountsForIngestJobs = new HashMap<>();
    private static int attrId = -1;
    private final boolean skipKnownFiles;
    private IngestJobContext context = null;
    private static final IngestModuleReferenceCounter refCounter = new IngestModuleReferenceCounter();
    //private int count = 0;

    FaceRadarFileIngestModule(FaceRadarIngestJobSettings settings) {
        this.skipKnownFiles = settings.skipKnownFiles();
        System.loadLibrary("opencv_java300");
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
         ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
         
    }

    @Override
    public void startUp(IngestJobContext context) throws IngestModuleException {
        this.context = context;
        refCounter.incrementAndGet(context.getJobId());
    }

    @Override
    public ProcessResult process(AbstractFile file) {
//        if (attrId == -1) {
//            return IngestModule.ProcessResult.ERROR;
//        }

        // Skip anything other than actual file system files.
        if ((file.getType() == TskData.TSK_DB_FILES_TYPE_ENUM.UNALLOC_BLOCKS)
                || (file.getType() == TskData.TSK_DB_FILES_TYPE_ENUM.UNUSED_BLOCKS)
                || (file.isFile() == false)) {
            return ProcessResult.OK;
        }

        // Skip NSRL / known files.
        if (skipKnownFiles && file.getKnown() == TskData.FileKnown.KNOWN) {
            return ProcessResult.OK;
        }

        // Skip unsupported file types
        if (!isImageFile(file)) {
            return ProcessResult.OK;
        }

        try {

            FaceRadarDetectedImage image = FaceRadarImageScanner.scanImage(file);

            if (image != null) {

                //count++;

                BlackboardArtifact artifact = file.newArtifact(BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT);
                BlackboardAttribute attribute = new BlackboardAttribute(BlackboardAttribute.ATTRIBUTE_TYPE.TSK_SET_NAME.getTypeID(), FaceRadarIngestModuleFactory.getModuleName(), "FaceRadar Detected Faces");
                artifact.addAttribute(attribute);

                // This method is thread-safe with per ingest job reference counted
                // management of shared data.
                addToBlackboardPostCount(context.getJobId(), 1L);

                // Fire an event to notify any listeners for blackboard postings.
                ModuleDataEvent event = new ModuleDataEvent(FaceRadarIngestModuleFactory.getModuleName(), BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT);
                IngestServices.getInstance().fireModuleDataEvent(event);
            }
            return IngestModule.ProcessResult.OK;

        } catch (TskCoreException ex) {
            IngestServices ingestServices = IngestServices.getInstance();
            Logger logger = ingestServices.getLogger(FaceRadarIngestModuleFactory.getModuleName());
            logger.log(Level.SEVERE, "Error processing file (id = " + file.getId() + ")", ex);
            return IngestModule.ProcessResult.ERROR;
        }
    }

    @Override
    public void shutDown() {
        // This method is thread-safe with per ingest job reference counted
        // management of shared data.
        reportBlackboardPostCount(context.getJobId());
    }

    synchronized static void addToBlackboardPostCount(long ingestJobId, long countToAdd) {
        Long fileCount = artifactCountsForIngestJobs.get(ingestJobId);

        // Ensures that this job has an entry
        if (fileCount == null) {
            fileCount = 0L;
            artifactCountsForIngestJobs.put(ingestJobId, fileCount);
        }

        fileCount += countToAdd;
        artifactCountsForIngestJobs.put(ingestJobId, fileCount);
    }

    synchronized static void reportBlackboardPostCount(long ingestJobId) {
        Long refCount = refCounter.decrementAndGet(ingestJobId);
        if (refCount == 0) {
            Long filesCount = artifactCountsForIngestJobs.remove(ingestJobId);
            String msgText = (filesCount == null) ? "Posted 0 times to the blackboard" : String.format("Posted %d times to the blackboard", filesCount);
            IngestMessage message = IngestMessage.createMessage(
                    IngestMessage.MessageType.INFO,
                    FaceRadarIngestModuleFactory.getModuleName(),
                    msgText);
            IngestServices.getInstance().postMessage(message);
        }
    }

    /**
     * Checks if should try to attempt to scan for faces. Currently checks if
     * JPEG, BMP, PNG, GIF or TIFF image (by signature)
     *
     * @param f file to be checked
     *
     * @return true if to be processed
     *
     * taken from SmutDetect4Autopsy by Rajmund Witt
     */
    private boolean isImageFile(AbstractFile f) {
        return hasImageFileHeader(f);

    }

    /**
     * Check if is image file based on header, does not parse files less than
     * 100 bytes.
     *
     * @param file
     *
     * @return true if image file, false otherwise
     *
     * taken from SmutDetect4Autopsy by Rajmund Witt
     */
    @SuppressWarnings("unchecked")
    private static boolean hasImageFileHeader(AbstractFile file) {

        // if less than 100 bytes, do not parse
        if (file.getSize() < 100) {
            return false;
        }

        // read bytes if unable do not parse
        byte[] fileHeaderBuffer = new byte[6];
        int bytesRead;
        try {
            bytesRead = file.read(fileHeaderBuffer, 0, 6);
        } catch (TskCoreException ex) {
            //ignore if can't read the first few bytes, not a JPEG
            return false;
        }
        if (bytesRead != 6) {
            return false;
        }
        /**
         * Check for the Image file headers Starting with most likely image
         * files first. Since Java bytes are signed, we cast them to an int
         * first.
         *
         * @TODO add config check if all image types are to be scanned
         *
         * Some more signatures from:
         * http://www.garykessler.net/library/file_sigs.html
         *
         * FF D8 FF ÿØÿ any JPG 00 00 00 00 6A 50 ....jP a JPEG 2000 file 47 49
         * 46 38 37 61 GIF87a A Gif File 47 49 46 38 39 61 GIF89a A Gif File 49
         * 20 49 I I TIF, TIFF - Tagged Image File Format file 49 49 2A 00 II*.
         * TIF, TIFF - Tagged Image File Format file (little endian, i.e., LSB
         * first in the byte; Intel) 4D 4D 00 2A MM.* TIF, TIFF - Tagged Image
         * File Format file (big endian, i.e., LSB last in the byte; Motorola)
         * 4D 4D 00 2B MM.+ TIF, TIFF - BigTIFF files; Tagged Image File Format
         * files >4 GB 89 50 4E 47 0D 0A 1A 0A ‰PNG.... PNG - Portable Network
         * Graphics 42 4D BM BMP - Windows Bitmap image
         *
         */
        if ( // if JPG [FF D8 FF]   
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0xFF) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0xD8)
                && ((int) (fileHeaderBuffer[2] & 0xFF) == 0xFF))
                || //or if BMP [42 4D]
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0x42) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0x4d))
                || // or if TIFF [49 20 49]
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0x49) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0x20)
                && ((int) (fileHeaderBuffer[2] & 0xFF) == 0x49))
                || // or if PNG [89 50 4E 47]; not testing for the 4 dots
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0x89) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0x50)
                && ((int) (fileHeaderBuffer[2] & 0xFF) == 0x4E) && ((int) (fileHeaderBuffer[3] & 0xFF) == 0x47))
                || // or if GIF [47 49 46 38]; not testing for subtypes 7a/9a
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0x47) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0x49)
                && ((int) (fileHeaderBuffer[2] & 0xFF) == 0x46) && ((int) (fileHeaderBuffer[3] & 0xFF) == 0x38))
                || // or if TIFF [49 49 2A 00]
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0x49) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0x49)
                && ((int) (fileHeaderBuffer[2] & 0xFF) == 0x2A) && ((int) (fileHeaderBuffer[3] & 0xFF) == 0x00))
                || // or if TIFF [4D 4D 00 2A]
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0x4D) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0x4D)
                && ((int) (fileHeaderBuffer[2] & 0xFF) == 0x00) && ((int) (fileHeaderBuffer[3] & 0xFF) == 0x2A))
                || // or if TIFF [4D 4D 00 2B]
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0x4D) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0x4D)
                && ((int) (fileHeaderBuffer[2] & 0xFF) == 0x00) && ((int) (fileHeaderBuffer[3] & 0xFF) == 0x2B))
                || // or if JPEG 2000 [00 00 00 00 6A 50]
                (((int) (fileHeaderBuffer[0] & 0xFF) == 0x00) && ((int) (fileHeaderBuffer[1] & 0xFF) == 0x00)
                && ((int) (fileHeaderBuffer[2] & 0xFF) == 0x00) && ((int) (fileHeaderBuffer[3] & 0xFF) == 0x00)
                && ((int) (fileHeaderBuffer[4] & 0xFF) == 0x6A) && ((int) (fileHeaderBuffer[5] & 0xFF) == 0x50))) {
            return true;
        } // end if is Image header
        return false;
    }

    
}
