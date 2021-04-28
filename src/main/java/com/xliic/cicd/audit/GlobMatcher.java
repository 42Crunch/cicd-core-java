package com.xliic.cicd.audit;

import io.github.azagniotov.matcher.AntPathMatcher;

public class GlobMatcher {
    private AntPathMatcher matcher;

    GlobMatcher() {
        matcher = new AntPathMatcher.Builder().build();
    }

    boolean matches(String pattern, String value) {
        return matcher.isMatch(pattern, value);
    }
}
