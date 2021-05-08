/*
 * EventBusMSGNormal - Java Class for Android
 * Created by G.Capelli on 05/08/17.
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
 * A class that is made to be used as parameter for EventBus messages.
 * This type of messages contain a track ID as additional data.
 */
public class EventBusMSGNormal {
    short eventBusMSG;
    long trackID;

    /**
     * Creates a new EventBusMSGNormal.
     *
     * @param eventBusMSG One of the EventBusMSG Values
     * @param trackID The ID of the Track
     */
    EventBusMSGNormal (short eventBusMSG, long trackID) {
        this.eventBusMSG = eventBusMSG;
        this.trackID = trackID;
    }
}