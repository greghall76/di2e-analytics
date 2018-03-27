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

import java.util.List;
import java.util.Map;

public interface CkanSync {
    
    /**
     * If verbose, Json formatted documents to be pushed to CKAN are echoed to the console.
     * @param verbose
     */
    void setVerbose(boolean verbose);

    /**
     * Creates the named dataset in CKAN
     * @param id - A unique ID for the dataset
     * @param name - dataset name
     * @param ownerOrg - A valid organization
     * @param uriStr - URL for the dataset
     * @return - true/success or false/failure
     */
    boolean createDataset(String id, String name, String ownerOrg, String uriStr );
    
    /**
     * Deletes the named dataset in CKAN.
     * WARNING. Due to CKAN's implementation, 
     * @param name
     * @return - true/success or false/failure
     */
    boolean deleteDataset(String name);
    
    /**
     * Takes a listing of datasets present in the CKAN instance.
     * @return
     */
    List<String> listDatasets();
    
    /**
     * Returns org names from CKAN
     * @return
     */
    List<String> listOrganizations();
    
    /**
     * Creates a CKAN org which could then own a dataset.
     * You can optionally manually create an org through the UI and then reference it
     * @param orgName - name/id for the org
     * @param description - simple description
     * @param imageUriStr - URL to an icon type image for the org. 
     * @return
     */
    boolean createOrganization(String orgName, String description, String imageUriStr);
    
    /**
     * Synchronize the specified DDF source. Configuration of the DDF Query is handled through config pages.
     * @param sourceId
     * @param dsId - in CKAN dataset ID to post documents into
     * @param dryRun - if true, connect to CKAN but don't create resource refs.
     * @return
     */
    Map<String, String> sync( String sourceId, String dsId, boolean dryRun );

}
