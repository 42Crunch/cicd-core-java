package com.xliic.cicd.audit;

import java.net.URI;
import java.util.Map;

public class AuditResults {
    public final int failures;
    public final Map<URI, AuditResult> summary;

    public AuditResults(Map<URI, AuditResult> summary, int failures) {
        this.summary = summary;
        this.failures = failures;
    }
}
