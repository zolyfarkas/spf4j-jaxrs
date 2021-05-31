package org.spf4j.jaxrs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;
import org.spf4j.failsafe.PartialResultRetryPredicate;
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
    return addDefaultRetryPredicated(builder).build();
  }

  public static <T extends  RetryPolicy.Builder> T addDefaultRetryPredicated(final T builder) {
    return (T) builder
            .withExceptionPartialPredicate(WebApplicationException.class, new HttpRetryHeaderExceptionRetryPredicate())
            .withResultPartialPredicate(Response.class, new HttpRetryHeaderResponseStatusses())
            .withExceptionPartialPredicate(WebApplicationException.class, new HttpDefaultRetryableStatusses())
            .withResultPartialPredicate(Response.class, new HttpDefaultResponseStatusses())
            .withExceptionPartialPredicate(ProcessingException.class, new DefaultThrowablePredicate())
            .withRetryOnException(Exception.class, 2) // will retry any other exception twice.
            .withInitialImmediateRetries(2)
            .withInitialDelay(10, TimeUnit.MILLISECONDS);
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

  private static class HttpRetryHeaderExceptionRetryPredicate
          implements PartialTypedExceptionRetryPredicate<Object, Callable<? extends Object>, WebApplicationException> {

    @Override
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public RetryDecision<Object, Callable<? extends Object>> getExceptionDecision(
            final WebApplicationException ex, final Callable<? extends Object> c) {
      return (RetryDecision) getHeaderBasedRetryDecision(ex.getResponse(), c);
    }

  }

  private static class HttpRetryHeaderResponseStatusses
          implements PartialResultRetryPredicate<Response, Callable<Response>> {

    @Override
    public RetryDecision<Response, Callable<Response>> getDecision(final Response value,
            final Callable<Response> what) {
      return getHeaderBasedRetryDecision(value, what);
    }

  }

  @Nullable
  public static <T> RetryDecision<T, Callable<T>> getHeaderBasedRetryDecision(
          final Response response, final Callable<T> c) {
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
    if (noRetry != null) {
      return RetryDecision.abort();
    }
    return null;
  }

  @Nullable
  public static <T> RetryDecision<T, Callable<T>> getDefaultResponseDecision(final Response response,
          final Callable<T> c) {
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
  }

  private static class HttpDefaultResponseStatusses
          implements PartialResultRetryPredicate<Response, Callable<Response>> {

    @Override
    public RetryDecision<Response, Callable<Response>> getDecision(
            final Response value, final Callable<Response> what) {
      return getDefaultResponseDecision(value, what);
    }

  }

  private static class HttpDefaultRetryableStatusses
          implements PartialTypedExceptionRetryPredicate<Object, Callable<? extends Object>, WebApplicationException> {

    @Override
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public RetryDecision<Object, Callable<? extends Object>> getExceptionDecision(
            final WebApplicationException ex, final Callable<? extends Object> c) {
      Response response = ex.getResponse();
      return (RetryDecision) getDefaultResponseDecision(response, c);
    }
  }

  private static class DefaultThrowablePredicate
          implements PartialTypedExceptionRetryPredicate<Object, Callable<? extends Object>, ProcessingException> {

    @Override
    public RetryDecision<Object, Callable<? extends Object>> getExceptionDecision(final ProcessingException value,
            final Callable<? extends Object> what) {
      Function<Throwable, Boolean> pred = org.spf4j.base.Throwables.getIsRetryablePredicate();
      Boolean result = pred.apply(value);
      if (result == null) {
        return null; // no decision.
      }
      if (result) {
        return RetryDecision.retryDefault(what);
      } else {
        return RetryDecision.abort();
      }
    }
  }

}
