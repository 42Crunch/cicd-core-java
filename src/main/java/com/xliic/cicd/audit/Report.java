package com.xliic.cicd.audit;

import java.net.URI;
import java.util.Map;

import com.xliic.common.Workspace;

public class Report {
    Logger logger;
    private ResultCollector resultCollector;
    private String platformUrl;

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
}
