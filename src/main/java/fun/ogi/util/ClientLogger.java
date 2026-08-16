package fun.ogi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientLogger {
   private static final Logger LOGGER = LoggerFactory.getLogger("CheapClient");

   private ClientLogger() {
   }

   public static void error(String message, Throwable throwable) {
      LOGGER.error(message, throwable);
   }

   public static void warn(String message, Throwable throwable) {
      LOGGER.warn(message, throwable);
   }

   public static void warn(String message) {
      LOGGER.warn(message);
   }

   public static void info(String message) {
      LOGGER.info(message);
   }
}

