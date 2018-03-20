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

import java.io.PrintStream;
import java.util.List;

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

@Command(scope = "cdr:elastic", name = "index", description = "Creates/deletes metacard indexes in Elastic to be used for synchronization and analysis. Defaults: creates index named metacards"
        + "Creates/deletes metacards indexes in Elastic used for analysis based on a configurable Elasticsearch connection. Defaults to metacards if names not provided.")
@Service
public class ElasticIndexCommand implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger( ElasticIndexCommand.class );

    @Argument(index = 0, name = "names", description = "The names of the indixes to create/delete", required = false, multiValued = true)
    @Completion(value = ElasticIndexCompleter.class)
    private List<String> indexes;

    @Option(name = "--create", description = "Create the index")
    private boolean create = true;
    
    @Option(name = "--delete", description = "Delete the index")
    private boolean delete = false;

    
    @Option(name = "--verbose", description = "Provide verbose output to the screen")
    private boolean verbose = false;

    @Reference
    private CatalogFramework framework;
    @Reference
    private ElasticSync elasticSync;

    private PrintStream console = System.out;
    
    public ElasticIndexCommand() {
    }

    @Override
    public Object execute() throws Exception {
        
        try {
            elasticSync.setVerbose( verbose );
            if ( CollectionUtils.isNotEmpty( indexes ) ) {
                indexes.forEach( idx -> {
                    int httpResponse;
                    if (delete) {
                      httpResponse = elasticSync.deleteIndex( idx );
                    } else {
                      httpResponse = elasticSync.createIndex( idx );
                    }
                    console.print( "HTTP response: " + httpResponse );
                } );
            } else {
                int httpResponse;
                if (delete) {
                  httpResponse = elasticSync.deleteIndex( "metacards" );
                } else {
                  httpResponse = elasticSync.createIndex( "metacards" );
                }
                console.print( "HTTP response: " + httpResponse );
            }
        } catch ( Exception e ) {
            console.println( "Encountered error while trying to perform command. Check log for more details." );
            LOGGER.warn( "Error while performing command.", e );
        }
        return null;
    }

}
