/*
 * Copyright 2020 SPF4J.
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
package org.spf4j.grizzly;

import java.util.function.Function;
import org.spf4j.base.Env;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.SysExits;
import org.spf4j.base.ThreadLocalContextAttacher;
import org.spf4j.base.Throwables;
import org.spf4j.io.Csv;
import org.spf4j.log.LogbackService;
import org.spf4j.os.OperatingSystem;
import org.spf4j.perf.ProcessVitals;
import org.spf4j.stackmonitor.FastStackCollector;
import org.spf4j.stackmonitor.ProfiledExecutionContextFactory;
import org.spf4j.stackmonitor.ProfilingTLAttacher;
import org.spf4j.stackmonitor.Sampler;
import org.spf4j.stackmonitor.TracingExecutionContexSampler;

/**
 *
 * @author Zoltan Farkas
 */
public final class JvmServicesBuilder {

  private static volatile JvmServices services;

  private String hostName;

  private String applicationName;

  private String logFolder;

  private int profilerSampleTimeMillis;

  private int profilerDumpTimeMillis;

  private boolean profilerJmx;

  private int openFilesSampleTimeMillis;
  private int memoryUseSampleTimeMillis;
  private int gcUseSampleTimeMillis;
  private int threadUseSampleTimeMillis;
  private int cpuUseSampleTimeMillis;

  private String metricsStoreConfig;

  private Function<ExecutionContext, String> aggregationGroups;

  private String extraStoreConfig;

  public JvmServicesBuilder() {
    this.profilerSampleTimeMillis = Env.getValue("PROFILER_SAMPLE_MILLIS", 10);
    this.profilerDumpTimeMillis = Env.getValue("PROFILER_DUMNP_MILLIS", 3600000);
    this.profilerJmx = Env.getValue("PROFILER_JMX", true);
    this.applicationName = null;
    this.hostName = null;
    this.logFolder = null;
    this.extraStoreConfig = "";
    this.openFilesSampleTimeMillis = Env.getValue("V_OPEN_FILES_S_MILLIS", 60000);
    this.memoryUseSampleTimeMillis = Env.getValue("V_MEM_USE_S_MILLIS", 10000);
    this.gcUseSampleTimeMillis = Env.getValue("V_GC_USE_S_MILLIS", 10000);
    this.threadUseSampleTimeMillis = Env.getValue("V_THREAD_USE_S_MILLIS", 10000);
    this.cpuUseSampleTimeMillis = Env.getValue("V_CPU_USE_S_MILLIS", 10000);
    this.aggregationGroups = (ctx) -> {
                        String name = ctx.getName();
                        if (name.startsWith("GET")) {
                          return "GET";
                        } else if (ctx.getName().startsWith("POST")) {
                          return "POST";
                        } else {
                          return "OTHER";
                        }
                      };
  }



  public JvmServicesBuilder withProfilingAggregationGroups(
          final Function<ExecutionContext, String> contextAggregations) {
    this.aggregationGroups = contextAggregations;
    return this;
  }

  public JvmServicesBuilder withHostName(final String phostName) {
    this.hostName = phostName;
    return this;
  }

  public JvmServicesBuilder withApplicationName(final String papplicationName) {
    this.applicationName = papplicationName;
    return this;
  }

  public JvmServicesBuilder withLogFolder(final String plogFolder) {
    this.logFolder = plogFolder;
    return this;
  }

  public JvmServicesBuilder withMetricsStore(final String pmetricsStoreConfig) {
    this.metricsStoreConfig = pmetricsStoreConfig;
    return this;
  }

  public JvmServicesBuilder withExtraMetricsStore(final String storeCfg) {
    if (extraStoreConfig.isEmpty()) {
      extraStoreConfig = storeCfg;
    } else {
      extraStoreConfig += ',' + Csv.CSV.toCsvElement(storeCfg);
    }
    return this;
  }


