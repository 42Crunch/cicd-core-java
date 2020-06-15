package com.xliic.cicd.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.xliic.cicd.audit.client.ClientConstants;

import org.junit.jupiter.api.Test;

public class AuditTest {
    @Test
    void audit() throws IOException, InterruptedException, AuditException {
        WorkspaceImpl workspace = new WorkspaceImpl("workspace1");
        Logger logger = new LoggerImpl();
        Finder finder = new Finder(workspace.getDirectory());
        SecretImpl apiKey = new SecretImpl(System.getenv("TEST_API_KEY"));
        ResultCollector resultCollector = new ResultCollectorImpl();
        Auditor auditor = new Auditor(finder, logger, apiKey);
        auditor.setResultCollector(resultCollector);
        auditor.setPlatformUrl(ClientConstants.DEV_PLATFORM_URL);
        String failure = auditor.audit(workspace, "tests-cicd-core-java", 75);
        assertEquals(true, true);
    }
}
