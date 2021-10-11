package org.spf4j.http;

import com.fasterxml.jackson.core.JsonGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.mail.internet.MimeUtility;
import org.spf4j.base.CharSequences;
import org.spf4j.base.Json;
import org.spf4j.base.JsonWriteable;
import org.spf4j.base.Slf4jMessageFormatter;
import org.spf4j.io.AppendableWriter;

/**
 * https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
 *
 * @author Zoltan Farkas
 */
public final class HttpWarning implements JsonWriteable {

  /**
   * Warning status - Response is stale.
   */
  public static final int STALE = 110;
  /**
   * Warning status - Revalidation failed.
   */
  public static final int REVALIDATION_FAILED = 111;
  /**
   * Warning status - Disconnected opertaion.
   */
  public static final int DISCONNECTED_OPERATION = 112;
  /**
   * Warning status - Heuristic expiration.
   */
  public static final int HEURISTIC_EXPIRATION = 113;
  /**
   * Warning status - Miscellaneous warning
   */
  public static final int MISCELLANEOUS = 199;
  /**
   * Warning status - Transformation applied.
   */
  public static final int TRANSFORMATION_APPLIED = 214;
  /**
   * Warning status - Miscellaneous warning
   */
  public static final int PERSISTENT_MISCELLANEOUS = 299;

  private final int code;
  private final String agent;
  private final String text;
  private final ZonedDateTime date;

  private static int parseString(final CharSequence source,
          final int from,
          final StringBuilder destination)  {
    if (source.charAt(from) != '"') {
      throw new IllegalArgumentException("No quoted-string at " + from + " in " + source);
    }
    boolean escaped = false;
    boolean closed = false;
    int i = from + 1;
    OUTER:
    for (int l = source.length(); i < l; i++) {
      char charAt = source.charAt(i);
      switch (charAt) {
        case '\\':
          if (escaped) {
            destination.append(charAt);
            escaped = false;
          } else {
            escaped = true;
          }
          break;
        case '"':
          if (escaped) {
            destination.append(charAt);
            escaped = false;
          } else {
            closed = true;
            break OUTER;
          }
          break;
        default:
          destination.append(charAt);
          if (escaped) {
            escaped = false;
          }
          break;
      }
    }
    if (!closed) {
      throw new IllegalArgumentException("Not closed quoted-string in " + source);
    }
    return i + 1;
  }


  @SuppressFBWarnings("SF_SWITCH_NO_DEFAULT") // not really
  private static void writeString(final CharSequence source,
          final Appendable destination) throws IOException  {
    destination.append('"');
    for (int i = 0, l = source.length(); i < l; i++) {
      char charAt = source.charAt(i);
      switch (charAt) {
        case '"':
        case '\\':
        case '\n':
        case '\r':
          destination.append('\\');
        default:
          destination.append(charAt);
      }
    }
    destination.append('"');
  }


  /**
   * Warning = "Warning" ":" 1#warning-value
   * warning-value = warn-code SP warn-agent SP warn-text [SP warn-date]
   * warn-code = 3DIGIT
   * warn-agent = ( host [ ":" port ] ) | pseudonym
   *  ; the name or pseudonym of the server adding
   *  ; the Warning header, for use in debugging
   *  warn-text = quoted-string
   *  warn-date = <"> HTTP-date <"> ;(RFC 1123)
   *
   * @param headerValue
   * @return
   */
  public static HttpWarning parse(final CharSequence headerValue) {
    int fsIdx = CharSequences.indexOf(headerValue, 0, headerValue.length(), ' ');
    if (fsIdx < 0) {
      throw new IllegalArgumentException("Invalid warning message: " + headerValue);
    }
    int code;
    try {
      code = CharSequences.parseUnsignedInt(headerValue, 10, 0, fsIdx);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid warning message: " + headerValue, ex);
    }
    int agStartIdx = fsIdx + 1;
    int ssIdx = CharSequences.indexOf(headerValue, agStartIdx, headerValue.length(), ' ');
    if (ssIdx < 0) {
      throw new IllegalArgumentException("Invalid warning message: " + headerValue);
    }
    String agent = headerValue.subSequence(agStartIdx, ssIdx).toString();
    StringBuilder textB = new StringBuilder();
    int txtEnd = parseString(headerValue, ssIdx + 1, textB);
    String text;
    try {
      text = MimeUtility.decodeText(textB.toString());
    } catch (UnsupportedEncodingException ex) {
      throw new IllegalArgumentException("Improperly encoded text message in header: " + headerValue, ex);
    }
    ZonedDateTime zdt;
    if (txtEnd >= headerValue.length()) {
      zdt = null;
    } else {
      StringBuilder dateB = new StringBuilder();
      parseString(headerValue, txtEnd + 1, dateB);
      zdt = DateTimeFormatter.RFC_1123_DATE_TIME.parse(dateB, ZonedDateTime::from);
    }
    return new HttpWarning(code, agent, zdt, text);
  }

  public static HttpWarning valueOf(final String from) {
    return parse(from);
  }

  public HttpWarning(final int code, final String agent, @Nullable final  ZonedDateTime date,
          final String format, final Object... params) {
    this(code, agent, null, Slf4jMessageFormatter.toString(format, params));
  }


  public HttpWarning(final int code, final String agent, final String format, final Object... params) {
    this(code, agent, Slf4jMessageFormatter.toString(format, params));
  }

  public HttpWarning(final int code, final String agent, final String text) {
    this(code, agent, null,  text);
  }

  public HttpWarning(final int code, final String agent, @Nullable final  ZonedDateTime date, final String text) {
    if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
      throw new IllegalArgumentException("No multiline warning messages supported: " + text);
    }
    this.code = code;
    this.agent = agent;
    this.text = text;
    this.date = date == null ? null : date.withNano(0);
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
    sb.append(Integer.toString(code));
    sb.append(' ');
    sb.append(agent);
    sb.append(' ');
    writeString(MimeUtility.encodeText(text), sb);
    if (date != null) {
      sb.append(" \"");
      DateTimeFormatter.RFC_1123_DATE_TIME.formatTo(date, sb);
      sb.append('"');
    }
  }

  public int getCode() {
    return code;
  }

  public String getAgent() {
    return agent;
  }

  public String getText() {
    return text;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP") // ZonedDateTime is immutable this is a false positive.
  public ZonedDateTime getDate() {
    return date;
  }

  @Override
  public int hashCode() {
    int hash = 77 + this.code;
    hash = 11 * hash + Objects.hashCode(this.agent);
    return 11 * hash + Objects.hashCode(this.text);
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
    final HttpWarning other = (HttpWarning) obj;
    if (this.code != other.code) {
      return false;
    }
    if (!Objects.equals(this.agent, other.agent)) {
      return false;
    }
    if (!Objects.equals(this.text, other.text)) {
      return false;
    }
    if (this.date != null) {
      if (other.date == null) {
        return false;
      } else {
        return this.date.toEpochSecond()  == other.date.toEpochSecond();
      }
    } else {
      return other.date == null;
    }
  }

  @Override
  public void writeJsonTo(final Appendable appendable) throws IOException {
    JsonGenerator gen = Json.FACTORY.createGenerator(new AppendableWriter(appendable));
    gen.writeStartObject();
    gen.writeFieldName("code");
    gen.writeNumber(code);
    gen.writeFieldName("agent");
    gen.writeString(agent);
    gen.writeFieldName("text");
    gen.writeString(text);
    if (date != null) {
      gen.writeFieldName("date");
      gen.writeString(DateTimeFormatter.RFC_1123_DATE_TIME.format(date));
    }
    gen.writeEndObject();
    gen.flush();
  }

}
