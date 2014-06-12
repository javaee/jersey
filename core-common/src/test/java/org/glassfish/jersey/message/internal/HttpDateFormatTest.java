package org.glassfish.jersey.message.internal;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

/**
 * {@link HttpDateFormat} unit tests.
 *
 * @author Derek P. Moore (derek.p.moore at gmail.com)
 */
public class HttpDateFormatTest {

    private final GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("Universal"));
    private Date date = null;

    public HttpDateFormatTest()
    {
        cal.clear(Calendar.MILLISECOND);
    }

    @Test
    public void testRFC1123() throws ParseException
    {
        // http://www.w3.org/Protocols/HTTP/1.0/spec.html
        cal.set(1994, 10, 6, 8, 49, 37);
        date = HttpDateFormat.readDate("Sun, 06 Nov 1994 08:49:37 GMT");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toUTCString
        cal.set(2006, 6, 3, 21, 44, 38);
        date = HttpDateFormat.readDate("Mon, 03 Jul 2006 21:44:38 GMT");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://msdn.microsoft.com/en-us/library/ie/ff743760(v=vs.94).aspx
        cal.set(2010, 5, 9, 22, 20, 00);
        date = HttpDateFormat.readDate("Wed, 9 Jun 2010 22:20:00 UTC");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.php.net/manual/en/class.datetime.php
        cal.set(2005, 7, 15, 15, 52, 1);
        date = HttpDateFormat.readDate("Mon, 15 Aug 2005 15:52:01 +0000");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        cal.set(82, 10, 19, 16, 14, 55);
        date = HttpDateFormat.readDate("Tue, 19 Nov 0082 16:14:55 GMT");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testRFC850() throws ParseException
    {
        // http://www.w3.org/Protocols/HTTP/1.0/spec.html
        cal.set(1994, 10, 6, 8, 49, 37);
        date = HttpDateFormat.readDate("Sunday, 06-Nov-94 08:49:37 GMT");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.php.net/manual/en/class.datetime.php
        cal.set(2005, 7, 15, 15, 52, 1);
        date = HttpDateFormat.readDate("Monday, 15-Aug-05 15:52:01 UTC");
        assertEquals(cal.getTimeInMillis(), date.getTime());
        date = HttpDateFormat.readDate("Monday, 15-Aug-2005 15:52:01 UTC");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.ietf.org/rfc/rfc0850.txt
        cal.set(1982, 10, 19, 16 + 5, 14, 55);
        date = HttpDateFormat.readDate("Friday, 19-Nov-82 16:14:55 EST");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testRFC1036() throws ParseException
    {
        // http://www.ietf.org/rfc/rfc1036.txt
        cal.set(1982, 10, 19, 16, 14, 55);
        date = HttpDateFormat.readDate("Fri, 19 Nov 82 16:14:55 GMT");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.ietf.org/rfc/rfc1036.txt
        cal.set(1982, 10, 19, 16 + 5, 14, 55);
        date = HttpDateFormat.readDate("Fri, 19 Nov 82 16:14:55 EST");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.ietf.org/rfc/rfc1036.txt
        cal.set(1983, 0, 1, 0 + 5, 0, 0);
        date = HttpDateFormat.readDate("Sat, 1 Jan 83 00:00:00 -0500");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.ietf.org/rfc/rfc1036.txt
        cal.set(1983, 0, 3, 0 + 7, 59, 15);
        date = HttpDateFormat.readDate("Mon, 3 Jan 83 00:59:15 MST");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.php.net/manual/en/class.datetime.php
        cal.set(2005, 7, 15, 15, 52, 1);
        date = HttpDateFormat.readDate("Mon, 15 Aug 05 15:52:01 +0000");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testRFC1036alt() throws ParseException
    {
        // http://www.ietf.org/rfc/rfc1036.txt
        cal.set(1986, 9, 1, 11, 26, 15);
        date = HttpDateFormat.readDate("1 Oct 86 11:26:15 GMT");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        cal.set(2005, 8, 9, 13 + 7, 51, 39);
        date = HttpDateFormat.readDate("9 Sep 2005 13:51:39 -0700");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testANSIC() throws ParseException
    {
        // http://www.w3.org/Protocols/HTTP/1.0/spec.html
        cal.set(1994, 10, 6, 8, 49, 37);
        date = HttpDateFormat.readDate("Sun Nov  6 08:49:37 1994");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.ietf.org/rfc/rfc1036.txt
        cal.set(1982, 10, 19, 16, 14, 55);
        date = HttpDateFormat.readDate("Fri Nov 19 16:14:55 1982");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.ietf.org/rfc/rfc1036.txt
        cal.set(1990, 0, 1, 0, 0, 0);
        date = HttpDateFormat.readDate("Mon Jan 1 00:00:00 1990");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testRFC2822() throws ParseException
    {
        // https://www.gnu.org/software/coreutils/manual/html_node/Examples-of-date.html
        cal.set(2005, 8, 9, 13 + 7, 51, 39);
        date = HttpDateFormat.readDate("Fri, 09 Sep 2005 13:51:39 -0700");
        assertEquals(cal.getTimeInMillis(), date.getTime());
        date = HttpDateFormat.readDate("Fri, 9 Sep 2005 13:51:39 -0700");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testRFC2822alt1() throws ParseException
    {
        cal.set(2005, 8, 9, 13 + 7, 51, 0);
        date = HttpDateFormat.readDate("Fri, 9 Sep 2005 13:51 -0700");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testRFC2822alt2() throws ParseException
    {
        cal.set(2005, 8, 9, 13 + 7, 51, 0);
        date = HttpDateFormat.readDate("9 Sep 2005 13:51 -0700");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testISO8601() throws ParseException
    {
        // http://www.w3.org/TR/xmlschema-2/#dateTime
        cal.set(1, 0, 1, 0, 0, 0);
        date = HttpDateFormat.readDate("0001-01-01T00:00:00");
        assertEquals(cal.getTimeInMillis(), date.getTime());
        date = HttpDateFormat.readDate("0001-01-01T00:00:00.1");
        assertEquals(cal.getTimeInMillis() + 1, date.getTime());
        date = HttpDateFormat.readDate("0001-01-01T00:00:00.01");
        assertEquals(cal.getTimeInMillis() + 1, date.getTime());
        date = HttpDateFormat.readDate("0001-01-01T00:00:00.001");
        assertEquals(cal.getTimeInMillis() + 1, date.getTime());
        date = HttpDateFormat.readDate("0001-01-01T00:00:00.100");
        assertEquals(cal.getTimeInMillis() + 100, date.getTime());
        date = HttpDateFormat.readDate("0001-01-01T00:00:00.1000");
        assertEquals(cal.getTimeInMillis() + 1000, date.getTime());
        date = HttpDateFormat.readDate("0001-01-01T00:00:01");
        assertEquals(cal.getTimeInMillis() + 1000, date.getTime());

        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toISOString
        cal.set(2011, 9, 5, 14, 48, 0);
        date = HttpDateFormat.readDate("2011-10-05T14:48:00.000Z");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://tools.ietf.org/html/rfc3339
        cal.set(1985, 3, 12, 23, 20, 50);
        date = HttpDateFormat.readDate("1985-04-12T23:20:50.52Z");
        assertEquals(cal.getTimeInMillis() + 52, date.getTime());

        // http://tools.ietf.org/html/rfc3339
        cal.set(1996, 11, 19, 16 + 8, 39, 57);
        date = HttpDateFormat.readDate("1996-12-19T16:39:57-08:00");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://tools.ietf.org/html/rfc3339
        cal.set(1990, 11, 31, 23, 59, 60);
        date = HttpDateFormat.readDate("1990-12-31T23:59:60Z");
        assertEquals(cal.getTimeInMillis(), date.getTime());
        date = HttpDateFormat.readDate("1990-12-31T15:59:60-08:00");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://tools.ietf.org/html/rfc3339
        cal.set(1937, 0, 1, 12, 0 - 20, 27);
        date = HttpDateFormat.readDate("1937-01-01T12:00:27.87+00:20");
        assertEquals(cal.getTimeInMillis() + 87, date.getTime());

        // JAXB output of Date type
        cal.set(2014, 4, 26, 17 + 5, 17, 0);
        date = HttpDateFormat.readDate("2014-05-26T17:17:00-05:00");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
        cal.set(2001, 6, 4, 12 + 7, 8, 56);
        date = HttpDateFormat.readDate("2001-07-04T12:08:56.235-0700");
        assertEquals(cal.getTimeInMillis() + 235, date.getTime());
        date = HttpDateFormat.readDate("2001-07-04T12:08:56-0700");
        assertEquals(cal.getTimeInMillis(), date.getTime());

        // http://www.php.net/manual/en/class.datetime.php
        cal.set(2005, 7, 15, 15, 52, 1);
        date = HttpDateFormat.readDate("2005-08-15T15:52:01+0000");
        assertEquals(cal.getTimeInMillis(), date.getTime());
        date = HttpDateFormat.readDate("2005-08-15T15:52:01+00:00");
        assertEquals(cal.getTimeInMillis(), date.getTime());
    }
}
