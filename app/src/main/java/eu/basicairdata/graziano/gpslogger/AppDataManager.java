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

import static eu.basicairdata.graziano.gpslogger.GPSApplication.getInstance;

import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

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

    String backupFileName = "BACKUP_GPSLogger_Tracklist.zip";

    private String appDataRootFolder = "/data/data/eu.basicairdata.graziano.gpslogger";
    private String zipFileFolder = GPSApplication.getInstance().getPrefExportFolder();

    /**
     * It exports the app data folder to a zip file into the exporting folder.
     * It creates a single zip file of the whole /data/data/eu.basicairdata.graziano.gpslogger folder.
     */
    public void exportAppDataToZipFile() {

        DocumentFile pickedDir;

        if (zipFileFolder.startsWith("content")) {
            Uri uri = Uri.parse(zipFileFolder);
            pickedDir = DocumentFile.fromTreeUri(getInstance(), uri);
        } else {
            pickedDir = DocumentFile.fromFile(new File(zipFileFolder));
        }
        if (!pickedDir.exists()) {
            Log.w("myApp", "[#] AppDataManager.java - UNABLE TO CREATE THE FOLDER");
            return;
        }

        DocumentFile zipDocumentFile = pickedDir.findFile( backupFileName);

        if ((zipDocumentFile != null) && (zipDocumentFile.exists())) zipDocumentFile.delete();
        zipDocumentFile = pickedDir.createFile("", backupFileName);

        try {
            OutputStream outputStream = GPSApplication.getInstance().getContentResolver().openOutputStream(zipDocumentFile.getUri(), "rw");
            zipFolder(appDataRootFolder, outputStream);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
            Log.w("myApp", "[#] AppDataManager.java - Adding file " + path + "/" + folder.getName());
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
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
        if (!srcFolder.endsWith("code_cache")) {
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


    //    // Example: Zipping a single file
//    public void zipFile(String sourcePath, OutputStream zipPath) throws IOException {
//        BufferedInputStream origin = null;
//        OutputStream dest = zipPath;
//        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
//
//        byte[] data = new byte[2048];
//        File file = new File(sourcePath);
//        FileInputStream fi = new FileInputStream(file);
//        origin = new BufferedInputStream(fi, 2048);
//
//        ZipEntry entry = new ZipEntry(file.getName());
//        out.putNextEntry(entry);
//        int count;
//        while ((count = origin.read(data, 0, 2048)) != -1) {
//            out.write(data, 0, count);
//        }
//        origin.close();
//        out.close();
//    }


    public void importTracklistFromZipFile() {
        try {
            DocumentFile zipDocumentFile;
            DocumentFile zipDocumentFileFolder;

            if (zipFileFolder.startsWith("content"))
                zipDocumentFileFolder = DocumentFile.fromTreeUri(GPSApplication.getInstance(), Uri.parse(zipFileFolder));
            else
                zipDocumentFileFolder = DocumentFile.fromFile(new File(zipFileFolder));

            zipDocumentFile = zipDocumentFileFolder.findFile(backupFileName);

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
            if (isDatabasePresent && isThumbnailsPresent) {
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
                        FileOutputStream fout = new FileOutputStream(folderThumbnails + "/" + ze.getName().substring(ze.getName().lastIndexOf("/") + 1));
                        for (int c = zipInputStream.read(); c != -1; c = zipInputStream.read()) {
                            fout.write(c);
                        }
                        zipInputStream.closeEntry();
                        fout.close();
                    }
                    // Import the Database
                    if (ze.getName().endsWith("/GPSLogger")) {
                        Log.w("myApp", "[#] AppDataManager.java - UNZIP Database: " + ze.getName().substring(ze.getName().lastIndexOf("/") + 1));
                        FileOutputStream fout = new FileOutputStream(folderDatabase + "/" + ze.getName().substring(ze.getName().lastIndexOf("/") + 1));
                        int i=0;
                        for (int c = zipInputStream.read(); c != -1; c = zipInputStream.read()) {
                            fout.write(c);
                            Log.w("myApp", "[#] AppDataManager.java - UNZIPPING: (" + i + ") " + c);
                            i++;
                        }
                        zipInputStream.closeEntry();
                        fout.close();
                    }
                    //create dir if required while unzipping
//                if (ze.isDirectory()) {
//                    dirChecker(ze.getName());
//                } else {
//                    FileOutputStream fout = new FileOutputStream(_targetLocation + ze.getName());
//                    for (int c = zin.read(); c != -1; c = zin.read()) {
//                        fout.write(c);
//                    }
//
//                    zin.closeEntry();
//                    fout.close();
//                }
                }
                zipInputStream.close();
                inputStream.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

//    public void unzip(String _zipFile, String _targetLocation) {
//
//        //create target location folder if not exist
//        dirChecker(_targetLocation);
//
//        try {
//            FileInputStream fin = new FileInputStream(_zipFile);
//            ZipInputStream zin = new ZipInputStream(fin);
//            ZipEntry ze = null;
//            while ((ze = zin.getNextEntry()) != null) {
//                if (ze.getName().startsWith("eu.basicairdata.graziano.gpslogger/files/Thumbnails/")) {
//                    Log.w("myApp", "[#] AppDataManager.java - UNZIP Thumbnail: " + ze.getName());
//                }
//                if (ze.getName().equals("eu.basicairdata.graziano.gpslogger/databases/GPSLogger")) {
//                    Log.w("myApp", "[#] AppDataManager.java - UNZIP Database: " + ze.getName());
//                }
//                //create dir if required while unzipping
////                if (ze.isDirectory()) {
////                    dirChecker(ze.getName());
////                } else {
////                    FileOutputStream fout = new FileOutputStream(_targetLocation + ze.getName());
////                    for (int c = zin.read(); c != -1; c = zin.read()) {
////                        fout.write(c);
////                    }
////
////                    zin.closeEntry();
////                    fout.close();
////                }
//
//            }
//            zin.close();
//        } catch (Exception e) {
//            System.out.println(e);
//        }
//    }
//
//    private void dirChecker(String dir) {
//        File f = new File(dir);
//        if (!f.isDirectory()) {
//            f.mkdirs();
//        }
//    }

}