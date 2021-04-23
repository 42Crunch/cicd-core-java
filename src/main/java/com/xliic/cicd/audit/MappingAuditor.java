/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import com.xliic.cicd.audit.JsonParser.Bundled;
import com.xliic.cicd.audit.client.Client;
import com.xliic.cicd.audit.client.RemoteApi;
import com.xliic.cicd.audit.client.RemoteApiMap;
import com.xliic.cicd.audit.config.Mapping;
import com.xliic.cicd.audit.model.api.ErrorMessage;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.common.Workspace;
import com.xliic.openapi.bundler.BundlingException;
import com.xliic.openapi.bundler.ReferenceResolutionFailure;

public class MappingAuditor {

    private Logger logger;
    private Client client;

    public MappingAuditor(Client client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    RemoteApiMap audit(Workspace workspace, Mapping mapping) throws IOException {
        RemoteApiMap uploaded = new RemoteApiMap();

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            URI file = workspace.resolve(entry.getKey());
            String apiId = entry.getValue();
            Maybe<Boolean> isOpenApi = Util.isOpenApiFile(file, workspace);
            if (isOpenApi.isOk() && isOpenApi.getResult() == true) {
                // this is good OpenAPIFile, upload it
                logger.info(
                        String.format("Uploading file for security audit: %s", workspace.relativize(file).getPath()));
                try {
                    Bundled bundled = JsonParser.bundle(file, workspace);
                    Maybe<RemoteApi> api = client.updateApi(apiId, bundled.json);
                    if (api.isOk()) {
                        api.getResult().setMapping(bundled.mapping);
                    }
                    uploaded.put(file, api);
                } catch (AuditException e) {
                    // in case of parsing error during bundling, do not stop audit
                    uploaded.put(file, new Maybe<RemoteApi>(new ErrorMessage(e)));
                } catch (BundlingException e) {
                    for (ReferenceResolutionFailure failure : e.getFailures()) {
                        logger.error(String.format("Failed to resolve reference in %s at %s: %s", failure.sourceFile,
                                failure.sourcePointer, failure.message));
                    }
                    uploaded.put(file, new Maybe<RemoteApi>(new ErrorMessage(e)));
                }
            } else if (isOpenApi.isOk() && isOpenApi.getResult() == false) {
                // not an OpenAPI file, but it is mapped so let's put an error message for it
                uploaded.put(file,
                        new Maybe<>(
                                new ErrorMessage(String.format("Mapped file '%s' API ID '%s' is not an OpenAPI file",
                                        workspace.relativize(file).getPath(), apiId))));
            } else {
                // failed to parse
                uploaded.put(file, new Maybe<>(new ErrorMessage(String.format("Mapped file '%s' API ID '%s': %s",
                        workspace.relativize(file).getPath(), apiId, isOpenApi.getError().getMessage()))));
            }
        }

        return uploaded;
    }

}
