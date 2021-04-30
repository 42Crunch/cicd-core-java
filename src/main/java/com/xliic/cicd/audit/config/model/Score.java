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

            if (map.containsKey("overall") && map.get("overall") instanceof Integer) {
                this.overall = (Integer) map.get("overall");
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
