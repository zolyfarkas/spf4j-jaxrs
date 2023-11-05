package org.spf4j.http;


import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spf4j.base.Json;
import org.spf4j.base.JsonWriteable;
import org.spf4j.base.MutableHolder;
import org.spf4j.io.AppendableWriter;

/**
 * https://w3c.github.io/server-timing/#the-server-timing-header-field
 *
 * @author Zoltan Farkas
 */
public final class ServerTiming implements JsonWriteable {


  /**
   *
   * server-timing-metric      = metric-name *( OWS ";" OWS server-timing-param )
   * metric-name               = token
   * server-timing-param       = server-timing-param-name OWS "=" OWS server-timing-param-value
   * server-timing-param-name  = token
   * server-timing-param-value = token / quoted-string
   *
   */
  public static final class ServerTimingMetric {

    private final String name;
    /** a duration in milliseconds is a recommendation... and you call this a "spec" */
    private final List<ServerTimingParameter> parameters;



    private static List<ServerTimingParameter> toParameters(final Double durationMs, final String description) {
      ArrayList parameters = new ArrayList<>(2);
      if (durationMs != null) {
        parameters.add(new ServerTimingParameter("dur", durationMs.toString(), false));
      }
      if (description != null && !description.isEmpty()) {
        parameters.add(new ServerTimingParameter("description", description, false));
      }
      return parameters;
    }

    public ServerTimingMetric(final String name, final Double durationMs) {
      this(name, toParameters(durationMs, null), true);
    }

    public ServerTimingMetric(final String name, final Double durationMs, final String description) {
      this(name, toParameters(durationMs, description), true);
    }

    private ServerTimingMetric(final String name,
            final List<ServerTimingParameter> parameters,
            final boolean validate
      ) {
      this.name = name;
      if (validate && !Headers.isValidToken(name)) {
        throw new IllegalArgumentException("Invalid parameter name: "
                + name + "; must be a valid token according HTTP spec");
      }
      this.parameters = parameters;
    }

    public ServerTimingMetric addParameter(final String pname, final String pvalue) {
      this.parameters.add(new ServerTimingParameter(pname, pvalue));
      return this;
    }

    @Nullable
    public String getParameterValue(final String pname) {
      for (ServerTimingParameter param : parameters) {
        if (param.name.equals(pname)) {
          return param.value;
        }
      }
      return null;
    }

    @Nullable
    public Double getDuration() {
      String strDur = this.getParameterValue("dur");
      if (strDur == null) {
        return null;
      }
      return Double.valueOf(strDur);
    }

    public static ServerTimingMetric valueOf(final CharSequence source) {
      return parse(source);
    }

    public static ServerTimingMetric parse(final CharSequence source) {
      MutableHolder<ServerTimingMetric> stp = new MutableHolder<>();
      parse(source, 0, stp::setValue);
      ServerTimingMetric result = stp.getValue();
      if (result == null) {
        throw new IllegalArgumentException("No Metric in " + source);
      }
      return result;
    }

    @Nullable
    public static int parse(final CharSequence source, final int from, final Consumer<ServerTimingMetric> metric) {
      int at = Headers.skipSpaces(source, from);
      int l = source.length();
      if (at >= l) {
        return at;
      }
      StringBuilder nameBuilder = new StringBuilder();
      at = Headers.parseToken(source, at, nameBuilder);
      at = Headers.skipSpaces(source, at);
      List<ServerTimingParameter> params  = new ArrayList<>(2);
      while (at < l && source.charAt(at) == ';') {
        at = Headers.skipSpaces(source, at + 1);
        at = ServerTimingParameter.parse(source, at, params::add);
      }
      metric.accept(new ServerTimingMetric(nameBuilder.toString(), params, false));
      return at;
    }

    public void writeStringTo(final Appendable sb) throws IOException {
      sb.append(name);
      for (ServerTimingParameter p : this.parameters) {
        sb.append(';');
        p.writeStringTo(sb);
      }
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      try {
        this.writeStringTo(result);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      return result.toString();
    }

    @Override
    public int hashCode() {
      int hash = 3;
      hash = 79 * hash + Objects.hashCode(this.name);
      return 79 * hash + Objects.hashCode(this.parameters);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final ServerTimingMetric other = (ServerTimingMetric) obj;
      if (!Objects.equals(this.name, other.name)) {
        return false;
      }
      return Objects.equals(this.parameters, other.parameters);
    }


  }

  /**
   * server-timing-param       = server-timing-param-name OWS "=" OWS server-timing-param-value
   * server-timing-param-name  = token
   * server-timing-param-value = token / quoted-string
   */
  public static final class ServerTimingParameter {
    private final String name;
    private final String value;

    public ServerTimingParameter(final String name, final String value) {
      this(name, value, true);
    }

