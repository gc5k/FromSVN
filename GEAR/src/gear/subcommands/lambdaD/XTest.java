package gear.subcommands.lambdaD;

import gear.util.Logger;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public class XTest
{

	public XTest(double[] DesStat, double n1, double n2)
	{
		this.DesStat = DesStat;
		this.n1 = n1;
		this.n2 = n2;
		Me = DesStat.length;
		CalZ();
	}

	public XTest(double[] DesStat, double cs1, double ctrl1, double cs2, double ctrl2)
	{
		this.DesStat = DesStat;
		this.cs1 = cs1;
		this.ctrl1 = ctrl1;
		this.cs2 = cs2;
		this.ctrl2 = ctrl2;
		this.n1 = cs1 + ctrl1;
		this.n2 = cs2 + ctrl2;
		Me = DesStat.length;
		CalZ();
		CalCC();
	}

	private void CalZ()
	{
		for(int i = 0; i < DesStat.length; i++)
		{
			XVec.addValue(DesStat[i]);
		}

		double XS = XVec.getSum();

		try
		{
			// two-tail tests
			Z = (XVec.getSum() - Me)/Math.sqrt(2 * Me);
			pZ = 2 * (1 - nDis.cumulativeProbability(Math.abs(Z)));
		}
		catch (MathException e)
		{
			Logger.handleException(e, "error in getting pvalue.");
		}

		//rho
		rho = (1 - XS/Me) * (Math.sqrt(n1/n2) + Math.sqrt(n2/n1))/2;
		//sigma_rho
		sigma_rho = (n1+n2)/Math.sqrt(2 * Me * n1 * n2);
		//n12
		n12 = (1 - XS/Me) * (n1 + n2)/2;
		//sigma_n12
		sigma_n12 = (n1 + n2)/Math.sqrt(2*Me);
	}

	private void CalCC()
	{
		n12cl = n12 / Math.sqrt(cs1/ctrl1 * cs2/ctrl2);
		sigma_n12cl = sigma_n12 / Math.sqrt(cs1/ctrl1 * cs2/ctrl2);

		n12cs = n12 * Math.sqrt(cs1/ctrl1 * cs2/ctrl2);
		sigma_n12cs = sigma_n12 * Math.sqrt(cs1/ctrl1 * cs2/ctrl2);
	}

	protected double getZ()
	{
		return Z;
	}

	protected double getpZ()
	{
		return pZ;
	}

	protected double getN12()
	{
		return n12;
	}

	protected double getRho()
	{
		return rho;
	}

	protected double getN12cs()
	{
		return n12cs;
	}
	
	protected double getN12cl()
	{
		return n12cl;
	}

	protected void PrintQT()
	{
		double z_l = -1.96;
		double z_h = 1.96;
		printZ(z_l, z_h);
	}

	protected void PrintCC()
	{
		double z_l = -1.96;
		double z_h = 1.96;
		printZ(z_l, z_h);
		Logger.printUserLog("Overlapping if only controls: " + n12cl + ", 95% confidence interval is (" + z_l * sigma_n12cl  + ", " + z_h * sigma_n12cl + ")");
		Logger.printUserLog("Overlapping if only cases: " + n12cs + ", 95% confidence interval is (" + z_l * sigma_n12cs  + ", " + z_h * sigma_n12cs + ")");
	}

	private void printZ(double z_l, double z_h)
	{
		Logger.printUserLog("Effective number of markers is: " + Me);
		Logger.printUserLog("Z score: " + Z);
		Logger.printUserLog("p-value for z score (two-tails): " + pZ);
		Logger.printUserLog("Correlation: " + rho + ", 95% confidence interval is (" + z_l * sigma_rho + ", " + z_h * sigma_rho + ")");
		Logger.printUserLog("Overlapping samples: " + n12 + ", 95% confidence interval is (" + z_l * sigma_n12  + ", " + z_h * sigma_n12 + ")");
	}

	private NormalDistributionImpl nDis = new NormalDistributionImpl();

	private double[] DesStat;
	private DescriptiveStatistics XVec = new DescriptiveStatistics();

	private double Z = 0;
	private double Me = 0;
	private double pZ = 0;
	
	private double rho = 0;
	private double sigma_rho = 0;

	private double n12 = 0;
	private double sigma_n12 = 0;

	private double n1 = 0;
	private double n2 = 0;

	private double cs1 = 0;
	private double ctrl1 = 0;
	private double cs2 = 0;
	private double ctrl2 = 0;

	private double n12cs = 0;
	private double sigma_n12cs = 0;
	private double n12cl = 0;
	private double sigma_n12cl = 0;
}
