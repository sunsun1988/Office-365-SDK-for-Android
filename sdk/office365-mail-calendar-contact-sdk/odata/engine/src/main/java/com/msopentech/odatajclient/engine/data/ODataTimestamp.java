/**
 * Copyright © Microsoft Open Technologies, Inc.
 *
 * All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 * ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
 * PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
 *
 * See the Apache License, Version 2.0 for the specific language
 * governing permissions and limitations under the License.
 */
package com.msopentech.odatajclient.engine.data;

import com.msopentech.odatajclient.engine.data.metadata.edm.EdmSimpleType;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Helper class for handling datetime and datetime-offset primitive values.
 *
 * @see com.msopentech.odatajclient.engine.data.metadata.edm.EdmSimpleType#DATE_TIME
 * @see com.msopentech.odatajclient.engine.data.metadata.edm.EdmSimpleType#DATE_TIME_OFFSET
 */
public final class ODataTimestamp implements Serializable {

    private static final long serialVersionUID = 4053990618660356004L;

    private final SimpleDateFormat sdf;

    private final Timestamp timestamp;

    private String timezone;

    private final boolean offset;

    public static ODataTimestamp getInstance(final EdmSimpleType type, final Timestamp timestamp) {
        return new ODataTimestamp(new SimpleDateFormat(type.pattern()),
                new Date(timestamp.getTime()), timestamp.getNanos(), type == EdmSimpleType.DateTimeOffset);
    }

    public static ODataTimestamp parse(final EdmSimpleType type, final String input) {
        final ODataTimestamp instance;

        final String inputWithOffset; // for example will be "2014-03-26T15:00:00+0000"
        final String inputWithoutOffset; // "2014-03-26T15:00:00.123546"
        // see http://docs.oasis-open.org/odata/odata/v4.0/os/abnf/odata-abnf-construction-rules.txt section dateTimeOffsetValue
        
        try {
            // first prepare two strings for parsing and store time zone (if present)
            String tz;
            if (input.endsWith("Z")) {
                tz = "Z";
            } else if (input.contains("T")) {
                if (input.substring(input.indexOf("T")).contains("+")) {
                    tz = input.substring(input.lastIndexOf('+'));
                } else if (input.substring(input.indexOf("T")).contains("-")) {
                    tz = input.substring(input.lastIndexOf('-'));
                } else {
                    tz = null;
                }
            } else {
                tz = null;
            }
            
            if ("+00:00".equals(tz) || "-00:00".equals(tz)) {
                tz = "Z";
            }
            
            int dotIndex = input.indexOf(".");
            if (dotIndex != -1) {
                if (tz != null) {
                    if (input.endsWith("Z")) {
                        inputWithOffset = input.substring(0, dotIndex) + "+0000"; // f. i. "2014-03-26T11:22:33.132456Z" -> "2014-03-26T11:22:33+0000"
                        inputWithoutOffset = input.replace("Z", ""); // // f. i. "2014-03-26T11:22:33.123456Z" -> "2014-03-26T11:22:33.123456"
                    } else {
                        int signIndex = input.lastIndexOf("+");
                        if (signIndex == -1) {
                            signIndex = input.lastIndexOf("-");
                        }
                        final String concatenated = input.substring(0, dotIndex) + input.substring(signIndex);
                        inputWithOffset = new StringBuilder(concatenated)
                            .replace(concatenated.lastIndexOf(":"), concatenated.lastIndexOf(":") + 1, "")
                            .toString(); // f. i. "2014-03-26T11:22:33.132456+00:00" -> "2014-03-26T11:22:33+0000"
                        inputWithoutOffset = input.substring(0, signIndex); // f. i. "2014-03-26T11:22:33.132456+00:00" -> "2014-03-26T11:22:33.123456"
                    }
                } else {
                    inputWithOffset = input.substring(0, dotIndex) + "+0000"; // f. i. "2014-03-26T11:22:33.132456" -> "2014-03-26T11:22:33+0000"
                    inputWithoutOffset = input; // f. i. "2014-03-26T11:22:33.132456" -> "2014-03-26T11:22:33+0000"
                }
            } else {
                if (tz != null) {
                    if (input.endsWith("Z")) {
                        inputWithOffset = input.replace("Z", "+0000"); // f. i. "2014-03-26T11:22:33Z" -> "2014-03-26T11:22:33+0000"
                        inputWithoutOffset = input.substring(0, input.length() - 1); // f. i. "2014-03-26T11:22:33Z" -> "2014-03-26T11:22:33"
                    } else {
                        inputWithOffset = new StringBuilder(input).replace(input.lastIndexOf(":"), input.lastIndexOf(":") + 1, "")
                                .toString(); // f. i. "2014-03-26T11:22:33+00:00" -> "2014-03-26T11:22:33+0000"
                        int signIndex = input.lastIndexOf("+");
                        if (signIndex == -1) {
                            signIndex = input.lastIndexOf("-");
                        }
                        inputWithoutOffset = input.substring(0, signIndex); // f. i. "2014-03-26T11:22:33+00:00" -> "2014-03-26T11:22:33"
                    }
                } else {
                    inputWithOffset = input + "+0000"; // f. i. "2014-03-26T11:22:33" -> "2014-03-26T11:22:33+0000"
                    inputWithoutOffset = input; // f. i. "2014-03-26T11:22:33" -> "2014-03-26T11:22:33"
                }
            }

            // second calculate date with seconds precision considering TZ offset
            final SimpleDateFormat sdf = new SimpleDateFormat(type.pattern());
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
            final boolean isOffset = type == EdmSimpleType.DateTimeOffset;
            final Date date = sdf.parse(inputWithOffset);

            // third parse fractional seconds (if present)
            final Timestamp timestamp;
            if (type.pattern().contains("T")) {
                timestamp = Timestamp.valueOf(inputWithoutOffset.replace("T", " "));
            } else {
                timestamp = null;
            }

            // fourth combine all of these
            instance = new ODataTimestamp(sdf, date, timestamp != null ? timestamp.getNanos() : 0, tz, isOffset);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse " + type.pattern(), e);
        }

        return instance;
    }

