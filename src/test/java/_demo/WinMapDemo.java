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
import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 * @author Philippe Charles
 */
public final class WinMapDemo {

    public static void main(String[] args) throws URISyntaxException {
        URI uri = new URI("https://www.nbb.be");

        System.out.println(new WinPowerShellProxySelector.GetSystemWebProxyCommand().apply(uri));
    }
}
