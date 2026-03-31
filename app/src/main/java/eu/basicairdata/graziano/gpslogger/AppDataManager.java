/*
 * AppDataManager - Java Class for Android
 * Created by G.Capelli on 14/3/2026
 * This file is part of BasicAirData GPS Logger
 *
 * Copyright (C) 2011 BasicAirData
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.basicairdata.graziano.gpslogger;

import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A class to manage some operations with the app data.
 * We use it for example to export and import the database of the tracks.
 */
public class AppDataManager {

    public boolean isLastOperationSuccessful = false;                                        // The outcome of the last operation carried out

    private String appDataRootFolder = "/data/data/eu.basicairdata.graziano.gpslogger";
    private String zipFileFolder = GPSApplication.getInstance().getPrefExportFolder();



    /**
     * It adds a specific folder recursively to a zip file.
     *
     * @param srcFolder The folder to add
     * @param destZipFile The zip file
     */
    private void zipFolder(String srcFolder, OutputStream destZipFile)
            throws Exception {
        ZipOutputStream zip = null;
        OutputStream fileWriter = null;
        fileWriter = destZipFile;
        zip = new ZipOutputStream(fileWriter);
        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    /**
     * It adds a specific file to a zip file.
     *
     * @param path The path of the file to add
     * @param srcFile The name of the file
     * @param zip The zip file
     */
    private void addFileToZip(String path, String srcFile,
                                     ZipOutputStream zip) throws Exception {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            try {
                if ((srcFile.contains("/databases/") || srcFile.contains("/Thumbnails/")) && (!srcFile.endsWith("-journal"))) {
                    FileInputStream in = new FileInputStream(srcFile);
                    zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                    Log.w("myApp", "[#] AppDataManager.java - Adding file " + path + "/" + folder.getName());
                    while ((len = in.read(buf)) > 0) {
                        zip.write(buf, 0, len);
                    }
                    if (srcFile.endsWith("GPSLogger")) {
                        isLastOperationSuccessful = true;
                        Log.w("myApp", "[#] AppDataManager.java - The database \"GPSLogger\" has been successfully backupped");
                    }
                }
            } catch (FileNotFoundException e) {

            }
        }
    }

    /**
     * It adds a specific folder to a zip file.
     *
     * @param path The path of the file to add
     * @param srcFolder The name of the file
     * @param zip The zip file
     */
    private void addFolderToZip(String path, String srcFolder,
                                       ZipOutputStream zip) throws Exception {
        // java.lang.RuntimeException: java.io.FileNotFoundException:
        // /data/data/eu.basicairdata.graziano.gpslogger/code_cache/.studio/.canary: open failed: EACCES (Permission denied)
        if (!srcFolder.endsWith("/code_cache") && !srcFolder.endsWith("/cache")) {
            Log.w("myApp", "[#] AppDataManager.java - Adding folder " + srcFolder);
            File folder = new File(srcFolder);
            for (String fileName : folder.list()) {
                if (path.equals("")) {
                    addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
                } else {
                    addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
                }
            }
        }
    }

