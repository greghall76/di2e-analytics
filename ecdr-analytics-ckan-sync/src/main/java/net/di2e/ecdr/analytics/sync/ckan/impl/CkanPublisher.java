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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.types.Media;
import eu.trentorise.opendata.jackan.CkanClient;
import eu.trentorise.opendata.jackan.exceptions.CkanException;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanOrganization;
import eu.trentorise.opendata.jackan.model.CkanPair;
import eu.trentorise.opendata.jackan.model.CkanResource;
import net.di2e.ecdr.analytics.sync.ckan.config.CkanConfiguration;

public class CkanPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger( CkanPublisher.class );

    private CkanConfiguration ckanConfig;
    private File tmpDir = new File("temp");
    
    public CkanPublisher(CkanConfiguration config) {
        this.ckanConfig = config;
        if ( !tmpDir.exists() ) {
           if ( tmpDir.mkdirs() ) {
               LOGGER.info( "Initialized tmp dir for working thumbnail uploads..." );
           }
        }
    }
    /**
     * 
     * @return
     */
    private CkanClient connect() throws CkanException {
        CkanClient client = null;
        // Construct a new Jackan client according to configuration via factory
        String uriStr = ckanConfig.getProtocol().endsWith( "://" ) 
                ? ckanConfig.getProtocol() : ckanConfig.getProtocol() +  "://" + ckanConfig.getHost() + ':' + ckanConfig.getPort();        
        try {
          URI uri = new URI(uriStr);
          client = CkanClient.builder().setCkanToken( ckanConfig.getToken() )
                                       .setCatalogUrl( uri.toString() )
                                       .setTimeout( ckanConfig.getTimeout() ).build();
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("CKAN publisher initialized with:" + uri);
          }
          
        } catch  (URISyntaxException e) {
           throw new CkanException("Bad URI for connect given:" + uriStr, null, e);
        }
        return client;
    }
    
    /**
     * List datasets created
     * @return
     * @throws CkanException
     */
    public List<String> listDatasets() throws CkanException {
        return connect().getDatasetList();
    }

    /**
     * Organizations in CKAN
     * @return
     * @throws CkanException
     */
    public List<String> listOrganizations() throws CkanException {
        return connect().getOrganizationNames();
    }
    
    /**
     * Create a dataset
     * @param id - The ID really should be unique because when you delete a dataset in CKAN, it doesn't really delete. Just gets marked inactive.
     *             Reuse of an ID can cause issues.
     * @param name - Name for the dataset
     * @param ownerOrg - owning org ( can be created either manually or through the API )
     * @param creatorUserId - The creator should be a valid user.
     * @param uri - A URI for the dataset overall.
     * @param props - searchable key/value pairs for dataset
     * @throws CkanException
     */
    public void createDataset( String id, String name, String ownerOrg, String creatorUserId, URI uri, Map<String, String> props) throws CkanException {
        // String settings = "\"settings\" : {\n" +
        // " \"number_of_shards\" : 5,\n" +
        // " \"number_of_replicas\" : 1\n" +
        // " }\n";
        CkanDataset ckanDs = new CkanDataset();
        // Lots of options here for maintainer, email, create, and licening....
        ckanDs.setId( id );
        ckanDs.setName( name );
        ckanDs.setAuthor( creatorUserId );
        ckanDs.setCreatorUserId( creatorUserId );
        ckanDs.setOwnerOrg( ownerOrg );
        ckanDs.setPriv( Boolean.TRUE ); // Doesn't seem to work
        ckanDs.setUrl( uri != null ? uri.toString() : "" );
        ckanDs.setMaintainer( "ecdr-analytics" );
        // searchable key/value pair attributes of the dataset
        for (String key : props.keySet()) {
          ckanDs.addExtras( new CkanPair(key, props.get( key )) );
        }
        ckanDs = connect().createDataset(ckanDs);
        LOGGER.info("Created dataset:" + name);
    }
    
    /**
     * Delete the specified dataset by wither name or ID
     * @param nameOrId
     * @throws IOException
     */
    public void deleteDataset( String nameOrId ) throws CkanException {
        connect().deleteDataset( nameOrId );
        LOGGER.debug("Deleted dataset:" + nameOrId);
    }
    
    /**
     * 
     * @param orgName
     * @throws CkanException
     */
    public void createOrganization(String orgName, String description, URI imageUri) throws CkanException {
        CkanOrganization ckanOrg = new CkanOrganization();
        ckanOrg.setCreated( new Timestamp(System.currentTimeMillis()) );
        ckanOrg.setDisplayName( orgName );
        ckanOrg.setId( orgName ); // I don't think this is a UID
        ckanOrg.setName( orgName );
        ckanOrg.setImageUrl( imageUri != null ? imageUri.toString() : "" );
        ckanOrg.setDescription( description );
        connect().createOrganization( ckanOrg );
    }
   
    /**
     * Given the specified dataset, and various attributes required, create a CKAN resource reference in the library back to this product.
     *
     * @param dsIdOrName - dataset id or name ( for foreign key reference )
     * @param rsrcId - ID for this resource
     * @param rsrcName - name for this resource
     * @param timestamp - create time for resource ref
     * @param size - size of the resource
     * @param contentType - mime type
     * @param thumbnail - thumbnail for resource
     * @param uri - URI to the resource ( optional, but preferred )
     * @param metacard
     * @throws IOException
     */
    public void addResource( String dsIdOrName, String rsrcId, String rsrcName, Date timestamp, String size,
                             String contentType, byte[] thumbnail, URI uri, @Deprecated JSONObject metacard) throws IOException {
        CkanResource ckanRs = new CkanResource(uri != null ? uri.toString() : "", dsIdOrName);
        ckanRs.setId( rsrcId );
        ckanRs.setName( rsrcName );
        ckanRs.setCreated( new Timestamp(timestamp.getTime()) );
        // Would be nice to make use of the thumbnail but I don't really think I can because it's either upload or link; not both
//        if (thumbnail != null && thumbnail.length > 0 ) {
//            File tmpFile = new File(tmpDir, String.valueOf(rsrcId) + "thumb.jpg");
//            try {
//                //NIO write thumbnail to disk
//                FileChannel.open( tmpFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE ).write( ByteBuffer.wrap( thumbnail ) );
//                ckanRs.setUpload( tmpFile, true );
//            } catch (IOException ioE) {
//                LOGGER.error( "Cannot create tmp file for thumbnail:" + tmpFile );
//            }
//        }
        ckanRs.setResourceType( "api" ); // options api, file, file.upload ( I believe file.upload is only for UI )
        ckanRs.setSize( size );
        ckanRs.setMimetype( contentType );
        ckanRs.setDescription( (String) metacard.get( "title" ) );
        String format = contentType;
        String mediaFormat = (String) metacard.get( Media.FORMAT );
        if (mediaFormat != null) {
          format = mediaFormat;
        }
        ckanRs.setFormat( format ); // traditional CKAN options are CSV, XML, JSON...
        Map<String, Object> flatMap = new HashMap<>();
        buildKeyMap(null, flatMap, metacard);
        ckanRs.setOthers( flatMap );
        connect().createResource( ckanRs );
    }

    /**
     * Recursive method to flatten map of maps into . deliminated keys
     * @param keyBase
     * @param props
     * @return
     */
    private String buildKeyMap(String keyBase, Map<String, Object> flatMap, Map<String, Object> props) {
        for (String key : props.keySet()) {
            String thisKey = keyBase != null ? keyBase + '.' + key : key;
            Object value   = props.get( key );
            if (value != null) {
                if (value instanceof Map) {
                    keyBase = buildKeyMap(thisKey, flatMap, (Map<String, Object>) value);
                } else if (value.getClass().isArray()) {
                    StringBuffer sb = new StringBuffer();
                    for (int idx = 0; idx < Array.getLength( value ); idx++) {
                        Object o = Array.get( value, idx );
                        if (o != null) {
                          sb.append(idx > 0 ? ", " : "").append(o.toString());
                        }
                    }
                    flatMap.put(thisKey, sb.toString());
                } else if (value instanceof Collection) { // kind of working around JSONArray wanting to escape and ruin URLs
                    StringBuffer sb = new StringBuffer();
                    boolean first = true;
                    for (Object o : (Collection) value) {
                        if (o != null) {
                          sb.append(first ? "" : ", ").append(o.toString());
                        }
                        first = false;
                    }
                    flatMap.put(thisKey, sb.toString());
                } else if (!thisKey.equals( "geometry.properties.thumbnail" )
                           && !thisKey.equals( "geometry.properties.metadata" )) {
                    // ignore the thumbnail. can't get CKAN to respect it.
                    // ignore the metadata XML as it's redundant   
                    // simple key/value found
                    flatMap.put( thisKey, value.toString() );
                }
            }
        }
        return keyBase;
    }
}
