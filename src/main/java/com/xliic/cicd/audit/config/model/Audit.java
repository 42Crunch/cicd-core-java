/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.config.model;

public class Audit {
    private Branches branches;
    private Tags tags;

    public Branches getBranches() {
        return this.branches;
    }

    public void setBranches(Branches branches) {
        this.branches = branches;
    }

    public Tags getTags() {
        return this.tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }
}
