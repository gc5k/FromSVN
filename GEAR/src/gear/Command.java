package gear;

import gear.util.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public abstract class Command implements Comparable<Command>
{
	public abstract String getName();
	
	public boolean hasAlias(String alias)
	{
		return aliases.contains(alias);
	}
	
	protected void addAlias(String alias)
	{
		aliases.add(alias);
	}
	
	public Set<String> getAliases()
	{
		return Collections.unmodifiableSet(aliases);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		String s = null;
		
		if (obj instanceof String)
		{
			s = (String)obj;
		}
		else if (obj instanceof Command)
		{
			s = ((Command)obj).getName();
		}
		
		return s != null && (getName().equals(s) || hasAlias(s));
	}
	
	public int compareTo(Command otherCmd)
	{
		if (equals(otherCmd))
		{
			return 0;
		}
		return getName().compareTo(otherCmd.getName());
	}
	
	private Set<String> aliases = new TreeSet<String>();
	
	public abstract String getDescription();
	
	public String getLongDescription()
	{
		return getDescription();
	}
	
	public String getFullDescription()
	{
		return "";
	}
	
	protected abstract void prepareOptions(Options options);
	
	public Options getOptions()
	{
		Options options = new Options();
		prepareOptions(options);
		return options;
	}
	
	protected abstract CommandArguments parse(CommandLine cmdLine) throws CommandArgumentException;
	protected abstract CommandImpl createCommandImpl();
	
	protected void printOptionsInEffect(CommandLine cmdLine)
	{
		Logger.printUserLog("Options in effect: ");
		
		@SuppressWarnings("rawtypes")
		Iterator optIter = cmdLine.iterator();
		
		while (optIter.hasNext())
		{
			Option opt = (Option)optIter.next();
			String line = opt.hasLongOpt() ? "\t--" + opt.getLongOpt() : "\t-" + opt.getOpt();
			String[] argValues = opt.getValues();
			if (argValues != null)
			{
				for (String value : argValues)
				{
					line += " " + value; 
				}
			}
			Logger.printUserLog(line);
		}
		
		Logger.printUserLog("");
	}
	
	public void execute(String[] args)
	{
		Options options = new Options();
		prepareOptions(options);
		CommandLineParser cmdLineParser = new PosixParser();
		try
		{
			CommandLine cmdLine = cmdLineParser.parse(options, args);
			CommandArguments cmdArgs = parse(cmdLine);
			
			Logger.hasUserLogTag(false);
			Logger.printUserLog(AboutInfo.WELCOME_MESSAGE);
			Logger.hasUserLogTag(true);
			
			printOptionsInEffect(cmdLine);
			
			CommandImpl cmdImpl = createCommandImpl();
			cmdImpl.preExecute();
			cmdImpl.execute(cmdArgs);
			cmdImpl.postExecute();
		}
		catch (ParseException e)
		{
			Logger.printUserError(e.getMessage());
			System.exit(1);
		}
		catch (CommandArgumentException e)
		{
			Logger.printUserError(e.getMessage());
			System.exit(1);
		}
	}
}
