package net.di2e.ecdr.analytics.util;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ddf.catalog.data.Metacard;

public class TemporalBoundsTracker {
  Map<String, TemporalCoverageHolder> coverageByType = new ConcurrentHashMap<>();
  
  public TemporalBoundsTracker() { }
  
  /**
   * Tracks all time windows types { effective, created, modified, expiration }
   * @param mCard
   */
  public void updateBounds(Metacard mCard) {
      updateBounds( Metacard.EFFECTIVE, mCard.getEffectiveDate() );
      updateBounds( Metacard.CREATED, mCard.getCreatedDate() );
      updateBounds( Metacard.MODIFIED, mCard.getModifiedDate() );
      updateBounds( Metacard.EXPIRATION, mCard.getExpirationDate() );
  }
  
  private TemporalCoverageHolder updateBounds(String type, Date date) {
      TemporalCoverageHolder tCoverage = coverageByType.get( type );
      if (tCoverage == null) {
          tCoverage = new TemporalCoverageHolder();
          coverageByType.put(type, tCoverage);
      }
      tCoverage.updateDate( date );
      return tCoverage;
  } 
  
  /**
   * Types must conform to one of Metacard.EFFECTIVE, Metacard.CREATED, Metacard.MODIFIED, Metacard.EXPIRATION }
   * @param type
   * @return
   */
  public TemporalCoverageHolder getTemporalCoverageHolder(String type) {
      return coverageByType.get( type );
  }
  
}
