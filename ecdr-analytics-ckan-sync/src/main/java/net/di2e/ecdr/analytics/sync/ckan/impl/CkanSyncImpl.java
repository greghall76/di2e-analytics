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
package net.di2e.ecdr.analytics.sync.ckan.impl;

import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.MetacardTransformer;

import ddf.security.Subject;
import net.di2e.ecdr.analytics.sync.ckan.CkanSync;
import net.di2e.ecdr.analytics.sync.ckan.config.CkanConfiguration;
import net.di2e.ecdr.analytics.sync.ckan.config.DibQueryConfiguration;
import net.di2e.ecdr.analytics.util.TemporalBoundsTracker;
import net.di2e.ecdr.analytics.util.TemporalCoverageHolder;

public class CkanSyncImpl implements CkanSync {
    
    private static DateTimeFormatter formatter;

    static {
        DateTimeParser[] parsers = { ISODateTimeFormat.date().getParser(), ISODateTimeFormat.dateTime().getParser(), ISODateTimeFormat.dateTimeNoMillis().getParser(),
                                     ISODateTimeFormat.basicDateTime().getParser(), ISODateTimeFormat.basicDateTimeNoMillis().getParser() };
        formatter = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger( CkanSyncImpl.class );
//    private static DateFormat df = new SimpleDateFormat( "yyyy-MM-dd\'T\'HH:mm:ss.SSSZ" );

    private static final String DDMS_NAMESPACE = "http://metadata.dod.mil/mdr/ns/DDMS/2.0/";
    private static final String ICISM_NAMESPACE = "urn:us:gov:ic:ism:v2";
//    private static final String VALUE_ATTRIBUTE = "value";
//    private static final String QUALIFIER_ATTRIBUTE = "qualifier";

    private Map<String, String> namespaceMap = new HashMap<>();
    
    private boolean verbose;
    
    private CatalogFramework framework;
    private FilterBuilder filterBuilder;
    private DibQueryConfiguration syncConfig;
    private CkanConfiguration ckanConfig;
    private MetacardTransformer geoJsonTransformer;
     
    private Map<String, Serializable> requestProperties;
    private TemporalBoundsTracker temporalBoundsTracker = new TemporalBoundsTracker();
    private final JSONParser jsonParser = new JSONParser();
    
    private PrintStream console = System.out;
    
    public CkanSyncImpl( CatalogFramework fw, FilterBuilder builder, 
                         DibQueryConfiguration config, CkanConfiguration ckanConfig,
                         MetacardTransformer geoJsonTransformer) {
        this.framework = fw;
        this.filterBuilder = builder;
        this.syncConfig = config;
        this.ckanConfig = ckanConfig;
        this.geoJsonTransformer = geoJsonTransformer;

        this.requestProperties = new HashMap<>();
        this.requestProperties.put( "ddf.security.subject", getSystemSubject() );
        namespaceMap.put( "ddms", DDMS_NAMESPACE );
        namespaceMap.put( "ICISM", ICISM_NAMESPACE );
        LOGGER.info( "CkanSyncImpl is online...." );
    }
    
    @Override
    public CkanConfiguration getCkanConfig() {
       return ckanConfig; 
    }

    @Override
    public void setVerbose( boolean verbose ) {
      this.verbose = verbose;
    }
    
    private CkanPublisher connect() {
      return new CkanPublisher(ckanConfig);
    }
    
    @Override
    public boolean createDatasets(String sourceId, String ownerOrg, String uriStr, Map<String, String> props, boolean andSync ) {
        return createDatasets(sourceId, ckanConfig.getCollectionProperty(), ownerOrg, uriStr, props, andSync);
    }
    
    @Override
    public boolean createDatasets(String sourceId, String splitProperty,  String ownerOrg, String uriStr, Map<String, String> props, boolean andSync ) {

        boolean success = true;
        
        boolean hasMoreRecords = true;

        Date startDate = StringUtils.isBlank( syncConfig.getStartDate() ) ? null 
                                              : formatter.parseDateTime( syncConfig.getStartDate() ).toDate();
        Date endDate = new Date();
        int maxRecordCount = syncConfig.getMaxRecordsPerPoll();
        int docCnt = 0;
    
        temporalBoundsTracker.clear();
        long beginTime = System.currentTimeMillis();
        
        try {
            final CkanPublisher ckanPublisher = connect();
            List<String> datasets = listDatasets();
            
            while ( hasMoreRecords && docCnt < ckanConfig.getMaxInput() ) {

                QueryResponse response = dibQuery(sourceId, startDate, endDate);
                List<Result> results = response.getResults();
                
                // Start date required since time series is the only way to segment results.
                hasMoreRecords = (results.size() == maxRecordCount) && startDate != null;
                console.printf( "Synchronizing query results for %d records from site %s\n", Integer.valueOf( results.size() ), sourceId );
                LOGGER.debug( "Synchronizing query results for {} records from site {} in the Content Collection date range [{} - {}] and keywords[{}]",
                               Integer.valueOf( results.size() ), sourceId, startDate, endDate, syncConfig.getKeywords() );
            
                for (Result result : results) {
                   docCnt++;

                   Metacard metacard = result.getMetacard();
                   Attribute splitPropAttrib = metacard.getAttribute( splitProperty );
                   if (splitPropAttrib != null) {
                      //Must be a simple primitive for collection split
                      String splitPropVal = splitPropAttrib.getValue().toString().toLowerCase();
                      String datasetName = sourceId.trim().replace( '.', '-' ) + "-" + splitPropVal + "-dataset";
                      if (!datasets.contains( datasetName )) {
                          //create the dataset
                          String dsUid = UUID.randomUUID().toString();
                          //this is a little hackey. Might not always work but need a unique URL for CKAN
                          String dsUrl = uriStr + "?source.id=" + sourceId + "&" + splitProperty + "=" + splitPropVal;
                          if (createDataset(dsUid, datasetName, ownerOrg, dsUrl, props)) {
                              datasets.add( datasetName );
                          }
                      }
                      if (andSync) {
                         createResource(ckanPublisher, datasetName, metacard);
                      }
                   } 

                   // Track max bounds seen as a way to ensure segmented increment through total result set possible
                   temporalBoundsTracker.updateBounds( metacard );
                   // Walk start date in case of a secondary query
                   TemporalCoverageHolder timeWindowProcessed = temporalBoundsTracker.getTemporalCoverageHolder( syncConfig.getDateType() );
                   if (timeWindowProcessed != null) {
                     startDate = timeWindowProcessed.getEndDate();
                   }
                }
            }
        } catch ( Throwable arg34 ) {
            LOGGER.warn( "Query failed against source {}", sourceId, arg34 );
            success = false;
        } finally {
            long deltaTime = System.currentTimeMillis() - beginTime;
            LOGGER.info( "Synchronized:" + docCnt + " records in " + String.valueOf(((double) deltaTime) / 1000.0) + "(s)");
            console.println( "Synchronized:" + docCnt + " records in " + String.valueOf(((double) deltaTime) / 1000.0) + "(s)");
        }
        return success;
    }
            
    @Override
    public boolean createDataset(String id, String name, String ownerOrg, String uriStr, Map<String, String> props ) {
        
        boolean success = false;
        
        try {
            URI uri;
            if (uriStr != null) {
                uri = URI.create( uriStr );
            } else {
                uri = null;
            }
            connect().createDataset( id, name, ownerOrg, ckanConfig.getUserId(), uri, props);
            if (verbose) {
              console.println( "Dataset: " + name + " created." );
            }
            success = true;
        } catch (Exception e) {
            LOGGER.error( "Exception creating dataset in ckan=>" + e );
            console.print( "Exception creating dataset:" + id + '/' + name + ". See log for details" );
        }
        return success;
    }
    
    @Override
    public boolean deleteDataset(String dsNameOrId) {
        
        boolean success = false;
        
        try {
          connect().deleteDataset( dsNameOrId );
          if (verbose) {
            console.println( "Dataset: " + dsNameOrId + " deleted." );
          }
          success = true;
        } catch (Exception e) {
          LOGGER.error( "Exception deleting dataset =>" + e );
          console.print( "Exception deleting dataset:" + dsNameOrId + ". See log for details" );
        }
        return success;
    }
    
    @Override
    public List<String> listDatasets() {
        List<String> datasets;
        try {
            datasets = connect().listDatasets();
        } catch (Exception e) {
            datasets = Collections.emptyList();
            LOGGER.error( "Exception listing datasets=>" + e );
            console.print( "Exception listing datasets. See log for details" );
        }
        return datasets;
    }
    
    @Override
    public boolean createOrganization(String orgName, String description, String imageUriStr) {
        boolean success = false;
        
        try {
            URI imageUri;
            if (imageUriStr != null) {
               imageUri = URI.create( imageUriStr ); 
            } else {
                imageUri = null;
            }
            connect().createOrganization( orgName, description, imageUri );
            if (verbose) {
              console.println( "Organization: " + orgName + " created." );
            }
            success = true;
        } catch (Exception e) {
            LOGGER.error( "Exception creating organization in ckan=>" + e );
            console.print( "Exception creating organization:" + orgName + ". See log for details" );
        }
        return success;
    }
    
    @Override
    public List<String> listOrganizations() {
        List<String> orgs;
        try {
            orgs = connect().listOrganizations();
            
        } catch (Exception e) {
            orgs = Collections.emptyList();
            LOGGER.error( "Exception listing organizations=>" + e );
            console.print( "Exception listing organizations. See log for details" );
        }
        return orgs;
    }
    
    @Override
    public Map<String, String> sync( String sourceId, String dsIdOrName, boolean dryRun ) {
        
        long beginMs = System.currentTimeMillis();
        
        HashMap<String, String> resultProperties = new HashMap<>();
        syncRecords( sourceId, dsIdOrName, resultProperties, dryRun );
        
        long delta = System.currentTimeMillis() - beginMs;
        resultProperties.put( "ckan.sync.queue.time.ms", String.valueOf( delta ) );
        
        return resultProperties;
    }
    
    private QueryResponse dibQuery(String sourceId, Date startDate, Date endDate) throws UnsupportedQueryException, 
                                                                                         FederationException, SourceUnavailableException {
        String queryKeywords = syncConfig.getKeywords();
        int maxRecordCount = syncConfig.getMaxRecordsPerPoll();
        QueryImpl query = new QueryImpl( getFilter( startDate, endDate, queryKeywords ), 1, maxRecordCount, getSortBy(), true, 300000L );
        QueryRequestImpl queryRequest = new QueryRequestImpl( query, Arrays.asList( new String[] { sourceId } ) );
        queryRequest.setProperties( this.requestProperties );
        QueryResponse response = framework.query( queryRequest );
        return response;
    }

    /**
     * 
     * @param sourceId
     * @param resultProperties
     */
    protected void syncRecords( String sourceId, String dsIdOrName, Map<String, String> resultProperties, boolean dryRun ) {
        
        console.println( "Querying sync source: " + sourceId );
        
        boolean hasMoreRecords = true;

        Date startDate = StringUtils.isBlank( syncConfig.getStartDate() ) ? null 
                                              : formatter.parseDateTime( syncConfig.getStartDate() ).toDate();
        Date endDate = new Date();
        int maxRecordCount = syncConfig.getMaxRecordsPerPoll();
        int docCnt = 0;

        temporalBoundsTracker.clear();
            
        long beginTime = System.currentTimeMillis();
        
        try {
            final CkanPublisher ckanPublisher = connect();
            
            while ( hasMoreRecords && docCnt < ckanConfig.getMaxInput() ) {

                QueryResponse response = dibQuery(sourceId, startDate, endDate);
                List<Result> results = response.getResults();
                // Start date required since time series is the only way to segment results.
                hasMoreRecords = (results.size() == maxRecordCount) && startDate != null;
                console.printf( "Synchronizing query results for %d records from site %s\n", Integer.valueOf( results.size() ), sourceId );
                LOGGER.debug( "Synchronizing query results for {} records from site {} in the Content Collection date range [{} - {}] and keywords[{}]",
                               Integer.valueOf( results.size() ), sourceId, startDate, endDate, syncConfig.getKeywords() );

                for ( Result result : results ) {
                    docCnt++;

                    Metacard metacard = result.getMetacard();
                    createResource(ckanPublisher, dsIdOrName, metacard);
                    
                    // Track max bounds seen as a way to ensure segmented increment through total result set possible
                    temporalBoundsTracker.updateBounds( metacard );
                    // Walk start date in case of a secondary query
                    TemporalCoverageHolder timeWindowProcessed = temporalBoundsTracker.getTemporalCoverageHolder( syncConfig.getDateType() );
                    if (timeWindowProcessed != null) {
                      startDate = timeWindowProcessed.getEndDate();
                    }
                }
            }

        } catch ( Throwable arg34 ) {
            LOGGER.warn( "Query failed against source {}", sourceId, arg34 );
        } finally {
            long deltaTime = System.currentTimeMillis() - beginTime;
            LOGGER.info( "Synchronized:" + docCnt + " records in " + String.valueOf(((double) deltaTime) / 1000.0) + "(s)");
            console.println( "Synchronized:" + docCnt + " records in " + String.valueOf(((double) deltaTime) / 1000.0) + "(s)");
            resultProperties.put( "ckan.doc.queue.count",  String.valueOf( docCnt ) );
            resultProperties.put( "ckan.doc.queue.time.ms", String.valueOf( deltaTime ) );
        }
    }
    
    private boolean createResource(CkanPublisher ckanPublisher, String dsIdOrName, Metacard metacard) {
        boolean success = false;
        try {
            BinaryContent binContent = geoJsonTransformer.transform( metacard, requestProperties );
            JSONObject jsonObject = (JSONObject) jsonParser.parse( new InputStreamReader(binContent.getInputStream()) );
            // See CKAN support for geo_points with http://extensions.ckan.org/extension/spatial/
            String wkt = metacard.getLocation();
            if ( StringUtils.isNotBlank( wkt ) ) {
               Geometry geometry = new WKTReader().read( wkt );
               Coordinate coord = geometry.getCentroid().getCoordinate();
               String center = Double.toString( coord.y ) + ',' + Double.toString( coord.x );
               Map<String, Object> props = (Map<String, Object>)  jsonObject.get( "properties" );
               props.put( "centroid", center );
            }
            //Derived resource link is preferred since it's previewable in CKAN
            Attribute downloadUrlAttr = metacard.getAttribute( Metacard.DERIVED_RESOURCE_DOWNLOAD_URL );
            if (downloadUrlAttr == null) {
                downloadUrlAttr = metacard.getAttribute( Metacard.RESOURCE_DOWNLOAD_URL );
            }
            URI downloadUri = downloadUrlAttr != null ? new URI(downloadUrlAttr.getValue().toString()) : null;

            ckanPublisher.addResource( dsIdOrName, 
                                       metacard.getId(), 
                                       metacard.getTitle(),
                                       metacard.getCreatedDate(), 
                                       metacard.getResourceSize(),  
                                       metacard.getContentTypeName(), // but the legit product mime type still used.
                                       metacard.getThumbnail(),
                                       downloadUri,
                                       jsonObject);
            success = true;
            if (verbose) {
                console.println("Queued:" + jsonObject.toJSONString());
            }
        } catch ( Exception e ) {
            LOGGER.error( "Error handling result {} ", metacard.getId(), e );
            console.printf( "Error handling result {} with Exception {} ", metacard.getId(), e.getLocalizedMessage() );
        }
        return success;
    }

    protected Filter getFilter( Date startDate, Date endDate, String keywords ) {
        LOGGER.debug( "Creating query for date type {} and date range {} through {} and keywords {}", syncConfig.getDateType(), startDate, endDate, keywords );
        Filter filter = null;
        
        if ( startDate != null ) {
            filter = filterBuilder.attribute( syncConfig.getDateType() ).during().dates( startDate, endDate );
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
