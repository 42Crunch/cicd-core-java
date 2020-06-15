package com.xliic.cicd.audit;

class ResultCollectorImpl implements ResultCollector {

    @Override
    public void collect(String filename, int score, String[] failures, String report) {
        System.out.println("REPORT: " + filename + " " + score);
    }

}