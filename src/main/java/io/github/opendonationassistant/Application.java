package io.github.opendonationassistant;

import io.github.opendonationassistant.rabbit.AMQPConfiguration;
import io.github.opendonationassistant.rabbit.Exchange;
import io.github.opendonationassistant.rabbit.RabbitClient;
import io.github.opendonationassistant.streamelements.listener.WidgetChangesEventListener;
import io.micronaut.cache.infinispan.InfinispanAsyncCache;
import io.micronaut.cache.infinispan.InfinispanCacheManager;
import io.micronaut.context.annotation.Factory;
import io.micronaut.rabbitmq.connect.ChannelInitializer;
import io.micronaut.rabbitmq.connect.ChannelPool;
import io.micronaut.runtime.Micronaut;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;

@Factory
public class Application {

  public static void main(String[] args) {
    Micronaut.build(args).mainClass(Application.class).banner(false).start();
  }

  @Singleton
  public ChannelInitializer rabbitConfiguration() {
    var bindings = new ArrayList<Exchange>();
    bindings.add(WidgetChangesEventListener.BINDING);
    return new AMQPConfiguration(bindings);
  }

  @Singleton
  @Named("commands")
  public RabbitClient commandsFacade(ChannelPool pool, ObjectMapper mapper) {
    return new RabbitClient(pool, mapper, "commands");
  }

  @Singleton
  public InfinispanAsyncCache cache(InfinispanCacheManager manager) {
    return (InfinispanAsyncCache) manager.getCache("streamelements").async();
  }
}
