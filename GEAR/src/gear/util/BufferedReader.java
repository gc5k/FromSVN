package gear.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

public class BufferedReader
{
	public static BufferedReader openTextFile(String fileName, String fileType)
	{
		java.io.BufferedReader reader = null;
		try
		{
			reader = new java.io.BufferedReader(new java.io.FileReader(fileName));
		}
		catch (Exception e)
		{
			Logger.handleException(e, "Cannot open the " + fileType + " file '" + fileName + "'.");
		}
		return new BufferedReader(reader, fileName, fileType);
	}
	
	public static BufferedReader openZipFile(String fileName, String fileType)
	{
		java.io.FileInputStream fileInStrm = null;
		try
		{
			fileInStrm = new java.io.FileInputStream(fileName);
		}
		catch (FileNotFoundException e)
		{
			Logger.handleException(e, "File '" + fileName + "' does not exist.");
		}
		
		GZIPInputStream gzip = null;
		try
		{
			gzip = new GZIPInputStream(fileInStrm);
		}
		catch (IOException e)
		{
			Logger.handleException(e, "Cannot open the archive '" + fileName + "'.");
		}
		
		java.io.InputStreamReader inStrmReader = new java.io.InputStreamReader(gzip);
		java.io.BufferedReader bufferedReader = new java.io.BufferedReader(inStrmReader);
		return new BufferedReader(bufferedReader, fileName, fileType);
	}
	
	private BufferedReader(java.io.BufferedReader reader, String fileName, String fileType)
	{
		innerReader = reader; 
		this.fileName = fileName;
		this.fileType = fileType;
		curLineNum = 1;
	}

	public void close()
	{
		try
		{
			innerReader.close();
		}
		catch (java.io.IOException e)
		{
			String msg = "An I/O exception occurred when closing the " + fileType + " file '" + fileName + "'.";
			Logger.printUserError(msg);
			Logger.getDevLogger().log(Level.WARNING, msg, e);
		}
	}

	public String readLine()
	{
		String line = null;
		
		try
		{
			line = innerReader.readLine();
		}
		catch (java.io.IOException e)
		{
			String msg = "";
			msg += "An I/O exception occurred when reading to line " + curLineNum;
			msg += " of the " + fileType + " file '" + fileName + "'.";
			Logger.handleException(e, msg);
		}
		
		if (line != null)
		{
			++curLineNum;
		}
		
		return line;
	}

	public String readNonEmptyLine()
	{
		String line = null;
		while ((line = readLine()) != null)
		{
			line = line.trim();
			if (!line.isEmpty())
			{
				break;
			}
		}
		return line;
	}

	/**
	 * @return split tokens on the next non-empty line
	 */
	public String[] readTokens()
	{
		String[] tokens = null;

		while (true)
		{
			String line = readLine();

			if (line == null)
			{
				tokens = null;
				break;
			}

			tokens = line.trim().split("\\s+");

			if (tokens.length > 0)
			{
				break;
			}
		}

		return tokens;
	}

	public String[] readTokens(int expectedNumCols)
	{
		String[] tokens = readTokens();
		if (tokens != null && tokens.length != expectedNumCols)
		{
			String msg = "";
			msg += "The format of the " + fileType + " file '" + fileName + "' is incorrect: ";
			msg += "A " + fileType + " file should consists of " + expectedNumCols + " columns, ";
			msg += "but line " + curLineNum + " contains " + tokens.length + " column(s).";
			Logger.printUserError(msg);
			System.exit(1);
		}
		return tokens;
	}

	public String getFileName()
	{
		return fileName;
	}

	public int getCurLineNum()
	{
		return curLineNum;
	}
	
	public void reportFormatError(String msg)
	{
		String begin = "";
		begin += "Line " + getCurLineNum() + " of the " + fileType + " file ";
		begin += "'" + getFileName() + "' contains an error: ";
		Logger.printUserError(begin + msg);
		System.exit(1);
	}

	private java.io.BufferedReader innerReader;

	private int curLineNum;

	private String fileName;

	private String fileType;
}
