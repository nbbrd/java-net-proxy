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
package internal.net.proxy;

import java.net.Proxy;
import java.net.URI;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import nbbrd.net.proxy.SystemProxySelector;

/**
 *
 * @author Philippe Charles
 */
@lombok.extern.java.Log
@lombok.AllArgsConstructor
public final class FailsafeSystemProxySpi implements SystemProxySelector.Spi {

    public static SystemProxySelector.Spi wrap(SystemProxySelector.Spi delegate) {
        return new FailsafeSystemProxySpi(delegate, FailsafeSystemProxySpi::logUnexpectedError);
    }

    @lombok.NonNull
    private final SystemProxySelector.Spi delegate;

    @lombok.NonNull
    private final BiConsumer<? super String, ? super RuntimeException> onUnexpectedError;

    @Override
    public Proxy getProxyOrNull(URI uri) {
        Objects.requireNonNull(uri);
        try {
            return delegate.getProxyOrNull(uri);
        } catch (RuntimeException ex) {
            onUnexpectedError.accept("While calling 'getProxyOrNull' on '" + delegate + "'", ex);
            return null;
        }
    }

    static void logUnexpectedError(String msg, RuntimeException ex) {
        if (log.isLoggable(Level.WARNING)) {
            log.log(Level.WARNING, msg, ex);
        }
    }
}
