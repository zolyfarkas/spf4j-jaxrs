package org.spf4j.jaxrs.aql;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
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
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.glassfish.hk2.api.Immediate;
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
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;
import org.spf4j.http.Headers;
import org.spf4j.http.HttpWarning;
import org.spf4j.http.ServerTiming;
import org.spf4j.jaxrs.JaxRsSecurityContext;

/**
 * @author Zoltan Farkas
 */
@Path("avql/query")
@SuppressFBWarnings({ "EXS_EXCEPTION_SOFTENING_NO_CHECKED" })
@Singleton
@SuppressWarnings("checkstyle:DesignForExtension")
@Immediate
@PermitAll// internal entitlements
public class AvroQueryResourceImpl implements AvroQueryResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(AvroQueryResourceImpl.class));


  private final FrameworkConfig config;


  @Inject
  public AvroQueryResourceImpl(final Iterable<AvroDataSetContract> resources) {
    SchemaPlus schema = Frameworks.createRootSchema(true);
    for (AvroDataSetContract res : resources) {
      String name = res.getName();
      LOG.debug("Registered {} table to schema", name);
      schema.add(name, new AvroDataSetAsProjectableFilterableTable(res));
    }
    SqlParser.Config cfg = SqlParser.config()
            .withCaseSensitive(true)
            .withIdentifierMaxLength(255)
            .withLex(Lex.JAVA);
    config = Frameworks.newConfigBuilder()
            .parserConfig(cfg)
            .defaultSchema(schema)
            .build();
  }

  @PostConstruct
  public void warmup() {
    query("select 1", SaSecurityContext.INSTANCE);
  }

  public FrameworkConfig getConfig() {
    return config;
  }

  private Planner getPlanner() {
     return Frameworks.getPlanner(config);
  }


  @Override
  public Response query(final String query, final JaxRsSecurityContext secCtx) {
    return query(new StringReader(query), secCtx);
  }

  @Override
  public Response query(final Reader query, final JaxRsSecurityContext secCtx) {
    long startTimeNs = TimeSource.nanoTime();
    RelNode relNode = parsePlan(query);
    long parseElapsedNs = TimeSource.nanoTime() - startTimeNs;
    LOG.debug("exec plan: {} optained in {} ns", new ReadablePlan(relNode), parseElapsedNs);
    RelDataType rowType = relNode.getRowType();
    LOG.debug("Return row type: {}", rowType);
    Schema from = Types.from(rowType);
    LOG.debug("Return row schema: {}", from);
    EmbededDataContext dc = new EmbededDataContext(new JavaTypeFactoryImpl(), secCtx);
    Interpreter interpreter = new Interpreter(dc, relNode);
    Response.ResponseBuilder rb = Response.ok(new IterableInterpreter(from, interpreter));
    if (secCtx.isUserInRole(JaxRsSecurityContext.OPERATOR_ROLE)) {
      rb.header(Headers.SERVER_TIMING, new ServerTiming(
              new ServerTiming.ServerTimingMetric("sql_parse_time", parseElapsedNs / 1000000.0)));
    }
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
  public Response plan(final Reader query, final JaxRsSecurityContext secCtx) {
    return Response.ok(parsePlan(query)).build();
  }

  @Override
  public Response plan(final String query, final JaxRsSecurityContext secCtx) {
    return plan(new StringReader(query), secCtx);
  }

  public RelNode parsePlan(final Reader query) {
    Planner planner = getPlanner();
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
    //return rel.project();
    return PlannerUtils.pushDownPredicatesAndProjection(rel.project());
  }

  @Override
  @AvroSchema("{ \"type\" : \"map\", \"values\" : { \"type\" : \"string\" , \"logicalType\" : \"avsc\"} } ")
  public Map<String, Schema> schemas(final JaxRsSecurityContext secCtx) {
    SchemaPlus defaultSchema = config.getDefaultSchema();
    if (defaultSchema == null) {
      throw new IllegalStateException("Default schema should be always set in config: " + config);
    }
    Set<String> tableNames = defaultSchema.getTableNames();
    Map<String, Schema> result = Maps.newHashMapWithExpectedSize(tableNames.size());
    for (String tableName : tableNames) {
      Table table = defaultSchema.getTable(tableName);
      if (table == null) {
        continue;
      }
      result.put(tableName, getTableSchema(table));
    }
    return result;
  }

  @Override
  @Nullable
  public Schema entitySchema(final String entityName, final JaxRsSecurityContext secCtx) {
    SchemaPlus defaultSchema = config.getDefaultSchema();
    if (defaultSchema == null) {
      throw new IllegalStateException("Default schema should be always set in config: " + config);
    }
    Table table = defaultSchema.getTable(entityName);
    if (table == null) {
      throw new NotFoundException("Table not found: " + entityName);
    }
    return getTableSchema(table);
  }

  private static Schema getTableSchema(final Table table) {
    if (table instanceof AvroDataSetAsProjectableFilterableTable) {
      return ((AvroDataSetAsProjectableFilterableTable) table).getDataSet().getElementSchema();
    } else {
      return Types.from(table.getRowType(new JavaTypeFactoryImpl()));
    }
  }

  @Override
  public String toString() {
    return "AvroQueryResourceImpl{" + "config=" + config + '}';
  }

  @Override
  public Schema schema(final String query, final JaxRsSecurityContext secCtx) {
    return schema(new StringReader(query), secCtx);
  }

  @Override
  public Schema schema(final Reader query, final JaxRsSecurityContext secCtx) {
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


}
