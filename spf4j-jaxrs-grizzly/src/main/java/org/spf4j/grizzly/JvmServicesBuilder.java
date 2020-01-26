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

import org.spf4j.base.Env;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.ThreadLocalContextAttacher;
import org.spf4j.log.SLF4JBridgeHandler;
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
public class JvmServicesBuilder {

  private String hostName;

  static volatile JvmServices services;

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


  public JvmServicesBuilder() {
    this.profilerSampleTimeMillis = Env.getValue("PROFILER_SAMPLE_MILLIS", 10);
    this.profilerDumpTimeMillis = Env.getValue("PROFILER_DUMNP_MILLIS", 3600000);
    this.profilerJmx = Env.getValue("PROFILER_JMX", true);
    this.applicationName = null;
    this.hostName = null;
    this.logFolder = null;
    this.openFilesSampleTimeMillis = Env.getValue("V_OPEN_FILES_S_MILLIS", 60000);
    this.memoryUseSampleTimeMillis = Env.getValue("V_MEM_USE_S_MILLIS", 10000);
    this.gcUseSampleTimeMillis = Env.getValue("V_GC_USE_S_MILLIS", 10000);
    this.threadUseSampleTimeMillis = Env.getValue("V_THREAD_USE_S_MILLIS", 10000);
    this.cpuUseSampleTimeMillis = Env.getValue("V_CPU_USE_S_MILLIS", 10000);
  }

  public JvmServicesBuilder withHostName(final String hostName) {
    this.hostName = hostName;
    return this;
  }

  public JvmServicesBuilder withApplicationName(final String applicationName) {
    this.applicationName = applicationName;
    return this;
  }

  public JvmServicesBuilder withLogFolder(final String logFolder) {
    this.logFolder = logFolder;
    return this;
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

  private void initLogConfig() {
    System.setProperty("appName", applicationName); // for logback config xml.
    System.setProperty("logFolder", logFolder); // for logback config xml.
    System.setProperty("spf4j.perf.ms.defaultTsdbFolderPath", logFolder); // for spf4j tsdb
    System.setProperty("spf4j.perf.ms.defaultSsdumpFolder", logFolder); // for spf4j ssdump
    // install java.util.logging -> org.slf4j.logging bridge
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
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
              (t) -> new TracingExecutionContexSampler(contextFactory::getCurrentThreadContexts,
                      (ctx) -> {
                        String name = ctx.getName();
                        if (name.startsWith("GET")) {
                          return "GET";
                        } else if (ctx.getName().startsWith("POST")) {
                          return "POST";
                        } else {
                          return "OTHER";
                        }
                      }), logFolder, applicationName);
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
    initLogConfig();
    initRequestAttributedProfiler();
    Sampler sampler = createSampler();
    svc = new JvmServicesImpl(sampler, new ProcessVitals(openFilesSampleTimeMillis,
            memoryUseSampleTimeMillis, gcUseSampleTimeMillis, threadUseSampleTimeMillis, cpuUseSampleTimeMillis), this);
    services = svc;
    return svc;
  }

  private static class JvmServicesImpl implements JvmServices {

    private final Sampler sampler;

    private final ProcessVitals vitals;

    private final  JvmServicesBuilder builder;

    public JvmServicesImpl(final Sampler sampler, final ProcessVitals vitals, final JvmServicesBuilder builder) {
      this.sampler = sampler;
      this.vitals = vitals;
      this.builder = builder;
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

  }
}
