/*
 * Copyright (C) 2026 BlissLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.blisslabs.inputctl.helpers;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

public class XmlHelper {
    private static final String TAG = "Inputctl_XmlHelper";
    private static final String FILE_PATH = "/data/system/custom-excluded-input-devices.xml";
    
    // ConfigurationProcessor
    private static final String TAG_DEVICES = "devices";
    private static final String TAG_DEVICE = "device";
    private static final String ATTR_NAME = "name";

    /**
     * Read the XML file and return a list of the names of the blacklisted devices.
     */
    public static Set<String> getBlacklistedDevices() {
        Set<String> blacklisted = new HashSet<>();
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return blacklisted; // return empty list if not found
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.getName().equals(TAG_DEVICE)) {
                    String deviceName = parser.getAttributeValue(null, ATTR_NAME);
                    if (deviceName != null && !deviceName.isEmpty()) {
                        blacklisted.add(deviceName);
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error! Cannot read custom-excluded-input-devices.xml !", e);
        }

        return blacklisted;
    }

    /**
     * Overwrite the new list in the XML file.
     */
    public static boolean saveBlacklistedDevices(Set<String> blacklistedDevices) {
        File file = new File(FILE_PATH);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, "utf-8");
            serializer.startDocument("utf-8", true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            serializer.startTag(null, TAG_DEVICES);

            for (String deviceName : blacklistedDevices) {
                serializer.startTag(null, TAG_DEVICE);
                serializer.attribute(null, ATTR_NAME, deviceName);
                serializer.endTag(null, TAG_DEVICE);
            }

            serializer.endTag(null, TAG_DEVICES);
            serializer.endDocument();
            
            // rw-rw-r--
            file.setReadable(true, false); 
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error! Cannot write custom-excluded-input-devices.xml !", e);
            return false;
        }
    }
}
