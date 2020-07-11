/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit.client;

import java.net.URI;
import java.util.HashMap;

import com.xliic.cicd.audit.model.api.Maybe;

@SuppressWarnings("serial")
public class RemoteApiMap extends HashMap<URI, Maybe<RemoteApi>> {
}
