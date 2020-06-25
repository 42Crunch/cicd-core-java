package com.xliic.cicd.audit;

import java.util.HashMap;
import java.util.Set;

import com.xliic.cicd.audit.model.assessment.AssessmentReport;

class ResultCollectorImpl implements ResultCollector {
    HashMap<String, Result> results = new HashMap<String, Result>();

    @Override
    public void collect(String filename, int score, AssessmentReport report, String[] failures, String reportUrl) {
        results.put(filename, new Result(score, report, failures, reportUrl));
    }

    public Result get(String filename) {
        return results.get(filename);
    }

    public Set<String> filenames() {
        return results.keySet();
    }

    public static class Result {
        int score;
        AssessmentReport report;
        String[] failures;
        String reportUrl;

        Result(int score, AssessmentReport report, String[] failures, String reportUrl) {
            this.score = score;
            this.report = report;
            this.failures = failures;
            this.reportUrl = reportUrl;
        }
    }
}