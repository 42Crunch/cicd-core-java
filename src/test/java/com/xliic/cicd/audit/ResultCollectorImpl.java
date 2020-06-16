package com.xliic.cicd.audit;

import java.util.HashMap;
import java.util.Set;

class ResultCollectorImpl implements ResultCollector {
    HashMap<String, Result> results = new HashMap<String, Result>();

    @Override
    public void collect(String filename, int score, String[] failures, String report) {
        results.put(filename, new Result(score, failures, report));
    }

    public Result get(String filename) {
        return results.get(filename);
    }

    public Set<String> filenames() {
        return results.keySet();
    }

    public static class Result {
        int score;
        String[] failures;
        String report;

        Result(int score, String[] failures, String report) {
            this.score = score;
            this.failures = failures;
            this.report = report;
        }
    }
}