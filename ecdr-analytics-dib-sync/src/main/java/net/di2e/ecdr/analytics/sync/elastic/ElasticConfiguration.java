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

public class ElasticConfiguration {
    private String  protocol;
    private String  host;
    private Integer port;
   
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol( String aProtocol ) {
        this.protocol = aProtocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost( String aHost ) {
        this.host = aHost;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort( Integer aPort ) {
        this.port = aPort;
    }
}
