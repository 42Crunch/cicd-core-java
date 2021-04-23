package com.xliic.cicd.audit;

import java.io.IOException;

import com.xliic.cicd.audit.ResultCollectorImpl.Result;

class AuditorImpl {
    ResultCollectorImpl results;
    LoggerImpl logger;
    private Auditor auditor;
    private String collectionName = "tests-cicd-core-java";
    private int score = 75;
    private WorkspaceImpl workspace;

    AuditorImpl(String dirname) throws IOException {
        results = new ResultCollectorImpl();
        logger = new LoggerImpl();
        workspace = new WorkspaceImpl(dirname);
        Finder finder = new Finder(workspace);
        SecretImpl apiKey = new SecretImpl(System.getenv("TEST_API_KEY"));
        auditor = new Auditor(finder, logger, apiKey);
        auditor.setResultCollector(results);
        auditor.setPlatformUrl("https://platform.dev.42crunch.com");
    }

    AuditorImpl(String dirname, int score) throws IOException {
        this(dirname);
        this.score = score;
    }

    String audit() throws IOException, InterruptedException, AuditException {
        return auditor.audit(workspace, collectionName, score);
    }

    public Result getResult(String filename) {
        return results.get(workspace.resolve(filename));
    }
}
