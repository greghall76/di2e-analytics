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
package net.di2e.ecdr.analytics.sync.ckan.config;

public class CkanConfiguration {
    private String  protocol;
    private String  host;
    private Integer port;
    private String  token;
    private Integer timeout;
    
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

    public String getToken() {
        return token;
    }

    public void setToken( String token ) {
        this.token = token;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout( Integer timeout ) {
        this.timeout = timeout;
    }

    
}
