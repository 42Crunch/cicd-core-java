package com.xliic.cicd.audit.config;

import com.xliic.cicd.audit.config.model.AuditConfig;

public class ConfigMerge {
    public static AuditConfig merge(AuditConfig primary, AuditConfig secondary) {

        // FIXME improve merging

        if (secondary == null) {
            return primary;
        }

        AuditConfig result = new AuditConfig();

        if (primary.getFailOn() != null) {
            result.setFailOn(primary.getFailOn());
        } else {
            result.setFailOn(secondary.getFailOn());
        }

        if (result.getFailOn().getScore() == null) {
            // copy score from secondary config
            result.getFailOn().setScore(secondary.getFailOn().getScore());
        }

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
