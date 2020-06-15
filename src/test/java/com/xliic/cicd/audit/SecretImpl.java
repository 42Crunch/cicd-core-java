package com.xliic.cicd.audit;

class SecretImpl implements Secret {

    private String secret;

    public SecretImpl(String secret) {
        this.secret = secret;
    }

    @Override
    public String getPlainText() {
        return secret;
    }
}