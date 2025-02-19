package org.jmqtt.common.serializer.kryo;

import akka.serialization.SerializerWithStringManifest;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import lombok.extern.slf4j.Slf4j;
import org.jmqtt.common.event.Event;

import java.io.ByteArrayOutputStream;

@Slf4j
public class KryoSerializer extends SerializerWithStringManifest {

    public static final int IDENTIFIER = 123;

    private final Pool<Kryo> kryoPool;

    public KryoSerializer() {
        this.kryoPool = new Pool<Kryo>(true, false, 8) {
            @Override
            protected Kryo create() {
                Kryo kryo = new Kryo();
                kryo.setRegistrationRequired(false);
                kryo.register(Event.class);
                return kryo;
            }
        };
    }

    @Override
    public int identifier() {
        return IDENTIFIER;
    }

    @Override
    public String manifest(Object o) {
        return o.getClass().getName();
    }

    @Override
    public byte[] toBinary(Object o) {
        Kryo kryoInstance = kryoPool.obtain();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             Output output = new Output(outputStream)) {
            kryoInstance.writeClassAndObject(output, o);
            output.flush();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Kryo serialization failed", e);
            throw new RuntimeException("Kryo serialization failed", e);
        } finally {
            kryoPool.free(kryoInstance);
        }
    }

    @Override
    public Object fromBinary(byte[] bytes, String manifest) {
        Kryo kryoInstance = kryoPool.obtain();
        try (Input input = new Input(bytes)) {
            return kryoInstance.readClassAndObject(input);
        } catch (Exception e) {
            log.error("Kryo deserialization failed", e);
            throw new RuntimeException("Kryo deserialization failed", e);
        } finally {
            kryoPool.free(kryoInstance);
        }
    }
}
