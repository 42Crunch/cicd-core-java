/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import com.xliic.cicd.audit.client.RemoteApi;
import com.xliic.cicd.audit.model.api.Maybe;

public class Summary {
    public Maybe<RemoteApi> api;
    public int score;
    public String[] failures;

    public Summary(Maybe<RemoteApi> api, int score, String[] failures) {
        this.api = api;
        this.score = score;
        this.failures = failures;
    }
}
