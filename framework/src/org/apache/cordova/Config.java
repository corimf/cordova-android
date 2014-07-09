/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package org.apache.cordova;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cordova.api.LOG;
import org.xmlpull.v1.XmlPullParserException;
import android.app.Activity;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.util.Log;

public class Config {

    public static final String TAG = "Config";

    private Whitelist internalWhitelist = new Whitelist();
    private Whitelist externalWhitelist = new Whitelist();
    private String startUrl;

    private static String errorUrl;

    private static Config self = null;

    public static void init(Activity action) {
        if (self == null) {
            self = new Config(action);
        }
    }

    // Intended to be used for testing only; creates an empty configuration.
    public static void init() {
        if (self == null) {
            self = new Config();
        }
    }

    // Intended to be used for testing only; creates an empty configuration.
    private Config() {
    }

    private Config(Activity action) {
        if (action == null) {
            LOG.i("CordovaLog", "There is no activity. Is this on the lock screen?");
            return;
        }

        int id = action.getResources().getIdentifier("config", "xml", action.getPackageName());
        if (id == 0) {
            id = action.getResources().getIdentifier("cordova", "xml", action.getPackageName());
            LOG.i("CordovaLog", "config.xml missing, reverting to cordova.xml");
        }
        if (id == 0) {
            LOG.i("CordovaLog", "cordova.xml missing. Ignoring...");
            return;
        }

        // Add implicitly allowed URLs
        internalWhitelist.addWhiteListEntry("file:///*", false);
        internalWhitelist.addWhiteListEntry("content:///*", false);
        internalWhitelist.addWhiteListEntry("data:*", false);

        XmlResourceParser xml = action.getResources().getXml(id);
        int eventType = -1;
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            if (eventType == XmlResourceParser.START_TAG) {
                String strNode = xml.getName();

                if (strNode.equals("access")) {
                    String origin = xml.getAttributeValue(null, "origin");
                    String subdomains = xml.getAttributeValue(null, "subdomains");
                    boolean external = (xml.getAttributeValue(null, "launch-external") != null);
                    if (origin != null) {
                        if (external) {
                            externalWhitelist.addWhiteListEntry(origin, (subdomains != null) && (subdomains.compareToIgnoreCase("true") == 0));
                        } else {
                            internalWhitelist.addWhiteListEntry(origin, (subdomains != null) && (subdomains.compareToIgnoreCase("true") == 0));
                        }
                    }
                }
                else if (strNode.equals("log")) {
                    String level = xml.getAttributeValue(null, "level");
                    LOG.i("CordovaLog", "Found log level %s", level);
                    if (level != null) {
                        LOG.setLogLevel(level);
                    }
                }
                else if (strNode.equals("preference")) {
                    String name = xml.getAttributeValue(null, "name");
                    /* Java 1.6 does not support switch-based strings
                       Java 7 does, but we're using Dalvik, which is apparently not Java.
                       Since we're reading XML, this has to be an ugly if/else.
                       
                       Also, due to cast issues, each of them has to call their separate putExtra!  
                       Wheee!!! Isn't Java FUN!?!?!?
                       
                       Note: We should probably pass in the classname for the variable splash on splashscreen!
                       */
                    if(name.equals("splashscreen")) {
                        String value = xml.getAttributeValue(null, "value");
                        int resource = 0;
                        if (value != null)
                        {
                            value = "splash";
                        }
                        resource = action.getResources().getIdentifier(value, "drawable", action.getPackageName());
                        
                        action.getIntent().putExtra(name, resource);
                        LOG.i("CordovaLog", "Found preference for %s=%s", name, value);
                        Log.d("CordovaLog", "Found preference for " + name + "=" + value);
                    }
                    else if(name.equals("backgroundColor")) {
                        int value = xml.getAttributeIntValue(null, "value", Color.BLACK);
                        action.getIntent().putExtra(name, value);
                        LOG.i("CordovaLog", "Found preference for %s=%d", name, value);
                        Log.d("CordovaLog", "Found preference for " + name + "=" + Integer.toString(value));
                    }
                    else if(name.equals("loadUrlTimeoutValue")) {
                        int value = xml.getAttributeIntValue(null, "value", 20000);
                        action.getIntent().putExtra(name, value);
                        LOG.i("CordovaLog", "Found preference for %s=%d", name, value);
                        Log.d("CordovaLog", "Found preference for " + name + "=" + Integer.toString(value));
                    }
                    else if(name.equals("keepRunning"))
                    {
                        boolean value = xml.getAttributeValue(null, "value").equals("true");
                        action.getIntent().putExtra(name, value);
                    }
                    else if(name.equals("InAppBrowserStorageEnabled"))
                    {
                        boolean value = xml.getAttributeValue(null, "value").equals("true");
                        action.getIntent().putExtra(name, value);
                    }
                    else if(name.equals("disallowOverscroll"))
                    {
                        boolean value = xml.getAttributeValue(null, "value").equals("true");
                        action.getIntent().putExtra(name, value);
                    }
                    else if(name.equalsIgnoreCase("errorurl"))
                    {
                        errorUrl = xml.getAttributeValue(null, "value");
                    }
                    else
                    {
                        String value = xml.getAttributeValue(null, "value");
                        action.getIntent().putExtra(name, value);
                        LOG.i("CordovaLog", "Found preference for %s=%s", name, value);
                        Log.d("CordovaLog", "Found preference for " + name + "=" + value);
                    }
                    /*
                    LOG.i("CordovaLog", "Found preference for %s=%s", name, value);
                    Log.d("CordovaLog", "Found preference for " + name + "=" + value);
                     */
                }
                else if (strNode.equals("content")) {
                    String src = xml.getAttributeValue(null, "src");

                    LOG.i("CordovaLog", "Found start page location: %s", src);

                    if (src != null) {
                        Pattern schemeRegex = Pattern.compile("^[a-z]+://");
                        Matcher matcher = schemeRegex.matcher(src);
                        if (matcher.find()) {
                            startUrl = src;
                        } else {
                            if (src.charAt(0) == '/') {
                                src = src.substring(1);
                            }
                            startUrl = "file:///android_asset/www/" + src;
                        }
                    }
                }

            }

            try {
                eventType = xml.next();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add entry to approved list of URLs (whitelist)
     *
     * @param origin        URL regular expression to allow
     * @param subdomains    T=include all subdomains under origin
     */
    public static void addWhiteListEntry(String origin, boolean subdomains) {
        if (self == null) {
            Log.e(TAG, "Config was not initialised. Did you forget to Config.init(this)?");
            return;
        }
        self.internalWhitelist.addWhiteListEntry(origin, subdomains);
    }

    /**
     * Determine if URL is in approved list of URLs to load.
     *
     * @param url
     * @return
     */
    public static boolean isUrlWhiteListed(String url) {
        if (self == null) {
            Log.e(TAG, "Config was not initialised. Did you forget to Config.init(this)?");
            return false;
        }
        return self.internalWhitelist.isUrlWhiteListed(url);
    }

    public static boolean isUrlExternallyWhiteListed(String url) {
        if (self == null) {
            Log.e(TAG, "Config was not initialised. Did you forget to Config.init(this)?");
            return false;
        }
        return self.externalWhitelist.isUrlWhiteListed(url);
    }

    public static String getStartUrl() {
        if (self == null || self.startUrl == null) {
            return "file:///android_asset/www/index.html";
        }
        return self.startUrl;
    }

    public static String getErrorUrl() {
        return errorUrl;
    }
}
