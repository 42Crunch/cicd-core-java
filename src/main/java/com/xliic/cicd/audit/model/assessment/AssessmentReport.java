/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.model.assessment;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonAnySetter;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({ "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD",
        "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD" })
public class AssessmentReport {
    public boolean valid;
    public String openapiState;
    public Section security;
    public Section data;
    public String[] index;

    public static class Section {
        public float score;
        public Issues issues;
    }

    @SuppressWarnings("serial")
    public static class Issues extends HashMap<String, Issue> {
        @JsonAnySetter
        public void set(String id, Issue issue) {
            this.put(id, issue);
        }
    }

    public static class Issue {
        public int criticality;
        public String description;
        public ArrayList<SubIssue> issues;
    }

    public static class SubIssue {
        public float score;
        public int pointer;
        public String specificDescription;
    }

}