  private void initUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandlerImpl());
  }


  private void initDefaults() {
    if (this.hostName == null) {
      this.hostName = Env.getValue("KUBE_POD_NAME", () -> OperatingSystem.getHostName());
    }
    if (this.logFolder == null) {
      this.logFolder = Env.getValue("LOG_FOLDER", "/var/log");
    }
    if (this.applicationName == null) {
      this.applicationName = Env.getValue("KUBE_APP_NAME", "KUBE_APP_NAME");
    }
  }

  private void initMetricsStorage() {
    String cfg;
    if (metricsStoreConfig == null || metricsStoreConfig.isEmpty()) {
      cfg = "TSDB_AVRO@" + logFolder + '/' + hostName + extraStoreConfig;
    } else {
      cfg = metricsStoreConfig + extraStoreConfig;
    }
    System.setProperty("spf4j.perf.ms.config", cfg);
  }


  private void initRequestAttributedProfiler() {
    // Enable Continuous profiling.
    System.setProperty("spf4j.execContext.tlAttacherClass", ProfilingTLAttacher.class.getName());
    System.setProperty("spf4j.execContext.factoryClass", ProfiledExecutionContextFactory.class.getName());
  }

  @Nullable
  private Sampler createSampler() {
    Sampler sampler;
    ThreadLocalContextAttacher threadLocalAttacher = ExecutionContexts.threadLocalAttacher();
    if (!(threadLocalAttacher instanceof ProfilingTLAttacher)) {
      Logger.getLogger(JvmServicesBuilder.class.getName()).log(Level.WARNING, "ProfilingTLAttacher is NOT active,"
              + " alternate profiling config already set up: {}", threadLocalAttacher);
      sampler = new Sampler(profilerSampleTimeMillis, profilerDumpTimeMillis,
              (t) -> new FastStackCollector(false, true, new Thread[]{t}), logFolder, applicationName);
    } else {
      ProfilingTLAttacher contextFactory = (ProfilingTLAttacher) threadLocalAttacher;
      sampler = new Sampler(profilerSampleTimeMillis, profilerDumpTimeMillis,
              (t) -> new TracingExecutionContexSampler(contextFactory::getCurrentThreadContexts, aggregationGroups),
              logFolder, applicationName);
    }
    if (profilerJmx) {
      sampler.registerJmx();
    }
    return sampler;
  }

  public JvmServices build() {
    JvmServices svc = services;
    if (svc != null) {
      throw new IllegalStateException();
    }
    initDefaults();
    initMetricsStorage();
    initUncaughtExceptionHandler();
    initRequestAttributedProfiler();
    Sampler sampler = createSampler();
    svc = new JvmServicesImpl(sampler, new ProcessVitals(openFilesSampleTimeMillis,
            memoryUseSampleTimeMillis, gcUseSampleTimeMillis, threadUseSampleTimeMillis, cpuUseSampleTimeMillis),
            new LogbackService(applicationName, logFolder, hostName),
            this);
    services = svc;
    return svc;
  }

  @Override
  public String toString() {
    return "JvmServicesBuilder{" + "hostName=" + hostName + ", applicationName=" + applicationName
            + ", logFolder=" + logFolder + ", profilerSampleTimeMillis=" + profilerSampleTimeMillis
            + ", profilerDumpTimeMillis=" + profilerDumpTimeMillis + ", profilerJmx=" + profilerJmx
            + ", openFilesSampleTimeMillis=" + openFilesSampleTimeMillis + ", memoryUseSampleTimeMillis="
            + memoryUseSampleTimeMillis + ", gcUseSampleTimeMillis=" + gcUseSampleTimeMillis
            + ", threadUseSampleTimeMillis=" + threadUseSampleTimeMillis + ", cpuUseSampleTimeMillis="
            + cpuUseSampleTimeMillis + '}';
  }




  private static class JvmServicesImpl implements JvmServices {

    private final Sampler sampler;

    private final ProcessVitals vitals;

    private final  JvmServicesBuilder builder;

    private final LogbackService logService;

    JvmServicesImpl(final Sampler sampler, final ProcessVitals vitals,
            final LogbackService logService,
            final JvmServicesBuilder builder) {
      this.sampler = sampler;
      this.vitals = vitals;
      this.builder = builder;
      this.logService = logService;
    }

    @Override
    public String getLogFolder() {
      return builder.logFolder;
    }

    @Override
    public Sampler getProfiler() {
      return sampler;
    }

    @Override
    public String getApplicationName() {
      return builder.applicationName;
    }

    @Override
    public String getHostName() {
      return builder.hostName;
    }

    @Override
    public ProcessVitals getVitals() {
      return vitals;
    }

    @Override
    public LogbackService getLoggingService() {
     return logService;
    }

  }

  private static class UncaughtExceptionHandlerImpl implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
      if (Throwables.containsNonRecoverable(e)) {
        org.spf4j.base.Runtime.goDownWithError(e, SysExits.EX_SOFTWARE);
      } else {
        Logger logger = Logger.getLogger("UNCAUGHT");
        logger.log(Level.SEVERE, "Error in thread {0}", t);
        logger.log(Level.SEVERE, "Exception detail", e);
      }
    }
  }
}
