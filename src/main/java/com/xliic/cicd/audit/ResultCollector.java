/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import com.xliic.cicd.audit.model.assessment.AssessmentReport;

public interface ResultCollector {
    public void collect(String filename, int score, AssessmentReport report, String[] failures, String reportUrl);
}
