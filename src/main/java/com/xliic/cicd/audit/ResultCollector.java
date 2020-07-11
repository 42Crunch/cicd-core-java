/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import java.net.URI;

import com.xliic.cicd.audit.model.assessment.AssessmentReport;
import com.xliic.openapi.bundler.Mapping;

public interface ResultCollector {
    public void collect(URI file, int score, AssessmentReport report, Mapping mapping, String[] failures,
            String reportUrl);
}
