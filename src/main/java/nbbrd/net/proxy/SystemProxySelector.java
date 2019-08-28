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

import internal.net.SystemProxySpiLoader;
import internal.net.SystemProxySpiProc;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import net.jcip.annotations.ThreadSafe;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Philippe Charles
 */
@ThreadSafe
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
public final class SystemProxySelector extends ProxySelector {

    @NonNull
    public static SystemProxySelector ofServiceLoader() {
        return builder()
                .providers(new SystemProxySpiLoader().get())
                .systemProperties(System::getProperty)
                .fallback(ProxySelector.getDefault())
                .build();
    }

    @lombok.Singular
    private final List<Spi> providers;

    @lombok.NonNull
    private final UnaryOperator<String> systemProperties;

    @lombok.NonNull
    private final ProxySelector fallback;

    @Override
    public List<Proxy> select(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }
        if (hasStaticProxyProperties()) {
            return fallback.select(uri);
        }
        return providers.stream()
                .map(provider -> provider.getProxyOrNull(uri))
                .filter(Objects::nonNull)
                .findFirst()
                .map(Collections::singletonList)
                .orElseGet(() -> fallback.select(uri));
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        fallback.connectFailed(uri, sa, ioe);
    }

    private boolean hasStaticProxyProperties() {
        return hasProperty("https.proxyPort")
                || hasProperty("https.proxyHost")
                || hasProperty("http.proxyPort")
                || hasProperty("http.proxyHost")
                || hasProperty("http.nonProxyHosts");
    }

    private boolean hasProperty(String property) {
        return systemProperties.apply(property) != null;
    }

    @ThreadSafe
    @ServiceDefinition(
            quantifier = Quantifier.MULTIPLE,
            preprocessor = SystemProxySpiProc.class,
            loaderName = "internal.net.SystemProxySpiLoader")
    public interface Spi {

        @Nullable
        Proxy getProxyOrNull(@NonNull URI uri);
    }
}
