/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.config;

public class Config {

    public static AuditConfig createDefault() {
        AuditConfig auditConfig = new AuditConfig();
        auditConfig.setDiscovery(Discovery.defaultConfig());
        auditConfig.setMapping(Mapping.emptyMapping());
        auditConfig.setFailOn(FailOn.defaultConfig());
        return auditConfig;
    }

    private Audit audit;

    public Audit getAudit() {
        return this.audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

}
