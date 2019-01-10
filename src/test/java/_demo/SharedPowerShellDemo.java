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

import internal.net.SharedPowerShell;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author Philippe Charles
 */
public class SharedPowerShellDemo {

    public static void main(String[] args) throws Exception {
        List<String> cmds = IntStream.range(0, 10).mapToObj(i -> "echo " + i).collect(Collectors.toList());

        System.out.println(cmds.stream().mapToLong(newTimer()).summaryStatistics());
        long start = System.currentTimeMillis();
        System.out.println(cmds.stream().parallel().mapToLong(newTimer()).summaryStatistics());
        System.out.println(System.currentTimeMillis() - start);
    }

    private static ToLongFunction<String> newTimer() {
        SharedPowerShell ps = new SharedPowerShell();
//        try {
//            ps.executeCommands("echo 'warming'");
//        } catch (IOException | PowerShellExecutionException ex) {
//        }
        return o -> {
            long start = System.currentTimeMillis();
            try {
                ps.executeCommands(o);
            } catch (Exception ex) {
                return -1;
            }
            return System.currentTimeMillis() - start;
        };
    }
}
