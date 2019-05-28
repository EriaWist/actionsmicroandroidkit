/*
 * Copyright (C) 2019 Actions Microelectronics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brsanthu.googleanalytics.discovery;

import static com.brsanthu.googleanalytics.internal.GaUtils.appendSystemProperty;
import static com.brsanthu.googleanalytics.internal.GaUtils.isEmpty;


import com.actionsmicro.utils.Log;
import com.brsanthu.googleanalytics.GoogleAnalyticsConfig;
import com.brsanthu.googleanalytics.request.DefaultRequest;

/**
 * Default request parameter discoverer. Discovers following parameters.
 * <ul>
 * <li>Creates User Agent as java/1.6.0_45-b06/Sun Microsystems Inc./Java HotSpot(TM) 64-Bit Server VM/Windows
 * 7/6.1/amd64</li>
 * <li>User Language, and Country</li>
 * <li>File Encoding</li>
 * </ul>
 * 
 * @author Santhosh Kumar
 */
public class DefaultRequestParameterDiscoverer implements RequestParameterDiscoverer {

    private static final String TAG =  "DefaultRequestParameterDiscoverer";
    public static final DefaultRequestParameterDiscoverer INSTANCE = new DefaultRequestParameterDiscoverer();

    @Override
    public DefaultRequest discoverParameters(GoogleAnalyticsConfig config, DefaultRequest request) {
        try {
            if (isEmpty(config.getUserAgent())) {
                if(!isEmpty(System.getProperty("http.agent"))){
                    config.setUserAgent(System.getProperty("http.agent"));
                } else {
                    config.setUserAgent(getUserAgentString());
                }
            }

            if (isEmpty(request.userLanguage())) {
                String region = System.getProperty("user.region");
                if (isEmpty(region)) {
                    region = System.getProperty("user.country");
                }
                request.userLanguage(System.getProperty("user.language") + "-" + region);
            }

            if (isEmpty(request.documentEncoding())) {
                request.documentEncoding(System.getProperty("file.encoding"));
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception while deriving the System properties for request " + request, e);
        }

        return request;
    }

    protected String getUserAgentString() {
        StringBuilder sb = new StringBuilder("java");
        appendSystemProperty(sb, "java.runtime.version");
        appendSystemProperty(sb, "java.specification.vendor");
        appendSystemProperty(sb, "java.vm.name");
        appendSystemProperty(sb, "os.name");
        appendSystemProperty(sb, "os.version");
        appendSystemProperty(sb, "os.arch");

        return sb.toString();
    }

}
