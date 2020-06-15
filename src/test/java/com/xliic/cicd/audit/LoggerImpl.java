package com.xliic.cicd.audit;

class LoggerImpl implements Logger {

    LoggerImpl() {
    }

    @Override
    public void log(final String message) {
        System.out.println("LOG: " + message);
    }
}