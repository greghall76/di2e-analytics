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
package net.di2e.ecdr.analytics.sync.elastic;

import java.io.File;
import java.util.Map;

public interface ElasticSync {
    
    /**
     * If verbose, Json formatted documents to be pushed to Elasticsearch are echoed to the console.
     * @param verbose
     */
    void setVerbose(boolean verbose);
    
    /**
     * If set, the sync process will dump Json formatted records to $DDF_HOME/sync using the format they 
     * are sent to Elasticsearch in. ( which adds a centroid in addition to the GeoJson geometry because geo_points are required for the coordinate map.
     * @param dumpDir
     */
    void setDumpDir(File dumpDir);

    /**
     * Creates the named index in Elasticsearch as well as a geospatially coherent mapping with type name "metacard"
     * @param idx
     * @return
     */
    int createIndex(String idx);
    
    /**
     * Deletes the named index in Elasticsearch
     * @param idx
     * @return
     */
    int deleteIndex(String idx);
    
    /**
     * Synchronize the specified DDF source. Configuration of the DDF Query is handled through config pages.
     * @param sourceId
     * @param targetIndex - index in Elasticsearch to post documents into
     * @param dryRun - if true, connect to Elasticsearch but don't index and documents.
     * @return
     */
    Map<String, String> sync( String sourceId, String targetIndex, boolean dryRun );
    
    /**
     * Synchronize all DDF sources to the specified ElasticSearch index. Configuration of the DDF Query is handles through config pages.
     * @param targetIndex - index in Elasticsearch to post documents into
     * @param dryRun - if true, connect to Elasticsearch but don't index and documents.
     * @return
     */
    Map<String, String> syncAll(String targetIndex, boolean dryRun);
}
