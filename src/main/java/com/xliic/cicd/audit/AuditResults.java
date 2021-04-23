/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import java.net.URI;
import java.util.Map;

public class AuditResults {
    public final int failures;
    public final Map<URI, AuditResult> summary;

    public AuditResults(Map<URI, AuditResult> summary, int failures) {
        this.summary = summary;
        this.failures = failures;
    }
}
