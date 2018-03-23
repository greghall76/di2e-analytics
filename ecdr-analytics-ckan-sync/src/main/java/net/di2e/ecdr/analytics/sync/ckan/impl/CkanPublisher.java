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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.types.Media;
import eu.trentorise.opendata.jackan.CkanClient;
import eu.trentorise.opendata.jackan.exceptions.CkanException;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanOrganization;
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
          LOGGER.info("CKAN publisher initialized with:" + uri);
          
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
     * Create a named dataset
     * @param name
     * @throws CkanException
     */
    public void createDataset( String name, String ownerOrg ) throws CkanException {
        // String settings = "\"settings\" : {\n" +
        // " \"number_of_shards\" : 5,\n" +
        // " \"number_of_replicas\" : 1\n" +
        // " }\n";
        CkanDataset ckanDs = new CkanDataset();
        ckanDs.setAuthor( "cdr/ckan" );
        // Lots of options here for maintainer, email, create, and licening....
        ckanDs.setName( name );
        ckanDs.setOwnerOrg( ownerOrg );
        ckanDs = connect().createDataset(ckanDs);
        LOGGER.info("Created dataset:" + name);
    }
    
    /**
     * Delete the specified dataset
     * @param name
     * @throws IOException
     */
    public void deleteDataset( String name ) throws CkanException {
        connect().deleteDataset( name );
        LOGGER.debug("Deleted dataset:" + name);
    }
    
    /**
     * 
     * @param orgName
     * @throws CkanException
     */
    public void createOrganization(String orgName) throws CkanException {
        CkanOrganization ckanOrg = new CkanOrganization();
        ckanOrg.setCreated( new Timestamp(System.currentTimeMillis()) );
        ckanOrg.setDisplayName( orgName );
        ckanOrg.setId( orgName ); // I don't think this is a UID
        ckanOrg.setName( orgName );
        ckanOrg.setDescription(" API created ");
        connect().createOrganization( ckanOrg );
    }
   
    /**
     * Given the specified dataset, and various attributes required, create a CKAN resource reference in the library back to this product.
     *
     * @param dataset
     * @param id
     * @param timestamp
     * @param size
     * @param contentType
     * @param thumbnail
     * @param uri
     * @param metacard
     * @throws IOException
     */
    public void addResource(String dataset, String id, Date timestamp, String size, String contentType, byte[] thumbnail, URI uri, JSONObject metacard) throws IOException {
        CkanResource ckanRs = new CkanResource(uri != null ? uri.toString() : "", dataset);
        ckanRs.setCreated( new Timestamp(timestamp.getTime()) );
        if (thumbnail != null && thumbnail.length > 0 ) {
            File tmpFile = new File(tmpDir, String.valueOf(id) + "thumb.png");
            try {
                //NIO write thumbnail to disk
                FileChannel.open( tmpFile.toPath() ).write( ByteBuffer.wrap( thumbnail ) );
                ckanRs.setUpload( tmpFile, true );
            } catch (IOException ioE) {
                LOGGER.error( "Cannot create tmp file for thumbnail:" + tmpFile );
            }
        }
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
        ckanRs.setOthers( metacard );
        connect().createResource( ckanRs );
    }
    
}
