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
import java.util.Map;
import java.util.Set;

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

@Command(scope = "ckan", name = "sync", description = "Synchronizes records into an Elastic index for analysis. Default index is named metacards."
        + "Synchronizes metacards to an Elasticsearch index based on a configurable DDF query and Elasticsearch connection.")
@Service
public class CkanSyncCommand implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger( CkanSyncCommand.class );
    static final String SYNC_DIR = "sync";

    @Argument(index = 0, name = "sourceIds", description = "The name of the Source/Site to synchronize", required = false, multiValued = true)
    @Completion(value = CkanSyncCompleter.class)
    private Set<String> ids;

    @Option(name = "--dryrun", description = "Tests the DDF query and prints or logs records as specified in other args. Connects to Elasticsearch without indexing records.")
    private boolean dryRun = false;
    
    @Option(name = "--verbose", description = "Provide verbose output on transferred records to the screen")
    private boolean verbose = false;

    @Reference
    private CatalogFramework framework;
    @Reference
    private CkanSync ckanSync;

    private PrintStream console = System.out;
    
    public CkanSyncCommand() {
    }

    @Override
    public Object execute() throws Exception {
        
        try {
            ckanSync.setVerbose( verbose );
            if ( CollectionUtils.isEmpty( ids ) ) {
                ids = framework.getSourceIds();
            }
            ids.forEach( sourceId -> { 
                // CKAN has restrictions on dataset names that they must be alphanumeric and optionally hyphens and underbars only
                final String dsId = sourceId.trim().replace( '.', '-' );
                Map<String, String> syncResults = ckanSync.sync( sourceId, dsId, dryRun );
                for (String key:syncResults.keySet()) {
                   console.println( key + '=' + syncResults.get( key ) );
                }
            } );
        } catch ( Exception e ) {
            console.println( "Encountered error while trying to perform command. Check log for more details." );
            LOGGER.warn( "Error while performing command.", e );
        }
        return null;
    }

}
