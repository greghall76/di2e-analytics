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
package net.di2e.ecdr.analytics.util;

import java.util.Date;

public class TemporalCoverageHolder {
    
    private Date startDate = null;
    private Date endDate = null;
    private String label = null;

    public void setLabel( String l ) {
        label = l;
    }

    public void updateDate( Date date ) {
        if ( date != null ) {
            if ( startDate == null ) {
                startDate = date;
                endDate = date;
            } else {
                startDate = startDate.before( date ) ? startDate : date;
                endDate = endDate.after( date ) ? endDate : date;
            }
        }
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getLabel() {
        return label;
    }
}
