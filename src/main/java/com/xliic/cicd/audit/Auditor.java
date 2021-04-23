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
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.xliic.cicd.audit.client.Client;
import com.xliic.cicd.audit.client.RemoteApi;
import com.xliic.cicd.audit.client.RemoteApiMap;
import com.xliic.cicd.audit.config.AuditConfig;
import com.xliic.cicd.audit.config.ConfigReader;
import com.xliic.cicd.audit.config.Discovery;
import com.xliic.cicd.audit.config.Mapping;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.cicd.audit.model.assessment.AssessmentReport;
import com.xliic.cicd.audit.model.assessment.AssessmentResponse;
import com.xliic.common.Workspace;

public class Auditor {
    private OpenApiFinder finder;
    private Logger logger;
    private Secret apiKey;
    private ResultCollector resultCollector;
    private String platformUrl;
    private Client client;

    public Auditor(OpenApiFinder finder, Logger logger, Secret apiKey) {
        this.finder = finder;
        this.logger = logger;
        this.apiKey = apiKey;
        this.client = new Client(apiKey, platformUrl, logger);
    }

    public void setResultCollector(ResultCollector resultCollector) {
        this.resultCollector = resultCollector;
    }

    public void setProxy(String proxyHost, int proxyPort) {
        this.client.setProxy(proxyHost, proxyPort);
        logger.warn(String.format("Using proxy server: %s:%d ", proxyHost, proxyPort));
    }

    public void setUserAgent(String userAgent) {
        this.client.setUserAgent(userAgent);
        logger.debug(String.format("Using user agent: %s", userAgent));
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
        logger.warn(String.format("Using platform URL: %s ", platformUrl));
        this.client.setPlatformUrl(platformUrl);
    }

    public String audit(Workspace workspace, String collectionName, int minScore)
            throws IOException, InterruptedException, AuditException {

        AuditConfig config = null;
        URI configFile = workspace.resolve(ConfigReader.CONFIG_FILE_NAME);
        if (workspace.exists(configFile)) {
            try {
                config = ConfigReader.read(workspace.read(configFile)).getAudit().getBranches().get("master");
            } catch (final IOException e) {
                throw new AuditException("Failed to read config file", e);
            }
        } else {
            // FIXME default
            // config = Config.createDefault();
        }

        final Discovery discovery = config.getDiscovery();
        final Mapping mapping = config.getMapping();
        final FailureConditions failureConditions = new FailureConditions(minScore, config.getFailOn());

        DiscoveryAuditor discoveryAuditor = new DiscoveryAuditor(this.client);
        MappingAuditor mappingAuditor = new MappingAuditor(this.client);

        final RemoteApiMap uploaded = new RemoteApiMap();
        // discover and upload apis
        if (discovery.isEnabled()) {
            uploaded.putAll(discoveryAuditor.audit(workspace, finder, collectionName, discovery.getSearch(), mapping));

        }

        uploaded.putAll(mappingAuditor.audit(workspace, mapping));

        HashMap<URI, Summary> report = readAssessment(workspace, uploaded, failureConditions);

        // Report.collectResults(report);
        // Report.displayReport(report, workspace);

        int totalFiles = report.size();
        // int filesWithFailures = Report.countFilesWithFailures(report);
        int filesWithFailures = 0;

        if (filesWithFailures > 0) {
            return String.format("Detected %d failure(s) in the %d OpenAPI file(s) checked", filesWithFailures,
                    totalFiles);

        }

        if (totalFiles == 0) {
            return "No OpenAPI files found.";
        }

        return null;
    }

    HashMap<URI, Summary> readAssessment(Workspace workspace, RemoteApiMap uploaded,
            FailureConditions failureConditions) throws IOException {
        HashMap<URI, Summary> report = new HashMap<>();
        for (Map.Entry<URI, Maybe<RemoteApi>> entry : uploaded.entrySet()) {
            URI file = entry.getKey();
            Maybe<RemoteApi> api = entry.getValue();
            logger.info(String.format("Retrieving audit results for: %s", workspace.relativize(file).getPath()));
            Maybe<AssessmentResponse> assessment = client.readAssessment(api);
            Summary summary = checkAssessment(api, assessment, failureConditions);
            report.put(file, summary);
        }
        return report;
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

    private AssessmentReport decodeReport(String data) throws JsonParseException, JsonMappingException, IOException {
        byte[] decoded = Base64.getDecoder().decode(data);
        return JsonParser.parse(decoded, AssessmentReport.class);
    }

}
