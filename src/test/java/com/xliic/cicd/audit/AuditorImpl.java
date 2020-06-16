package com.xliic.cicd.audit;

import java.io.IOException;

import com.xliic.cicd.audit.client.ClientConstants;

class AuditorImpl {
    ResultCollectorImpl results;
    LoggerImpl logger;
    private Auditor auditor;
    private String collectionName = "tests-cicd-core-java";
    private int score = 75;
    private WorkspaceImpl workspace;

    AuditorImpl(String dirname) {
        results = new ResultCollectorImpl();
        logger = new LoggerImpl();
        workspace = new WorkspaceImpl(dirname);
        Finder finder = new Finder(workspace.getDirectory());
        SecretImpl apiKey = new SecretImpl(System.getenv("TEST_API_KEY"));
        auditor = new Auditor(finder, logger, apiKey);
        auditor.setResultCollector(results);
        auditor.setPlatformUrl(ClientConstants.DEV_PLATFORM_URL);
    }

    AuditorImpl(String dirname, int score) {
        this(dirname);
        this.score = score;
    }

    String audit() throws IOException, InterruptedException, AuditException {
        return auditor.audit(workspace, collectionName, score);
    }
}