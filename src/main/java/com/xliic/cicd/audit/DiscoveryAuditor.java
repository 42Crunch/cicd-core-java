/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.xliic.cicd.audit.JsonParser.Bundled;
import com.xliic.cicd.audit.client.Client;
import com.xliic.cicd.audit.client.RemoteApi;
import com.xliic.cicd.audit.client.RemoteApiMap;
import com.xliic.cicd.audit.config.Mapping;
import com.xliic.cicd.audit.model.api.Api;
import com.xliic.cicd.audit.model.api.ApiCollection;
import com.xliic.cicd.audit.model.api.ApiCollections;
import com.xliic.cicd.audit.model.api.ErrorMessage;
import com.xliic.cicd.audit.model.api.Maybe;
import com.xliic.common.Workspace;
import com.xliic.openapi.bundler.BundlingException;
import com.xliic.openapi.bundler.ReferenceResolutionFailure;

public class DiscoveryAuditor {
    private String collectionId;
    private Logger logger;
    private Client client;

    public DiscoveryAuditor(Client client) {
        this.client = client;
    }

    RemoteApiMap audit(Workspace workspace, OpenApiFinder finder, String collectionName, String[] search,
            Mapping mapping) throws IOException, InterruptedException, AuditException {
        DiscoveredOpenApiFiles discovered = discoverOpenApiFiles(workspace, finder, search, mapping);

        // collect list of successfully detected OpenAPI files
        ArrayList<URI> openApiFilenames = new ArrayList<>();
        for (Entry<URI, Maybe<Boolean>> openapi : discovered.entrySet()) {
            if (openapi.getValue().isOk()) {
                openApiFilenames.add(openapi.getKey());
            }
        }

        collectionId = createOrFindCollectionId(Util.makeName(collectionName));

        RemoteApiMap remoteApis = uploadFilesToCollection(openApiFilenames, workspace, collectionId);

        // add files which failed to parse to the list of errors
        for (Entry<URI, Maybe<Boolean>> openapi : discovered.entrySet()) {
            if (openapi.getValue().isError()) {
                remoteApis.put(openapi.getKey(), new Maybe<>(openapi.getValue().getError()));
            }
        }

        return remoteApis;
    }

    private DiscoveredOpenApiFiles discoverOpenApiFiles(Workspace workspace, OpenApiFinder finder, String[] search,
            Mapping mapping) throws IOException, InterruptedException, AuditException {
        DiscoveredOpenApiFiles discovered = new DiscoveredOpenApiFiles();

        List<URI> files = findOpenapiFiles(workspace, finder, search);
        String filenames = files.stream().map(file -> workspace.relativize(file).getPath())
                .collect(Collectors.joining(","));
        logger.info(String.format("Files matching search criteria: %s", filenames));
        for (URI file : files) {
            if (!mapping.containsKey(workspace.relativize(file).getPath())) {
                Maybe<Boolean> openapi = Util.isOpenApiFile(file, workspace);
                // put discovered OpenAPI files onto the list of discovered ones
                // also add there parsing errors
                if (openapi.isOk() && openapi.getResult() == true || openapi.isError()) {
                    discovered.put(file, openapi);
                }
            }
        }

        String discoveredFilenames = discovered.keySet().stream().map(file -> workspace.relativize(file).getPath())
                .collect(Collectors.joining(","));
        logger.info(String.format("Discovered OpenAPI files: %s", discoveredFilenames));

        return discovered;
    }

    private RemoteApiMap uploadFilesToCollection(ArrayList<URI> files, Workspace workspace, String collectionId)
            throws IOException, AuditException {
        RemoteApiMap uploaded = new RemoteApiMap();

        purgeCollection(collectionId);
        for (URI file : files) {
            logger.info(String.format("Uploading file for security audit: %s", workspace.relativize(file).getPath()));
            try {
                Bundled bundled = JsonParser.bundle(file, workspace);
                String apiName = Util.makeName(workspace.relativize(file).getPath());
                Maybe<RemoteApi> api = client.createApi(collectionId, apiName, bundled.json);
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
        }

        return uploaded;
    }

    private void purgeCollection(String collectionId) throws IOException, AuditException {
        Maybe<ApiCollection> collection = client.listCollection(collectionId);
        if (collection.isError()) {
            throw new AuditException("Unable to read collection: " + collection.getError().getMessage());
        }
        for (Api api : collection.getResult().list) {
            Maybe<String> deleted = client.deleteApi(api.desc.id);
            if (deleted.isError()) {
                throw new AuditException("Unable to delete collection: " + deleted.getError().getMessage());
            }
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private String createOrFindCollectionId(String collectionName) throws AuditException, IOException {

        // check existing collections to see if collection with collectionName already
        // exists
        Maybe<ApiCollections> collections = client.listCollections();
        if (collections.isError()) {
            throw new AuditException("Unable to list collection: " + collections.getError().getMessage());
        }
        for (ApiCollections.ApiCollection collection : collections.getResult().list) {
            if (collection.desc.name.equals(collectionName)) {
                return collection.desc.id;
            }
        }

        // no collection collectionName found, create a new one
        Maybe<ApiCollections.ApiCollection> collection = client.createCollection(collectionName);
        if (collection.isError()) {
            throw new AuditException("Unable to create collection: " + collection.getError().getMessage());
        }
        return collection.getResult().desc.id;
    }

    private List<URI> findOpenapiFiles(Workspace workspace, OpenApiFinder finder, String[] search)
            throws IOException, InterruptedException, AuditException {
        finder.setPatterns(search);
        List<URI> openApiFiles = finder.find();
        return openApiFiles;
    }
}
