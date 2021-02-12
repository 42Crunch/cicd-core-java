package com.xliic.cicd.audit;

class LoggerImpl implements Logger {

    LoggerImpl() {
    }

    @Override
    public void warn(final String message) {
        System.out.println("WARN: " + message);
    }

    @Override
    public void info(final String message) {
        System.out.println("INFO: " + message);
    }

    @Override
    public void debug(final String message) {
        System.out.println("DEBUG: " + message);
    }

    @Override
    public void error(final String message) {
        System.out.println("ERROR: " + message);
    }

    @Override
    public void fatal(final String message) {
        System.out.println("FATAL: " + message);
    }

    @Override
    public void setLevel(int level) {
        // ignore levels, log everything
    }
}