    /**
     * It exports the app data folder to a zip file into the exporting folder.
     * It creates a single zip file of the whole /data/data/eu.basicairdata.graziano.gpslogger folder.
     * This is the method used for Android API 14 to 18. API 19+ uses the other method:
     * exportAppDataToZipFile(Uri zipDocumentUri)
     */
    public void exportAppDataToZipFile_API14() {
        isLastOperationSuccessful = false;
        String backupFileName = "BACKUP GPSLogger Tracklist.zip";
        DocumentFile pickedDir;
        pickedDir = DocumentFile.fromFile(new File(zipFileFolder));
        DocumentFile zipDocumentFile = pickedDir.findFile(backupFileName);
        if ((zipDocumentFile != null) && (zipDocumentFile.exists())) zipDocumentFile.delete();
        zipDocumentFile = pickedDir.createFile("", backupFileName);

        try {
            OutputStream outputStream = GPSApplication.getInstance().getContentResolver().openOutputStream(zipDocumentFile.getUri(), "w");
            zipFolder(appDataRootFolder, outputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            Log.w("myApp", "[#] AppDataManager.java - UNABLE TO CREATE THE ZIP FILE into " + zipFileFolder);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            Log.w("myApp", "[#] AppDataManager.java - UNABLE TO CREATE THE ZIP FILE into" + zipFileFolder);
            throw new RuntimeException(e);
        }
    }

    /**
     * It exports the app data folder to the zip file specified as parameter.
     * It creates a single zip file of the whole /data/data/eu.basicairdata.graziano.gpslogger folder.
     * * @param zipDocumentUri The Uri of the new ZIP file
     */
    public void exportAppDataToZipFile(Uri zipDocumentUri) {
        isLastOperationSuccessful = false;

        try {
            DocumentFile zipDocumentFile;
            zipDocumentFile = DocumentFile.fromSingleUri(GPSApplication.getInstance(), zipDocumentUri);
            OutputStream outputStream = GPSApplication.getInstance().getContentResolver().openOutputStream(zipDocumentFile.getUri(), "w");
            zipFolder(appDataRootFolder, outputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            Log.w("myApp", "[#] AppDataManager.java - UNABLE TO CREATE THE ZIP FILE " + zipDocumentUri.toString());
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            Log.w("myApp", "[#] AppDataManager.java - UNABLE TO CREATE THE ZIP FILE " + zipDocumentUri.toString());
            throw new RuntimeException(e);
        }
    }

    /**
     * It imports the Database and the Thumbnails from the zip file specified as parameter into the private data folder of the app.
     * * @param zipDocumentUri The Uri of the ZIP file
     */
    public void importTracklistFromZipFile(Uri zipDocumentUri) {
        isLastOperationSuccessful = false;
        try {
            DocumentFile zipDocumentFile = getDocumentFile(zipDocumentUri);

            // Open InputStream from the DocumentFile
            InputStream inputStream = GPSApplication.getInstance().getBaseContext().getContentResolver().openInputStream(zipDocumentFile.getUri());
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));

            ZipEntry ze = null;

            // Check if the archive contains the database and the thumbnails folder
            boolean isThumbnailsPresent = false;
            boolean isDatabasePresent = false;
            while ((ze = zipInputStream.getNextEntry()) != null) {
                if (ze.getName().contains("/Thumbnails/") && !isThumbnailsPresent) {
                    Log.w("myApp", "[#] AppDataManager.java - Thumbnails folder found");
                    isThumbnailsPresent = true;
                }
                if (ze.getName().endsWith("/GPSLogger") && !isDatabasePresent) {
                    Log.w("myApp", "[#] AppDataManager.java - Database found");
                    isDatabasePresent = true;
                }
            }
            zipInputStream.close();
            inputStream.close();

            // If the ZIP file is valid, restore the Tracklist files
            if (isDatabasePresent) {
                inputStream = GPSApplication.getInstance().getBaseContext().getContentResolver().openInputStream(zipDocumentFile.getUri());
                zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));

                // Delete all the existing Thumbnails
                File folderThumbnails = new File( GPSApplication.getInstance().getFilesDir() + "/Thumbnails");
                Log.w("myApp", "[#] AppDataManager.java - Thumbnail Folder: " + folderThumbnails.getPath());
                if (folderThumbnails.isDirectory())
                {
                    String[] children = folderThumbnails.list();
                    for (int i = 0; i < children.length; i++)
                    {
                        new File(folderThumbnails, children[i]).delete();
                    }
                }

                // Delete the existing Database
                File folderDatabase = new File(GPSApplication.getInstance().getDatabasePath("GPSLogger").getParent());
                Log.w("myApp", "[#] AppDataManager.java - Databases Folder: " + folderDatabase.getPath());
                if (folderDatabase.isDirectory())
                {
                    String[] children = folderDatabase.list();
                    for (int i = 0; i < children.length; i++)
                    {
                        new File(folderDatabase, children[i]).delete();
                    }
                }

                // the ZIP file is valid
                Log.w("myApp", "[#] AppDataManager.java - The ZIP file is valid, Restoring...");
                while ((ze = zipInputStream.getNextEntry()) != null) {
                    // Import a Thumbnail
                    if (ze.getName().contains("/Thumbnails/")) {
                        Log.w("myApp", "[#] AppDataManager.java - UNZIP Thumbnail: " + ze.getName().substring(ze.getName().lastIndexOf("/") + 1));
                        byte[] buffer = new byte[1024];
                        FileOutputStream fout = new FileOutputStream(folderThumbnails + "/" + ze.getName().substring(ze.getName().lastIndexOf("/") + 1));
                        int i = 0;
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fout.write(buffer, 0, len);
                        }
                        zipInputStream.closeEntry();
                        fout.close();
                    }
                    // Import the Database
                    if (ze.getName().endsWith("/GPSLogger")) {
                        Log.w("myApp", "[#] AppDataManager.java - UNZIP Database: " + ze.getName().substring(ze.getName().lastIndexOf("/") + 1));
                        byte[] buffer = new byte[1024];
                        FileOutputStream fout = new FileOutputStream(folderDatabase + "/" + ze.getName().substring(ze.getName().lastIndexOf("/") + 1));
                        int i = 0;
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fout.write(buffer, 0, len);
                        }
                        zipInputStream.closeEntry();
                        fout.close();
                        isLastOperationSuccessful = true;
                    }
                }
                zipInputStream.close();
                inputStream.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Nullable
    private static DocumentFile getDocumentFile(Uri zipDocumentUri) {
        DocumentFile zipDocumentFile;
        zipDocumentFile = DocumentFile.fromSingleUri(GPSApplication.getInstance(), zipDocumentUri);
        return zipDocumentFile;
    }
}