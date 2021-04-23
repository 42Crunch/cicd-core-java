/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import com.xliic.cicd.audit.model.assessment.AssessmentReport;
import com.xliic.openapi.bundler.Mapping;

public class AuditResult {

    public final int score;
    public final AssessmentReport report;
    public final Mapping mapping;
    public final String[] failures;
    public final String reportUrl;

    public AuditResult(int score, AssessmentReport report, Mapping mapping, String[] failures, String reportUrl) {
        this.score = score;
        this.report = report;
        this.mapping = mapping;
        this.failures = failures;
        this.reportUrl = reportUrl;
    }
}
