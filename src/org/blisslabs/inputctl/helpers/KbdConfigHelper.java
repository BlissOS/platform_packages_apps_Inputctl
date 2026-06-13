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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class KbdConfigHelper {
    private static final String TAG = "KbdConfigHelper";
    private static final String CONFIG_FILE_PATH = "/data/system/kbd_config.xml";

    public static class KbdActionConfig {
        public int key = -1;
        public int mod1 = -1;
        public int mod2 = -1;
    }

    public static class KbdDeviceConfig {
        public String name;
        public KbdActionConfig rot0 = new KbdActionConfig();
        public KbdActionConfig rot90 = new KbdActionConfig();
        public KbdActionConfig rot180 = new KbdActionConfig();
        public KbdActionConfig rot270 = new KbdActionConfig();
    }

    public static List<KbdDeviceConfig> loadConfig() {
        List<KbdDeviceConfig> configs = new ArrayList<>();
        File file = new File(CONFIG_FILE_PATH);
        if (!file.exists()) {
            return configs;
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList deviceList = doc.getElementsByTagName("device");
            for (int i = 0; i < deviceList.getLength(); i++) {
                Element deviceElement = (Element) deviceList.item(i);
                KbdDeviceConfig deviceConfig = new KbdDeviceConfig();
                deviceConfig.name = deviceElement.getAttribute("name");

                parseAction(deviceElement, "rot0", deviceConfig.rot0);
                parseAction(deviceElement, "rot90", deviceConfig.rot90);
                parseAction(deviceElement, "rot180", deviceConfig.rot180);
                parseAction(deviceElement, "rot270", deviceConfig.rot270);

                configs.add(deviceConfig);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load kbd_config.xml", e);
        }
        return configs;
    }

    public static boolean saveConfig(List<KbdDeviceConfig> configs) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element rootElement = doc.createElement("kbd_config");
            doc.appendChild(rootElement);

            for (KbdDeviceConfig config : configs) {
                Element deviceElement = doc.createElement("device");
                deviceElement.setAttribute("name", config.name);

                appendAction(doc, deviceElement, "rot0", config.rot0);
                appendAction(doc, deviceElement, "rot90", config.rot90);
                appendAction(doc, deviceElement, "rot180", config.rot180);
                appendAction(doc, deviceElement, "rot270", config.rot270);

                rootElement.appendChild(deviceElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(CONFIG_FILE_PATH));
            transformer.transform(source, result);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save kbd_config.xml", e);
            return false;
        }
    }

    private static void parseAction(Element deviceElement, String tagName, KbdActionConfig action) {
        NodeList nodeList = deviceElement.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Element actionElement = (Element) nodeList.item(0);
            try {
                if (actionElement.hasAttribute("key")) {
                    action.key = Integer.parseInt(actionElement.getAttribute("key"));
                }
                if (actionElement.hasAttribute("mod1")) {
                    action.mod1 = Integer.parseInt(actionElement.getAttribute("mod1"));
                }
                if (actionElement.hasAttribute("mod2")) {
                    action.mod2 = Integer.parseInt(actionElement.getAttribute("mod2"));
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private static void appendAction(Document doc, Element deviceElement, String tagName, KbdActionConfig action) {
        if (action.key != -1) {
            Element actionElement = doc.createElement(tagName);
            actionElement.setAttribute("key", String.valueOf(action.key));
            if (action.mod1 != -1) {
                actionElement.setAttribute("mod1", String.valueOf(action.mod1));
            }
            if (action.mod2 != -1) {
                actionElement.setAttribute("mod2", String.valueOf(action.mod2));
            }
            deviceElement.appendChild(actionElement);
        }
    }
}
