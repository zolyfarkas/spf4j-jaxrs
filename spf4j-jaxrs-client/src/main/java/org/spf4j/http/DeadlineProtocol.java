package org.spf4j.http;

import java.util.function.BiConsumer;
import java.util.function.Function;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;

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

  /**
   * A protocol, that will not read/write deadline/timeout to http headers.
   * Useful when interacting with services that do not support this.
   */
  DeadlineProtocol NONE = new DeadlineProtocol() {
    @Override
    public long deserialize(final Function<String, String> headers, final long currentTimeNanos) {
     return ExecutionContexts.getContextDeadlineNanos();
    }

    @Override
    public long serialize(final BiConsumer<String, String> headers, final long deadlineNanos) {
      return deadlineNanos - TimeSource.nanoTime();
    }
  };


}
