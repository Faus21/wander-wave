package com.dama.wanderwave.redis;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.ByteBuffer;

public class LongRedisSerializer implements RedisSerializer<Long> {

    @Override
    public byte[] serialize(Long aLong) throws SerializationException {
        if (aLong == null) {
            return new byte[0];
        }
        return ByteBuffer.allocate(Long.BYTES).putLong(aLong).array();
    }

    @Override
    public Long deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).put(bytes, 0, Long.BYTES);
        buffer.flip();
        return buffer.getLong();
    }
}
