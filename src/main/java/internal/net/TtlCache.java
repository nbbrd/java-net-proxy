/*
 * Copyright 2018 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package internal.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Philippe Charles
 * @param <K>
 * @param <V>
 */
@lombok.extern.java.Log
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
public final class TtlCache<K, V> {

    @NonNull
    public static <K, V> TtlCache of() {
        return builder()
                .minTtlInMillis(10)
                .maxTtlInMillis(1000 * 60)
                .ttlFactor(10)
                .storage(new ConcurrentHashMap<>())
                .clock(System::currentTimeMillis)
                .onEvent(TtlCache::logEvent)
                .build();
    }

    private final long minTtlInMillis;
    private final long maxTtlInMillis;
    private final long ttlFactor;
    private final ConcurrentMap<K, Entry<V>> storage;
    private final LongSupplier clock;
    private final BiConsumer<? super K, Event> onEvent;
    private final int evictThreshold = 1000;

    @Nullable
    public V get(@NonNull K key, @NonNull Function<K, V> loader) {
        long now = clock.getAsLong();
        Entry<V> entry = storage.get(key);
        if (entry != null) {
            if (!entry.hasExpired(now)) {
                onEvent.accept(key, Event.HIT);
                return entry.getNullableValue();
            } else {
                return load(key, loader, now, false);
            }
        }
        return load(key, loader, now, true);
    }

    private V load(K key, Function<K, V> loader, long before, boolean miss) {
        V result = loader.apply(key);
        long after = clock.getAsLong();
        long ttl = (after - before) * ttlFactor;
        if (ttl >= minTtlInMillis) {
            onEvent.accept(key, miss ? Event.MISS_SLOW : Event.EXP_SLOW);
            evictExpiredEntries(after);
            storage.put(key, new Entry<>(after + Math.min(maxTtlInMillis, ttl), result));
        } else {
            onEvent.accept(key, miss ? Event.MISS_FAST : Event.EXP_FAST);
            if (!miss) {
                storage.remove(key);
            }
        }
        return result;
    }

    private void evictExpiredEntries(long currentTimeInMillis) {
        if (storage.size() > evictThreshold) {
            storage.entrySet()
                    .stream()
                    .filter(o -> o.getValue().hasExpired(currentTimeInMillis))
                    .forEach(storage::remove);
        }
    }

    @lombok.AllArgsConstructor
    public static final class Entry<V> {

        private final long expiration;
        @lombok.Getter
        private final V nullableValue;

        public boolean hasExpired(long currentTimeInMillis) {
            return expiration <= currentTimeInMillis;
        }
    }

    public enum Event {
        HIT, EXP_SLOW, EXP_FAST, MISS_SLOW, MISS_FAST
    }

    private static void logEvent(Object key, Event e) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "TtlCache {0} on key ''{1}''", new Object[]{e, key});
        }
    }
}
