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
package net.di2e.ecdr.analytics.sync.elastic.config;

public class ElasticConfiguration {
    private String  protocol;
    private String  host;
    private Integer port;
    private Integer concurrencyPerRoute;
    private Integer maxConcurrency;
    private Integer maxBulkIndexingRequests;
    
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol( String protocol ) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost( String host ) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort( Integer port ) {
        this.port = port;
    }

    public Integer getConcurrencyPerRoute() {
        return concurrencyPerRoute;
    }

    public void setConcurrencyPerRoute( Integer concurrencyPerRoute ) {
        this.concurrencyPerRoute = concurrencyPerRoute;
    }

    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency( Integer maxConcurrency ) {
        this.maxConcurrency = maxConcurrency;
    }

    public Integer getMaxBulkIndexingRequests() {
        return maxBulkIndexingRequests;
    }

    public void setMaxBulkIndexingRequests( Integer maxBulkIndexingRequests ) {
        this.maxBulkIndexingRequests = maxBulkIndexingRequests;
    }
    
    
}
