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
package net.di2e.ecdr.analytics.sync.elastic.commands;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

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
import net.di2e.ecdr.analytics.sync.elastic.ElasticSync;

@Command(scope = "elasticsearch", name = "sync", description = "Synchronizes records into an Elastic index for analysis. Default index is named metacards."
        + "Synchronizes metacards to an Elasticsearch index based on a configurable DDF query and Elasticsearch connection.")
@Service
public class ElasticSyncCommand implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger( ElasticSyncCommand.class );
    static final String SYNC_DIR = "sync";

    @Argument(index = 0, name = "sourceIds", description = "The name of the Source/Site to synchronize", required = false, multiValued = true)
    @Completion(value = ElasticSyncCompleter.class)
    private List<String> ids;

    @Option(name = "--index", description = "Specify --index=my_index you would like to sync metacards into")
    private String index = "metacards";

    @Option(name = "--log", description = "Logs metacards to disk under the $DDF_HOME/sync directory")
    private boolean log = false;

    @Option(name = "--dryrun", description = "Tests the DDF query and prints or logs records as specified in other args. Connects to Elasticsearch without indexing records.")
    private boolean dryRun = false;
    
    @Option(name = "--verbose", description = "Provide verbose output on transferred records to the screen")
    private boolean verbose = false;

    @Reference
    private CatalogFramework framework;
    @Reference
    private ElasticSync elasticSync;

    private PrintStream console = System.out;
    
    public ElasticSyncCommand() {
    }

    @Override
    public Object execute() throws Exception {
        
        try {
            elasticSync.setVerbose( verbose );
            if (log) {
               elasticSync.setDumpDir( new File( SYNC_DIR ) );
            }
            if ( CollectionUtils.isNotEmpty( ids ) ) {
                ids.forEach( sourceId -> { 
                    Map<String, String> syncResults = elasticSync.sync( sourceId, index, dryRun );
                    for (String key:syncResults.keySet()) {
                       console.println( key + '=' + syncResults.get( key ) );
                    }
                } );
            } else {
               framework.getSourceIds().forEach( ( sourceId ) -> {
                   Map<String, String> syncResults = elasticSync.sync( sourceId, index, dryRun );
                   for (String key:syncResults.keySet()) {
                      console.println( key + '=' + syncResults.get( key ) );
                   }
               } );
            }
        } catch ( Exception e ) {
            console.println( "Encountered error while trying to perform command. Check log for more details." );
            LOGGER.warn( "Error while performing command.", e );
        }
        return null;
    }

}