    private ServerTimingParameter(final String name, final String value, final boolean validate) {
      if (validate && !Headers.isValidToken(name)) {
        throw new IllegalArgumentException("Invalid parameter name: "
                + name + "; must be a valid token according HTTP spec");
      }
      this.name = name;
      this.value = value;
    }

    public static ServerTimingParameter valueOf(final CharSequence source) {
      return parse(source);
    }

    public static ServerTimingParameter parse(final CharSequence source) {
       MutableHolder<ServerTimingParameter> stp = new MutableHolder<>();
       parse(source, 0, stp::setValue);
       ServerTimingParameter result = stp.getValue();
       if (result == null) {
         throw new IllegalArgumentException("No server timing param: " + source);
       }
       return result;
    }

    public static int parse(final CharSequence source, final int from, final Consumer<ServerTimingParameter> to) {
      StringBuilder nameBuilder = new StringBuilder();
      int at = Headers.parseToken(source, from, nameBuilder);
      if (at == from) {
        return at;
      }
      if (source.charAt(at) != '=') {
        throw new IllegalArgumentException("Excepceted '=' at " + at + " in: " + source);
      }
      StringBuilder valueBuilder = new StringBuilder();
      at = Headers.parseTokenOrQuotedString(source, at + 1, valueBuilder);
      to.accept(new ServerTimingParameter(nameBuilder.toString(), valueBuilder.toString(), false));
      return at;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 97 * hash + Objects.hashCode(this.name);
      return 97 * hash + Objects.hashCode(this.value);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final ServerTimingParameter other = (ServerTimingParameter) obj;
      if (!Objects.equals(this.name, other.name)) {
        return false;
      }
      if (!Objects.equals(this.value, other.value)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      try {
        this.writeStringTo(result);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
      return result.toString();
    }

    public void writeStringTo(final Appendable sb) throws IOException {
      sb.append(name);
      sb.append('=');
      if (Headers.isValidToken(value)) {
        sb.append(value);
      } else {
          Headers.writeQuotedString(value, sb);
      }
    }

  }

  private final List<ServerTimingMetric> metrics;

  /**
   *
   * Server-Timing             = #server-timing-metric
   * server-timing-metric      = metric-name *( OWS ";" OWS server-timing-param )
   * metric-name               = token
   * server-timing-param       = server-timing-param-name OWS "=" OWS server-timing-param-value
   * server-timing-param-name  = token
   * server-timing-param-value = token / quoted-string
   *
   * @param headerValue
   * @return
   */
  public static ServerTiming parse(final CharSequence headerValue) {
    List<ServerTimingMetric> metrics = new ArrayList<>(2);
    int at = ServerTimingMetric.parse(headerValue, 0, metrics::add);
    if (metrics.isEmpty()) {
      throw new IllegalArgumentException("No metrics in " + headerValue);
    }
    at = Headers.skipSpaces(headerValue, at);
    int l = headerValue.length();
    while (at < l && headerValue.charAt(at) == ',') {
      at = Headers.skipSpaces(headerValue, at + 1);
      at = ServerTimingMetric.parse(headerValue, at, metrics::add);
      at = Headers.skipSpaces(headerValue, at);
    }
    return new ServerTiming(metrics);
  }

  public static ServerTiming valueOf(final String from) {
    return parse(from);
  }


  public ServerTiming(final ServerTimingMetric... metrics) {
    this.metrics = Arrays.asList(metrics);
  }

  private ServerTiming(final List<ServerTimingMetric> metrics) {
    if (metrics.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one timing metric: " + metrics);
    }
    this.metrics = metrics;
  }

  @Override
  public void writeJsonTo(final Appendable appendable) throws IOException {
    JsonGenerator gen = Json.FACTORY.createGenerator(new AppendableWriter(appendable));
    gen.writeStartArray();
    for (ServerTimingMetric metric: this.metrics) {
       gen.writeStartObject();
       gen.writeFieldName("name");
       gen.writeString(metric.name);
       gen.writeFieldName("parameters");
       gen.writeStartObject();
       for (ServerTimingParameter param : metric.parameters) {
         gen.writeFieldName(param.name);
         gen.writeString(param.value);
       }
       gen.writeEndObject();
       gen.writeEndObject();
    }
    gen.writeEndArray();
    gen.flush();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(32);
    try {
      writeStringTo(sb);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return sb.toString();
  }

  public void writeStringTo(final Appendable sb) throws IOException {
    Iterator<ServerTimingMetric> it = this.metrics.iterator();
    if (it.hasNext()) {
      ServerTimingMetric metric = it.next();
      metric.writeStringTo(sb);
      while (it.hasNext()) {
        metric = it.next();
        sb.append(", ");
        metric.writeStringTo(sb);
      }
    }
  }

  @Override
  public int hashCode() {
    int hash = 7;
    return 17 * hash + Objects.hashCode(this.metrics);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ServerTiming other = (ServerTiming) obj;
    return Objects.equals(this.metrics, other.metrics);
  }

}
