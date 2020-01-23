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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.ThreadLocalContextAttacher;
import org.spf4j.log.SLF4JBridgeHandler;
import org.spf4j.os.OperatingSystem;
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

  private boolean startProfiler;

  private boolean profilerJmx;

  public JvmServicesBuilder withApplicationName(final String applicationName) {
    this.applicationName = applicationName;
    return this;
  }

  public JvmServicesBuilder withLogFolder(final String logFolder) {
    this.logFolder = logFolder;
    return this;
  }


  public JvmServicesBuilder() {
    this.logFolder = Env.getValue("LOG_FOLDER", "/var/log");
    this.profilerSampleTimeMillis = Env.getValue("PROFILER_SAMPLE_MILLIS", 10);
    this.profilerDumpTimeMillis = Env.getValue("PROFILER_DUMNP_MILLIS", 3600000);
    this.startProfiler = Env.getValue("PROFILER_START", true);
    this.profilerJmx = Env.getValue("PROFILER_JMX", true);
    this.applicationName = Env.getValue("KUBE_APP_NAME", "KUBE_APP_NAME");
    this.hostName = Env.getValue("KUBE_POD_NAME", () -> OperatingSystem.getHostName());
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
    if (startProfiler) {
      sampler.start();
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
    initLogConfig();
    initRequestAttributedProfiler();
    Sampler sampler = createSampler();
    svc = new JvmServices() {
      @Override
      public String getLogFolder() {
        return logFolder;
      }

      @Override
      public Sampler getProfiler() {
        return sampler;
      }

      @Override
      public String getApplicationName() {
        return applicationName;
      }

      @Override
      public String getHostName() {
        return hostName;
      }

      @Override
      public void close() throws Exception {
        sampler.stop();
      }
    };
    services = svc;
    return svc;
  }
}
