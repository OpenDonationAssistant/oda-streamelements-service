package io.github.opendonationassistant.streamelements;

import io.github.opendonationassistant.SerdeableEntryMarshaller;
import io.github.opendonationassistant.streamelements.repository.StreamElementsData;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.UTF8StringMarshaller;

@Factory
public class StreamElementsStateCacheConfiguration {

  private static final String CACHE_NAME = "streamelements";

  @Singleton
  public Configuration configuration(
    @Value("${infinispan.client.hotrod.server.host}") String host,
    @Value("${infinispan.client.hotrod.server.port}") int port,
    @Value("${infinispan.client.hotrod.security.username}") String username,
    @Value("${infinispan.client.hotrod.security.password}") String password
  ) {
    return new ConfigurationBuilder()
      .addServer()
      .host("localhost")
      .port(port)
      .security()
      .authentication()
      .username(username)
      .password(password)
      .build();
  }

  @Singleton
  public RemoteCacheManager cacheManager(Configuration configuration) {
    var manager = new RemoteCacheManager(configuration);
    manager.start();
    return manager;
  }

  @Singleton
  @Requires(env = "standalone")
  public Map<String, StreamElementsData> streamelementsCache(
    RemoteCacheManager cacheManager
  ) {
    return cacheManager
      .getCache(CACHE_NAME)
      .withDataFormat(
        DataFormat.builder()
          .keyMarshaller(new UTF8StringMarshaller())
          .valueMarshaller(
            new SerdeableEntryMarshaller(StreamElementsData.class)
          )
          .build()
      );
  }

  @Singleton
  @Requires(env = "allinone")
  public Map<String, StreamElementsData> streamelementsCache() {
    return new HashMap<>();
  }
}
