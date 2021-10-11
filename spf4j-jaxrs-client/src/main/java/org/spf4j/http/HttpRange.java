/*
 * Copyright 2019 SPF4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.http;

import com.google.common.collect.Range;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * see https://tools.ietf.org/html/rfc7233#section-3.1
 *
 * @author Zoltan Farkas
 */
public final class HttpRange {

  private static final Pattern RANGE_SET_PATTERN;

  private static final Pattern RANGE_SPECIFIER_PATTERN;

  static {
    String rangeSetRegex = "(((?<rangeSpec>(?<firstPos>\\d+)-(?<lastPos>\\d+)?))(,|$))";
    String rangesSpecifierRegex = "(?<unit>\\w+)=(?<rangeSet>" + rangeSetRegex + "{1,})";
    RANGE_SET_PATTERN = Pattern.compile(rangeSetRegex);
    RANGE_SPECIFIER_PATTERN = Pattern.compile(rangesSpecifierRegex);
  }

  private final String unit;

  private final List<Range<Long>> ranges;

  public HttpRange(final String unit, final List<Range<Long>> ranges) {
    this.unit = unit;
    this.ranges = Collections.unmodifiableList(new ArrayList<>(ranges));
  }

  public HttpRange(final String unit, final Range<Long>... ranges) {
    this.unit = unit;
    this.ranges = Collections.unmodifiableList(Arrays.asList(ranges));
  }

  public String getUnit() {
    return unit;
  }

  public boolean isByteRange() {
    return "bytes".equals(unit);
  }

  @SuppressFBWarnings("EI_EXPOSE_REP") // not realy... false positive.
  public List<Range<Long>> getRanges() {
    return ranges;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(20);
    result.append(unit).append('=');
    Iterator<Range<Long>> iterator = ranges.iterator();
    if (iterator.hasNext()) {
      Range<Long> range = iterator.next();
      appendRange(result, range);
      while (iterator.hasNext()) {
        result.append(',');
        range = iterator.next();
        appendRange(result, range);
      }

    }
    return result.toString();
  }

  private static void appendRange(final StringBuilder result, final Range<Long> range) {
    result.append(range.lowerEndpoint());
    result.append('-');
    if (range.hasUpperBound()) {
      result.append(range.upperEndpoint());
    }
  }

  public static HttpRange valueOf(final String from) {
    return parse(from);
  }

  public static HttpRange parse(final CharSequence rangeHeader) {
    Matcher byteRangesSpecifierMatcher = RANGE_SPECIFIER_PATTERN.matcher(rangeHeader);
    if (byteRangesSpecifierMatcher.matches()) {
      String unit = byteRangesSpecifierMatcher.group("unit");
      String rangeSet = byteRangesSpecifierMatcher.group("rangeSet");
      Matcher rangeSetMatcher = RANGE_SET_PATTERN.matcher(rangeSet);
      List<Range<Long>> ranges = new ArrayList<>(2);
      while (rangeSetMatcher.find()) {
        if (rangeSetMatcher.group("rangeSpec") != null) {
          String start = rangeSetMatcher.group("firstPos");
          String end = rangeSetMatcher.group("lastPos");
          if (end != null) {
            ranges.add(Range.closed(Long.valueOf(start), Long.valueOf(end)));
          } else {
            ranges.add(Range.atLeast(Long.valueOf(start)));
          }
        } else {
          throw new IllegalArgumentException("Invalid range header: " + rangeHeader);
        }
      }
      if (ranges.isEmpty()) {
        throw new IllegalArgumentException("Invalid range header: " + rangeHeader);
      }
      return new HttpRange(unit, ranges);
    } else {
      throw new IllegalArgumentException("Invalid range header: " + rangeHeader);
    }
  }

}
