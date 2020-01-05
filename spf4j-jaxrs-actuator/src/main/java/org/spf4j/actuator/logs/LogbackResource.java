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
package org.spf4j.actuator.logs;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.LogLevel;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.os.OperatingSystem;

/**
 *
 * @author Zoltan Farkas
 */
@Path("logback/local/status")
@RolesAllowed("operator")
@Singleton
@SuppressWarnings("checkstyle:DesignForExtension")// methods cannot be final due to interceptors
public class LogbackResource {

  private final String hostName;

  @Inject
  public LogbackResource(@ConfigProperty(name = "hostName", defaultValue = "") final String hostName) {
    this.hostName = hostName.isEmpty() ? OperatingSystem.getHostName() : hostName;
  }

  @GET
  @Produces({"application/json", "application/avro"})
  public Collection<LogRecord> status(@QueryParam("limit") @DefaultValue("1000") final int limit) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    StatusManager statusManager = lc.getStatusManager();
    List<Status> statuses = statusManager.getCopyOfStatusList();
    PriorityQueue<LogRecord> result = new PriorityQueue<>(statuses.size(), LogUtils.TS_ORDER_ASC);
    addStatuses(statuses, result, limit);
    return result;
  }

  @DELETE
  public void clear() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    StatusManager statusManager = lc.getStatusManager();
    statusManager.clear();
  }

  private void addStatuses(final Iterable<Status> statuses,
          final PriorityQueue<LogRecord> result, final int limit) {
    for (Status status : statuses) {
      if (result.size() >= limit) {
        result.poll();
      }
      Throwable t = status.getThrowable();
      LogLevel level;
      switch (status.getLevel()) {
        case Status.INFO:
          level = LogLevel.INFO;
          break;
        case Status.WARN:
          level = LogLevel.WARN;
          break;
        case Status.ERROR:
          level = LogLevel.ERROR;
          break;
        default:
          level = LogLevel.UNKNOWN;
          break;
      }
      result.add(new LogRecord(Objects.toString(hostName + ':' + status.getOrigin()),
              "", level, Instant.ofEpochMilli(status.getDate()), "status",
              Thread.currentThread().getName(),
              status.getMessage(), Collections.EMPTY_LIST, Collections.EMPTY_MAP,
              t == null ? null : Converters.convert(t),
              Collections.EMPTY_LIST));
      if (status.hasChildren()) {
        addStatuses(() -> status.iterator(), result, limit);
      }
    }
  }
}
