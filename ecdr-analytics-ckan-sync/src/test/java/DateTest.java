import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

public class DateTest {
    
    public static void main( String args [] ){
        Date startDate = new Date();
        startDate = DateUtils.addDays( startDate, -20 );
        if ( startDate != null ){
            System.out.println( startDate );
            LocalDateTime start = LocalDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault());
            System.out.println( start );
            Duration duration = Duration.between( start, LocalDateTime.now() );
            System.out.println( 4 / duration.toHours() );
            System.out.println( (13*7) / duration.toDays()  );
        }
    }

}
