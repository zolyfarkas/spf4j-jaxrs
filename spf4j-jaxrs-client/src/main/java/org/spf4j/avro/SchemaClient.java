package org.spf4j.avro;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipError;
import javax.annotation.Nullable;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.failsafe.HedgePolicy;
import org.spf4j.failsafe.RetryDecision;
import org.spf4j.http.DeadlineProtocol;
import org.spf4j.io.Streams;
import org.spf4j.jaxrs.Utils;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.log.ExecContextLogger;

/**
 * @author Zoltan Farkas
 */
public final class SchemaClient implements SchemaResolver {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(SchemaClient.class));

  private final String schemaArtifactClassifier;

  private final String schemaArtifactExtension;

  private final int failureCacheMillis;

  private final int snapshotCacheMillis;

  private final URI remoteMavenRepo;

  private final Path localMavenRepo;

  private final LoadingCache<String, Schema> memoryCache;

  private final Spf4JClient client;

  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public SchemaClient(final URI remoteMavenRepo) {
    this(remoteMavenRepo, Paths.get(org.spf4j.base.Runtime.USER_HOME, ".m2", "repository"),
            "", "jar");
  }

  public SchemaClient(final URI remoteMavenRepo, final Path localMavenRepo,
          final String schemaArtifactClassifier, final String schemaArtifactExtension) {
    this(remoteMavenRepo, localMavenRepo, schemaArtifactClassifier, schemaArtifactExtension, createDefaultClient());
  }

  public SchemaClient(final URI remoteMavenRepo, final Path localMavenRepo,
          final String schemaArtifactClassifier, final String schemaArtifactExtension,
          final Spf4JClient client) {
    this.schemaArtifactClassifier = schemaArtifactClassifier;
    this.schemaArtifactExtension = schemaArtifactExtension;
    this.failureCacheMillis = 5000;
    this.snapshotCacheMillis = 300000;
    try {
      this.remoteMavenRepo = remoteMavenRepo.getPath().endsWith("/") ? remoteMavenRepo
              : new URI(remoteMavenRepo.getScheme(),
                      remoteMavenRepo.getUserInfo(),
                      remoteMavenRepo.getHost(),
                      remoteMavenRepo.getPort(), remoteMavenRepo.getRawPath() + '/',
                      remoteMavenRepo.getRawQuery(), remoteMavenRepo.getRawFragment());
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Invalid repo url: " + remoteMavenRepo, ex);
    }
    this.localMavenRepo = localMavenRepo;
    this.client = client;
    this.memoryCache = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<String, Schema>() {
      @Override
      public Schema load(final String key) throws Exception {
        return loadSchema(key);
      }
    });
  }

  public static Spf4JClient createDefaultClient() {
    ClientBuilder builder = ClientBuilder
            .newBuilder().connectTimeout(2, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS);
    return Spf4JClient.create(builder
            .register(new ExecutionContextClientFilter(DeadlineProtocol.NONE,
                    Boolean.parseBoolean(System.getProperty("spf4j.http.client.hideAuthorizationWhenLogging", "true"))))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build())
            .withRetryPolicy(Utils.createHttpRetryPolicy((WebApplicationException ex, Callable<? extends Object> c) -> {
              Response response = ex.getResponse();
              if (404 == response.getStatus()) {
                return RetryDecision.retryDefault(c);
              }
              return null;
            }, 10))
            .withHedgePolicy(HedgePolicy.NONE);
  }


  @Override
  @SuppressFBWarnings("LEST_LOST_EXCEPTION_STACK_TRACE") // I am fine withit for now.
  public Schema resolveSchema(final String id) {
    try {
      return memoryCache.get(id);
    } catch (ExecutionException | UncheckedExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new UncheckedExecutionException(cause);
      }
    }
  }

  @SuppressFBWarnings("PCAIL_POSSIBLE_CONSTANT_ALLOCATION_IN_LOOP")
  Schema loadSchema(final String id) throws IOException {
    Schema fromClassPath = getFromClassPath(id);
    if (fromClassPath != null) {
      return fromClassPath;
    }
    SchemaRef sr = new SchemaRef(id);
    Path schemaPackage = getSchemaPackage(sr);
    URI zipUri = URI.create("jar:" + schemaPackage.toUri().toURL());
    FileSystem zipFs;
    synchronized (zipUri.toString().intern()) { // newFileSystem fails if already one there...
      try {
        zipFs = FileSystems.newFileSystem(zipUri, Collections.emptyMap());
      } catch (FileSystemAlreadyExistsException ex) {
        zipFs = FileSystems.getFileSystem(zipUri);
      } catch (ZipError ze) {
        LOG.debug("zip error with {}", zipUri, ze);
        Files.delete(schemaPackage);
        return loadSchema(id);
      }
    }
    for (Path root : zipFs.getRootDirectories()) {
      Path index = root.resolve("schema_index.properties");
      if (Files.exists(index)) {
        Properties prop = new Properties();
        try (BufferedReader indexReader = Files.newBufferedReader(index)) {
          prop.load(indexReader);
        }
        String schemaName = prop.getProperty(sr.getRef());
        if (schemaName == null) {
          throw new IOException("unable to resolve schema: " + id + " missing from index " + index);
        }
        Path schemaPath = root.resolve(schemaName.replace('.', '/') + ".avsc");
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(schemaPath))) {
          Schema.Parser parser = new Schema.Parser(new AvroNamesRefResolver(this));
          parser.setValidate(false);
          return parser.parse(bis);
        }
      }
    }
    throw new IOException("unable to resolve schema: " + id);
  }

  @Nullable
  @VisibleForTesting
  Schema getFromClassPath(final String ref) {
    String name = Lazy.SCHEMA_CP_INDEX.get(ref);
    if (name == null) {
      return null;
    }
    try {
      Class<?> genClass = Class.forName(name);
      return (Schema) genClass.getField("SCHEMA$").get(null);
    } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalAccessException ex) {
      LOG.warn("Exception while loading and extracting avro schema from class " + name, ex);
      return null;
    }
  }

  private static class Lazy {

    private static final Map<String, String> SCHEMA_CP_INDEX;

    static {
      try {
        SCHEMA_CP_INDEX = loadSchemaIndexes();
      } catch (IOException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
  }

  @SuppressFBWarnings("STT_TOSTRING_MAP_KEYING") // on purpose, lookups do not concatente anything.
  private static Map<String, String> loadSchemaIndexes() throws IOException {
    Map<String, String> idToSchemafile = new HashMap<>();
    Enumeration<URL> resEnum = ClassLoader.getSystemResources("schema_index.properties");
    List<URL> urls = new ArrayList<>();
    while (resEnum.hasMoreElements()) {
      URL elem = resEnum.nextElement();
      urls.add(elem);
      Properties props = new Properties();
      try (Reader reader = new BufferedReader(new InputStreamReader(elem.openStream(), StandardCharsets.UTF_8))) {
        props.load(reader);
      }
      //_pkg=org.spf4j.avro:core-schema:0.15-SNAPSHOT
      String pkgInfo = props.getProperty("_pkg");
      if (pkgInfo == null) {
        continue;
      }
      for (Map.Entry<Object, Object> entry : props.entrySet()) {
        String key = (String) entry.getKey();
        if (!"_pkg".equals(key)) {
          String val = entry.getValue().toString();
          String mvnId = pkgInfo + ':' + key;
          String ex = idToSchemafile.put(mvnId, val);
          if (ex != null && !val.equals(ex)) {
            throw new IllegalStateException("Invalid indexes in your classpath (" + urls + ")  for "
                    + mvnId + ", " + ex + " != " + val);
          }
        }
      }
    }
    return idToSchemafile;
  }

  /**
   * Retrieves a schema package locally if needed.
   *
   * sample repo url: https://dl.bintray.com/zolyfarkas/core/org/spf4j/avro/core-schema/0.1/core-schema-0.1.jar
   *
   * @param ref
   * @return
   * @throws IOException
   */
  Path getSchemaPackage(final SchemaRef ref) throws IOException {
    String groupPath = ref.getGroupId().replace('.', '/') + '/';
    String artifactId = ref.getArtifactId();
    String version = ref.getVersion();
    Path folder = localMavenRepo.resolve(groupPath)
            .resolve(artifactId).resolve(version);
    String fileName = artifactId + '-' + version
            + (schemaArtifactClassifier.isEmpty() ? "" : ('-' + schemaArtifactClassifier))
            + '.' + schemaArtifactExtension;
    Path result = folder.resolve(fileName);
    if (Files.isReadable(result) && Files.size(result) > 0) {
      if (!version.contains("SNAPSHOT")) {
        return result;
      }
      FileTime lastModifiedTime = Files.getLastModifiedTime(result);
      if (lastModifiedTime.toMillis() + snapshotCacheMillis > System.currentTimeMillis()) {
        return result;
      }
    } else {
      Path ftsFile = folder.resolve(fileName + ".fts");
      if (Files.exists(ftsFile)) {
        Instant lastModifiedTime = Instant.parse(Files.readAllLines(ftsFile, StandardCharsets.UTF_8).get(0));
        long time = lastModifiedTime.toEpochMilli() + failureCacheMillis - System.currentTimeMillis();
        if (time > 0) {
          throw new NotFoundException("Artifact " + ref + " not available, re-attempt in " + time + " ms");
        }
      }
    }
    URI mUri = remoteMavenRepo.resolve(groupPath).resolve(artifactId + '/').resolve(version + '/')
            .resolve(fileName);
    Files.createDirectories(folder);
    Path tmpDownload = Files.createTempFile(folder, ".schArtf", ".tmp");
    try {
      try (InputStream is = client.target(mUri).request(MediaType.WILDCARD_TYPE).get(InputStream.class);
              BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(tmpDownload))) {
        long nrb = Streams.copy(is, bos);
        LOG.debug("Downloaded {} package to {}", nrb, tmpDownload);
      }
      Files.move(tmpDownload, result, StandardCopyOption.ATOMIC_MOVE);
      LOG.debug("Renamed package to {}", result);
    } catch (IOException | RuntimeException ex) {
      LOG.debug("Cannot download {}", mUri, ex);
      Files.write(tmpDownload, Collections.singletonList(Instant.now().toString()), StandardCharsets.UTF_8);
      Files.move(tmpDownload, folder.resolve(fileName + ".fts"), StandardCopyOption.ATOMIC_MOVE);
      throw ex;
    }
    return result;
  }

  @Override
  @Nullable
  public String getId(final Schema schema) {
    return schema.getProp("mvnId");
  }

  @Override
  public String toString() {
    return "SchemaClient{" + "schemaArtifactClassifier=" + schemaArtifactClassifier
            + ", schemaArtifactExtension=" + schemaArtifactExtension + ", failureCacheMillis="
            + failureCacheMillis + ", snapshotCacheMillis=" + snapshotCacheMillis + ", remoteMavenRepo="
            + remoteMavenRepo + ", localMavenRepo=" + localMavenRepo + ", memoryCache=" + memoryCache
            + ", client=" + client + '}';
  }

}
