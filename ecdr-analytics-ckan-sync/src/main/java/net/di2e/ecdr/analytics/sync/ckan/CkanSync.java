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

import net.di2e.ecdr.analytics.sync.ckan.config.CkanConfiguration;

public interface CkanSync {
    
    /**
     * Get the config driving this sync instance
     * @return
     */
    CkanConfiguration getCkanConfig();
    /**
     * If verbose, Json formatted documents to be pushed to CKAN are echoed to the console.
     * @param verbose
     */
    void setVerbose(boolean verbose);
    
    /**
     * Following a naming convention of sourceid-$configuredSplitPropValue-dataset, creating and populate datasets for a source
     * @param sourceId - DIB source to create datasets for
     * @param ownerOrg - A valid organization
     * @param uriStr - URL for the dataset
     * @param props - searchable key/value props for the dataset
     * @param andSync - and create resource refs
     * @return
     */
    boolean createDatasets(String sourceId, String ownerOrg, String uriStr, Map<String, String> props, boolean andSync );
    /**
     * Following a naming convention of sourceid-$splitPropValue-dataset, creating and populate datasets for a source
     * @param sourceId - DIB source to create datasets for
     * @param splitProperty - This property ( like isr.category ) is a DIB metacard prop whose unique value will cause dataset splits
     * @param ownerOrg - A valid organization
     * @param uriStr - URL for the dataset
     * @param props - searchable key/value props for the dataset
     * @param andSync - and create resource refs
     * @return
     */
    boolean createDatasets(String sourceId, String splitProperty,  String ownerOrg, String uriStr, Map<String, String> props, boolean andSync );
    /**
     * Creates the named dataset in CKAN
     * @param id - A unique ID for the dataset
     * @param name - dataset name
     * @param ownerOrg - A valid organization
     * @param uriStr - URL for the dataset
     * @param props - searchable key/value props for the dataset
     * @return - true/success or false/failure
     */
    boolean createDataset(String id, String name, String ownerOrg, String uriStr, Map<String, String> props);
    
    /**
     * Deletes dataset(s) in CKAN.
     * WARNING. Due to CKAN's implementation, 
     * @param dsNameOrId
     * @return - true/success or false/failure
     */
    boolean deleteDataset(String dsNameOrId);
    
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
     * @param dsNameOrId - CKAN dataset ID or Name to associate resources to
     * @param dryRun - if true, connect to CKAN but don't create resource refs.
     * @return
     */
    Map<String, String> sync( String sourceId, String dsNameOrId, boolean dryRun );

}
