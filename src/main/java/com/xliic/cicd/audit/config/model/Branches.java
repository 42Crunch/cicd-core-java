/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.config.model;

import java.util.Comparator;
import java.util.TreeMap;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Branches extends TreeMap<String, AuditConfig> {
    Branches() {
        // Branches is a TreeMap sorted by key length with longest key first
        super(Comparator.comparingInt(String::length).reversed().thenComparing(Function.identity()));
    }

    @JsonAnySetter
    public void set(String name, AuditConfig config) {
        this.put(name, config);
    }

    public static Branches emptyBranches() {
        return new Branches();
    }
}
