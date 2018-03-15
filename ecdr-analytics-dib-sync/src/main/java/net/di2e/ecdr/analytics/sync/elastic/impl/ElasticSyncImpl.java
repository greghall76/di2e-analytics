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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.common.Security;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.transform.MetacardTransformer;

import ddf.security.Subject;
import net.di2e.ecdr.analytics.sync.dib.SyncConfiguration;
import net.di2e.ecdr.analytics.sync.elastic.ElasticConfiguration;
import net.di2e.ecdr.analytics.sync.elastic.ElasticSync;

public class ElasticSyncImpl implements ElasticSync {
    
    private static DateTimeFormatter formatter;

    static {
        DateTimeParser[] parsers = { ISODateTimeFormat.date().getParser(), ISODateTimeFormat.dateTime().getParser(), ISODateTimeFormat.dateTimeNoMillis().getParser(),
                                     ISODateTimeFormat.basicDateTime().getParser(), ISODateTimeFormat.basicDateTimeNoMillis().getParser() };
        formatter = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger( ElasticSyncImpl.class );
//    private static DateFormat df = new SimpleDateFormat( "yyyy-MM-dd\'T\'HH:mm:ss.SSSZ" );
// 
//    private static final String RESULT_WKT = "result-wkt";
//    private static final String XPATH_KEYWORD = "//*[local-name()=\'keyword\']";
//    private static final String XPATH_CATEGORY = "//*[local-name()=\'category\']";
//    private static final String XPATH_TYPE = "//ddms:type";
//    private static final String XPATH_SECURITY = "//*[local-name()=\'security\']";
    private static final String DDMS_NAMESPACE = "http://metadata.dod.mil/mdr/ns/DDMS/2.0/";
    private static final String ICISM_NAMESPACE = "urn:us:gov:ic:ism:v2";
//    private static final String VALUE_ATTRIBUTE = "value";
//    private static final String QUALIFIER_ATTRIBUTE = "qualifier";

    private Map<String, String> namespaceMap = new HashMap<>();
    
    private boolean verbose;
    private File dumpDir;
    private CatalogFramework framework;
    private FilterBuilder filterBuilder;
    private SyncConfiguration syncConfig;
    private ElasticConfiguration elasticConfig;
    private ElasticPublisher elasticPublisher;
    private MetacardTransformer geoJsonTransformer;
     
    private Map<String, Serializable> requestProperties;
    
    private final JSONParser jsonParser = new JSONParser();
    
    private PrintStream console = System.out;
    
    public ElasticSyncImpl( CatalogFramework fw, FilterBuilder builder, 
                            SyncConfiguration config, ElasticConfiguration elasticConfig,
                            MetacardTransformer geoJsonTransformer) {
        this.framework = fw;
        this.filterBuilder = builder;
        this.syncConfig = config;
        this.elasticConfig = elasticConfig;
        this.geoJsonTransformer = geoJsonTransformer;

        this.requestProperties = new HashMap<>();
        this.requestProperties.put( "ddf.security.subject", getSystemSubject() );
        namespaceMap.put( "ddms", DDMS_NAMESPACE );
        namespaceMap.put( "ICISM", ICISM_NAMESPACE );
        LOGGER.info( "ElasticSyncImpl is online...." );
    }
    
    private void connect() throws URISyntaxException, UnknownHostException {
        if (elasticPublisher == null) {
            this.elasticPublisher = new ElasticPublisher(elasticConfig);
        }
    }

    @Override
    public void setVerbose( boolean verbose ) {
      this.verbose = verbose;
    }

    @Override
    public void setDumpDir( File dumpDir ) {
      this.dumpDir = dumpDir;   
    }

    @Override
    public int createIndex(String idx) {
        int response = 0;
        try {
            connect();
            response = elasticPublisher.createIndex( idx );
            console.println( "Index create: HTTP Response: " + response );
            if (response == 200) {
                console.print( "Creating metacard mapping..." );
                // ensure our mapping is present for any index we create
                response = elasticPublisher.createMetacardMapping(idx);
            }
        } catch (Exception e) {
            LOGGER.error( "Exception creating index in Elasticsearch=>" + e );
            console.print( "Exception creating index:" + idx + ". See log for details" );
        }
        return response;
    }
    
    @Override
    public int deleteIndex(String idx) {
        int response = 0;
        try {
          connect();
          response = elasticPublisher.deleteIndex( idx );
        } catch (Exception e) {
          LOGGER.error( "Exception deleting index in Elasticsearch=>" + e );
          console.print( "Exception deleting index:" + idx + ". See log for details" );
        }
        return response;
    }
    
    @Override
    public Map<String, String> sync( String sourceId, String targetIndex, boolean dryRun ) {
        long beginMs = System.currentTimeMillis();
        HashMap<String, String> resultProperties = new HashMap<>();
        syncRecords( sourceId, targetIndex, resultProperties, dryRun );
        try {
            if (!dryRun) {
              elasticPublisher.flushBulkRequest( targetIndex );
            }
        } catch (IOException ioE) {
            LOGGER.error( "IOException syncing records to Elasticsearch=>" + ioE );
        } finally {
            long delta = System.currentTimeMillis() - beginMs;
            resultProperties.put( "elastic.sync.queue.time.ms", String.valueOf( delta ) );
          }
        return resultProperties;
    }

    @Override
    public Map<String, String> syncAll(String targetIndex, boolean dryRun) {
        long beginMs = System.currentTimeMillis();
        HashMap<String, String> resultProperties = new HashMap<>();
        framework.getSourceIds().forEach( ( sourceId ) -> {
            syncRecords( sourceId, targetIndex, resultProperties, dryRun );
        } );
     
        try {
          if (!dryRun) {
            elasticPublisher.flushBulkRequest( targetIndex );
          }
        } catch (IOException ioE) {
          LOGGER.error( "IOException syncing records to Elasticsearch=>" + ioE );
        } finally {
          long delta = System.currentTimeMillis() - beginMs;
          resultProperties.put( "elastic.sync.queue.time.ms", String.valueOf( delta ) );
        }
        return resultProperties;
    }

    /**
     * 
     * @param sourceId
     * @param resultProperties
     */
    protected void syncRecords( String sourceId, String index, Map<String, String> resultProperties, boolean dryRun ) {
        
        console.println( "Querying sync source: " + sourceId );
        
        boolean hasMoreRecords = true;

        Date startDate = StringUtils.isBlank( syncConfig.getStartDate() ) ? null 
                                              : formatter.parseDateTime( syncConfig.getStartDate() ).toDate();
        Date endDate = new Date();
        String queryKeywords = syncConfig.getKeywords();
        int maxRecordCount = syncConfig.getMaxRecordsPerPoll();
        int docCnt = 0;
        long deltaTime = 0;
        
        try {
            connect();
            
            if ( dumpDir != null ) {
                if ( !dumpDir.exists() ) {
                   dumpDir.mkdirs();
                }
            }
            while ( hasMoreRecords ) {
                long beginTime = System.currentTimeMillis();
                QueryImpl query = new QueryImpl( getFilter( startDate, endDate, queryKeywords ), 1, maxRecordCount, getSortBy(), true, 300000L );
                QueryRequestImpl queryRequest = new QueryRequestImpl( query, Arrays.asList( new String[] { sourceId } ) );
                queryRequest.setProperties( this.requestProperties );
                QueryResponse response = framework.query( queryRequest );

                List<Result> results = response.getResults();
                hasMoreRecords = (results.size() == maxRecordCount) && startDate != null;
                console.printf( "Adding details from query results for %d records from site %s", Integer.valueOf( results.size() ), sourceId );
                LOGGER.debug( "Adding details from query results for {} records from site {} in the Content Collection date range [{} - {}] and keywords[{}]",
                               Integer.valueOf( results.size() ), sourceId, startDate, endDate, queryKeywords );

                for ( Result result : results ) {
                    try {
                        Metacard metacard = result.getMetacard();
                        BinaryContent binContent = geoJsonTransformer.transform( metacard, requestProperties );
                        JSONObject jsonObject = (JSONObject) jsonParser.parse( new InputStreamReader(binContent.getInputStream()) );
                        // See about inserting a centroid for Elastic to have a simple geo_point since it's charts struggle w/ shapes
                        String wkt = metacard.getLocation();
                        if ( StringUtils.isNotBlank( wkt ) ) {
                           Geometry geometry = new WKTReader().read( wkt );
                           Coordinate coord = geometry.getCentroid().getCoordinate();
                           String center = Double.toString( coord.y ) + ',' + Double.toString( coord.x );
                           Map<String, Object> props = (Map<String, Object>)  jsonObject.get( "properties" );
                           props.put( "centroid", center );
                        }
                        docCnt++;
                        if (!dryRun) {
                          elasticPublisher.queueBundleRequest( index, metacard.getId(), jsonObject);
                        }
                        deltaTime += System.currentTimeMillis() - beginTime;
                        if (verbose) {
                            console.println("Queued:" + jsonObject.toJSONString());
                        }
                        if (dumpDir != null) {
                            FileOutputStream fos = new FileOutputStream(new File(dumpDir, '/' + metacard.getId() + ".json"));
                            fos.write(jsonObject.toJSONString().getBytes());
                            fos.close();
                        }
                            
                    } catch ( Exception e ) {
                        LOGGER.error( "Error handling result {} ", result.getMetacard().getId(), e );
                        console.printf( "Error handling result {} with Exception {} ", result.getMetacard().getId(), e.getLocalizedMessage() );
                    }
                }
            }
        } catch ( Throwable arg34 ) {
            LOGGER.warn( "Query failed against source {}", sourceId, arg34 );
        } finally {
            LOGGER.info( "Synchronized:" + docCnt + " records in " + String.valueOf(((double) deltaTime) / 1000.0) + "(s)");
            console.println( "Synchronized:" + docCnt + " records in " + String.valueOf(((double) deltaTime) / 1000.0) + "(s)");
            resultProperties.put( "elastic.doc.queue.count",  String.valueOf( docCnt ) );
            resultProperties.put( "elastic.doc.queue.time.ms", String.valueOf( deltaTime ) );
        }
    }


    private String getAttributeValue( Node item, String namespace, String attribute ) {
        String attributeValue = null;
        Node node = item.getAttributes().getNamedItemNS( namespace, attribute );
        if ( node != null ) {
            attributeValue = node.getTextContent();
        }
        return attributeValue;
    }

    protected Filter getFilter( Date startDate, Date endDate, String keywords ) {
        LOGGER.debug( "Creating query for date type {} and date range {} through {} and keywords {}", syncConfig.getDateType(), startDate, endDate,
                keywords );
        Filter filter = null;
        if ( startDate != null ) {
            filter = this.filterBuilder.attribute( syncConfig.getDateType() ).during().dates( startDate, endDate );
        }
        if ( StringUtils.isNotBlank( keywords ) ) {
            filter = filter == null ? filterBuilder.attribute( Metacard.ANY_TEXT ).like().text( keywords )
                    : filterBuilder.allOf( filter, filterBuilder.attribute( Metacard.ANY_TEXT ).like().text( keywords ) );
        }
        return filter;
    }

    protected SortBy getSortBy() {
        SortByImpl sortBy = new SortByImpl( syncConfig.getSortDateType(), SortOrder.ASCENDING );
        return sortBy;
    }


    protected Subject getSystemSubject() {
        return (Subject) Security.getInstance().runAsAdmin( () -> {
            return Security.getInstance().getSystemSubject();
        } );
    }


}
