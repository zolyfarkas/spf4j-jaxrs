/*
 * Copyright 2021 SPF4J.
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
package org.spf4j.jaxrs.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.spf4j.base.ExecutionContext;
import static org.spf4j.base.ExecutionContexts.start;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Wrapper;
import org.spf4j.http.RequestContextTags;

/**
 *
 * @author Zoltan Farkas
 */
public interface HttpCallable<T> extends Callable<T> {

  URI getUri();

  String getHttpMethod();

  long getStartNanos();

  long getDeadlineNanos();


  static <T> HttpCallable<T> invocationHandler(
          final ExecutionContext ctx,
          final Callable<T> callable,
          @Nullable final String name,
          final URI uri,
          final String method,
          final ClientExceptionMapper exMapper,
          final long nanoTime,
          final long deadlineNanos,
          final long callableTimeoutNanos) {
    return new InvocationHandler(callable, ctx, name, uri, method, exMapper,
            nanoTime, deadlineNanos, callableTimeoutNanos);
  }

  final class InvocationHandler<T> implements HttpCallable<T>, Wrapper<Callable<T>> {

    private final Callable<T> task;
    private final ExecutionContext current;

    private final String name;

    private final long nanoTime;

    private final long deadlineNanos;

    private final long callableTimeoutNanos;

    private final AtomicInteger tryCount;

    private final URI uri;

    private final String method;

    private final ClientExceptionMapper exMapper;

    InvocationHandler(final Callable<T> task,
            final ExecutionContext current,
            @Nullable final String name,
            final URI uri,
            final String method,
            final ClientExceptionMapper exMapper,
            final long nanoTime,
            final long deadlineNanos, final long callableTimeoutNanos) {
      this.task = task;
      this.current = current;
      this.name = name;
      this.uri = uri;
      this.method = method;
      this.nanoTime = nanoTime;
      this.deadlineNanos = deadlineNanos;
      this.callableTimeoutNanos = callableTimeoutNanos;
      this.tryCount = new AtomicInteger(1);
      this.exMapper = exMapper;
    }

    @Override
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public T call() throws Exception {
      long aDeadlineNanos;
      if (callableTimeoutNanos < 0) {
        aDeadlineNanos = deadlineNanos;
      } else {
        aDeadlineNanos = TimeSource.getDeadlineNanos(callableTimeoutNanos, TimeUnit.NANOSECONDS);
        if (aDeadlineNanos > deadlineNanos) {
          aDeadlineNanos = deadlineNanos;
        }
      }
      try (ExecutionContext ctx = start(toString(), current, aDeadlineNanos)) {
        ctx.put(RequestContextTags.TRY_COUNT, tryCount.getAndIncrement());
        return task.call();
      } catch (Exception ex) {
        throw exMapper.handleServiceError(ex, current);
      }
    }

    @Override
    public String toString() {
      return name == null ? task.toString() : name;
    }

    @Override
    public Callable<T> getWrapped() {
      return task;
    }

    @Override
    public URI getUri() {
      return uri;
    }

    @Override
    public String getHttpMethod() {
      return method;
    }

    @Override
    public long getStartNanos() {
      return this.nanoTime;
    }

    @Override
    public long getDeadlineNanos() {
      return this.deadlineNanos;
    }

  }


}
