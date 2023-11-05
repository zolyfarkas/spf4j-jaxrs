package org.spf4j.http;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import java.io.IOException;

/**
 * default HTTP headers.
 * @author Zoltan Farkas
 */
public final class Headers {

  private Headers() { }

  /**
   * request-deadline → SecondsSinceEpoch Nanos?
   */
  public static final String REQ_DEADLINE = "request-deadline";

  /**
   * request-timeout → TimeoutValue TimeoutUnit?
   *  TimeoutValue → {positive integer as ASCII string of at most 8 digits}
   *  TimeoutUnit → Hour / Minute / Second / Millisecond / Microsecond / Nanosecond
   *  Hour → "H"
   *  Minute → "M"
   *  Second → "S"
   *  Millisecond → "m"
   *  Microsecond → "u"
   *  Nanosecond → "n"
   */
  public static final String REQ_TIMEOUT = "request-timeout";

  /**
   * A ID in the form of [chars]/[chars] where / delimits the execution context hierarchy.
   */
  public static final String REQ_ID = "request-id";

  /**
   * the avro schema of the content.
   * @deprecated should use Content-Type parameter avsc instead.
   */
  @Deprecated
  public static final String CONTENT_SCHEMA = "content-schema";

  /**
   * Overwrite the log level of the context to make logs be persisted by the backend.
   */
  public static final String CTX_LOG_LEVEL = "log-level";

  /**
   * see https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html for detail.
   */
  public static final String WARNING = "Warning";


  private static final char[] DELIMITER_CHARS = "\"(),/:;<=>?@[\\]{}\t".toCharArray();
  private static final TCharSet DELIMITERS = new TCharHashSet();

  static {
    for (char c: DELIMITER_CHARS) {
      DELIMITERS.add(c);
    }
  }

  public static boolean isDelimiter(final char c) {
    return DELIMITERS.contains(c);
  }

  public static boolean isValidTokenChar(final char c) {
    if (c < 0x21 || c > 0x7E) {
      return false;
    }
    return !isDelimiter(c);
  }

  /**
   * https://httpwg.org/specs/rfc7230.html#field.components
   *
   *   token          = 1*tchar
   *
   *   tchar    = "!" / "#" / "$" / "%" / "&" / "'" / "*"
   *             / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
   *              / DIGIT / ALPHA
   *              ; any VCHAR, except delimiters
   *
   *   VCHAR =  %x21-7E ; visible (printing) characters
   *   Delimiters: (DQUOTE and "(),/:;<=>?@[\]{} ")
   *
   * @return
   */
  static int parseToken(final CharSequence source,
          final int from,
          final StringBuilder destination) {
      int i = from;
      int l = source.length();
      char c;
      while (i < l && isValidTokenChar(c = source.charAt(i))) {
        destination.append(c);
        i++;
      }
      return i;
  }


  static boolean isValidToken(final CharSequence source) {
       for (int i = 0, l = source.length(); i < l; i++) {
         char c = source.charAt(i);
         if (!isValidTokenChar(c)) {
           return false;
         }
       }
       return true;
  }

  static void writeToken(final CharSequence source,
          final Appendable destination) throws IOException {
       for (int i = 0, l = source.length(); i < l; i++) {
         char c = source.charAt(i);
         if (isValidTokenChar(c)) {
           destination.append(c);
         } else {
           throw new IllegalArgumentException("Invalid HTTP header token: " + source);
         }
       }
  }


  /**
   * Parse a quoted String from a CharSequence.
   * If not a Quoted string there, IllegalArgumentException will be raised.
   * @param source
   * @param from
   * @param destination
   * @return the index after the last quote.
   */
  static int parseQuotedString(final CharSequence source,
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
  static void writeQuotedString(final CharSequence source,
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

  public static int parseTokenOrQuotedString(final CharSequence source,
          final int from,
          final StringBuilder destination) {

    if (source.charAt(from) == '"') {
      return parseQuotedString(source, from, destination);
    } else {
      return parseToken(source, from, destination);
    }
  }

  public static int skipSpaces(final CharSequence source,
          final int from) {
    int i = from;
    int l = source.length();
    while (i < l && source.charAt(i) == ' ') {
      i++;
    }
    return i;
  }

}
