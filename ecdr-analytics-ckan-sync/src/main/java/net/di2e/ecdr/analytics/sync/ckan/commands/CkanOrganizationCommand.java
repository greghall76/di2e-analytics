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
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.di2e.ecdr.analytics.sync.ckan.CkanSync;

@Command(scope = "ckan", name = "org", description = "Creates/deletes organizations in CKAN which can be referenced later to create a dataset."
        + "Creates/lists organizations in CKAN ( no delete )")
@Service
public class CkanOrganizationCommand implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger( CkanOrganizationCommand.class );

    @Argument(index = 0, name = "name", description = "The name of the organization to create", required = false)
    @Completion(value = CkanDatasetCompleter.class)
    private String orgName;
    
    @Argument(index = 1, name = "description", description = "The description of the organization to create", required = false, valueToShowInHelp = "description of $orgName")
    @Completion(value = CkanDatasetCompleter.class)
    private String description;
    
    @Option(name = "--create", description = "Create an organization and pass the org name")
    private boolean create = true;
            
    @Option(name = "--list", description = "List all organizations")
    private boolean list = false;

    @Option(name = "--verbose", description = "Provide verbose output to the screen")
    private boolean verbose = false;
    
    @Reference
    private CkanSync ckanSync;

    private PrintStream console = System.out;
    
    public CkanOrganizationCommand() {
    }

    @Override
    public Object execute() throws Exception {
        
        try {
            ckanSync.setVerbose( verbose );
            if ( list ) {
                List<String> orgListing = ckanSync.listOrganizations();
                orgListing.forEach( org -> {
                    console.print( org + ',');
                } );
            } else if ( create ) {
                if (  orgName != null && !orgName.isEmpty() ) {
                    if ( description == null) {
                        description = "description of " + orgName;
                    }
                    ckanSync.createOrganization( orgName, description, "" );
                } else {
                    console.print( "You must provide the organization name." );
                }
            } else {
                console.print( "You must specify either --list or --create and the organization name(s)." );
            }
            
        } catch ( Throwable e ) {
            console.println( "Encountered error while trying to perform command. Check log for more details." );
            LOGGER.warn( "Error while performing command.", e );
        }
        return null;
    }

}
