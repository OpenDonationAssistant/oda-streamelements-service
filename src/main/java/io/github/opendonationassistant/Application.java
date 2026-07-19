package io.github.opendonationassistant;

import io.github.opendonationassistant.rabbit.AMQPConfiguration;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.streamelements.listener.EventsListener;
import io.github.opendonationassistant.streamelements.listener.WidgetChangesEventListener;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.runtime.Micronaut;
import io.micronaut.serde.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

@Factory
@OpenAPIDefinition(info = @Info(title = "oda-streamelements-service"))
public class Application {

  public static void main(String[] args) {
    Micronaut.build(args)
      .defaultEnvironments("standalone")
      .mainClass(Application.class)
      .banner(false)
      .start();
  }

  @Singleton
  public RemoteCacheManager remoteCacheManager(
    @Value("${infinispan.client.hotrod.server.host}") String host,
    @Value("${infinispan.client.hotrod.server.port}") int port,
    @Value(
      "${infinispan.client.hotrod.security.authentication.username}"
    ) String username,
    @Value(
      "${infinispan.client.hotrod.security.authentication.password}"
    ) String password
  ) {
    var conf = new ConfigurationBuilder()
      .addServer()
      .host(host)
      .port(port)
      .security()
      .authentication()
      .username(username)
      .password(password)
      .build();
    var manager = new RemoteCacheManager(conf);
    manager.start();
    return manager;
  }

  @Singleton
  public EmbeddedCacheManager embeddedCacheManager() {
    // prettier-ignore ON
    var configuration = new GlobalConfigurationBuilder()
      .nonClusteredDefault()
      .build();
    // prettier-ignore OFF
    var manager = new DefaultCacheManager(configuration);
    manager.start();
    return manager;
  }

  @Singleton
  public ChannelInitializer rabbitConfiguration() {
    var bindings = new ArrayList<Exchange>();
    bindings.add(WidgetChangesEventListener.BINDING);
    bindings.addAll(EventsListener.BINDING);
    return new AMQPConfiguration(bindings);
  }

  @Singleton
  @Named("commands")
  public RabbitClient commandsFacade(ChannelPool pool, ObjectMapper mapper) {
    return new RabbitClient(pool, mapper, "commands");
  }
}
