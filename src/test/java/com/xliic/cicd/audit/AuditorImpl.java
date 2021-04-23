package com.xliic.cicd.audit;

import java.io.IOException;

class AuditorImpl {
    LoggerImpl logger;
    private Auditor auditor;
    private int score = 75;
    private WorkspaceImpl workspace;
    private AuditResults results;

    AuditorImpl(String dirname) throws IOException {
        logger = new LoggerImpl();
        workspace = new WorkspaceImpl(dirname);
        Finder finder = new Finder(workspace);
        SecretImpl apiKey = new SecretImpl(System.getenv("TEST_API_KEY"));
        auditor = new Auditor(finder, logger, apiKey);
        auditor.setPlatformUrl("https://platform.dev.42crunch.com");
    }

    AuditorImpl(String dirname, int score) throws IOException {
        this(dirname);
        this.score = score;
    }

    AuditResults audit(String branch) throws IOException, InterruptedException, AuditException {
        this.results = auditor.audit(workspace, "test-repo", branch, score);
        return this.results;
    }

    public AuditResult getResult(String filename) {
        return results.summary.get(workspace.resolve(filename));
    }
}
