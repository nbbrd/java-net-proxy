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
package _demo;

import internal.net.proxy.x.WinPowerShellProxySelector;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

/**
 *
 * @author Philippe Charles
 */
public class WinParallelDemo {

    public static void main(String[] args) throws URISyntaxException {
        URI[] x = {
            new URI("http://ec.europa.eu"),
            new URI("http://dataservices.imf.org"),
            new URI("http://data.un.org"),
            new URI("https://sdmxcentral.imf.org"),
            new URI("http://andmebaas.stat.ee"),
            new URI("http://www.ilo.org"),
            new URI("http://bdm.insee.fr"),
            new URI("https://sdw-wsrest.ecb.europa.eu"),
            new URI("http://sdmx.snieg.mx"),
            new URI("https://stats.oecd.org"),
            new URI("http://stat.data.abs.gov.au"),
            new URI("https://stat.nbb.be"),
            new URI("http://wits.worldbank.org"),
            new URI("http://data.uis.unesco.org"),
            new URI("https://api.worldbank.org"),
            new URI("http://sdmx.istat.it")
        };

        WinPowerShellProxySelector proxySelector = new WinPowerShellProxySelector();
        proxySelector.getProxyOrNull(new URI("http://localhost"));
        Stream.of(x).parallel().forEach(o -> {
            long start = System.currentTimeMillis();
            Proxy p = proxySelector.getProxyOrNull(o);
            System.out.println(o + " >>> " + p + " >>> " + (System.currentTimeMillis() - start));
        });
    }
}
