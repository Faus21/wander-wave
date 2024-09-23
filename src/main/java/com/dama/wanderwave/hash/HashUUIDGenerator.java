package com.dama.wanderwave.hash;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component
public class HashUUIDGenerator implements IdentifierGenerator {

    private static final int ID_LENGTH = 12;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        String uuid = UUID.randomUUID().toString();
        return encodeString(uuid);
    }

    public String encodeString(String text) {
        byte[] uuidBytes = text.getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes)
                       .substring(0, ID_LENGTH);
    }
}