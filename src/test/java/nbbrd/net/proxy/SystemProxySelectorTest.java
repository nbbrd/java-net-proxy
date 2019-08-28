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

import _test.ProxyMap;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
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

        ProxySelector fallbackSelector = ProxyMap.builder().proxy(uri, fallbackProxy).build();
        UnaryOperator<String> noProperties = o -> null;

        assertThatIllegalArgumentException()
                .isThrownBy(
                        () -> SystemProxySelector
                                .builder()
                                .systemProperties(noProperties)
                                .fallback(fallbackSelector)
                                .build()
                                .select(null)
                );

        assertThat(SystemProxySelector
                .builder()
                .systemProperties(noProperties)
                .fallback(fallbackSelector)
                .build()
                .select(uri)
        ).containsExactly(fallbackProxy);

        assertThat(SystemProxySelector
                .builder()
                .provider(o -> mainProxy)
                .systemProperties(noProperties)
                .fallback(fallbackSelector)
                .build()
                .select(uri)
        ).containsExactly(mainProxy);

        assertThat(SystemProxySelector
                .builder()
                .provider(o -> mainProxy)
                .systemProperties(o -> "http.proxyHost")
                .fallback(fallbackSelector)
                .build()
                .select(uri)
        ).containsExactly(fallbackProxy);
    }

    private final Proxy mainProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("main", 1234));
    private final Proxy fallbackProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("fallback", 1234));
}
