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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.searchbox.action.BulkableAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.mapping.PutMapping;
import net.di2e.ecdr.analytics.sync.elastic.config.ElasticConfiguration;

public class ElasticPublisher implements AutoCloseable {
    
    private static final String METACARD_TYPE = "metacard";
    private static final Logger LOGGER = LoggerFactory.getLogger( ElasticPublisher.class );
    
    private static final class ActionCountingBulkBuilder extends Bulk.Builder {
        private int actionCnt;
        
        @Override
        public Bulk.Builder addAction(@SuppressWarnings( "rawtypes" ) BulkableAction action) { 
            super.addAction( action );
            actionCnt++; 
            return this; 
        }
        
        public int getQueuedActionsCount() { 
            return actionCnt; 
        }
    }
    
    
    private JestClient client;
    private ElasticConfiguration config;
    private Map<String, ActionCountingBulkBuilder> bulkBuilders = new ConcurrentHashMap<>();
    
    public ElasticPublisher(ElasticConfiguration config) throws URISyntaxException, UnknownHostException {
        this.config = config;
        // Construct a new Jest client according to configuration via factory
        JestClientFactory factory = new JestClientFactory();
        URI uri = new URI( config.getProtocol().endsWith( "://" ) 
                           ? config.getProtocol() : config.getProtocol() +  "://" + config.getHost() + ':' + config.getPort());
        factory.setHttpClientConfig(new HttpClientConfig
                               .Builder(uri.toString())
                               .multiThreaded(true)
                               //Per default this implementation will create no more than 2 concurrent connections per given route
                               .defaultMaxTotalConnectionPerRoute(config.getConcurrencyPerRoute())
                               // and no more 20 connections in total
                               .maxTotalConnection(config.getMaxConcurrency())
                               .build());
        client = factory.getObject();
        LOGGER.info("Elastic publisher initialized with:" + uri);
    }
    
    /**
     * Create the specified index
     * @param name
     */
    public int createIndex( String name ) throws IOException {
        // String settings = "\"settings\" : {\n" +
        // " \"number_of_shards\" : 5,\n" +
        // " \"number_of_replicas\" : 1\n" +
        // " }\n";
        JestResult result = client.execute(new CreateIndex.Builder( name ).build());
        LOGGER.info("Create index response code:" + result.getResponseCode());
        // add support settings without extra dependencies on elaticsearch
        // .settings(
        // Settings.builder().loadFromSource(settings).build().getAsMap()).build());
        return result.getResponseCode();
    }
    
    /**
     * Delete the specified index
     * @param name
     * @throws IOException
     */
    public int deleteIndex( String name ) throws IOException {
        JestResult result = client.execute(new DeleteIndex.Builder( name ).build());
        LOGGER.debug("Deleting index response code:" + result.getResponseCode());
        return result.getResponseCode();
    }

    /*
     * This mapping has a hard-coded mapping name in as much as the fields are for a specific purpose.
     * It maps a singular "centroid" property which is manually calculated and appended by the ElasticSync code.
     * This is in addition to treating the already standardized GeoJson geometry as a proper shape.
     * Thus, geo_shape queries supported by Elasticsearch can take advantage of true polygon intersection logic;
     * while at the same time a quick centroid based lookup can be done on the centroid by the Kibana coordinate map in particular.
     * If the mapping system would have supported both the geo_shape and coordinates as geo_points at the same time,
     * the centroid addition might have been avoided. However, it wouldn't have been as efficient anyway.
     */
    private static final String METACARD_MAPPING = 
          "{ \"" + METACARD_TYPE + "\": {"
          + "\"properties\": {"
          +    "\"properties.centroid\": {"
          +      "\"type\": \"geo_point\""
          +    "},"
          +    "\"geometry\": {"
          +              "\"type\": \"geo_shape\","
          +              "\"tree\": \"quadtree\","
          +              "\"precision\": \"1m\""
          +    "}"
          + "}"
          + "} }";
    
    /**
     * Built both for mapping the geojson standardized geometry as an Elasticsearch geo_shape and
     * presumes the extra centroid point we'll add for the geo_point only capable charts.
     * @param index
     */
    public int createMetacardMapping(String index) throws IOException {
        //try to delete in case it exists
        JestResult result = client.execute(new DeleteIndex.Builder(index).type(METACARD_TYPE).build());
        LOGGER.debug("Delete index mapping response code:" + result.getResponseCode());
        result = client.execute(new PutMapping.Builder(index, METACARD_TYPE, METACARD_MAPPING).build());
        LOGGER.info("Create index mapping response code:" + result.getResponseCode());
        return result.getResponseCode();
    }
    
    /**
     * Given the specific index, and the implicit type code of metacard, append
     * @param idx
     * @param id
     * @param metacard
     * @return
     * @throws IOException
     */
    public void queueBundleRequest(String idx, String id, JSONObject metacard) throws IOException {
       ActionCountingBulkBuilder bulkBuilder = bulkBuilders.get( idx );
       if (bulkBuilder == null) {
          bulkBuilder = (ActionCountingBulkBuilder)
                  new ActionCountingBulkBuilder()
                      .defaultIndex(idx)
                      .defaultType(METACARD_TYPE);
          bulkBuilders.put( idx, bulkBuilder );
       }
       bulkBuilder.addAction(new Index.Builder(metacard).id( id ).build());
       if (bulkBuilder.getQueuedActionsCount() >= config.getMaxBulkIndexingRequests()) {
           flushBulkRequest(idx);
       }
    }
    
    
    /**
     * Give the specified index, flush any pending document indexing requests as a bulk operation.
     * @param idx
     * @throws IOException
     */
    public void flushBulkRequest(String idx) throws IOException {
       Bulk.Builder bulkBuilder = bulkBuilders.remove( idx );
       //add a check for queue count > 0
       if (bulkBuilder != null) {
          JestResult result = client.execute( bulkBuilder.build() );
          LOGGER.info("Flush document index request response code:" + result.getResponseCode());
       }
    }
    
    public void close() throws Exception {
        client.close();
    }
    
}
