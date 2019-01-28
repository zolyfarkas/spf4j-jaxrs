package org.spf4j.http;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A protocol to transmit deadline over HTTP headers.
 * @author Zoltan Farkas
 */
public interface DeadlineProtocol {

  /**
   * De-serialize the deadline transmitted via HTTP headers.
   * @param headers the headers.
   * @param currentTimeNanos the current time.
   * @return the deadline in nanoseconds.
   */
  long deserialize(Function<String, String> headers, long currentTimeNanos);


  /**
   * Serialize a deadline to HTTP headers.
   * @param headers
   * @param deadlineNanos
   * @return the timeout in nanoseconds.
   */
  long serialize(BiConsumer<String, String> headers, long deadlineNanos);

}
