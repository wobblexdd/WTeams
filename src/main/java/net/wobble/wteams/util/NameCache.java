package net.wobble.wteams.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NameCache {

    private final Map<UUID, String> cache = new HashMap<>();

    public void put(UUID uuid, String name) {
        cache.put(uuid, name);
    }

    public String get(UUID uuid) {
        return cache.get(uuid);
    }
}
