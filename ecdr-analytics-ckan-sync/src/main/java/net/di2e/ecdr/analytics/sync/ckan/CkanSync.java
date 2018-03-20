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
package net.di2e.ecdr.analytics.sync.ckan;

import java.util.Map;

public interface CkanSync {
    
    /**
     * If verbose, Json formatted documents to be pushed to CKAN are echoed to the console.
     * @param verbose
     */
    void setVerbose(boolean verbose);

    /**
     * Creates the named dataset in CKAN  ...will add geospatially coherent mapping with type name "metacard"
     * @param name
     * @return - true/success or false/failure
     */
    boolean createDataset(String name);
    
    /**
     * Deletes the named dataset in CKAN
     * @param name
     * @return - true/success or false/failure
     */
    boolean deleteDataset(String name);
    
    /**
     * Synchronize the specified DDF source. Configuration of the DDF Query is handled through config pages.
     * @param sourceId
     * @param dataset - in CKAN to post documents into
     * @param dryRun - if true, connect to CKAN but don't create resource refs.
     * @return
     */
    Map<String, String> sync( String sourceId, String dataset, boolean dryRun );
    
    /**
     * Synchronize all DDF sources to the specified CKAN dataset. Configuration of the DDF Query is handles through config pages.
     * @param dataset - in CKAN to post documents into
     * @param dryRun - if true, connect to CKAN but don't don't create resource refs.
     * @return
     */
    Map<String, String> syncAll(String dataset, boolean dryRun);
}
