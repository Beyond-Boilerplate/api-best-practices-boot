package com.github.sardul3.io.api_best_practices_boot.logAndMonitor.logging.controllers;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;

/**
 * Controller for dynamically updating the log level of the application at runtime.
 * <p>
 * This controller allows users to change the log level of the root logger via HTTP requests.
 * It's useful in production environments where you want to increase the log verbosity for debugging
 * without restarting the application.
 * </p>
 */
@RestController
public class LogLevelController {

    /**
     * Updates the log level of the root logger.
     * <p>
     * This method allows users to dynamically update the log level by passing a valid log level
     * (e.g., DEBUG, INFO, WARN, ERROR) as a query parameter. If the provided log level is invalid,
     * an error message is returned.
     * </p>
     *
     * @param level the log level to set (e.g., DEBUG, INFO, WARN, ERROR)
     * @return a string message indicating the success or failure of the log level change
     */
    @GetMapping("/management/log")
    public String setLogLevel(@RequestParam String level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("ROOT");

        // Use a helper method to get a valid Level or null if invalid
        Level newLevel = getLogLevel(level.toUpperCase());

        if (newLevel == null) {
            // Return an error message if the log level is invalid
            return "Invalid log level: " + level + ". Valid levels are { ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF }";
        }

        // Set the root logger level if valid
        rootLogger.setLevel(newLevel);

        return "Log level successfully changed to " + newLevel.toString();
    }

    /**
     * Validates the log level provided by the user.
     * <em>
     * The default Level class defaults log-level to DEBUG if a unknown value is passed
     * thus, this extra redundant code is required to make sure invalid log levels when passed
     * in the QueryParameter does not result in unexpected DEBUG log flood
     * </em>
     * <p>
     * This helper method checks if the provided log level string is a valid logback Level.
     * If valid, the corresponding {@link Level} object is returned. Otherwise, it returns null.
     * </p>
     *
     * @param level the log level string to validate
     * @return a {@link Level} object if valid, or null if invalid
     */
    private Level getLogLevel(String level) {
        return switch (level) {
            case "ALL" -> Level.ALL;
            case "TRACE" -> Level.TRACE;
            case "DEBUG" -> Level.DEBUG;
            case "INFO" -> Level.INFO;
            case "WARN" -> Level.WARN;
            case "ERROR" -> Level.ERROR;
            case "OFF" -> Level.OFF;
            default -> null;  // Return null if the log level is invalid
        };
    }
}


