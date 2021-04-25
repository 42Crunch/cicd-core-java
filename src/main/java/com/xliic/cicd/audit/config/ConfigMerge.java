package com.xliic.cicd.audit.config;

import com.xliic.cicd.audit.config.model.AuditConfig;
import com.xliic.cicd.audit.config.model.Score;

public class ConfigMerge {
    public static AuditConfig merge(AuditConfig primary, AuditConfig secondary) {
        if (secondary == null) {
            return primary;
        }

        AuditConfig result = new AuditConfig();

        if (primary.getFailOn() != null) {
            result.setFailOn(primary.getFailOn());
        } else {
            result.setFailOn(secondary.getFailOn());
        }

        Score score = new Score(primary.getFailOn().getScore(), secondary.getFailOn().getScore());
        result.getFailOn().setScore(score);

        if (primary.getDiscovery() != null) {
            result.setDiscovery(primary.getDiscovery());
        } else {
            result.setDiscovery(secondary.getDiscovery());
        }

        if (primary.getMapping() != null) {
            result.setMapping(primary.getMapping());
        } else {
            result.setMapping(secondary.getMapping());
        }

        return result;
    }
}
