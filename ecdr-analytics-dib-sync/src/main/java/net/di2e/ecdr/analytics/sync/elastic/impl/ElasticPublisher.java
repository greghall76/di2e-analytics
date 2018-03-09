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
package net.di2e.ecdr.analytics.sync.elastic.impl;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.json.simple.JSONObject;

import net.di2e.ecdr.analytics.sync.elastic.ElasticConfiguration;

public class ElasticPublisher {
    
    
    private RestHighLevelClient client;
    
    public ElasticPublisher(ElasticConfiguration config) {
         client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(config.getHost(), config.getPort(), config.getProtocol())));
    }
    
    /**
     * 
     * @param name
     */
    public void createIndex(String name) throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(name);
        //add Settings? e.g.        
//            "settings" : {
//                "index" : {
//                    "number_of_shards" : 3,
//                    "number_of_replicas" : 2
//                }
//            }
        
        client.indices().create( createIndexRequest );
    }
    
    /**
     * 
     * @param idx
     * @param type
     * @param id
     * @param metacard
     * @return
     * @throws IOException
     */
    public void sendBundleRequest(String idx, String type, String id, JSONObject metacard) throws IOException {
        IndexRequest mcardIndexRequest = new IndexRequest(idx, type, id);
        BulkRequest bulkRequest = new BulkRequest().add(mcardIndexRequest, (Object) metacard.toJSONString());
        //headers
        client.bulk( bulkRequest, null );
    }
    
}
