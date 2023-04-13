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
package _test;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public final class LogCollector implements List<LogRecord>, AutoCloseable {

    public static LogCollector of(Class<?> target) {
        return of(Logger.getLogger(target.getName()));
    }

    public static LogCollector of(Logger logger) {
        LogCollector result = new LogCollector(logger, logger.getUseParentHandlers(), logger.getLevel());
        result.init();
        return result;
    }

    private final Logger logger;
    private final boolean savedUseParentHandlers;
    private final Level savedLevel;

    @lombok.experimental.Delegate
    private final List<LogRecord> logs = new CopyOnWriteArrayList<>();
    private final Handler handler = new ListHandler();

    private void init() {
        handler.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    @Override
    public void close() {
        logger.removeHandler(handler);
        logger.setLevel(savedLevel);
        logger.setUseParentHandlers(savedUseParentHandlers);
        logs.clear();
    }

    private final class ListHandler extends Handler {

        @Override
        public void publish(LogRecord lr) {
            logs.add(lr);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    public static String getFormattedMessage(LogRecord o) {
        return new MessageFormat(o.getMessage(), Locale.ROOT).format(o.getParameters());
    }
}
