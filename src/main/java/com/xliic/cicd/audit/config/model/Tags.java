/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.config.model;

import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Tags extends HashMap<String, AuditConfig> {
    @JsonAnySetter
    public void set(String name, AuditConfig config) {
        this.put(name, config);
    }

    public static Tags emptyTags() {
        return new Tags();
    }
}
