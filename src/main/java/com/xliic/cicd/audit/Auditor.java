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
import com.xliic.cicd.audit.config.ConfigMerge;
import com.xliic.cicd.audit.config.ConfigReader;
import com.xliic.cicd.audit.config.DefaultConfig;
import com.xliic.cicd.audit.config.model.AuditConfig;
import com.xliic.cicd.audit.config.model.Config;
import com.xliic.cicd.audit.config.model.Discovery;
import com.xliic.cicd.audit.config.model.FailOn;
import com.xliic.cicd.audit.config.model.Mapping;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.cicd.audit.model.assessment.AssessmentReport;
import com.xliic.cicd.audit.model.assessment.AssessmentResponse;
import com.xliic.common.Workspace;

public class Auditor {
    private OpenApiFinder finder;
    private Logger logger;
    private String platformUrl;
    private Client client;

    public Auditor(OpenApiFinder finder, Logger logger, Secret apiKey) {
        this.finder = finder;
        this.logger = logger;
        this.client = new Client(apiKey, platformUrl, logger);
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

    public AuditResults audit(Workspace workspace, String repoName, String branchName, int minScore)
            throws IOException, InterruptedException, AuditException {

        AuditConfig config = DefaultConfig.create();
        URI configFile = workspace.resolve(ConfigReader.CONFIG_FILE_NAME);
        if (workspace.exists(configFile)) {
            try {
                Config yamlConfig = ConfigReader.read(workspace.read(configFile));
                config = ConfigMerge.merge(yamlConfig.getAudit().getBranches().get("master"), config);
            } catch (final IOException e) {
                throw new AuditException("Failed to read config file", e);
            }
        }

        final Discovery discovery = config.getDiscovery();
        final Mapping mapping = config.getMapping();

        DiscoveryAuditor discoveryAuditor = new DiscoveryAuditor(workspace, this.client, this.logger);
        MappingAuditor mappingAuditor = new MappingAuditor(this.client, this.logger);

        final RemoteApiMap uploaded = new RemoteApiMap();
        // discover and upload apis
        if (discovery.isEnabled()) {
            uploaded.putAll(discoveryAuditor.audit(workspace, finder, repoName, branchName, "default",
                    discovery.getSearch(), mapping));
        }

        uploaded.putAll(mappingAuditor.audit(workspace, mapping));

        HashMap<URI, Summary> report = readAssessment(workspace, uploaded, config.getFailOn());

        return collectResults(report);
    }

    public void displayReport(AuditResults report, Workspace workspace) {
        report.summary.forEach((file, summary) -> {
            logger.error(String.format("Audited %s, the API score is %d", workspace.relativize(file).getPath(),
                    summary.score));
            if (summary.failures.length > 0) {
                for (String failure : summary.failures) {
                    logger.error("    " + failure);
                }
            } else {
                logger.error("    No blocking issues found.");
            }
            if (summary.reportUrl != null) {
                logger.error("    Details:");
                logger.error(String.format("    %s", platformUrl, summary.reportUrl));
            }
            logger.error("");
        });
    }

    HashMap<URI, Summary> readAssessment(Workspace workspace, RemoteApiMap uploaded, FailOn failureConditions)
            throws IOException {
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

    AuditResults collectResults(Map<URI, Summary> report) {
        Map<URI, AuditResult> result = new HashMap<>();
        int failures = 0;

        for (Map.Entry<URI, Summary> entry : report.entrySet()) {
            URI filename = entry.getKey();
            Summary summary = entry.getValue();
            String reportUrl = null;
            com.xliic.openapi.bundler.Mapping mapping = null;
            if (summary.api.isOk()) {
                reportUrl = String.format("%s/apis/%s/security-audit-report", platformUrl,
                        summary.api.getResult().apiId);
                mapping = summary.api.getResult().mapping;
            }
            if (summary.failures.length > 0) {
                failures++;
            }
            result.put(filename, new AuditResult(summary.score, summary.report, mapping, summary.failures, reportUrl));
        }

        return new AuditResults(result, failures);
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private Summary checkAssessment(Maybe<RemoteApi> api, Maybe<AssessmentResponse> assessment, FailOn conditions)
            throws JsonParseException, JsonMappingException, IOException {
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
