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

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

@Service
public class CkanOrganizationCompleter implements Completer {

    /**
     * @param session
     *            the beginning string typed by the user
     * @param commandLine
     *            the position of the cursor
     * @param candidates
     *            the list of completions proposed to the user
     */
    public int complete( Session session, CommandLine commandLine, List<String> candidates ) {
        StringsCompleter delegate = new StringsCompleter();
        delegate.getStrings().add( "myorg-name" );
        return delegate.complete( session, commandLine, candidates );
    }

}
