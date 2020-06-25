package com.xliic.cicd.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.xliic.cicd.audit.ResultCollectorImpl.Result;

import org.junit.jupiter.api.Test;

public class AuditTest {
    @Test
    void audit() throws IOException, InterruptedException, AuditException {
        AuditorImpl auditor = new AuditorImpl("workspace1");
        auditor.audit();
        Result result = auditor.results.get("multi-file-petstore/openapi.yaml");
        List<String> failures = Arrays.asList(result.failures);

        assertEquals(result.score, 18);
        assertTrue(failures.contains("The API score 18 is lower than the set minimum score of 75"));
        assertTrue(failures.contains("Found 35 issues with severity \"medium\" or higher"));
        assertTrue(failures.contains("Found issue \"v3-global-http-clear\""));
        assertTrue(failures.contains("Found issue \"v3-response-schema-undefined\""));

        System.out.println(Arrays.toString(result.report.index));
    }
}
