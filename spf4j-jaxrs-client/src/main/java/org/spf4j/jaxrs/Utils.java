package org.spf4j.jaxrs;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;
import org.spf4j.failsafe.PartialTypedExceptionRetryPredicate;
import org.spf4j.failsafe.RetryDecision;
import org.spf4j.failsafe.RetryPolicy;

/**
 * @author Zoltan Farkas
 */
public final class Utils {

  public static final RetryPolicy DEFAULT_HTTP_RETRY_POLICY = createHttpRetryPolicy(null, 0);

  private Utils() {
  }

  public static RetryPolicy createHttpRetryPolicy(
          @Nullable final PartialTypedExceptionRetryPredicate<Object, Callable<? extends Object>,
                  WebApplicationException> predicate, final int retryCount) {
    RetryPolicy.Builder<Object, Callable<? extends Object>> builder = RetryPolicy.newBuilder()
            .withDefaultThrowableRetryPredicate();
    if (predicate != null) {
      builder.withExceptionPartialPredicate(WebApplicationException.class, predicate, retryCount);
    }
    return builder.withExceptionPartialPredicate(WebApplicationException.class,
                    (WebApplicationException ex, Callable<? extends Object> c) -> {
                      Response response = ex.getResponse();
                      String retryAfter = response.getHeaderString("Retry-After");
                      if (retryAfter != null && !retryAfter.isEmpty()) {
                        if (Character.isDigit(retryAfter.charAt(0))) {
                          return RetryDecision.retry(Long.parseLong(retryAfter), TimeUnit.SECONDS, c);
                        } else {
                          return RetryDecision.retry(Duration.between(Instant.now(),
                                  DateTimeFormatter.RFC_1123_DATE_TIME.parse(retryAfter,
                                          Instant::from)).toNanos(),
                                  TimeUnit.NANOSECONDS, c);
                        }
                      }
                      String noRetry = response.getHeaderString("No-Retry");
                      // Not standard,
                      // but a way for the server to tell the client there is no point for the client to retry.
                      if (noRetry == null) {
                        return RetryDecision.abort();
                      }
                      int status = response.getStatus();
                      switch (status) {
                        case 408:
                        case 409:
                        case 419:
                        case 420:
                        case 423:
                        case 429:
                        case 440:
                        case 449:
                        case 503:
                        case 504:
                        case 509:
                        case 522:
                        case 524:
                        case 599:
                          return RetryDecision.retryDefault(c);
                        default:
                          if (status >= 400 && status < 500) {
                            return RetryDecision.abort();
                          }
                      }
                      return null;
                    })
            .withRetryOnException(Exception.class, 2) // will retry any other exception twice.
            .build();
  }

  public static RetryPolicy defaultRetryPolicy() {
    return DEFAULT_HTTP_RETRY_POLICY;
  }

  public static int getIntConfigValue(final Configuration cfg, final String cfgKey, final int defaultValue) {
    Number nr = (Number) cfg.getProperty(cfgKey);
    if (nr == null) {
      return Integer.getInteger(cfgKey, defaultValue);
    } else {
      return nr.intValue();
    }
  }

  public static String getStringConfigValue(final Configuration cfg, final String cfgKey, final String defaultValue) {
    String val = (String) cfg.getProperty(cfgKey);
    if (val == null) {
      return System.getProperty(cfgKey, defaultValue);
    } else {
      return val;
    }
  }

}
