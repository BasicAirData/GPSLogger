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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A class to manage some operations with the app data.
 * We use it for example to export and import the database of the tracks.
 */
public class AppDataManager {

    String backupFileName = "GPSLogger_Tracklist_BACKUP.zip";

    /**
     * It exports the app data folder to a zip file into the exporting folder.
     * It creates a single zip file of the whole /data/data/eu.basicairdata.graziano.gpslogger folder.
     */
    public void exportAppDataToZipFile() {
        String source = "/data/data/eu.basicairdata.graziano.gpslogger";
        String saveIntoFolder = GPSApplication.getInstance().getPrefExportFolder();
        DocumentFile pickedDir;

        if (saveIntoFolder.startsWith("content")) {
            Uri uri = Uri.parse(saveIntoFolder);
            pickedDir = DocumentFile.fromTreeUri(getInstance(), uri);
        } else {
            pickedDir = DocumentFile.fromFile(new File(saveIntoFolder));
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
            zipFolder(source, outputStream);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
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


    /**
     * It adds a specific folder recursively to a zip file.
     *
     * @param srcFolder The folder to add
     * @param destZipFile The zip file
     */
    static private void zipFolder(String srcFolder, OutputStream destZipFile)
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
    static private void addFileToZip(String path, String srcFile,
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
    static private void addFolderToZip(String path, String srcFolder,
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
//
//                //create dir if required while unzipping
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