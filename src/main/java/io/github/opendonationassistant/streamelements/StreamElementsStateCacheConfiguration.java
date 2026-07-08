package io.github.opendonationassistant.streamelements;

import io.github.opendonationassistant.streamelements.repository.StreamElementsData;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import java.util.Map;
import org.infinispan.commons.api.CacheContainerAdmin.AdminFlag;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;

@Factory
public class StreamElementsStateCacheConfiguration {

  private static final String CACHE_NAME = "streamelements";

  @Singleton
  public Map<String, StreamElementsData> streamelementsCache(
    EmbeddedCacheManager cacheManager
  ) {
    var configuration = new ConfigurationBuilder().simpleCache(true).build();
    return cacheManager
      .administration()
      .withFlags(AdminFlag.VOLATILE)
      .getOrCreateCache(CACHE_NAME, configuration);
  }
}
