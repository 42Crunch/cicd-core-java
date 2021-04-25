/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.config.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Score {

    private Integer overall;
    private Integer data;
    private Integer security;

    public Score(int overall) {
        this.overall = overall;
    }

    public Score(Score primary, Score secondary) {
        if (primary != null) {
            this.overall = primary.overall;
            this.data = primary.data;
            this.security = primary.security;
        }

        if (secondary != null) {
            if (this.overall == null) {
                this.overall = secondary.overall;
            }
            if (this.data == null) {
                this.data = secondary.data;
            }
            if (this.security == null) {
                this.security = secondary.security;
            }
        }
    }

    @JsonCreator
    @SuppressWarnings("unchecked")
    private Score(Object value) {
        if (value instanceof Integer) {
            this.overall = (Integer) value;
        } else if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.containsKey("data") && map.get("data") instanceof Integer) {
                this.data = (Integer) map.get("data");
            }
            if (map.containsKey("security") && map.get("security") instanceof Integer) {
                this.security = (Integer) map.get("security");
            }
        }
    }

    public Integer getOverall() {
        return overall;
    }

    public Integer getData() {
        return data;
    }

    public Integer getSecurity() {
        return security;
    }

}
