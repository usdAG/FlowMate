package utils;

import burp.api.montoya.logging.Logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;

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
        FileSystemUtil.checkLogFile();
        String timeStamp = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss:S] ").format(new java.util.Date());
        try {
            Files.writeString(FileSystemUtil.LOG_PATH.toPath(), timeStamp + msg + "\n", StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
