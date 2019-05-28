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

package com.brsanthu.googleanalytics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.brsanthu.googleanalytics.discovery.DefaultRequestParameterDiscoverer;
import com.brsanthu.googleanalytics.discovery.RequestParameterDiscoverer;
import com.actionsmicro.analytics.googleanalytics.httpclient.AMHttplClientImpl;
import com.brsanthu.googleanalytics.httpclient.HttpClient;
import com.brsanthu.googleanalytics.internal.GaUtils;
import com.brsanthu.googleanalytics.internal.GoogleAnalyticsImpl;
import com.brsanthu.googleanalytics.internal.GoogleAnalyticsThreadFactory;
import com.brsanthu.googleanalytics.request.DefaultRequest;

public class GoogleAnalyticsBuilder {
    private GoogleAnalyticsConfig config = new GoogleAnalyticsConfig();
    private DefaultRequest defaultRequest = new DefaultRequest();
    private HttpClient httpClient;
    private ExecutorService executor;

    public GoogleAnalyticsBuilder withConfig(GoogleAnalyticsConfig config) {
        this.config = GaUtils.firstNotNull(config, new GoogleAnalyticsConfig());
        return this;
    }

    public GoogleAnalyticsBuilder withTrackingId(String trackingId) {
        defaultRequest.trackingId(trackingId);
        return this;
    }

    public GoogleAnalyticsBuilder withAppName(String value) {
        defaultRequest.applicationName(value);
        return this;
    }

    public GoogleAnalyticsBuilder withAppVersion(String value) {
        defaultRequest.applicationVersion(value);
        return this;
    }

    public GoogleAnalyticsBuilder withDefaultRequest(DefaultRequest defaultRequest) {
        this.defaultRequest = GaUtils.firstNotNull(defaultRequest, new DefaultRequest());
        return this;
    }

    public GoogleAnalyticsBuilder withExecutor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    public GoogleAnalyticsBuilder withHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public GoogleAnalytics build() {
        if (config.isDiscoverRequestParameters()) {
            RequestParameterDiscoverer discoverer = GaUtils.firstNotNull(config.getRequestParameterDiscoverer(),
                    DefaultRequestParameterDiscoverer.INSTANCE);

            discoverer.discoverParameters(config, defaultRequest);
        }

        return new GoogleAnalyticsImpl(config, defaultRequest, createHttpClient(), createExecutor());
    }

    protected HttpClient createHttpClient() {
        if (httpClient != null) {
            return httpClient;
        }

        return new AMHttplClientImpl(config);
    }

    protected ExecutorService createExecutor() {
        if (executor != null) {
            return executor;
        }

        return new ThreadPoolExecutor(config.getMinThreads(), config.getMaxThreads(), config.getThreadTimeoutSecs(), TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>(config.getThreadQueueSize()), createThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    protected ThreadFactory createThreadFactory() {
        return new GoogleAnalyticsThreadFactory(config.getThreadNameFormat());
    }
}
