/**
 * ExportingTask - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 15/6/2019
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

public class ExportingTask {

    static final short STATUS_PENDING           = 0;    // Task not yet started
    static final short STATUS_RUNNING           = 1;    // Task is running...
    static final short STATUS_ENDED_SUCCESS     = 2;    // Task ended with success
    static final short STATUS_ENDED_FAILED      = 3;    // Task failed to export

    private long    id                          = 0;
    private long    NumberOfPoints_Total        = 0;
    private long    NumberOfPoints_Processed    = 0;
    private short   Status                      = STATUS_PENDING;
    private String  Name                        = "";


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNumberOfPoints_Total() {
        return NumberOfPoints_Total;
    }

    public void setNumberOfPoints_Total(long numberOfPoints_Total) {
        NumberOfPoints_Total = numberOfPoints_Total;
    }

    public long getNumberOfPoints_Processed() {
        return NumberOfPoints_Processed;
    }

    public void setNumberOfPoints_Processed(long numberOfPoints_Processed) {
        NumberOfPoints_Processed = numberOfPoints_Processed;
    }

    public short getStatus() {
        return Status;
    }

    public void setStatus(short status) {
        Status = status;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
}

