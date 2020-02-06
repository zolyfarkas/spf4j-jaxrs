package org.spf4j.http;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.spf4j.base.CharSequences;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;

/**
 * @author Zoltan Farkas
 */
public final class DefaultDeadlineProtocol implements DeadlineProtocol {

  private final String deadlineHeaderName;

  private final String timeoutHeaderName;

  private final long defaultTimeoutNanos;

  private final long maxTimeoutNanos;

  public DefaultDeadlineProtocol() {
    this(25, 300, TimeUnit.SECONDS);
  }

  public DefaultDeadlineProtocol(
          final long defaultTimeout, final long maxTimeout, final TimeUnit unit) {
    this(Headers.REQ_DEADLINE, Headers.REQ_TIMEOUT, unit.toNanos(defaultTimeout), unit.toNanos(maxTimeout));
  }

  public DefaultDeadlineProtocol(final String deadlineHeaderName, final String timeoutHeaderName,
          final long defaultTimeoutNanos, final long maxTimeoutNanos) {
    if (defaultTimeoutNanos > maxTimeoutNanos) {
      throw new IllegalArgumentException("Invalid server configuration,"
              + " default timeout must be smaller than max timeout " + defaultTimeoutNanos + " < " + maxTimeoutNanos);
    }
    this.deadlineHeaderName = deadlineHeaderName;
    this.timeoutHeaderName = timeoutHeaderName;
    this.defaultTimeoutNanos = defaultTimeoutNanos;
    this.maxTimeoutNanos = maxTimeoutNanos;
  }

  @Override
  public long serialize(final BiConsumer<String, String> headers, final long deadlineNanos) {
    long timeoutNanos = deadlineNanos - TimeSource.nanoTime();
    Instant deadline = Timing.getCurrentTiming().fromNanoTimeToInstant(deadlineNanos);
    headers.accept(deadlineHeaderName, Long.toString(deadline.getEpochSecond()) + ' ' + deadline.getNano());
    headers.accept(timeoutHeaderName, timeoutNanos + " n");
    return timeoutNanos;
  }

  @Override
  public long deserialize(final Function<String, String> headers, final long currentTimeNanos) {
    String deadlineStr = headers.apply(deadlineHeaderName);
    long deadlineNanos;
    if (deadlineStr == null) {
      String timeoutStr = headers.apply(timeoutHeaderName);
      if (timeoutStr == null) {
        deadlineNanos = currentTimeNanos + defaultTimeoutNanos;
      } else {
        long parseTimeoutNanos = parseTimeoutNanos(timeoutStr);
        if (parseTimeoutNanos > maxTimeoutNanos) {
          Logger.getLogger(DefaultDeadlineProtocol.class.getName())
                  .log(Level.WARNING, "Overwriting client supplied timeout {0} ns with {1} ns",
                          new Object[]{parseTimeoutNanos, maxTimeoutNanos});
          deadlineNanos = currentTimeNanos + maxTimeoutNanos;
        } else {
          deadlineNanos = currentTimeNanos + parseTimeoutNanos;
        }
      }
    } else {
      deadlineNanos = parseDeadlineNanos(deadlineStr);
      long timeoutNanos = deadlineNanos - currentTimeNanos;
      if (timeoutNanos > maxTimeoutNanos) {
        Logger.getLogger(DefaultDeadlineProtocol.class.getName())
                .log(Level.WARNING, "Overwriting client supplied timeout {0} ns with {1} ns",
                        new Object[]{timeoutNanos, maxTimeoutNanos});
        deadlineNanos = currentTimeNanos + maxTimeoutNanos;
      }
    }
    return deadlineNanos;
  }

  /**
   * Hour → "H" Minute → "M" Second → "S" Millisecond → "m" Microsecond → "u" Nanosecond → "n"
   * @return null if not a timeUnit...
   */
  @Nullable
  public static TimeUnit from(final char uc) {
    switch (uc) {
      case 'H':
        return TimeUnit.HOURS;
      case 'M':
        return TimeUnit.MINUTES;
      case 'S':
        return TimeUnit.SECONDS;
      case 'm':
        return TimeUnit.MILLISECONDS;
      case 'u':
        return TimeUnit.MICROSECONDS;
      case 'n':
        return TimeUnit.NANOSECONDS;
      default:
        return null;
    }
  }

  /**
   * @param deadlineHeaderValue with format:
   * [seconds since epoch decimal long] [nanos component decimal int]?
   * @return deadline nanos.
   */
  public static long parseDeadlineNanos(final CharSequence deadlineHeaderValue) {
    long deadlineSeconds = CharSequences.parseUnsignedLong(deadlineHeaderValue, 10, 0);
    long nanoTime = Timing.getCurrentTiming().fromEpochMillisToNanoTime(deadlineSeconds * 1000);
    int indexOf = CharSequences.indexOf(deadlineHeaderValue, 0, deadlineHeaderValue.length(), ' ');
    if (indexOf < 0) {
      return nanoTime;
    } else {
      return nanoTime + CharSequences.parseUnsignedInt(deadlineHeaderValue, 10, indexOf + 1);
    }
  }

  /**
   * @param timeoutHeaderValue with format [long decimal value] [unit]
   * @return timeout value in milliseconds.
   */
  public static long parseTimeoutNanos(final CharSequence timeoutHeaderValue) {
    int length = timeoutHeaderValue.length();
    TimeUnit unit = from(timeoutHeaderValue.charAt(length - 1));
    if (unit == null) {
      return TimeUnit.MILLISECONDS.toNanos(
              CharSequences.parseUnsignedLong(timeoutHeaderValue, 10, 0, length, true));
    } else {
      int i = length - 2;
      while (timeoutHeaderValue.charAt(i) == ' ') {
        i--;
      }
      return unit.toNanos(CharSequences.parseUnsignedLong(timeoutHeaderValue, 10, 0, i + 1, true));
    }
  }

  @Override
  public String toString() {
    return "DefaultDeadlineProtocol{" + "deadlineHeaderName=" + deadlineHeaderName
            + ", timeoutHeaderName=" + timeoutHeaderName + ", defaultTimeoutNanos="
            + defaultTimeoutNanos + ", maxTimeoutNanos=" + maxTimeoutNanos + '}';
  }

}
