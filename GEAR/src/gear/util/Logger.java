package gear.util;

import gear.ConstValues;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.logging.Level;

public class Logger
{
	public enum LogLevel { INFO, WARNING, ERROR }
	
	private static PrintWriter userLogWriter;
	private static java.util.logging.Logger devLogger;

	static
	{
		setLogFiles("gear");
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{
			public void run()
			{
				if (userLogWriter != null)
				{
					userLogWriter.close();
					userLogWriter = null;
				}
			}
		}));

		devLogger = java.util.logging.Logger.getLogger("GEAR");
		devLogger.setUseParentHandlers(false);
		devLogger.setLevel(java.util.logging.Level.ALL);
	}

	public static void setLogFiles(String namePrefix)
	{
		logFileNamePrefix = namePrefix;
		
		String userLogFileName = namePrefix + ".log";
		try
		{
			userLogWriter = new PrintWriter(userLogFileName);
		}
		catch (FileNotFoundException e)
		{
			handleException(e, "Unable to create the log file '" + userLogFileName + "'");
		}
	}
	
	public static void printUserLog(LogLevel level, String msg)
	{
		String tag = "";
		PrintStream printStrm = System.out;
		
		switch (level)
		{
		case INFO:
			tag = "[INFO] ";
			break;
			
		case WARNING:
			tag = "[WARNING] ";
			printStrm = System.err;
			break;
			
		case ERROR:
			tag = "[ERROR] ";
			printStrm = System.err;
			break;
		}
		
		if (!hasUserLogTagPrefix)
		{
			tag = "";
		}
		
		String finalMsg = tag + msg;
		printStrm.println(finalMsg);
		
		if (userLogWriter != null)
		{
			userLogWriter.println(finalMsg);
		}
	}

	public static void printUserLog(String msg)
	{
		printUserLog(LogLevel.INFO, msg);
	}
	
	public static void printUserWarning(String msg)
	{
		printUserLog(LogLevel.WARNING, msg);
	}

	public static void printUserError(String msg)
	{
		printUserLog(LogLevel.ERROR, msg);
	}
	
	public static void hasUserLogTag(boolean hasUserLogTagPrefix)
	{
		Logger.hasUserLogTagPrefix = hasUserLogTagPrefix;
	}

	public static java.util.logging.Logger getDevLogger()
	{
		initDevLogger();
		return devLogger;
	}

	public static void handleException(Exception e, String msg)
	{
		initDevLogger();
		printUserError(msg);
		printUserError("Exception Message: " + e.getMessage());
		devLogger.log(Level.SEVERE, msg, e);
		System.exit(1);
	}
	
	public static void initDevLogger()
	{
		if (!isDevLoggerInited)
		{
			try
			{
				java.util.logging.FileHandler devLogHandler =
						new java.util.logging.FileHandler(getDevLogFileName());
				devLogHandler.setFormatter(new java.util.logging.SimpleFormatter());
				devLogger.addHandler(devLogHandler);
			}
			catch (IOException e)
			{
				handleException(e, "Unable to create the log file '" + getDevLogFileName() + "'");
			}
			isDevLoggerInited = true;	
		}
	}
	
	public static void warnInternalBug(String msg)
	{
		getDevLogger().warning(printInternalBug(msg, /*fatal*/false));
	}
	
	public static void fatalInternalBug(String msg)
	{
		getDevLogger().severe(printInternalBug(msg, /*fatal*/true));
		System.exit(1);
	}
	
	private static String printInternalBug(String msg, boolean fatal)
	{
		String userMsg = "";
		userMsg += "There's " + (fatal ? "a fatal" : "an") + " internal bug in the program: ";
		userMsg += msg;
		userMsg += " Please contact us (GEAR's authors) and send '" + getDevLogFileName();
		userMsg += "' to us to help improve the software.";
		
		if (fatal)
		{
			printUserError(userMsg);
		}
		else
		{
			printUserWarning(userMsg);
		}
		
		String devMsg = msg;
		devMsg += ConstValues.LINE_SEPARATOR;
		devMsg += "Stack trace:" + ConstValues.LINE_SEPARATOR;
		for (StackTraceElement stElem : Thread.currentThread().getStackTrace())
		{
			devMsg += stElem + ConstValues.LINE_SEPARATOR;
		}
		return devMsg;
	}
	
	public static String getDevLogFileName()
	{
		return logFileNamePrefix + "_dev.log";
	}

	private static String logFileNamePrefix = "gear";
	private static boolean isDevLoggerInited = false;
	private static boolean hasUserLogTagPrefix = true;
}
