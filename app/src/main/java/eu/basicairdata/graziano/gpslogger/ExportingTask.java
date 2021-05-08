/*
 * ExportingTask - Java Class for Android
 * Created by G.Capelli on 15/6/2019
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

/**
 * The data structure that stores all the information needed to export a Track.
 * It stores the properties, the amount of work,
 * and the status of the exportation.
 */
public class ExportingTask {

    static final short STATUS_PENDING           = 0;    // Task not yet started
    static final short STATUS_RUNNING           = 1;    // Task is running...
    static final short STATUS_ENDED_SUCCESS     = 2;    // Task ended with success
    static final short STATUS_ENDED_FAILED      = 3;    // Task failed to export

    private long    id                          = 0;
    private long    numberOfPoints_Total        = 0;
    private long    numberOfPoints_Processed    = 0;
    private short   status                      = STATUS_PENDING;
    private String  name                        = "";

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNumberOfPoints_Total() {
        return numberOfPoints_Total;
    }

    public void setNumberOfPoints_Total(long numberOfPoints_Total) {
        this.numberOfPoints_Total = numberOfPoints_Total;
    }

    public long getNumberOfPoints_Processed() {
        return numberOfPoints_Processed;
    }

    public void setNumberOfPoints_Processed(long numberOfPoints_Processed) {
        this.numberOfPoints_Processed = numberOfPoints_Processed;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

