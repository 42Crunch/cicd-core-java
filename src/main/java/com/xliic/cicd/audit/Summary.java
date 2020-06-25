/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import com.xliic.cicd.audit.client.RemoteApi;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.cicd.audit.model.assessment.AssessmentReport;

public class Summary {
    public final Maybe<RemoteApi> api;
    public final int score;
    public final String[] failures;
    public final AssessmentReport report;

    public Summary(final Maybe<RemoteApi> api, final int score, final AssessmentReport report,
            final String[] failures) {
        this.api = api;
        this.score = score;
        this.report = report;
        this.failures = failures;
    }
}
