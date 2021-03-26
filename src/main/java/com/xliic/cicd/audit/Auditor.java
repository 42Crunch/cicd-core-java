/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.xliic.cicd.audit.JsonParser.Bundled;
import com.xliic.cicd.audit.client.Client;
import com.xliic.cicd.audit.client.ClientConstants;
import com.xliic.cicd.audit.client.RemoteApi;
import com.xliic.cicd.audit.client.RemoteApiMap;
import com.xliic.cicd.audit.config.Config;
import com.xliic.cicd.audit.config.ConfigReader;
import com.xliic.cicd.audit.config.Discovery;
import com.xliic.cicd.audit.config.Mapping;
import com.xliic.cicd.audit.model.OpenApiFile;
import com.xliic.cicd.audit.model.api.Api;
import com.xliic.cicd.audit.model.api.ApiCollection;
import com.xliic.cicd.audit.model.api.ApiCollections;
import com.xliic.cicd.audit.model.api.ErrorMessage;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.cicd.audit.model.assessment.AssessmentReport;
import com.xliic.cicd.audit.model.assessment.AssessmentResponse;
import com.xliic.common.Workspace;
import com.xliic.openapi.bundler.BundlingException;
import com.xliic.openapi.bundler.ReferenceResolutionFailure;

public class Auditor {
    static int MAX_NAME_LEN = 64;
    private OpenApiFinder finder;
    private Logger logger;
    private Secret apiKey;
    private ResultCollector resultCollector;
    private String platformUrl = ClientConstants.PLATFORM_URL;
    private String collectionId;

    public Auditor(OpenApiFinder finder, Logger logger, Secret apiKey) {
        this.finder = finder;
        this.logger = logger;
        this.apiKey = apiKey;
    }

    public void setResultCollector(ResultCollector resultCollector) {
        this.resultCollector = resultCollector;
    }

    public void setProxy(String proxyHost, int proxyPort) {
        Client.setProxy(proxyHost, proxyPort);
        logger.warn(String.format("Using proxy server: %s:%d ", proxyHost, proxyPort));
    }

    public void setUserAgent(String userAgent) {
        Client.setUserAgent(userAgent);
        logger.debug(String.format("Using user agent: %s", userAgent));
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
        logger.warn(String.format("Using platform URL: %s ", platformUrl));
        Client.setPlatformUrl(platformUrl);
    }

    public String getCollectionId() {
        return collectionId;
    }

    public String audit(Workspace workspace, String collectionName, int minScore)
            throws IOException, InterruptedException, AuditException {

        Config config;
        URI configFile = workspace.resolve(ConfigReader.CONFIG_FILE_NAME);
        if (workspace.exists(configFile)) {
            try {
                config = ConfigReader.read(workspace.read(configFile));
            } catch (final IOException e) {
                throw new AuditException("Failed to read config file", e);
            }
        } else {
            config = Config.createDefault();
        }

        final Discovery discovery = config.getAudit().getDiscovery();
        final Mapping mapping = config.getAudit().getMapping();
        final FailureConditions failureConditions = new FailureConditions(minScore, config.getAudit().getFailOn());

        final RemoteApiMap uploaded = new RemoteApiMap();
        // discover and upload apis
        if (discovery.isEnabled()) {
            uploaded.putAll(uploadDiscoveredFiles(workspace, finder, collectionName, discovery.getSearch(), mapping));
        }

        uploaded.putAll(uploadMappedFiles(workspace, mapping));

        HashMap<URI, Summary> report = readAssessment(workspace, uploaded, failureConditions);

        collectResults(report);
        displayReport(report, workspace);

        int totalFiles = report.size();
        int filesWithFailures = countFilesWithFailures(report);

        if (filesWithFailures > 0) {
            return String.format("Detected %d failure(s) in the %d OpenAPI file(s) checked", filesWithFailures,
                    totalFiles);

        }

        if (totalFiles == 0) {
            return "No OpenAPI files found.";
        }

        return null;
    }

