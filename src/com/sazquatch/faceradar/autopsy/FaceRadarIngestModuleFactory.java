/*
 *  FaceRadar
 *  Copyright (C) 2015 Blaize Strothers
 * 
 *  Derived from Sample Module provided with Autopsy 3.1.
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

import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.sleuthkit.autopsy.coreutils.Version;
import org.sleuthkit.autopsy.ingest.DataSourceIngestModule;
import org.sleuthkit.autopsy.ingest.FileIngestModule;
import org.sleuthkit.autopsy.ingest.IngestModuleFactory;
import org.sleuthkit.autopsy.ingest.IngestModuleGlobalSettingsPanel;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettings;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettingsPanel;

@ServiceProvider(service = IngestModuleFactory.class) // Sample is discarded at runtime 
public class FaceRadarIngestModuleFactory implements IngestModuleFactory {

    private static final String VERSION_NUMBER = Version.getVersion();

    // This class method allows the ingest module instances created by this 
    // factory to use the same display name that is provided to the Autopsy
    // ingest framework by the factory.
    static String getModuleName() {
        return NbBundle.getMessage(FaceRadarIngestModuleFactory.class, "FaceRadarIngestModuleFactory.moduleName");
    }

    @Override
    public String getModuleDisplayName() {
        return getModuleName();
    }

    @Override
    public String getModuleDescription() {
        return NbBundle.getMessage(FaceRadarIngestModuleFactory.class, "FaceRadarIngestModuleFactory.moduleDescription");
    }

    @Override
    public String getModuleVersionNumber() {
        return VERSION_NUMBER;
    }

    @Override
    public boolean hasGlobalSettingsPanel() {
        return false;
    }

    @Override
    public IngestModuleGlobalSettingsPanel getGlobalSettingsPanel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IngestModuleIngestJobSettings getDefaultIngestJobSettings() {
        return new FaceRadarIngestJobSettings();
    }

    @Override
    public boolean hasIngestJobSettingsPanel() {
        return true;
    }

    @Override
    public IngestModuleIngestJobSettingsPanel getIngestJobSettingsPanel(IngestModuleIngestJobSettings settings) {
        if (!(settings instanceof FaceRadarIngestJobSettings)) {
            throw new IllegalArgumentException("Expected settings argument to be instance of FaceRadarIngestJobSettings");
        }
        return new FaceRadarIngestJobSettingsPanel((FaceRadarIngestJobSettings) settings);
    }

    @Override
    public boolean isDataSourceIngestModuleFactory() {
        return false;
    }

    /*
    @Override
    public DataSourceIngestModule createDataSourceIngestModule(IngestModuleIngestJobSettings ingestOptions) {
        return new SampleExecutableDataSourceIngestModule();
    }*/
    @Override
    public DataSourceIngestModule createDataSourceIngestModule(IngestModuleIngestJobSettings settings) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean isFileIngestModuleFactory() {
        return true;
    }

    @Override
    public FileIngestModule createFileIngestModule(IngestModuleIngestJobSettings settings) {
        if (!(settings instanceof FaceRadarIngestJobSettings)) {
            throw new IllegalArgumentException("Expected settings argument to be instance of FaceRadarIngestJobSettings");
        }
        return new FaceRadarFileIngestModule((FaceRadarIngestJobSettings) settings);
    }

}
