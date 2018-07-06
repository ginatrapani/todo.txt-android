/**
 * This file is part of Todo.txt for Android, an app for managing your todo.txt file (http://todotxt.com).
 * <p>
 * Copyright (c) 2009-2013 Todo.txt for Android contributors (http://todotxt.com)
 * <p>
 * LICENSE:
 * <p>
 * Todo.txt for Android is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 * <p>
 * Todo.txt for Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with Todo.txt for Android. If not, see
 * <http://www.gnu.org/licenses/>.
 * <p>
 * Todo.txt for Android's source code is available at https://github.com/ginatrapani/todo.txt-android
 *
 * @author Todo.txt for Android contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2013 Todo.txt for Android contributors (http://todotxt.com)
 */

package com.todotxt.todotxttouch.remote;

import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadUploader;
import com.dropbox.core.v2.files.WriteMode;
import com.todotxt.todotxttouch.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

public class DropboxFileUploader {
    final static String TAG = DropboxFileUploader.class.getSimpleName();

    private DbxClientV2 dbxClient;
    private DropboxFileStatus status;
    private Collection<DropboxFile> files;
    private boolean overwrite;

    /**
     * @param files
     */
    public DropboxFileUploader(DbxClientV2 client,
                               Collection<DropboxFile> files, boolean overwrite) {
        this.dbxClient = client;
        this.files = files;
        this.overwrite = overwrite;
        status = DropboxFileStatus.INITIALIZED;
    }

    /**
     * @return the status
     */
    public DropboxFileStatus getStatus() {
        return status;
    }

    /**
     * @return the files
     */
    public Collection<DropboxFile> getFiles() {
        return files;
    }

    public void pushFiles() {
        status = DropboxFileStatus.STARTED;

        Log.d(TAG, "pushFiles started");

        // load each metadata
        for (DropboxFile file : files) {
            loadMetadata(file);
        }

        // upload each file that has changed
        for (DropboxFile file : files) {
            if (file.getStatus() == DropboxFileStatus.FOUND
                    || file.getStatus() == DropboxFileStatus.NOT_FOUND) {
                uploadFile(file);
            }
        }

        status = DropboxFileStatus.SUCCESS;
    }

    private void loadMetadata(DropboxFile file) {
        Log.d(TAG, "Loading metadata for " + file.getRemoteFile());

        Metadata metadata = null;

        try {
            metadata = metadata = dbxClient.files().getMetadata(file.getRemoteFile());
        } catch (GetMetadataErrorException mde) {
            if (mde.errorValue.isPath() && mde.errorValue.getPathValue().isNotFound()) {
                Log.d(TAG, "metadata NOT found! Returning NOT_FOUND status.");

                file.setStatus(DropboxFileStatus.NOT_FOUND);

                return;
            }
            throw new RemoteException("Server Exception: " + mde.errorValue + " " + mde.getUserMessage(), mde);
        } catch (DbxException e) {
            throw new RemoteException("Dropbox Exception: " + e.getMessage(), e);
        }

        DeletedMetadata deletedMetadata = null;
        if(metadata instanceof DeletedMetadata){
            deletedMetadata = (DeletedMetadata) metadata;
        }

        FileMetadata fileMetadata = null;
        if(metadata instanceof FileMetadata) {
            file.setLoadedMetadata((FileMetadata) metadata);
            fileMetadata = (FileMetadata) metadata;
            Log.d(TAG, "Metadata retrieved. rev on Dropbox = " + fileMetadata.getRev());
        }

        Log.d(TAG, "local rev = " + file.getOriginalRev());

        if (deletedMetadata != null) {
            Log.d(TAG, "File marked as deleted on Dropbox! Returning NOT_FOUND status.");

            file.setStatus(DropboxFileStatus.NOT_FOUND);
        } else {
            file.setLoadedMetadata(fileMetadata);

            if (!overwrite && !fileMetadata.getRev().equals(file.getOriginalRev())) {
                Log.d(TAG, "revs don't match! Returning CONFLICT status.");

                file.setStatus(DropboxFileStatus.CONFLICT);

                throw new RemoteConflictException("Local file "
                        + file.getRemoteFile()
                        + " conflicts with remote version.");
            } else {
                Log.d(TAG, "revs match (or we're forcing the upload). returning FOUND status.");

                file.setStatus(DropboxFileStatus.FOUND);
            }
        }
    }

    private void uploadFile(DropboxFile file) {
        Log.d(TAG, "Uploading " + file.getRemoteFile());

        File localFile = file.getLocalFile();

        try {
            if (!localFile.exists()) {
                Util.createParentDirectory(localFile);
                localFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RemoteException("Failed to ensure that file exists", e);
        }

        String rev = null;

        if (file.getLoadedMetadata() != null) {
            rev = file.getLoadedMetadata().getRev();
        }

        Log.d(TAG, "Sending parent_rev = " + rev);

        FileMetadata metadata = null;
        try (FileInputStream inputStream = new FileInputStream(localFile)) {
            metadata = dbxClient.files().uploadBuilder(file.getRemoteFile())
                .withMode(WriteMode.OVERWRITE)
                .withClientModified(new Date(localFile.lastModified()))
                .uploadAndFinish(inputStream);
        }
        catch (FileNotFoundException e1) {
            throw new RemoteException("File " + localFile.getAbsolutePath()
                    + " not found", e1);
        } catch (DbxException e) {
            e.printStackTrace();

            throw new RemoteException("Something went wrong while uploading: " + e.getMessage(), e);
        } catch (IOException e) {
            e.printStackTrace();

            throw new RemoteException("Problem with IO", e);
        }

        Log.d(TAG, "Upload succeeded. new rev = " + metadata.getRev() + ". path = " + metadata.getPathDisplay());

        file.setLoadedMetadata(metadata);

        if (!metadata.getPathDisplay().equalsIgnoreCase(file.getRemoteFile())) {
            // If the uploaded remote path does not match our expected
            // remotePath,
            // then a conflict occurred and we should announce the conflict to
            // the user.
            Log.d(TAG, "upload created new file! Returning CONFLICT status.");

            file.setStatus(DropboxFileStatus.CONFLICT);

            throw new RemoteConflictException("Local file "
                    + file.getRemoteFile() + " conflicts with remote version.");
        } else {
            Log.d(TAG, "Returning SUCCESS status");

            file.setStatus(DropboxFileStatus.SUCCESS);
        }
    }
}