    private RemoteApiMap uploadDiscoveredFiles(Workspace workspace, OpenApiFinder finder, String collectionName,
            String[] search, Mapping mapping) throws IOException, InterruptedException, AuditException {
        DiscoveredOpenApiFiles discovered = discoverOpenApiFiles(workspace, finder, search, mapping);

        // collect list of successfully detected OpenAPI files
        ArrayList<URI> openApiFilenames = new ArrayList<>();
        for (Entry<URI, Maybe<Boolean>> openapi : discovered.entrySet()) {
            if (openapi.getValue().isOk()) {
                openApiFilenames.add(openapi.getKey());
            }
        }

        collectionId = createOrFindCollectionId(makeName(collectionName));

        RemoteApiMap remoteApis = uploadFilesToCollection(openApiFilenames, workspace, collectionId);

        // add files which failed to parse to the list of errors
        for (Entry<URI, Maybe<Boolean>> openapi : discovered.entrySet()) {
            if (openapi.getValue().isError()) {
                remoteApis.put(openapi.getKey(), new Maybe<>(openapi.getValue().getError()));
            }
        }

        return remoteApis;
    }

    private RemoteApiMap uploadMappedFiles(Workspace workspace, Mapping mapping) throws IOException {
        RemoteApiMap uploaded = new RemoteApiMap();

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            URI file = workspace.resolve(entry.getKey());
            String apiId = entry.getValue();
            Maybe<Boolean> isOpenApi = isOpenApiFile(file, workspace);
            if (isOpenApi.isOk() && isOpenApi.getResult() == true) {
                // this is good OpenAPIFile, upload it
                logger.info(
                        String.format("Uploading file for security audit: %s", workspace.relativize(file).getPath()));
                try {
                    Bundled bundled = JsonParser.bundle(file, workspace);
                    Maybe<RemoteApi> api = Client.updateApi(apiId, bundled.json, apiKey, logger);
                    if (api.isOk()) {
                        api.getResult().setMapping(bundled.mapping);
                    }
                    uploaded.put(file, api);
                } catch (AuditException e) {
                    // in case of parsing error during bundling, do not stop audit
                    uploaded.put(file, new Maybe<RemoteApi>(new ErrorMessage(e)));
                } catch (BundlingException e) {
                    for (ReferenceResolutionFailure failure : e.getFailures()) {
                        logger.error(String.format("Failed to resolve reference in %s at %s: %s", failure.sourceFile,
                                failure.sourcePointer, failure.message));
                    }
                    uploaded.put(file, new Maybe<RemoteApi>(new ErrorMessage(e)));
                }
            } else if (isOpenApi.isOk() && isOpenApi.getResult() == false) {
                // not an OpenAPI file, but it is mapped so let's put an error message for it
                uploaded.put(file,
                        new Maybe<>(
                                new ErrorMessage(String.format("Mapped file '%s' API ID '%s' is not an OpenAPI file",
                                        workspace.relativize(file).getPath(), apiId))));
            } else {
                // failed to parse
                uploaded.put(file, new Maybe<>(new ErrorMessage(String.format("Mapped file '%s' API ID '%s': %s",
                        workspace.relativize(file).getPath(), apiId, isOpenApi.getError().getMessage()))));
            }
        }

