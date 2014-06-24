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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.cordova.api.CordovaInterface;
import org.apache.cordova.api.LOG;

import android.content.res.AssetManager;
import android.net.Uri;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

public class IceCreamCordovaWebViewClient extends CordovaWebViewClient {


    private static final String TAG = "IceCreamCordovaWebViewClient";
    private CordovaUriHelper helper;

    public IceCreamCordovaWebViewClient(CordovaInterface cordova) {
        super(cordova);
    }
    
    public IceCreamCordovaWebViewClient(CordovaInterface cordova, CordovaWebView view) {
        super(cordova, view);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        //Check if plugins intercept the request
        WebResourceResponse ret = super.shouldInterceptRequest(view, url);
        
        // Check the against the whitelist and lock out access to the WebView directory
        // Changing this will cause problems for your application
        // Mike: Pulled this in from b0b628ff but kept the old way of generating
        // a response from 630c42812ab - did not pull in URI resolver stuff 
        // from CB-3384 commit 99341bce
        if (isUrlHarmful(url)) {
            LOG.w(TAG, "URL blocked by whitelist: " + url);
            // Results in a 404.
            // Mike: This used to be getWhitelistResponse() which used an empty byte stream
            // instead of null - not sure the logic behind this
            return new WebResourceResponse("text/plain", "UTF-8", null);
        }
        else if(ret == null && (url.contains("?") || url.contains("#") || needsIceCreamSpaceInAssetUrlFix(url))){
            ret = generateWebResourceResponse(url);
        }
        else if (ret == null && this.appView.pluginManager != null) {
            ret = this.appView.pluginManager.shouldInterceptRequest(url);
        }
        return ret;
    }

    private WebResourceResponse generateWebResourceResponse(String url) {
        final String ANDROID_ASSET = "file:///android_asset/";
        if (url.startsWith(ANDROID_ASSET)) {
            String niceUrl = url;
            niceUrl = url.replaceFirst(ANDROID_ASSET, "");
            if(niceUrl.contains("?")){
                niceUrl = niceUrl.split("\\?")[0];
            }
            else if(niceUrl.contains("#"))
            {
                niceUrl = niceUrl.split("#")[0];
            }

            String mimetype = null;
            if(niceUrl.endsWith(".html")){
                mimetype = "text/html";
            }

            try {
                AssetManager assets = cordova.getActivity().getAssets();
                Uri uri = Uri.parse(niceUrl);
                InputStream stream = assets.open(uri.getPath(), AssetManager.ACCESS_STREAMING);
                WebResourceResponse response = new WebResourceResponse(mimetype, "UTF-8", stream);
                return response;
            } catch (IOException e) {
                LOG.e("generateWebResourceResponse", e.getMessage(), e);
            }
        }
        return null;
    }

    private boolean isUrlHarmful(String url) {
        return ((url.startsWith("http:") || url.startsWith("https:")) && !Config.isUrlWhiteListed(url))
            || url.contains("app_webview");
    }

    private static boolean needsIceCreamSpaceInAssetUrlFix(String url) {
        if (!url.contains("%20")){
            return false;
        }

        switch(android.os.Build.VERSION.SDK_INT){
            case android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH:
            case android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1:
                return true;
            default:
                return false;
        }
    }
    
}
