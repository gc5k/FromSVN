package gear.subcommands.metawatchdog.decrypt;

import gear.subcommands.CommandArguments;

public class MetaWatchdog2CommandArguments extends CommandArguments
{
	public String getDataset1()
	{
		return dataset1;
	}
	public void setDataset1(String dataset1)
	{
		this.dataset1 = dataset1;
	}
	public String getDataset2()
	{
		return dataset2;
	}
	public void setDataset2(String dataset2)
	{
		this.dataset2 = dataset2;
	}
	public void setChisq(double chi)
	{
		chisqFlag = true;
		chisqCutoff = chi;
	}
	public boolean getChisqFlag()
	{
		return chisqFlag;
	}
	public double getChisqCutoff()
	{
		return chisqCutoff;
	}
	public double getRegB()
	{
		return regB;
	}
	public void setRegB(double cutoff)
	{
		regBFlag = true;
		this.regB = cutoff;
	}
	public boolean getRegFlag()
	{
		return regBFlag;
	}

	public void setEncodeFile(String ecodeFile)
	{
		this.encodeFile = ecodeFile;
		encodeFlag = true;
	}
	public String getEncodeFile()
	{
		return encodeFile;
	}
	public boolean getEncodeFlag()
	{
		return encodeFlag;
	}

	public void setVerbose()
	{
		verbose = true;
	}
	public boolean getVerbose()
	{
		return verbose;
	}
	private String encodeFile;
	private boolean encodeFlag = false;
	private String dataset1;
	private String dataset2;

	private boolean regBFlag = false;
	private double regB;

	private boolean chisqFlag = false;
	private double chisqCutoff;
	
	private boolean verbose = false;
	
}
