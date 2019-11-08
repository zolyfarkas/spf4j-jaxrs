package org.spf4j.jaxrs.aql;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Path;
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

/**
 * @author Zoltan Farkas
 */
@Path("avql/query")
@SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
public final class AvroQueryResourceImpl implements AvroQueryResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(AvroQueryResourceImpl.class));

  private final Planner planner;

  private final Map<String, Schema> schemas;

  private final FrameworkConfig config;

  @Inject
  public AvroQueryResourceImpl(final Iterable<AvroDataSetContract> resources) {
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
  }

  @Override
  public IterableArrayContent<GenericRecord> query(final String query) {
    return query(new StringReader(query));
  }

  @Override
  public IterableArrayContent<GenericRecord> query(final Reader query) {
    RelNode relNode = parsePlan(query);
    LOG.debug("exec plan: {}", new ReadablePlan(relNode));
    RelDataType rowType = relNode.getRowType();
    LOG.debug("Return row type: {}", rowType);
    Schema from = Types.from(rowType);
    LOG.debug("Return row schema: {}", from);
    Interpreter interpreter = new Interpreter(new EmbededDataContext(new JavaTypeFactoryImpl()), relNode);
    return new IterableInterpreter(from, interpreter);
  }

  @Override
  public CharSequence plan(final Reader query) {
    return RelOptUtil.toString(parsePlan(query));
  }

  @Override
  public CharSequence plan(final String query) {
    return RelOptUtil.toString(parsePlan(new StringReader(query)));
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
// Interpreter does not implement corelations
//    relNode = RelDecorrelator.decorrelateQuery(relNode, config.getSqlToRelConverterConfig()
//            .getRelBuilderFactory().create(relNode.getCluster(), null));
//    LOG.debug("Decorelated plan {}", new ReadablePlan(relNode));
    return PlannerUtils.pushDownPredicatesAndProjection(relNode);
  }

  @Override
  @AvroSchema("{ \"type\" : \"map\", \"values\" : { \"type\" : \"string\" , \"logicalType\" : \"avsc\"} } ")
  public Map<String, Schema> schemas() {
    return schemas;
  }

  @Override
  public Schema entitySchema(final String entityName) {
    return schemas.get(entityName);
  }

  @Override
  public String toString() {
    return "QueryResourceImpl{" + "schemas=" + schemas + '}';
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
          LOG.debug("RawRow {}",  row);
          GenericRecord record = IndexedRecords.fromRecord(rowSchema, row);
          LOG.debug("Row",  record);
          return record;
        }
      };
    }
  }

}
