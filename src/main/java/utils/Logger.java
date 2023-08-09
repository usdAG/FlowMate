package utils;

import burp.api.montoya.logging.Logging;

public class Logger {
    private static Logger instance = null;
    private Logging burpInternalLogging;

    public static void initialize(Logging burpLogging){
        if(Logger.instance == null){
            instance = new Logger(burpLogging);
        }        
    }

    public static Logger getInstance(){
        return Logger.instance;
    }

    private Logger(Logging burpLogging){
        this.burpInternalLogging = burpLogging;
    }

    public void logToOutput(String msg){
        this.burpInternalLogging.logToOutput(msg);
    }

    public void logToError(String msg){
        this.burpInternalLogging.logToError(msg);
    }
}
