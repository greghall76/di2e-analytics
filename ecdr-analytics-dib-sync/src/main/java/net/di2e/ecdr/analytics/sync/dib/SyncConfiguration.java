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
package net.di2e.ecdr.analytics.sync.dib;

import java.util.ArrayList;
import java.util.List;

public class SyncConfiguration {

    public static final String IDENTIFIER_QUALIFIER = "GUIDE";
    public static final String GUIDE_PREFIX = "guide://999715/";
    public static final String CONTENT_TYPE_QUALIFIER = "dib-content-type";

    private String collectionOriginator = null;
    private int maxRecordsPerPoll = 1000;
    private String classification = null;
    private List<String> ownerProducer = null;
    private String startDate = "2000-01-01";
    private String keywords = null;
    private String dateType = "effective";
    private String sortDateType = "effective";

    public SyncConfiguration() {
        collectionOriginator = "Elastic Sync";

        classification = "U";
        ownerProducer = new ArrayList<>();
        ownerProducer.add( "USA" );
    }

    public String getCollectionOriginator() {
        return collectionOriginator;
    }

    public void setCollectionOriginator( String orig ) {
        this.collectionOriginator = orig;
    }

    public int getMaxRecordsPerPoll() {
        return maxRecordsPerPoll;
    }

    public void setMaxRecordsPerPoll( int maxRecords ) {
        this.maxRecordsPerPoll = maxRecords;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification( String clas ) {
        this.classification = clas;
    }

    public List<String> getOwnerProducer() {
        return ownerProducer;
    }

    public void setOwnerProducer( List<String> op ) {
        this.ownerProducer = op;
    }
  
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate( String start ) {
        this.startDate = start;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords( String words ) {
        this.keywords = words;
    }

    public String getDateType() {
        return dateType;
    }

    public void setDateType( String date ) {
        this.dateType = date;
    }

    public String getSortDateType() {
        return sortDateType;
    }

    public void setSortDateType( String sort ) {
        this.sortDateType = sort;
    }

}
