package gear.subcommands.metawatchdog.encrypt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.apache.commons.math.random.RandomDataImpl;

import gear.subcommands.CommandArguments;
import gear.subcommands.CommandImpl;
import gear.subcommands.profile.ProfileCommandArguments;
import gear.subcommands.profile.ProfileCommandImpl;
import gear.util.BinaryInputFile;
import gear.util.BufferedReader;
import gear.util.Logger;

public class EnigmaCommandImpl extends CommandImpl
{
	@Override
	public void execute(CommandArguments cmdArgs)
	{
		EnigmaCommandArguments enigmaArgs = (EnigmaCommandArguments)cmdArgs;
		
		readEncodeFile(enigmaArgs.getEncodeFile());
		readRefAlleles(enigmaArgs.getRefFile());
		
		RandomDataImpl rnd = new RandomDataImpl();
		rnd.reSeed(seed);
		double[][] beta = new double[ref.size()][numCols];
		for(int i = 0; i < beta.length; i++)
		{
			for(int j = 0; j < beta[i].length; j++)
			{
				beta[i][j] = rnd.nextGaussian(0, 1);				
			}
		}
		
		String scoreFileName = enigmaArgs.getOutRoot() + ".enigma";
		PrintWriter writer = null;
		try 
		{
			writer = new PrintWriter(new BufferedWriter(new FileWriter(scoreFileName)));
		}
		catch (IOException e)
		{
			Logger.handleException(e, "An I/O exception occurred when writing '" + scoreFileName + "'.");
		}
		
		DecimalFormat df=new DecimalFormat("##.0000");

		for(int snpIdx = 0; snpIdx < ref.size(); snpIdx++)
		{
			writer.print(ref.get(snpIdx));
			for (int colIdx = 0; colIdx < numCols; colIdx++)
			{
				writer.print("\t" + df.format(beta[snpIdx][colIdx]));
			}
			writer.println();
		}
		writer.close();
		Logger.printUserLog(ref.size() + " SNPs were used for generating Enigma scores.");
		
		// Execute the profile part
		ProfileCommandArguments profArgs = enigmaArgs.getProfileCommandArguments();
		profArgs.setScoreFile(scoreFileName);
		profArgs.setHasScoreHeader(false);
		ProfileCommandImpl profImpl = new ProfileCommandImpl();
		profImpl.execute(profArgs);
		Logger.printUserLog("");
		Logger.printUserLog("Generated profile scores have been saved in '" + enigmaArgs.getOutRoot() + ".profile'.");
	}

	private void readEncodeFile(String fileName)
	{
		BinaryInputFile file = new BinaryInputFile(fileName, "encode");
		double K = file.readDouble();
		seed = file.readLong();
		double alpha = file.readDouble();
		long tests = file.readInt();

		double beta = file.readDouble();
		double b = file.readDouble();
		double q = file.readDouble();
		int method = file.readInt();

		numCols = (int) Math.ceil(K);
		if (numCols > K)
		{
			Logger.printUserLog("Columns: " + K + " (round up to " + numCols + ")");			
		}
		else
		{
			Logger.printUserLog("Columns: " + (K));
		}
		file.close();
	}

	private void readRefAlleles(String mapFile)
	{
		BufferedReader reader = BufferedReader.openTextFile(mapFile, "map");
		String[] tokens;
		while ((tokens = reader.readTokensAtLeast(2)) != null)
		{
			ref.add(tokens[0] + "\t" + tokens[1]);
		}
	}
	
	private ArrayList<String> ref = new ArrayList<String>();
	private long seed;
	private int numCols;
}
