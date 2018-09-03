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

import _test.LogCollector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class TtlCacheTest {

    @Test
    @SuppressWarnings("null")
    public void test() {
        ConcurrentMap<String, TtlCache.Entry<Integer>> map = new ConcurrentHashMap<>();
        AtomicLong clock = new AtomicLong(0);
        AtomicInteger counter = new AtomicInteger();
        AtomicReference<TtlCache.Event> event = new AtomicReference<>();
        AtomicLong duration = new AtomicLong(10);

        TtlCache<String, Integer> cache = TtlCache
                .<String, Integer>builder()
                .minTtlInMillis(10)
                .maxTtlInMillis(1000)
                .ttlFactor(1)
                .cache(map)
                .clock(clock::get)
                .onEvent((k, e) -> event.set(e))
                .build();

        Function<String, Integer> loader = o -> {
            clock.addAndGet(duration.get());
            return counter.incrementAndGet();
        };

        assertThatNullPointerException().isThrownBy(() -> cache.get(null, loader));
        assertThatNullPointerException().isThrownBy(() -> cache.get("a", null));

        duration.set(5);
        assertThat(cache.get("a", loader)).isEqualTo(1);
        assertThat(event).hasValue(TtlCache.Event.MISS_FAST);
        assertThat(map).doesNotContainKey("a");
        assertThat(clock).hasValue(5);

        duration.set(10);
        assertThat(cache.get("a", loader)).isEqualTo(2);
        assertThat(event).hasValue(TtlCache.Event.MISS_SLOW);
        assertThat(map).containsKey("a");
        assertThat(clock).hasValue(15);

        duration.set(10);
        assertThat(cache.get("a", loader)).isEqualTo(2);
        assertThat(event).hasValue(TtlCache.Event.HIT);
        assertThat(map).containsKey("a");
        assertThat(clock).hasValue(15);

        clock.addAndGet(1000);
        duration.set(10);
        assertThat(cache.get("a", loader)).isEqualTo(3);
        assertThat(event).hasValue(TtlCache.Event.EXP_SLOW);
        assertThat(map).containsKey("a");
        assertThat(clock).hasValue(1025);

        clock.addAndGet(1000);
        duration.set(5);
        assertThat(cache.get("a", loader)).isEqualTo(4);
        assertThat(event).hasValue(TtlCache.Event.EXP_FAST);
        assertThat(map).doesNotContainKey("a");
        assertThat(clock).hasValue(2030);
    }

    @Test
    public void testLog() {
        try (LogCollector logs = LogCollector.of(TtlCache.class)) {
            TtlCache.of().get("hello", o -> "world");
            assertThat(logs)
                    .hasSize(1)
                    .element(0)
                    .extracting(LogCollector::getFormattedMessage)
                    .asString()
                    .contains("hello", TtlCache.Event.MISS_FAST.name());
        }
    }
}
