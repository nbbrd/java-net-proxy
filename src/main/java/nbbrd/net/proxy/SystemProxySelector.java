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

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.jcip.annotations.ThreadSafe;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Philippe Charles
 */
@ThreadSafe
@lombok.extern.java.Log
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
public final class SystemProxySelector extends ProxySelector {

    @NonNull
    public static SystemProxySelector ofServiceLoader() {
        List<Spi> providers = StreamSupport
                .stream(ServiceLoader.load(Spi.class).spliterator(), false)
                .collect(Collectors.toList());
        return builder()
                .providers(providers)
                .systemProperties(System::getProperty)
                .fallback(ProxySelector.getDefault())
                .onUnexpectedError(SystemProxySelector::logUnexpectedError)
                .build();
    }

    @lombok.Singular
    private final List<Spi> providers;

    @lombok.NonNull
    private final UnaryOperator<String> systemProperties;

    @lombok.NonNull
    private final ProxySelector fallback;

    @lombok.NonNull
    private final BiConsumer<? super String, ? super RuntimeException> onUnexpectedError;

    @Override
    public List<Proxy> select(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }
        if (hasStaticProxyProperties()) {
            return fallback.select(uri);
        }
        return providers.stream()
                .map(o -> tryGetProxyOrNull(o, uri))
                .filter(Objects::nonNull)
                .findFirst()
                .map(Collections::singletonList)
                .orElseGet(() -> fallback.select(uri));
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        fallback.connectFailed(uri, sa, ioe);
    }

    private Proxy tryGetProxyOrNull(Spi o, URI uri) {
        try {
            return o.getProxyOrNull(uri);
        } catch (RuntimeException ex) {
            onUnexpectedError.accept("While calling 'getProxyOrNull' on '" + o + "'", ex);
            return null;
        }
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

    private static void logUnexpectedError(String msg, RuntimeException ex) {
        if (log.isLoggable(Level.WARNING)) {
            log.log(Level.WARNING, msg, ex);
        }
    }

    @ThreadSafe
    public interface Spi {

        @Nullable
        Proxy getProxyOrNull(@NonNull URI uri);
    }
}
