/*
 * Copyright 2019 National Bank of Belgium
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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiConsumer;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class FailsafeSystemProxySpiTest {

    @Test
    public void testFactories() throws URISyntaxException {
        assertThatNullPointerException()
                .isThrownBy(() -> new FailsafeSystemProxySpi(null, this::doNothing));

        assertThatNullPointerException()
                .isThrownBy(() -> new FailsafeSystemProxySpi(this::pass, null));

        assertThatNullPointerException()
                .isThrownBy(() -> FailsafeSystemProxySpi.wrap(null));
    }

    @Test
    public void testGetProxyOrNull() throws URISyntaxException {
        URI uri = new URI("https://www.nbb.be");

        Queue<Exception> errorStack = new LinkedList<>();
        BiConsumer<String, Exception> pushError = (m, e) -> errorStack.add(e);

        assertThatNullPointerException()
                .isThrownBy(() -> new FailsafeSystemProxySpi(this::pass, pushError).getProxyOrNull(null));
        assertThat(errorStack).isEmpty();

        assertThat(new FailsafeSystemProxySpi(this::pass, pushError).getProxyOrNull(uri)).isEqualTo(mainProxy);
        assertThat(errorStack).isEmpty();

        assertThat(new FailsafeSystemProxySpi(this::fail, pushError).getProxyOrNull(uri)).isNull();
        assertThat(errorStack).hasSize(1);
    }

    @Test
    public void testLogUnexpectedError() throws URISyntaxException {
        URI uri = new URI("https://www.nbb.be");

        try (LogCollector logs = LogCollector.of(FailsafeSystemProxySpi.class)) {
            new FailsafeSystemProxySpi(this::fail, FailsafeSystemProxySpi::logUnexpectedError).getProxyOrNull(uri);
            assertThat(logs)
                    .hasSize(1)
                    .element(0)
                    .extracting(record -> record.getThrown().getMessage())
                    .asString()
                    .contains("boom");
        }
    }

    private final Proxy mainProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("main", 1234));

    private Proxy pass(URI o) {
        return mainProxy;
    }

    private Proxy fail(URI o) {
        throw new RuntimeException("boom");
    }

    private void doNothing(String msg, Exception ex) {
    }
}
