package com.mesalu.viv2.android_ui;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimeParsingTest {
    @Test
    public void testExampleTimeStamp() {
        String iso8601TimeStamp = "2022-04-30T17:57:15.004523Z";
        TemporalAccessor accessor = DateTimeFormatter
                .ISO_DATE_TIME
                .parse(iso8601TimeStamp);

        ZonedDateTime dt = ZonedDateTime.from(accessor);

        assertEquals(17, dt.getHour());
        assertEquals(57, dt.getMinute());
        assertEquals(ZoneId.of("Z"), dt.getZone());
    }
}