    private ODataTimestamp(final SimpleDateFormat sdf, final Date date, final boolean offset) {
        this.sdf = sdf;
        this.timestamp = new Timestamp(date.getTime());
        this.offset = offset;
    }

    private ODataTimestamp(final SimpleDateFormat sdf, final Date date, final int nanos, final boolean offset) {
        this(sdf, date, offset);
        this.timestamp.setNanos(nanos);
    }

    private ODataTimestamp(
            final SimpleDateFormat sdf, final Date date, final int nanos, final String timezone, final boolean offset) {
        this(sdf, date, nanos, offset);
        this.timezone = timezone;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getTimezone() {
        return timezone;
    }

    public boolean isOffset() {
        return offset;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "sdf");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "sdf");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        final StringBuilder formatted = new StringBuilder().append(sdf.format(timestamp));
        // remove time zone info: we'll add it after fractional seconds
        if (formatted.indexOf("T") != -1 && formatted.substring(formatted.indexOf("T")).indexOf('+') != -1) {
            formatted.delete(formatted.lastIndexOf("+"), formatted.length());
        } else if (formatted.indexOf("T") != -1 && formatted.substring(formatted.indexOf("T")).indexOf('-') != -1) {
            formatted.delete(formatted.lastIndexOf("-"), formatted.length());
        }
        if (timestamp.getNanos() > 0) {
            formatted.append('.').append(String.valueOf(timestamp.getNanos()));
            // remove trailing zeros
            while (formatted.charAt(formatted.length() - 1) == '0') {
                formatted.deleteCharAt(formatted.length() - 1);
            }
        }
        if (StringUtils.isNotBlank(timezone)) {
            formatted.append(timezone);
        }
        return formatted.toString();
    }
}
