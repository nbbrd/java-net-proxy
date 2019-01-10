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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Philippe Charles
 */
public final class SinglePowerShell {

    private static final long MAIN_TIMEOUT_MILLIS = 1000 * 10;
    private static final int FALLBACK_MAX_INSTANCES = 3;

    private final ReentrantLock lock;
    private final Semaphore fallbackInstances;
    private FixedPowerShell ps;

    public SinglePowerShell() {
        this.lock = new ReentrantLock();
        this.fallbackInstances = new Semaphore(FALLBACK_MAX_INSTANCES);
        this.ps = null;
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public String executeCommands(String cmd) throws IOException, PowerShellExecutionException {
        try {
            if (lock.tryLock(MAIN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                return execOnMain(cmd);
            }
        } catch (InterruptedException ex) {
            kill(ex);
            throw new RuntimeException(ex);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return execOnFallback(cmd);
    }

    private String execOnMain(String cmd) throws IOException, PowerShellExecutionException {
        try {
            if (ps == null) {
                ps = FixedPowerShell.open();
            } else {
            }
            return ps.executeCommands(cmd);
        } catch (RuntimeException unexpected) {
            kill(unexpected);
            throw unexpected;
        }
    }

    private String execOnFallback(String cmd) throws IOException, PowerShellExecutionException {
        if (fallbackInstances.tryAcquire()) {
            try (FixedPowerShell temp = FixedPowerShell.open()) {
                return temp.executeCommands(cmd);
            } finally {
                fallbackInstances.release();
            }
        }
        throw new IOException("No more resource available");
    }

    private void kill(Exception unexpected) {
        if (ps != null) {
            try {
                ps.close();
            } catch (RuntimeException suppressed) {
                unexpected.addSuppressed(suppressed);
            }
        }
    }

    private void shutdown() {
        if (ps != null) {
            try {
                ps.close();
            } catch (RuntimeException ex) {
            }
        }
    }
}
