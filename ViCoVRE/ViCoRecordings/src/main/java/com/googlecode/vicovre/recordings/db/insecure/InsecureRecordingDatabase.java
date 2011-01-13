/**
 * Copyright (c) 2009, University of Manchester
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the name of the and the University of Manchester nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.googlecode.vicovre.recordings.db.insecure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.xml.sax.SAXException;

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.DefaultLayout;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.PlaybackManager;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.ExtensionFilter;

/**
 * A database for recordings
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class InsecureRecordingDatabase implements RecordingDatabase {

    private static final String VENUE_SERVER_FILE = "venueServers.xml";

    private Vector<String> knownVenueServers = new Vector<String>();

    private InsecureFolder topLevelFolder = null;

    private HashMap<File, InsecureFolder> folderCache =
        new HashMap<File, InsecureFolder>();

    private RtpTypeRepository typeRepository = null;

    private LayoutRepository layoutRepository = null;

    private HarvestFormatRepository harvestFormatRepository = null;

    private boolean readOnly = false;

    private long defaultRecordingLifetime = 0;

    private Vector<HarvestSourceListener> harvestSourceListeners =
        new Vector<HarvestSourceListener>();

    private Vector<UnfinishedRecordingListener> unfinishedRecordingListeners =
        new Vector<UnfinishedRecordingListener>();

    private Vector<RecordingListener> recordingListeners =
        new Vector<RecordingListener>();

    /**
     * Creates a Database
     * @param directory The directory containing the database
     * @param typeRepository The type repository
     * @param layoutRepository The layout repository
     * @throws SAXException
     * @throws IOException
     */
    public InsecureRecordingDatabase(String directory,
            RtpTypeRepository typeRepository,
            LayoutRepository layoutRepository,
            HarvestFormatRepository harvestFormatRepository, boolean readOnly,
            long defaultRecordingLifetime)
            throws SAXException, IOException {
        this.typeRepository = typeRepository;
        this.layoutRepository = layoutRepository;
        this.harvestFormatRepository = harvestFormatRepository;
        this.readOnly = readOnly;
        this.defaultRecordingLifetime = defaultRecordingLifetime;

        File topLevel = new File(directory);
        topLevel.mkdirs();
        topLevelFolder = new InsecureFolder(topLevel, "", typeRepository,
                layoutRepository, harvestFormatRepository, readOnly,
                defaultRecordingLifetime);
        folderCache.put(topLevelFolder.getFile(), topLevelFolder);
        traverseFolders(topLevelFolder);

        File venueServerFile = new File(topLevel, VENUE_SERVER_FILE);
        if (venueServerFile.exists()) {
            FileInputStream input = new FileInputStream(venueServerFile);
            String[] venueServers = VenueServerReader.readVenueServers(input);
            for (String server : venueServers) {
                knownVenueServers.add(server);
            }
            input.close();
        }
    }

    private InsecureFolder getFolder(File path) {
        if (path == null || path.equals(topLevelFolder.getFile())) {
            return topLevelFolder;
        }
        InsecureFolder folder = folderCache.get(path);
        if (folder == null) {
            if (path.exists() && path.isDirectory()) {
                File recordingIndex = new File(path,
                        RecordingConstants.RECORDING_INDEX);
                File recordingInProgress = new File(path,
                        RecordingConstants.RECORDING_INPROGRESS);

                if (!recordingIndex.exists()
                        && !recordingInProgress.exists()) {
                    String folderName = path.getAbsolutePath().substring(
                        topLevelFolder.getFile().getAbsolutePath().length());
                    folderName = folderName.replace('\\', '/');
                    folder = new InsecureFolder(path, folderName,
                        typeRepository, layoutRepository,
                        harvestFormatRepository, readOnly,
                        defaultRecordingLifetime);
                    File metadataFile = new File(path,
                            RecordingConstants.FOLDER_METADATA);
                    if (metadataFile.exists()) {
                        try {
                            FileInputStream input = new FileInputStream(
                                    metadataFile);
                            folder.setMetadata(MetadataReader.readMetadata(
                                    input));
                            input.close();
                        } catch (Exception e) {
                            System.err.println(
                                "Warning: error reading folder metadata: "
                                    + e.getMessage());
                        }
                    }
                    folderCache.put(path, folder);
                }
            }
        }
        return folder;
    }

    private void traverseFolders(InsecureFolder folder) {
        List<HarvestSource> harvestSources = folder.getHarvestSources();
        for (HarvestSource harvestSource : harvestSources) {
            for (HarvestSourceListener listener : harvestSourceListeners) {
                listener.sourceAdded(harvestSource);
            }
        }
        List<UnfinishedRecording> unfinishedRecordings =
            folder.getUnfinishedRecordings();
        for (UnfinishedRecording recording : unfinishedRecordings) {
            for (UnfinishedRecordingListener listener
                    : unfinishedRecordingListeners) {
                listener.recordingAdded(recording);
            }
        }
        List<Recording> recordings =
            folder.getRecordings();
        for (Recording recording : recordings) {
            for (RecordingListener listener : recordingListeners) {
                listener.recordingAdded(recording);
            }
        }
        for (String subFolderName : folder.getFolders()) {
            File subFile = new File(folder.getFile(), subFolderName);
            InsecureFolder subFolder = getFolder(subFile);
            traverseFolders(subFolder);
        }
    }

    public String[] getKnownVenueServers() {
        return knownVenueServers.toArray(new String[0]);
    }

    public void addVenueServer(String url) {
        if (!readOnly) {
            synchronized (knownVenueServers) {
                try {
                    if (!knownVenueServers.contains(url)) {
                        knownVenueServers.add(url);
                        File venueServerFile = new File(
                                topLevelFolder.getFile(),
                                VENUE_SERVER_FILE);
                        FileOutputStream output = new FileOutputStream(
                                venueServerFile);
                        VenueServerReader.writeVenueServers(
                            knownVenueServers.toArray(new String[0]), output);
                        output.close();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public File getFile(String folder) {
        if ((folder == null) || folder.equals("") || folder.equals("/")) {
            return topLevelFolder.getFile();
        }
        File file = new File(topLevelFolder.getFile(), folder);
        return file;
    }

    public List<String> getSubFolders(String folder) {
        InsecureFolder f = getFolder(getFile(folder));
        if (f == null) {
            return null;
        }
        return f.getFolders();
    }

    private void editHarvestSource(HarvestSource harvestSource)
            throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        InsecureFolder folder = getFolder(getFile(harvestSource.getFolder()));
        folder.addHarvestSource(harvestSource);
        File file = new File(folder.getFile(), harvestSource.getId()
                + RecordingConstants.HARVEST_SOURCE);
        FileOutputStream output = new FileOutputStream(file);
        HarvestSourceReader.writeHarvestSource(harvestSource, output);
        output.close();
    }

    public void addHarvestSource(HarvestSource harvestSource)
            throws IOException {
        editHarvestSource(harvestSource);
        for (HarvestSourceListener listener : harvestSourceListeners) {
            listener.sourceAdded(harvestSource);
        }
    }

    public void deleteHarvestSource(HarvestSource harvestSource)
            throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        InsecureFolder folder = getFolder(getFile(harvestSource.getFolder()));
        folder.deleteHarvestSource(harvestSource.getId());
        File file = new File(folder.getFile(), harvestSource.getId()
                + RecordingConstants.HARVEST_SOURCE);
        file.delete();
        for (HarvestSourceListener listener : harvestSourceListeners) {
            listener.sourceDeleted(harvestSource);
        }
    }

    public void updateHarvestSource(HarvestSource harvestSource)
            throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        editHarvestSource(harvestSource);
        for (HarvestSourceListener listener : harvestSourceListeners) {
            listener.sourceUpdated(harvestSource);
        }
    }

    public HarvestSource getHarvestSource(String folder, String id) {
        InsecureFolder f = getFolder(getFile(folder));
        if (f == null) {
            return null;
        }
        return f.getHarvestSource(id);
    }

    public List<HarvestSource> getHarvestSources(String folder) {
        InsecureFolder f = getFolder(getFile(folder));
        if (f == null) {
            return null;
        }
        return f.getHarvestSources();
    }

    private void editUnfinishedRecording(UnfinishedRecording recording,
            HarvestSource creator) throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        InsecureFolder folder = getFolder(getFile(recording.getFolder()));
        folder.addUnfinishedRecording(recording);
        File file = new File(folder.getFile(),
            recording.getId() + RecordingConstants.UNFINISHED_RECORDING_INDEX);
        FileOutputStream output = new FileOutputStream(file);
        UnfinishedRecordingReader.writeRecording(this, recording, output);
        output.close();
    }

    public void addUnfinishedRecording(UnfinishedRecording recording,
            HarvestSource creator) throws IOException {
        editUnfinishedRecording(recording, creator);
        for (UnfinishedRecordingListener listener
                : unfinishedRecordingListeners) {
            listener.recordingAdded(recording);
        }
    }

    public void finishUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        recording.stopRecording();
        InsecureFolder folder = getFolder(getFile(recording.getFolder()));
        folder.deleteUnfinishedRecording(recording.getId());
        File file = new File(folder.getFile(), recording.getId()
                + RecordingConstants.UNFINISHED_RECORDING_INDEX);
        File metadata = new File(folder.getFile(),
                recording.getId() + RecordingConstants.METADATA);
        file.delete();
        metadata.delete();
    }

    public void deleteUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        finishUnfinishedRecording(recording);
        for (UnfinishedRecordingListener listener
                : unfinishedRecordingListeners) {
            listener.recordingDeleted(recording);
        }
    }

    public void updateUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        editUnfinishedRecording(recording, null);
        for (UnfinishedRecordingListener listener
                : unfinishedRecordingListeners) {
            listener.recordingUpdated(recording);
        }
    }

    public UnfinishedRecording getUnfinishedRecording(String folder,
            String id) {
        InsecureFolder f = getFolder(getFile(folder));
        if (f == null) {
            return null;
        }
        return f.getUnfinishedRecording(id);
    }

    public List<UnfinishedRecording> getUnfinishedRecordings(String folder) {
        InsecureFolder f = getFolder(getFile(folder));
        if (f == null) {
            return null;
        }
        return f.getUnfinishedRecordings();
    }

    private void editRecording(Recording recording, UnfinishedRecording creator)
            throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        InsecureFolder folder = getFolder(getFile(recording.getFolder()));
        folder.addRecording(recording);
        FileOutputStream output = new FileOutputStream(
                new File(recording.getDirectory(),
                        RecordingConstants.RECORDING_INDEX));
        RecordingReader.writeRecording(recording, output);
        output.close();
    }

    public void addRecording(Recording recording, UnfinishedRecording creator)
            throws IOException {
        editRecording(recording, creator);
        for (RecordingListener listener : recordingListeners) {
            listener.recordingAdded(recording);
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        directory.delete();
    }

    public void deleteRecording(Recording recording) throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        InsecureFolder folder = getFolder(getFile(recording.getFolder()));
        folder.deleteRecording(recording.getId());
        deleteDirectory(recording.getDirectory());
        for (RecordingListener listener : recordingListeners) {
            listener.recordingDeleted(recording);
        }
    }

    public Recording getRecording(String folder, String id) {
        InsecureFolder f = getFolder(getFile(folder));
        if (f == null) {
            return null;
        }
        return f.getRecording(id);
    }

    public List<Recording> getRecordings(String folder) {
        InsecureFolder f = getFolder(getFile(folder));
        if (f == null) {
            return null;
        }
        return f.getRecordings();
    }

    public void updateRecordingMetadata(Recording recording)
            throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        File metadataFile = new File(recording.getDirectory(),
                RecordingConstants.METADATA);
        FileOutputStream outputStream = new FileOutputStream(metadataFile);
        MetadataReader.writeMetadata(recording.getMetadata(),
                outputStream);
        outputStream.close();
        for (RecordingListener listener : recordingListeners) {
            listener.recordingMetadataUpdated(recording);
        }
    }

    public void updateRecordingLayouts(Recording recording) throws IOException {
        if (readOnly) {
            throw new IOException("Cannot edit in read only mode");
        }
        File[] currentLayouts = recording.getDirectory().listFiles(
                new ExtensionFilter(RecordingConstants.LAYOUT));
        for (File layout : currentLayouts) {
            layout.delete();
        }
        for (ReplayLayout layout : recording.getReplayLayouts()) {
            File layoutOutput = new File(recording.getDirectory(),
                    layout.getName() + RecordingConstants.LAYOUT);
            FileOutputStream outputLayout = new FileOutputStream(layoutOutput);
            LayoutReader.writeLayout(layout, outputLayout);
            outputLayout.close();
        }
        for (RecordingListener listener : recordingListeners) {
            listener.recordingLayoutsUpdated(recording);
        }
    }

    public void updateRecordingLifetime(Recording recording)
            throws IOException {
        LifetimeReader.writeLifetime(recording.getDirectory(),
                recording.getLifetime());
        for (RecordingListener listener : recordingListeners) {
            listener.recordingLifetimeUpdated(recording);
        }
    }

    public void shutdown() {
        System.err.println("Shutting down PlaybackManager...");
        PlaybackManager.shutdown();
        System.err.println("Database Shutdown Complete");
    }

    private void traverseHarvestSources(InsecureFolder folder,
            HarvestSourceListener listener) {
        for (HarvestSource harvestSource : folder.getHarvestSources()) {
            listener.sourceAdded(harvestSource);
        }
        for (String subFolderName : folder.getFolders()) {
            File subFile = new File(folder.getFile(), subFolderName);
            InsecureFolder subFolder = getFolder(subFile);
            traverseHarvestSources(subFolder, listener);
        }
    }

    public void addHarvestSourceListener(HarvestSourceListener listener) {
        harvestSourceListeners.add(listener);
        traverseHarvestSources(topLevelFolder, listener);
    }

    private void traverseUnfinishedRecordings(InsecureFolder folder,
            UnfinishedRecordingListener listener) {
        for (UnfinishedRecording recording : folder.getUnfinishedRecordings()) {
            listener.recordingAdded(recording);
        }
        for (String subFolderName : folder.getFolders()) {
            File subFile = new File(folder.getFile(), subFolderName);
            InsecureFolder subFolder = getFolder(subFile);
            traverseUnfinishedRecordings(subFolder, listener);
        }
    }

    public void addUnfinishedRecordingListener(
            UnfinishedRecordingListener listener) {
        unfinishedRecordingListeners.add(listener);
        traverseUnfinishedRecordings(topLevelFolder, listener);
    }

    private void traverseRecordings(InsecureFolder folder,
            RecordingListener listener) {
        for (Recording recording : folder.getRecordings()) {
            listener.recordingAdded(recording);
        }
        for (String subFolderName : folder.getFolders()) {
            File subFile = new File(folder.getFile(), subFolderName);
            InsecureFolder subFolder = getFolder(subFile);
            traverseRecordings(subFolder, listener);
        }
    }

    public void addRecordingListener(RecordingListener listener) {
        recordingListeners.add(listener);
        traverseRecordings(topLevelFolder, listener);
    }

    public void setDefaultLayout(String folderName, DefaultLayout layout)
            throws IOException {
        InsecureFolder folder = getFolder(getFile(folderName));
        if (folder == null) {
            throw new IOException("Folder " + folderName + " not found");
        }
        File layoutFile = new File(folder.getFile(),
                layout.getName() + RecordingConstants.LAYOUT);
        FileOutputStream output = new FileOutputStream(layoutFile);
        DefaultLayoutReader.writeLayout(output, layout);
        output.close();

        List<Recording> recordings = folder.getRecordings();
        for (Recording recording : recordings) {
            if (recording.getReplayLayouts().isEmpty()) {
                ReplayLayout replayLayout = layout.matchLayout(recording);
                if (replayLayout != null) {
                    recording.setReplayLayout(replayLayout);
                }
            }
        }
    }

    public boolean addFolder(String parent, String folder) {
        File file = getFile(parent + "/" + folder);
        return file.mkdirs();
    }

    public void deleteFolder(String folder) throws IOException {
        File file = getFile(folder);
        if (file.isDirectory() && (file.listFiles().length == 0)) {
            file.delete();
        } else {
            throw new IOException("The folder must be empty to be deleted");
        }
    }

    public boolean canReadFolder(String folder) {
        return true;
    }

    public boolean canWriteFolder(String folder) {
        return true;
    }

    public Metadata getFolderMetadata(String folderPath) {
        InsecureFolder folder = getFolder(getFile(folderPath));
        if (folder != null) {
            return folder.getMetadata();
        }
        return null;
    }

    public void setFolderMetadata(String folderPath, Metadata metadata)
            throws IOException {
        File file = getFile(folderPath);
        InsecureFolder folder = getFolder(file);
        if (folder != null) {
            folder.setMetadata(metadata);
            FileOutputStream output = new FileOutputStream(
                    new File(file, RecordingConstants.FOLDER_METADATA));
            MetadataReader.writeMetadata(metadata, output);
            output.close();
        }
    }

    public void updateRecordingAnnotations(Recording recording)
            throws IOException {
        File annotationFile = new File(recording.getDirectory(),
                RecordingConstants.ANNOTATIONS);
        FileOutputStream output = new FileOutputStream(annotationFile);
        AnnotationsReader.writeAnnotations(output, recording.getAnnotations());
        output.close();
    }

    public void moveRecording(Recording recording, String newFolder)
            throws IOException {
        File path = recording.getDirectory();
        File newFolderFile = getFile(newFolder);
        if (!newFolderFile.exists()) {
            throw new IOException("The destination folder does not exist!");
        }
        if (path.renameTo(new File(newFolderFile, recording.getId()))) {
            InsecureFolder folder = getFolder(getFile(recording.getFolder()));
            InsecureFolder folderTo = getFolder(newFolderFile);
            folder.deleteRecording(recording.getId());
            for (RecordingListener listener : recordingListeners) {
                listener.recordingMoved(recording,
                        folderTo.getRecording(recording.getId()));
            }
        } else {
            throw new IOException("Unknown error moving recording");
        }
    }

    private void updateAnnotations(Recording recording) throws IOException {
        List<Annotation> annotations = recording.getAnnotations();
        File annotationFile = new File(recording.getDirectory(),
                 RecordingConstants.ANNOTATIONS);
        FileOutputStream outputStream = new FileOutputStream(
                annotationFile);
        AnnotationsReader.writeAnnotations(outputStream, annotations);
        outputStream.close();
    }

    public void addAnnotation(Recording recording, Annotation annotation)
            throws IOException {
        InsecureRecording insecureRecording = (InsecureRecording) getRecording(
                recording.getFolder(), recording.getId());
        insecureRecording.addAnnotation(annotation);
        updateAnnotations(recording);
    }

    public void deleteAnnotation(Recording recording, Annotation annotation)
            throws IOException {
        InsecureRecording insecureRecording = (InsecureRecording) getRecording(
                recording.getFolder(), recording.getId());
        insecureRecording.deleteAnnotation(annotation);
        updateAnnotations(recording);
    }

    public void updateAnnotation(Recording recording, Annotation annotation)
            throws IOException {
        InsecureRecording insecureRecording = (InsecureRecording) getRecording(
                recording.getFolder(), recording.getId());
        insecureRecording.updateAnnotation(annotation);
        updateAnnotations(recording);
    }
}