        return uploaded;
    }

    private HashMap<URI, Summary> readAssessment(Workspace workspace, RemoteApiMap uploaded,
            FailureConditions failureConditions) throws IOException {
        HashMap<URI, Summary> report = new HashMap<>();
        for (Map.Entry<URI, Maybe<RemoteApi>> entry : uploaded.entrySet()) {
            URI file = entry.getKey();
            Maybe<RemoteApi> api = entry.getValue();
            logger.info(String.format("Retrieving audit results for: %s", workspace.relativize(file).getPath()));
            Maybe<AssessmentResponse> assessment = Client.readAssessment(api, apiKey, logger);
            Summary summary = checkAssessment(api, assessment, failureConditions);
            report.put(file, summary);
        }
        return report;
    }

    private AssessmentReport decodeReport(String data) throws JsonParseException, JsonMappingException, IOException {
        byte[] decoded = Base64.getDecoder().decode(data);
        return JsonParser.parse(decoded, AssessmentReport.class);
    }

    private void collectResults(Map<URI, Summary> report) {
        if (this.resultCollector != null) {
            report.forEach((filename, summary) -> {
                String reportUrl = null;
                com.xliic.openapi.bundler.Mapping mapping = null;
                if (summary.api.isOk()) {
                    reportUrl = String.format("%s/apis/%s/security-audit-report", platformUrl,
                            summary.api.getResult().apiId);
                    mapping = summary.api.getResult().mapping;
                }
                this.resultCollector.collect(filename, summary.score, summary.report, mapping, summary.failures,
                        reportUrl);
            });
        }
    }

    private void displayReport(Map<URI, Summary> report, Workspace workspace) {
        report.forEach((file, summary) -> {
            logger.error(String.format("Audited %s, the API score is %d", workspace.relativize(file).getPath(),
                    summary.score));
            if (summary.failures.length > 0) {
                for (String failure : summary.failures) {
                    logger.error("    " + failure);
                }
            } else {
                logger.error("    No blocking issues found.");
            }
            if (summary.api.isOk()) {
                logger.error("    Details:");
                logger.error(String.format("    %s/apis/%s/security-audit-report", platformUrl,
                        summary.api.getResult().apiId));
            }
            logger.error("");
        });
    }

    public int countFilesWithFailures(Map<URI, Summary> report) {
        int failures = 0;
        for (Summary summary : report.values()) {
            if (summary.failures.length > 0) {
                failures++;
            }
        }
        return failures;
    }

    private DiscoveredOpenApiFiles discoverOpenApiFiles(Workspace workspace, OpenApiFinder finder, String[] search,
            Mapping mapping) throws IOException, InterruptedException, AuditException {
        DiscoveredOpenApiFiles discovered = new DiscoveredOpenApiFiles();

        List<URI> files = findOpenapiFiles(workspace, finder, search);
        String filenames = files.stream().map(file -> workspace.relativize(file).getPath())
                .collect(Collectors.joining(","));
        logger.info(String.format("Files matching search criteria: %s", filenames));
        for (URI file : files) {
            if (!mapping.containsKey(workspace.relativize(file).getPath())) {
                Maybe<Boolean> openapi = isOpenApiFile(file, workspace);
                // put discovered OpenAPI files onto the list of discovered ones
                // also add there parsing errors
                if (openapi.isOk() && openapi.getResult() == true || openapi.isError()) {
                    discovered.put(file, openapi);
                }
            }
        }

        String discoveredFilenames = discovered.keySet().stream().map(file -> workspace.relativize(file).getPath())
                .collect(Collectors.joining(","));
        logger.info(String.format("Discovered OpenAPI files: %s", discoveredFilenames));

        return discovered;
    }

    private List<URI> findOpenapiFiles(Workspace workspace, OpenApiFinder finder, String[] search)
            throws IOException, InterruptedException, AuditException {
        finder.setPatterns(search);
        List<URI> openApiFiles = finder.find();
        return openApiFiles;
    }

    private static Maybe<Boolean> isOpenApiFile(URI file, Workspace workspace) {
        try {
            OpenApiFile openApiFile = JsonParser.parse(workspace.read(file), OpenApiFile.class,
                    file.getPath().toLowerCase().endsWith(".yaml") || file.getPath().toLowerCase().endsWith(".yml"));
            return new Maybe<Boolean>(openApiFile.isOpenApi());
        } catch (Exception ex) {
            return new Maybe<Boolean>(new ErrorMessage(String.format("Filed to parse a file '%s': %s",
                    workspace.relativize(file).getPath(), ex.getMessage())));
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private String createOrFindCollectionId(String collectionName) throws AuditException, IOException {

        // check existing collections to see if collection with collectionName already
        // exists
        Maybe<ApiCollections> collections = Client.listCollections(apiKey, logger);
        if (collections.isError()) {
            throw new AuditException("Unable to list collection: " + collections.getError().getMessage());
        }
        for (ApiCollections.ApiCollection collection : collections.getResult().list) {
            if (collection.desc.name.equals(collectionName)) {
                return collection.desc.id;
            }
        }

        // no collection collectionName found, create a new one
        Maybe<ApiCollections.ApiCollection> collection = Client.createCollection(collectionName, apiKey, logger);
        if (collection.isError()) {
            throw new AuditException("Unable to create collection: " + collection.getError().getMessage());
        }
        return collection.getResult().desc.id;
    }

    private RemoteApiMap uploadFilesToCollection(ArrayList<URI> files, Workspace workspace, String collectionId)
            throws IOException, AuditException {
        RemoteApiMap uploaded = new RemoteApiMap();

        purgeCollection(collectionId);
        for (URI file : files) {
            logger.info(String.format("Uploading file for security audit: %s", workspace.relativize(file).getPath()));
            try {
                Bundled bundled = JsonParser.bundle(file, workspace);
                String apiName = makeName(workspace.relativize(file).getPath());
                Maybe<RemoteApi> api = Client.createApi(collectionId, apiName, bundled.json, apiKey, logger);
                if (api.isOk()) {
                    api.getResult().setMapping(bundled.mapping);
                }
                uploaded.put(file, api);
            } catch (AuditException e) {
                // in case of parsing error during bundling, do not stop audit
                uploaded.put(file, new Maybe<RemoteApi>(new ErrorMessage(e)));
            } catch (BundlingException e) {
                for (ReferenceResolutionFailure failure : e.getFailures()) {
                    logger.error(String.format("Failed to resolve reference in %s at %s: %s", failure.sourceFile,
                            failure.sourcePointer, failure.message));
                }
                uploaded.put(file, new Maybe<RemoteApi>(new ErrorMessage(e)));
            }
        }

        return uploaded;
    }

    private void purgeCollection(String collectionId) throws IOException, AuditException {
        Maybe<ApiCollection> collection = Client.listCollection(collectionId, apiKey, logger);
        if (collection.isError()) {
            throw new AuditException("Unable to read collection: " + collection.getError().getMessage());
        }
        for (Api api : collection.getResult().list) {
            Maybe<String> deleted = Client.deleteApi(api.desc.id, apiKey, logger);
            if (deleted.isError()) {
                throw new AuditException("Unable to delete collection: " + deleted.getError().getMessage());
            }
        }
    }

    private String makeName(String name) {
        String mangled = name.replaceAll("[^A-Za-z0-9_\\-\\.\\ ]", "-");
        if (mangled.length() > MAX_NAME_LEN) {
            return mangled.substring(0, MAX_NAME_LEN);
        }
        return mangled;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private Summary checkAssessment(Maybe<RemoteApi> api, Maybe<AssessmentResponse> assessment,
            FailureConditions conditions) throws JsonParseException, JsonMappingException, IOException {
        if (assessment.isError()) {
            return new Summary(api, 0, null, new String[] { assessment.getError().getMessage() });
        }

        AssessmentReport assessmentReport = decodeReport(assessment.getResult().data);
        FailureChecker checker = new FailureChecker();
        ArrayList<String> failures = checker.checkAssessment(assessment.getResult(), assessmentReport, conditions);
        return new Summary(api, Math.round(assessment.getResult().attr.data.grade), assessmentReport,
                failures.toArray(new String[0]));
    }
}
