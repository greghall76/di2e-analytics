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

import java.io.File;
import java.util.Map;

public interface ElasticSync {
    
    void setVerbose(boolean verbose);
    
    void setDumpDir(File dumpDir);

    int createIndex(String idx);
    
    int deleteIndex(String idx);
    
    Map<String, String> sync( String sourceId, String targetIndex );
    
    Map<String, String> syncAll(String targetIndex);
}
