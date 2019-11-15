package org.spf4j.jaxrs.aql;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Reader;
import java.io.StringReader;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.AvroSchema;
import org.apache.calcite.config.Lex;
import org.apache.calcite.interpreter.Interpreter;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.calcite.EmbededDataContext;
import org.spf4j.avro.calcite.IndexedRecords;
import org.spf4j.avro.calcite.Types;
import org.spf4j.jaxrs.IterableArrayContent;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.avro.calcite.AvroDataSetAsProjectableFilterableTable;
import org.spf4j.aql.AvroDataSetContract;
import org.spf4j.avro.calcite.PlannerUtils;
import org.spf4j.http.Headers;
import org.spf4j.http.HttpWarning;

/**
 * @author Zoltan Farkas
 */
@Path("avql/query")
@SuppressFBWarnings({ "EXS_EXCEPTION_SOFTENING_NO_CHECKED",  "SIC_INNER_SHOULD_BE_STATIC_ANON" })
public final class AvroQueryResourceImpl implements AvroQueryResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(AvroQueryResourceImpl.class));

  private final Planner planner;

  private final Map<String, Schema> schemas;

  private final FrameworkConfig config;

  private final AbacAuthorizer authorizer;

  @Inject
  public AvroQueryResourceImpl(final Iterable<AvroDataSetContract> resources,
          @Nullable final AbacAuthorizer authorizer) {
    SchemaPlus schema = Frameworks.createRootSchema(true);
    schemas = new HashMap<>();
    for (AvroDataSetContract res : resources) {
      String name = res.getName();
      Schema tschema = res.getElementSchema();
      schemas.put(name, tschema);
      LOG.debug("Registered {} table to schema", name);
      schema.add(name, new AvroDataSetAsProjectableFilterableTable(res));
    }
    SqlParser.Config cfg = SqlParser.configBuilder()
            .setCaseSensitive(true)
            .setIdentifierMaxLength(255)
            .setLex(Lex.JAVA).build();
    config = Frameworks.newConfigBuilder()
            .parserConfig(cfg)
            .defaultSchema(schema)
            .build();
    this.planner = Frameworks.getPlanner(config);
    if (authorizer == null)   {
      this.authorizer = AbacAuthorizer.NO_ACCESS;
    } else {
      this.authorizer = authorizer;
    }
  }

  @Override
  public Response query(final String query, final SecurityContext secCtx) {
    return query(new StringReader(query), secCtx);
  }

  @Override
  public Response query(final Reader query, final SecurityContext secCtx) {
    RelNode relNode = parsePlan(query);
    LOG.debug("exec plan: {}", new ReadablePlan(relNode));
    RelDataType rowType = relNode.getRowType();
    LOG.debug("Return row type: {}", rowType);
    Schema from = Types.from(rowType);
    LOG.debug("Return row schema: {}", from);
    EmbededDataContext dc = new EmbededDataContext(new JavaTypeFactoryImpl(),
            new SecurityContextAdapter(secCtx, authorizer));
    Interpreter interpreter = new Interpreter(dc, relNode);
    Response.ResponseBuilder rb = Response.ok(new IterableInterpreter(from, interpreter));
    Map<String, String> deprecations = (Map<String, String>) dc.get(EmbededDataContext.DEPRECATIONS);
    if (deprecations != null && !deprecations.isEmpty()) {
      for (Map.Entry<String, String> dep : deprecations.entrySet()) {
        rb.header(Headers.WARNING, new HttpWarning(HttpWarning.MISCELLANEOUS, "deprecation",
          "Deprecated " + dep.getKey() + "; " + dep.getValue()));
      }
    }
    return rb.build();
  }

  @Override
  public RelNode plan(final Reader query, final SecurityContext secCtx) {
    return parsePlan(query);
  }

  @Override
  public RelNode plan(final String query, final SecurityContext secCtx) {
    return plan(new StringReader(query), secCtx);
  }

  public RelNode parsePlan(final Reader query) {
    SqlNode parse;
    try {

      parse = planner.parse(query);
    } catch (SqlParseException ex) {
      throw new ClientErrorException("Cannot parse query: " + query, 400, ex);
    }
    try {
      parse = planner.validate(parse);
    } catch (ValidationException ex) {
      throw new ClientErrorException("Cannot validate query: " + query, 400, ex);
    }
    RelRoot rel;
    try {
      rel = planner.rel(parse);
    } catch (RelConversionException ex) {
      throw new RuntimeException(ex);
    }
    RelNode relNode = rel.project();
    return PlannerUtils.pushDownPredicatesAndProjection(relNode);
  }

  @Override
  @AvroSchema("{ \"type\" : \"map\", \"values\" : { \"type\" : \"string\" , \"logicalType\" : \"avsc\"} } ")
  public Map<String, Schema> schemas(final SecurityContext secCtx) {
    return schemas;
  }

  @Override
  public Schema entitySchema(final String entityName, final SecurityContext secCtx) {
    return schemas.get(entityName);
  }

  @Override
  public String toString() {
    return "QueryResourceImpl{" + "schemas=" + schemas + '}';
  }

  @Override
  public Schema schema(final String query, final SecurityContext secCtx) {
    return schema(new StringReader(query), secCtx);
  }

  @Override
  public Schema schema(final Reader query, final SecurityContext secCtx) {
    return Types.from(parsePlan(query).getRowType());
  }



  private static class ReadablePlan extends Object {

    private final RelNode relNode;

    ReadablePlan(final RelNode relNode) {
      this.relNode = relNode;
    }

    @Override
    public String toString() {
      return RelOptUtil.toString(relNode);
    }
  }

  private static class IterableInterpreter implements IterableArrayContent<GenericRecord> {

    private final Schema rowSchema;
    private final Interpreter interpreter;

    IterableInterpreter(final Schema rowSchema, final Interpreter interpreter) {
      this.rowSchema = rowSchema;
      this.interpreter = interpreter;
    }

    @Override
    public Schema getElementSchema() {
      return rowSchema;
    }

    @Override
    public void close() {
      interpreter.close();
    }

    @Override
    public Iterator<GenericRecord> iterator() {
      return new Iterator<GenericRecord>() {
        private final Iterator<Object[]>  it = interpreter.iterator();

        @Override
        public boolean hasNext() {
          return it.hasNext();
        }

        @Override
        public GenericRecord next() {
          Object[] row = it.next();
          LOG.debug("Raw Row {}",  (Object) row);
          GenericRecord record = IndexedRecords.fromRecord(rowSchema, row);
          LOG.debug("Row",  record);
          return record;
        }
      };
    }
  }

  private static class SecurityContextAdapter implements org.spf4j.security.SecurityContext {

    private final SecurityContext secCtx;

    private final AbacAuthorizer authorizer;

    SecurityContextAdapter(final SecurityContext secCtx, final AbacAuthorizer authorizer) {
      this.secCtx = secCtx;
      this.authorizer = authorizer;
    }

    @Override
    public Principal getUserPrincipal() {
      return secCtx.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(final String role) {
      return secCtx.isUserInRole(role);
    }

    @Override
    public boolean canAccess(final Properties resource, final Properties action, final Properties env) {
      return authorizer.canAccess(this.getUserPrincipal(), resource, action, env);
    }
  }

}
