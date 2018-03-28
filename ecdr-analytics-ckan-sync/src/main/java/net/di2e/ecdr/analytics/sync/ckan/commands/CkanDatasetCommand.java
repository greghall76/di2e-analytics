/**
 * Copyright (C) 2016 Pink Summit, LLC (info@pinksummit.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.di2e.ecdr.analytics.sync.ckan.commands;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import net.di2e.ecdr.analytics.sync.ckan.CkanSync;

@Command(scope = "ckan", name = "dataset", description = "Creates/deletes resource datasets in CKAN to be used. Defaults: creates CKAN Dataset named metacards"
        + "Creates/deletes metacards as datasets in CKAN used for convenient review on a configurable CKAN connection. Defaults to metacards if names not provided.")
@Service
public class CkanDatasetCommand implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger( CkanDatasetCommand.class );

    @Argument(index = 0, name = "sourceIds", description = "The name of the DIB/DDF Source/Site IDs to synchronize", required = false, multiValued = true)
    @Completion(value = CkanSyncCompleter.class)
    private Set<String> ids;
    
    @Option(name = "--create", description = "Create the dataset")
    private boolean create = true;
    
    @Option(name = "--org", description = "Specify --org=owning_org Required when creating a dataset" )
    private String organization;
    
    @Option(name = "--delete", description = "Delete the dataset")
    private boolean delete = false;
    
    @Option(name = "--list", description = "List all datasets")
    private boolean list = false;
    
    @Option(name = "--verbose", description = "Provide verbose output to the screen")
    private boolean verbose = false;

    @Reference
    private CatalogFramework framework;
    
    @Reference
    private CkanSync ckanSync;

    private PrintStream console = System.out;
    
    public CkanDatasetCommand() {
    }

    @Override
    public Object execute() throws Exception {
        
        try {
            ckanSync.setVerbose( verbose );
            if ( list ) {
               List<String> dsetListing = ckanSync.listDatasets();
               dsetListing.forEach( ds -> {
                  console.print(ds + ',');
               });
            } else {
                if ( CollectionUtils.isEmpty( ids ) ) {
                    ids = framework.getSourceIds();
                }

                ids.forEach( ( sourceId ) -> {
                    boolean result;
                    final String dsId = UUID.randomUUID().toString();
                    // CKAN has restrictions on dataset names that they must be alphanumeric and optionally hyphens and underbars only
                    final String dsName = sourceId.trim().replace( '.', '-' ) + "-dataset";
                    if ( delete ) {
                        result = ckanSync.deleteDataset( dsName );
                    } else if ( organization != null ) {
                        // FIXME - need dynamic URI lookup
                        String uri = "https://localhost:8993/search/catalog/";
                        if (verbose) {
                           console.println("Creating dataset:" + dsId + '/' + dsName);
                        }
                        Map<String, String> props = new HashMap<>();
                        props.put("source.id", sourceId);
                        result = ckanSync.createDataset(dsId, dsName, organization, uri, props );
                    } else {
                        console.println( "You must provide an organization when creating a dataset" );
                        result = false;
                    }
                    console.print( "Success: " + result );
                } );
            }
        } catch ( Throwable e ) {
            console.println( "Encountered error while trying to perform command. Check log for more details." );
            LOGGER.warn( "Error while performing command.", e );
        }
        return null;
    }

}
