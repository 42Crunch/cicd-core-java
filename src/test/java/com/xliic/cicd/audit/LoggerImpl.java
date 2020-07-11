package com.xliic.cicd.audit;

class LoggerImpl implements Logger {

    LoggerImpl() {
    }

    @Override
    public void log(final String message) {
        System.out.println("LOG: " + message);
    }

    @Override
    public void progress(final String message) {
        System.out.println("PROGRESS: " + message);
    }

    @Override
    public void report(final String message) {
        System.out.println("REPORT: " + message);
    }
}