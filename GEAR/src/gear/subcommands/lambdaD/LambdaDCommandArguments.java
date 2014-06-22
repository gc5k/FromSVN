package gear.subcommands.lambdaD;

import java.util.ArrayList;

import gear.subcommands.CommandArguments;
import gear.util.BufferedReader;
import gear.util.FileUtil;
import gear.util.Logger;
import gear.util.NewIt;

public class LambdaDCommandArguments extends CommandArguments
{

	public void setMetaBatch(String batch)
	{
		FileUtil.exists(batch);
		md = NewIt.newArrayList();
		BufferedReader reader = BufferedReader.openTextFile(batch, "MetaBatch");

		String[] tokens = null;
		while((tokens = reader.readTokens())!=null)
		{
			md.add(tokens[0]);
		}
	}

	public void setMetaFile(String[] m)
	{
		md = NewIt.newArrayList();
		for (int i = 0; i < m.length; i++)
		{
			FileUtil.exists(m[i]);
			md.add(m[i]);
		}
	}

	public String[] getMetaFile() 
	{
		return md.toArray(new String[0]);
	}

	public void setGZ(boolean flag)
	{
		isGZ = flag;
	}
	
	public boolean isGZ()
	{
		return isGZ;
	}

	public void setCCbatch(String ccBatch)
	{
		FileUtil.exists(ccBatch);
		ArrayList<String> s = NewIt.newArrayList();
		BufferedReader reader = BufferedReader.openTextFile(ccBatch, "CC Batch");

		String[] tokens = null;
		while((tokens = reader.readTokensAtLeast(2))!=null)
		{
			s.add(tokens[0]);
			s.add(tokens[1]);
		}

		String[] cc = s.toArray(new String[0]);
		setCC(cc);
	}

	public void setCC(String[] cc)
	{
		ccSize = new double[cc.length];

		for (int i = 0; i < cc.length; i++)
		{
			ccSize[i] = Double.parseDouble(cc[i]);
			if(ccSize[i] <= 1)
			{
				Logger.printUserError("The sample size should be greater than 1.");
				System.exit(0);
			}
		}
		isQT = false;
		if ( (ccSize.length/2) != md.size())
		{
			Logger.printUserLog("The cc sample size parameters [" + ccSize.length + "] do not meet the length of the meta files [" + md.size()+"].");
			System.exit(0);
		}

	}
	
	public double[] getCCsize()
	{
		return ccSize;
	}

	public void setQTbatch(String qtBatch)
	{
		ArrayList<String> s = NewIt.newArrayList();
		BufferedReader reader = BufferedReader.openTextFile(qtBatch, "QT Batch");

		String[] tokens = null;
		while((tokens = reader.readTokensAtLeast(1))!=null)
		{
			s.add(tokens[0]);
		}

		String[] qt = s.toArray(new String[0]);
		setQT(qt);
	}
	
	public void setQT(String[] qt)
	{
		qtSize = new double[qt.length];
		
		for (int i = 0; i < qtSize.length; i++)
		{
			qtSize[i] = Double.parseDouble(qt[i]);
			if (qtSize[i] <= 1)
			{
				Logger.printUserError("The sample size should be greater than 1.");
				System.exit(0);
			}
		}
		isQT = true;
		
		if ( qtSize.length != md.size())
		{
			Logger.printUserLog("The qt sample size parameters [" + qtSize.length + "] do not meet the length of the meta files [" + md.size()+"].");
			System.exit(0);
		}
	}

	public double[] getQTsize()
	{
		return qtSize;
	}

	public boolean isQT() 
	{
		return isQT;
	}

	public String getKey(int i)
	{
		return field[i];
	}

	public void setKey(String[] k)
	{
		field[0] = k[0];
		field[1] = k[1];
		field[3] = k[2];
		field[4] = k[3];
		field[5] = k[4];
	}
	
	public void setVerbose()
	{
		isVerbose = true;
	}
	
	public void setVerboseGZ()
	{
		isVerbose = true;
		isVerboseGZ = true;
	}
	
	public boolean isVerbose()
	{
		return isVerbose;
	}
	
	public boolean isVerboseGZ()
	{
		return isVerboseGZ;
	}

	private ArrayList<String> md;
	private boolean isGZ = false;
	private boolean isQT = true;
	private boolean isVerbose = false;
	private boolean isVerboseGZ = false;
	private double[] qtSize;
	private double[] ccSize;
	private String[] field = {"snp", "chr", "bp", "beta", "or", "se", "p", "a1", "a2", };

	public static final int SNP = 0;
	public static final int CHR = 1;
	public static final int BP = 2;
	public static final int BETA = 3;
	public static final int OR = 4;
	public static final int SE = 5;
	public static final int P = 6;
	public static final int A1 = 7;
	public static final int A2 = 8;
	
}
