/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.config;

public class Config {

    public static Config createDefault() {
        Audit audit = new Audit();
        audit.setDiscovery(Discovery.defaultDiscovery());
        audit.setMapping(Mapping.emptyMapping());
        Config config = new Config();
        config.setAudit(audit);
        return config;
    }

    private Audit audit;

    public Audit getAudit() {
        return this.audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }
}
