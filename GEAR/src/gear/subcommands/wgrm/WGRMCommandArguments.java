package gear.subcommands.wgrm;

import gear.subcommands.CommandArguments;
import gear.util.FileUtil;
import gear.util.Logger;

public class WGRMCommandArguments extends CommandArguments
{
	private boolean isGZ = true;
	private boolean isVar = false;
	private boolean isDom = false;
	private double maf = 1e-5;
	private boolean isVanRaden = false;
	private boolean isWeight = false;
	private String wFile = null;

	public void setGZ()
	{
		isGZ = true;
	}

	public void setTxt()
	{
		isGZ = false;
	}

	public boolean isGZ()
	{
		return isGZ;
	}

	public void setAdjVar()
	{
		isVar = true;
	}

	public boolean isAdjVar()
	{
		return isVar;
	}

	public void setDom() 
	{
		isDom = true;
	}

	public boolean isDom()
	{
		return isDom;
	}

	public void setMAF(String m)
	{
		maf = Double.parseDouble(m);
	}

	public double getMAF()
	{
		return maf;
	}

	public void setVanRaden()
	{
		isVanRaden = true;
		isWeight = false;
	}

	public boolean isWeight()
	{
		return isWeight;
	}

	public boolean isVanRaden()
	{
		return isVanRaden;
	}

	public void setWeightFile(String wF)
	{
		FileUtil.exists(wF);
		wFile = wF;
	}
	
	public String getWeightFile()
	{
		return wFile;
	}
}
