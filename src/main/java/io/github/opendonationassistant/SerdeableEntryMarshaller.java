package io.github.opendonationassistant;

import io.micronaut.serde.ObjectMapper;
import java.io.IOException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.AbstractMarshaller;

public class SerdeableEntryMarshaller extends AbstractMarshaller {

  private final Class<?> target;

  public SerdeableEntryMarshaller(Class<?> target) {
    this.target = target;
  }

  @Override
  public Object objectFromByteBuffer(byte[] buf, int offset, int length)
    throws IOException, ClassNotFoundException {
    Object object = ObjectMapper.getDefault().readValue(buf, target);
    if (object == null) {
      throw new IOException("Can't unmarshal buffer");
    }
    return object;
  }

  @Override
  public boolean isMarshallable(Object o) throws Exception {
    return true;
  }

  @Override
  public MediaType mediaType() {
    return MediaType.TEXT_PLAIN;
  }

  @Override
  protected ByteBuffer objectToBuffer(Object o, int estimatedSize)
    throws IOException, InterruptedException {
    return ByteBufferImpl.create(
      ObjectMapper.getDefault().writeValueAsBytes(o)
    );
  }
}
