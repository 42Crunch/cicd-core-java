package com.xliic.cicd.audit.config;

import com.xliic.cicd.audit.config.model.AuditConfig;
import com.xliic.cicd.audit.config.model.Discovery;
import com.xliic.cicd.audit.config.model.FailOn;
import com.xliic.cicd.audit.config.model.Mapping;
import com.xliic.cicd.audit.config.model.Score;

public class DefaultConfig {
    private static final String[] DEFAULT_SEARCH = new String[] { "**/*.json", "**/*.yaml", "**/*.yml",
            "!node_modules/**", "!tsconfig.json" };

    public static AuditConfig create(int minScore) {
        AuditConfig config = new AuditConfig();

        // discovery is enabled by default
        Discovery discovery = new Discovery(true);
        discovery.setSearch(DEFAULT_SEARCH);
        config.setDiscovery(discovery);

        // empty mapping
        config.setMapping(new Mapping());

        // default overall score of 75 and fail on invalid contract
        FailOn failOn = new FailOn();
        failOn.setScore(new Score(minScore));
        failOn.setInvalidContract(true);
        config.setFailOn(failOn);

        return config;
    }

}
