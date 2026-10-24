package com.jmqtt.core.cluster.seria;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import com.jmqtt.core.cluster.ClusterMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class KryoSerializer implements StreamSerializer<Object> {

    private final Pool<Kryo> kryoPool;

    public KryoSerializer() {
        this.kryoPool = new Pool<>(true, false, 50) {
            @Override
            protected Kryo create() {
                Kryo kryo = new Kryo();
                kryo.setRegistrationRequired(false);
                kryo.register(ClusterMessage.class);
                return kryo;
            }
        };
    }

    @Override
    public void write(ObjectDataOutput objectDataOutput, Object o) throws IOException {
        Kryo kryoInstance = kryoPool.obtain();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             Output output = new Output(outputStream)) {
            kryoInstance.writeClassAndObject(output, o);
            output.flush();
            objectDataOutput.writeByteArray(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("Kryo serialization failed", e);
            throw new RuntimeException("Kryo serialization failed", e);
        } finally {
            kryoPool.free(kryoInstance);
        }
    }

    @Override
    public Object read(ObjectDataInput objectDataInput) throws IOException {
        Kryo kryoInstance = kryoPool.obtain();
        byte[] bytes = objectDataInput.readByteArray();
        if (bytes == null) {
            bytes = new byte[0];
        }
        try (Input input = new Input(bytes)) {
            return kryoInstance.readClassAndObject(input);
        } catch (Exception e) {
            log.error("Kryo deserialization failed", e);
            throw new RuntimeException("Kryo deserialization failed", e);
        } finally {
            kryoPool.free(kryoInstance);
        }
    }

    @Override
    public int getTypeId() {
        return 999;
    }

}
