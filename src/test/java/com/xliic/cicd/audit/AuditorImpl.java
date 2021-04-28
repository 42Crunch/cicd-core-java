package com.xliic.cicd.audit;

import java.io.IOException;

class AuditorImpl {
    LoggerImpl logger;
    private Auditor auditor;
    private WorkspaceImpl workspace;
    private AuditResults results;

    AuditorImpl(String dirname) throws IOException {
        logger = new LoggerImpl();
        workspace = new WorkspaceImpl(dirname);
        Finder finder = new Finder(workspace);
        SecretImpl apiKey = new SecretImpl(System.getenv("TEST_API_KEY"));
        auditor = new Auditor(finder, logger, apiKey, "https://platform.dev.42crunch.com", "CICD Tests/1.0", "default");
    }

    AuditorImpl(String dirname, int score) throws IOException {
        this(dirname);
        auditor.setMinScore(score);
    }

    AuditResults audit(String branch) throws IOException, InterruptedException, AuditException {
        this.results = auditor.audit(workspace, "https://github.com/42Crunch/cicd-core-java.git", branch);
        return this.results;
    }

    public AuditResult getResult(String filename) {
        return results.summary.get(workspace.resolve(filename));
    }
}
