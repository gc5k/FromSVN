package gear.subcommands.metahet;

import java.util.ArrayList;

import gear.subcommands.CommandArguments;
import gear.util.BufferedReader;
import gear.util.FileUtil;
import gear.util.Logger;
import gear.util.NewIt;

public class MetaHetArguments extends CommandArguments
{

	public void setMe(String me)
	{
		Me = Double.parseDouble(me);
	}

	public double getMe()
	{
		return Me;
	}

	public void setXMFile(String xmFile)
	{
		FileUtil.exists(xmFile);
		this.xmFile = xmFile;
	}

	public String getXMFile()
	{
		return xmFile;
	}

	public void setQMFile(String qmFile)
	{
		FileUtil.exists(qmFile);
		this.qmFile = qmFile;
		isQT = true;
	}

	public String getQMFile()
	{
		return qmFile;
	}

	public void setCCMFile(String ccmFile)
	{
		FileUtil.exists(ccmFile);
		this.ccmFile = ccmFile;
		isQT = false;
	}

	public String getCCMFile()
	{
		return ccmFile;
	}

	public boolean isQT()
	{
		return isQT;
	}
	
	public void setCCbatch(String ccBatch)
	{
		FileUtil.exists(ccBatch);
		this.ccBatchFile = ccBatch;
		ArrayList<String> s = NewIt.newArrayList();
		BufferedReader reader = BufferedReader.openTextFile(ccBatch, "CC Batch File");

		String[] tokens = null;
		while((tokens = reader.readTokensAtLeast(2))!=null)
		{
			s.add(tokens[0]);
			s.add(tokens[1]);
		}

		String[] cc = s.toArray(new String[0]);
		setCC(cc);
	}

	private void setCC(String[] cc)
	{
		ccSize = new double[cc.length/2][2];

		for (int i = 0; i < cc.length/2; i++)
		{
			ccSize[i][0] = Double.parseDouble(cc[i*2]);
			ccSize[i][1] = Double.parseDouble(cc[i*2+1]);
			if(ccSize[i][0] <= 1 && ccSize[i][1] <=1)
			{
				Logger.printUserError("The sample size should be greater than 1.");
				System.exit(0);
			}
		}

		isQT = false;
	}

	public String getCCBatchFile()
	{
		return ccBatchFile;
	}

	public double[][] getCCsize()
	{
		return ccSize;
	}

	private double Me;
	private String xmFile;
	private String qmFile;
	private boolean isQT = true;
	private String ccmFile;

	private String ccBatchFile;
	private double[][] ccSize;
}
