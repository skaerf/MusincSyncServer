package xyz.skaerf.MusincServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ErrorHandler {

    static boolean logDir;

    /**
     * Creates a fatal error with a message to be printed to console + stack trace to be saved as a file in the 'logs' directory.
     * @param errorMessage the error message to be printed to console, detailing the error
     * @param stackTrace the stack trace that came with the error that was thrown
     */
    public static void fatal(String errorMessage, StackTraceElement[] stackTrace) {
        long currTime = System.currentTimeMillis();
        errorMessage = "[FATAL] "+errorMessage;
        errorMessage = "\n\n\n"+errorMessage+".\n\n\n";
        System.out.println(errorMessage);
        if (logDir) {
            File logFile = new File("logs"+File.separator+"log_fatal_"+currTime+".txt");
            try {
                if (logFile.createNewFile()) {
                    FileWriter writer = new FileWriter(logFile.getAbsolutePath());
                    writer.write("At "+currTime+" (time in millis),\n\n");
                    for (StackTraceElement i : stackTrace) {
                        writer.write(i.toString()+"\n");
                    }
                    writer.close();
                    System.out.println("Error output has been saved to "+logFile.getName());
                }
            }
            catch (IOException e) {
                System.out.println("Could not create log file. Turning off log file creation for the remaining run duration, will try again upon restart.");
                logDir = false;
            }
        }
        Main.endProcess(0);
    }

    /**
     * Creates a warn error for the given message and stack trace
     * @param errorMessage the message to be printed to the console, detailing the error
     * @param stackTrace the stack trace that came with the error that was thrown
     */
    public static void warn(String errorMessage, StackTraceElement[] stackTrace) {
        errorMessage = "[WARN] "+errorMessage;
        errorMessage = "\n\n\n"+errorMessage+".\n\n\n";
        System.out.println(errorMessage);
        long currTime = System.currentTimeMillis();
        if (logDir) {
            File logFile = new File("logs"+File.separator+"log_warn_"+currTime+".txt");
            try {
                if (logFile.createNewFile()) {
                    FileWriter writer = new FileWriter(logFile.getAbsolutePath());
                    writer.write("At "+currTime+" (time in millis),\n\n");
                    for (StackTraceElement i : stackTrace) {
                        writer.write(i.toString()+"\n");
                    }
                    writer.close();
                    System.out.println("Error output has been saved to "+logFile.getName());
                }
            }
            catch (IOException e) {
                System.out.println("Could not create log file. Turning off log file creation for the remaining run duration, will try again upon restart.");
                logDir = false;
            }
        }
    }

    /**
     * Creates a warn error for the given message. Used in case of no stack trace thrown -
     * no actual error to save the stack trace of into a file.
     * Used in a case of warning that has not been created from a method error.
     * @param errorMessage the message to be printed to console, detailing the error
     */
    public static void warn(String errorMessage) {
        errorMessage = "[WARN] "+errorMessage;
        errorMessage = errorMessage+".";
        System.out.println(errorMessage);
    }

}
