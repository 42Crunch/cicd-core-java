/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

import com.xliic.cicd.audit.model.api.ApiCollections;
import com.xliic.cicd.audit.model.api.ErrorMessage;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.cicd.audit.model.api.TechnicalCollection;
import com.xliic.cicd.audit.JsonParser;
import com.xliic.cicd.audit.Logger;
import com.xliic.cicd.audit.Secret;
import com.xliic.cicd.audit.model.api.Api;
import com.xliic.cicd.audit.model.api.ApiCollection;
import com.xliic.cicd.audit.model.assessment.AssessmentResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.config.RequestConfig;

public class Client {
    private String proxyHost;
    private int proxyPort;
    private String userAgent;
    private String platformUrl;
    private Secret apiKey;
    private Logger logger;

    public Client(Secret apiKey, String platformUrl, Logger logger) {
        this.apiKey = apiKey;
        this.platformUrl = platformUrl;
        this.logger = logger;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setProxy(String proxyHost, int proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    public Maybe<RemoteApi> createApi(String collectionId, String name, String json) throws IOException {
        HttpPost request = new HttpPost(platformUrl + "/api/v1/apis");

        HttpEntity data = MultipartEntityBuilder
                .create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addBinaryBody("specfile",
                        json.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON, "swagger.json")
                .addTextBody("name", name).addTextBody("cid", collectionId).build();
        request.setEntity(data);

        Maybe<Api> api = new ProxyClient<Api>(request, apiKey, Api.class, logger).execute();
        if (api.isError()) {
            return new Maybe<RemoteApi>(api.getError());

        }
        return new Maybe<RemoteApi>(new RemoteApi(api.getResult().desc.id, ApiStatus.freshApiStatus()));
    }

    public Maybe<RemoteApi> updateApi(String apiId, String json) throws IOException {
        // read api status first
        Maybe<ApiStatus> status = readApiStatus(apiId);
        if (status.isError()) {
            return new Maybe<RemoteApi>(status.getError());
        }
        // update the api
        HttpPut request = new HttpPut(platformUrl + "/api/v1/apis/" + apiId);
        String encodedJson = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        request.setEntity(
                new StringEntity(String.format("{\"specfile\": \"%s\"}", encodedJson), ContentType.APPLICATION_JSON));
        Maybe<String> result = new ProxyClient<String>(request, apiKey, String.class, logger).execute();
        if (result.isError()) {
            return new Maybe<RemoteApi>(result.getError());
        }
        return new Maybe<RemoteApi>(new RemoteApi(apiId, status.getResult()));
    }

    public Maybe<String> deleteApi(String apiId) throws IOException {
        HttpDelete request = new HttpDelete(String.format("%s/api/v1/apis/%s", platformUrl, apiId));
        return new ProxyClient<String>(request, apiKey, String.class, logger).execute();
    }

    public Maybe<AssessmentResponse> readAssessment(Maybe<RemoteApi> api) throws ClientProtocolException, IOException {
        if (api.isError()) {
            return new Maybe<AssessmentResponse>(api.getError());
        }
        HttpGet request = new HttpGet(platformUrl + "/api/v1/apis/" + api.getResult().apiId + "/assessmentreport");
        ProxyClient<AssessmentResponse> client = new ProxyClient<AssessmentResponse>(request, apiKey,
                AssessmentResponse.class, logger);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        while (Duration.between(start, now).toMillis() < ClientConstants.ASSESSMENT_MAX_WAIT) {
            Maybe<ApiStatus> status = readApiStatus(api.getResult().apiId);

            // check if assessment is ready, or bail out with the error
            if (status.isOk() && status.getResult().isProcessed
                    && status.getResult().lastAssessment.isAfter(api.getResult().previousStatus.lastAssessment)) {
                return client.execute();
            } else if (status.isError()) {
                return new Maybe<AssessmentResponse>(status.getError());
            }

            // sleep if assessment is not yet ready
            try {
                Thread.sleep(ClientConstants.ASSESSMENT_RETRY);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            now = LocalDateTime.now();
        }

        return new Maybe<AssessmentResponse>(
                new ErrorMessage("Timed out waiting for audit result for API ID: " + api.getResult().apiId));
    }

    public Maybe<ApiStatus> readApiStatus(String apiId) throws IOException {
        HttpGet request = new HttpGet(platformUrl + "/api/v1/apis/" + apiId);
        Maybe<Api> result = new ProxyClient<Api>(request, apiKey, Api.class, logger).execute();
        if (result.isError()) {
            return new Maybe<ApiStatus>(result.getError());
        }
        return new Maybe<ApiStatus>(
                new ApiStatus(result.getResult().assessment.isProcessed, result.getResult().assessment.last));
    }

    public Maybe<ApiCollection> listCollection(String collectionId) throws IOException {
        HttpGet request = new HttpGet(String.format("%s/api/v1/collections/%s/apis", platformUrl, collectionId));
        return new ProxyClient<ApiCollection>(request, apiKey, ApiCollection.class, logger).execute();
    }

    public Maybe<TechnicalCollection> readTechnicalCollection(String name) throws IOException {
        // FIXME need proper JSON encoder to escsape name value
        HttpPost request = new HttpPost(platformUrl + "/api/v1/collections/technicalName");
        request.setEntity(
                new StringEntity(String.format("{\"technicalName\": \"%s\"}", name), ContentType.APPLICATION_JSON));
        return new ProxyClient<TechnicalCollection>(request, apiKey, TechnicalCollection.class, logger).execute();
    }

    public Maybe<ApiCollections.ApiCollection> createTechnicalCollection(String name) throws IOException {
        HttpPost request = new HttpPost(platformUrl + "/api/v1/collections");
        request.setEntity(new StringEntity(
                String.format("{\"technicalName\": \"%s\", \"name\": \"%s\", \"source\": \"default\"}", name, name),
                ContentType.APPLICATION_JSON));
        return new ProxyClient<ApiCollections.ApiCollection>(request, apiKey, ApiCollections.ApiCollection.class,
                logger).execute();

    }

    public Maybe<String> deleteCollection(String collectionId) throws IOException {
        HttpDelete request = new HttpDelete(String.format("%s/api/v1/collections/%s", platformUrl, collectionId));
        return new ProxyClient<String>(request, apiKey, String.class, logger).execute();
    }

    class ProxyClient<T> {
        private java.lang.Class<T> contentClass;
        private Logger logger;
        private HttpRequestBase request;
        private Secret apiKey;

        ProxyClient(HttpRequestBase request, Secret apiKey, Class<T> contentClass, Logger logger) {
            this.request = request;
            this.apiKey = apiKey;
            this.contentClass = contentClass;
            this.logger = logger;
        }

        Maybe<T> execute() throws IOException {
            CloseableHttpClient httpClient = HttpClients.createSystem();
            CloseableHttpResponse response = null;
            configureRequest(request, apiKey, logger);

            try {
                response = httpClient.execute(request);
                int status = response.getStatusLine().getStatusCode();
                this.logger.debug(String.format("%s %s %d", request.getMethod(), request.getURI(), status));
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();

                    if (contentClass.equals(String.class)) {
                        return new Maybe<T>((T) EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
                    } else {

                        return new Maybe<T>(JsonParser.parse(entity.getContent(), contentClass));
                    }
                }

                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (status == 409 && responseBody.contains("limit reached")) {
                    return new Maybe<T>(new ErrorMessage(String.format(
                            "You have reached your maximum number of APIs. Please sign into %s and upgrade your account.",
                            platformUrl), status));
                } else if (status == 403) {
                    return new Maybe<T>(new ErrorMessage(
                            "Received 'Forbidden 403' response. Check that your API IDs are correct and API Token has required permissions: "
                                    + responseBody,
                            status));
                } else if (status == 401) {
                    return new Maybe<T>(new ErrorMessage(
                            "Received 'Unauthorized 401' response. Check that the API token is correct: "
                                    + responseBody,
                            status));
                }
                return new Maybe<T>(
                        new ErrorMessage(String.format("HTTP Request: %s %s failed with unexpected status code %s",
                                request.getMethod(), request.getURI(), status), status));
            } finally {
                if (response != null) {
                    response.close();
                }
                httpClient.close();
            }
        }

        private HttpHost getProxyHost() {
            if (proxyHost != null) {
                return new HttpHost(proxyHost, proxyPort, "http");
            }
            return null;
        }

        private void configureRequest(HttpRequestBase request, Secret apiKey, Logger logger) {
            request.setHeader("Accept", "application/json");
            request.setHeader("X-API-KEY", apiKey.getPlainText());
            if (userAgent != null) {
                request.setHeader("User-Agent", userAgent);
            }

            HttpHost proxy = getProxyHost();
            if (proxy != null) {
                RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
                request.setConfig(config);
            }
        }
    }
}
