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

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import lombok.AccessLevel;
import nbbrd.net.SystemProxySelector;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Philippe Charles
 */
@ServiceProvider(service = SystemProxySelector.Spi.class)
@lombok.extern.java.Log
@lombok.AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class WinPowerShellProxySelector implements SystemProxySelector.Spi {

    private final TtlCache<String, Proxy> cache;
    private final Function<URI, Map<String, String>> powerShellCommand;
    private final UnaryOperator<String> sys;

    public WinPowerShellProxySelector() {
        this(TtlCache.of()
                .toBuilder()
                .minTtlInMillis(Duration.ofMillis(100).toMillis())
                .maxTtlInMillis(Duration.ofMinutes(1).toMillis())
                .ttlFactor(100)
                .onEvent((k, e) -> logCacheEvent(k, (TtlCache.Event) e))
                .build(),
                new GetSystemWebProxyCommand().andThen(WinPowerShellProxySelector::parseMap),
                System::getProperty
        );
    }

    @Override
    public Proxy getProxyOrNull(URI uri) {
        return isWindows(sys) ? cache.get(uri.getHost(), (o) -> getSystemWebProxy(uri)) : null;
    }

    private Proxy getSystemWebProxy(URI uri) {
        Map<String, String> result = powerShellCommand.apply(uri);
        return isDirect(result, uri) ? Proxy.NO_PROXY : parseProxy(result);
    }

    private static boolean isWindows(UnaryOperator<String> sys) {
        String result = sys.apply("os.name");
        return result != null && result.startsWith("Windows");
    }

    public static boolean isDirect(Map<String, String> webProxy, URI uri) {
        return (hasDefaultPort(webProxy, uri) || hasSamePort(webProxy, uri))
                && hasSameHost(webProxy, uri);
    }

    private static boolean hasDefaultPort(Map<String, String> webProxy, URI uri) {
        return "True".equals(webProxy.get("IsDefaultPort")) && uri.getPort() == -1;
    }

    private static boolean hasSamePort(Map<String, String> webProxy, URI uri) {
        return String.valueOf(uri.getPort()).equals(webProxy.get("Port"));
    }

    private static boolean hasSameHost(Map<String, String> webProxy, URI uri) {
        return uri.getHost().equals(webProxy.get("Host"));
    }

    private static Proxy parseProxy(Map<String, String> webProxy) {
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(webProxy.get("Host"), Integer.valueOf(webProxy.get("Port"))));
    }

    private static Map<String, String> parseMap(String input) {
        Map<String, String> result = new HashMap<>();
        for (String row : input.split(System.lineSeparator(), -1)) {
            String[] fields = row.split(" : ", -1);
            if (fields.length == 2) {
                result.put(fields[0].trim(), fields[1].trim());
            }
        }
        return result;
    }

    private static void logCacheEvent(Object key, TtlCache.Event e) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Proxy cache: {0} on key ''{1}''", new Object[]{e, key});
        }
    }

    public static final class GetSystemWebProxyCommand implements Function<URI, String> {

        private final SharedPowerShell ps = new SharedPowerShell();

        @Override
        public String apply(URI uri) {
            try {
                return ps.executeCommands("([System.Net.WebRequest]::GetSystemWebproxy()).GetProxy('" + uri + "')");
            } catch (IOException | PowerShellExecutionException ex) {
                if (log.isLoggable(Level.WARNING)) {
                    log.log(Level.WARNING, "Failed to execute powershell command", ex);
                }
                return "";
            }
        }
    }
}
