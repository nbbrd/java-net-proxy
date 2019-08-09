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
package nbbrd.net.proxy;

import _test.LogCollector;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class SystemProxySelectorTest {

    @Test
    public void test() throws URISyntaxException {
        URI uri = new URI("https://www.nbb.be");
        Queue<Exception> errorStack = new LinkedList<>();
        ProxySelector fallbackSelector = new FallbackProxySelector(fallbackProxy);
        UnaryOperator<String> noProperties = o -> null;
        BiConsumer<String, Exception> pushError = (m, e) -> errorStack.add(e);

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> SystemProxySelector
                        .builder()
                        .systemProperties(noProperties)
                        .fallback(fallbackSelector)
                        .onUnexpectedError(pushError)
                        .build()
                        .select(null)
                );

        assertThat(SystemProxySelector
                .builder()
                .systemProperties(noProperties)
                .fallback(fallbackSelector)
                .onUnexpectedError(pushError)
                .build()
                .select(uri)
        ).containsExactly(fallbackProxy);
        assertThat(errorStack).isEmpty();

        assertThat(SystemProxySelector
                .builder()
                .provider(o -> mainProxy)
                .systemProperties(noProperties)
                .fallback(fallbackSelector)
                .onUnexpectedError(pushError)
                .build()
                .select(uri)
        ).containsExactly(mainProxy);
        assertThat(errorStack).isEmpty();

        assertThat(SystemProxySelector
                .builder()
                .provider(o -> mainProxy)
                .systemProperties(o -> "http.proxyHost")
                .fallback(fallbackSelector)
                .onUnexpectedError(pushError)
                .build()
                .select(uri)
        ).containsExactly(fallbackProxy);
        assertThat(errorStack).isEmpty();

        assertThat(SystemProxySelector
                .builder()
                .provider(this::boom)
                .systemProperties(noProperties)
                .fallback(fallbackSelector)
                .onUnexpectedError(pushError)
                .build()
                .select(uri)
        ).containsExactly(fallbackProxy);
        assertThat(errorStack).hasSize(1);
    }

    @Test
    public void testLog() throws URISyntaxException {
        try (LogCollector logs = LogCollector.of(SystemProxySelector.class)) {
            SystemProxySelector.ofServiceLoader()
                    .toBuilder()
                    .clearProviders()
                    .provider(this::boom)
                    .build()
                    .select(new URI("https://www.nbb.be"));
            assertThat(logs)
                    .hasSize(1)
                    .element(0)
                    .extracting(o -> o.getThrown().getMessage())
                    .asString()
                    .contains("boom");
        }
    }

    private final Proxy mainProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("main", 1234));
    private final Proxy fallbackProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("fallback", 1234));

    private Proxy boom(URI o) {
        throw new RuntimeException("boom");
    }

    @lombok.AllArgsConstructor
    private static final class FallbackProxySelector extends ProxySelector {

        private final Proxy proxy;

        @Override
        public List<Proxy> select(URI uri) {
            return Collections.singletonList(proxy);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        }
    }
}
