package gear.subcommands.lambdaD;

import gear.util.Logger;
import gear.util.Sample;
import gear.util.stat.PrecisePvalue;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.regression.SimpleRegression;

public class XTest
{

	public XTest(double[] DesStat, double n1, double n2, double trim)
	{
//		this.DesStat = DesStat;
		checkStat(DesStat, trim);

		this.n1 = n1;
		this.n2 = n2;

		ChisqSample = Sample.sampleChisq(DesStat.length, 1);
		CalZ();
	}

	public XTest(double[] DesStat, double cs1, double ctrl1, double cs2, double ctrl2, double trim)
	{
//		this.DesStat = DesStat;
		checkStat(DesStat, trim);

		this.cs1 = cs1;
		this.ctrl1 = ctrl1;
		this.cs2 = cs2;
		this.ctrl2 = ctrl2;
		this.n1 = cs1 + ctrl1;
		this.n2 = cs2 + ctrl2;

		ChisqSample = Sample.sampleChisq(DesStat.length, 1);
		CalZ();
		CalCC();
	}

	private void checkStat(double[] ds, double trim)
	{
		int start = (int) (ds.length * trim);
		int end = (int) (ds.length * (1- trim));

		DesStat = new double[end-start];
		int cnt = 0;
		for(int i = 0;  i < DesStat.length; i++)
		{
			if(ds[start+i] == Double.POSITIVE_INFINITY)
			{
				cnt++;
				DesStat[i] = ds[start+i-1] + 0.05;
			}
			else
			{
				DesStat[i] = ds[start+i];
			}
		}
		Me = this.DesStat.length;
		for(int i = 0; i < this.DesStat.length; i++)
		{
			XVec.addValue(DesStat[i]);
		}

		String pt = cnt >1 ? "point has":"points have";
		if(cnt > 0)
		{
			Logger.printUserLog(cnt + " " + pt + " been corrected in X statistics.");
		}
	}
	
	private void CalZ()
	{
		try
		{
			// two-tail tests
			Z = (XVec.getSum() - Me)/Math.sqrt(2 * Me);
			if (Math.abs(Z) < 8)
			{
				pZ = 2 * (1 - nDis.cumulativeProbability(Math.abs(Z)));
			}
			else
			{
				pZ = PrecisePvalue.TwoTailZcumulativeProbability(Math.abs(Z));
			}
		}
		catch (MathException e)
		{
			Logger.handleException(e, "error in getting pvalue.");
		}

		//rho
		rho = -Z * (n1 + n2)/Math.sqrt(2 * Me * n1 * n2);
		//sigma_rho
		sigma_rho = (n1 + n2)/Math.sqrt(2 * Me * n1 * n2);
		//n12
		n12 = -Z * (n1 + n2)/Math.sqrt(2 * Me);
		//sigma_n12
		sigma_n12 = (n1 + n2)/Math.sqrt(2 * Me);

//		RegChi();
		if(XVec.getN() % 2 == 0)
		{
			ChiMedian = ( XVec.getElement((int) (XVec.getN()/2-1)) + XVec.getElement((int) XVec.getN()/2) )/2;
		}
		else
		{
			ChiMedian = XVec.getElement((int) (XVec.getN()+1)/2);
		}
		
		lambdaM = ChiMedian/ChiMedianConstant;
		lambdaMSD();
	}

	private void CalCC()
	{
		n12cl = n12 / Math.sqrt(cs1/ctrl1 * cs2/ctrl2);
		sigma_n12cl = sigma_n12 / Math.sqrt(cs1/ctrl1 * cs2/ctrl2);

		n12cs = n12 * Math.sqrt(cs1/ctrl1 * cs2/ctrl2);
		sigma_n12cs = sigma_n12 * Math.sqrt(cs1/ctrl1 * cs2/ctrl2);
	}

	public double getX()
	{
		return XVec.getSum();
	}

	public double getZ()
	{
		return Z;
	}

	public double getpZ()
	{
		return pZ;
	}

	public double getN12()
	{
		return n12;
	}

	public double getRho()
	{
		return rho;
	}

	public double getN12cs()
	{
		return n12cs;
	}
	
	public double getN12cl()
	{
		return n12cl;
	}

	public double getLambda()
	{
		return lambdaM;
	}

	public void PrintQT()
	{
		double z_l = -1.96;
		double z_h = 1.96;
		printZ(z_l, z_h);
	}

	public void PrintCC()
	{
		double z_l = -1.96;
		double z_h = 1.96;
		printZ(z_l, z_h);
		Logger.printUserLog("Overlapping if only controls: " + n12cl + ", 95% confidence interval is (" + z_l * sigma_n12cl  + ", " + z_h * sigma_n12cl + ")");
		Logger.printUserLog("Overlapping if only cases: " + n12cs + ", 95% confidence interval is (" + z_l * sigma_n12cs  + ", " + z_h * sigma_n12cs + ")");
	}

	public void printZ(double z_l, double z_h)
	{
		Logger.printUserLog("===Fixed effect model===================");
		Logger.printUserLog("Effective number of markers is: " + Me);
		Logger.printUserLog("Z score: " + Z);
		Logger.printUserLog("p-value for z score (two-tails): " + pZ);
		Logger.printUserLog("LambdaMeta: " + lambdaM + " (based on " + Me + " markers)");
		Logger.printUserLog("p-value for LambdaMeta: " + pLam);
		Logger.printUserLog("Correlation: " + rho + ", 95% confidence interval is (" + z_l * sigma_rho + ", " + z_h * sigma_rho + ")");
		Logger.printUserLog("Overlapping samples: " + n12 + ", 95% confidence interval is (" + z_l * sigma_n12  + ", " + z_h * sigma_n12 + ")");
	}

	private void lambdaMSD()
	{
		long seed = 2014;
		double alpha = XVec.getN()/2;
		double beta = XVec.getN() - alpha + 1;

		DescriptiveStatistics LamVec = new DescriptiveStatistics();
		ChiSquaredDistributionImpl chiDis = new ChiSquaredDistributionImpl(1);		
		RandomDataImpl rdg = new RandomDataImpl();
		rdg.reSeed(seed);
		try
		{
			for(int i = 0; i < Me; i++)
			{
				double p = rdg.nextBeta(alpha, beta);
				double lam = chiDis.inverseCumulativeProbability(p)/ChiMedianConstant;
				LamVec.addValue(lam);
			}
			sigma_lambdaM = LamVec.getStandardDeviation();
			double z1 = Math.abs(lambdaM - 1) / sigma_lambdaM;
			if(z1 < 8)
			{
				pLam = 2*(1 - nDis.cumulativeProbability(z1));				
			}
			else
			{
				pLam = PrecisePvalue.TwoTailZcumulativeProbability(z1);
			}
		}
		catch (MathException e)
		{
			Logger.printUserError("Problems in generating beta distribution.");
		}
	}

	private void RegChi()
	{
		int start= (int) (Me * 0.05);
		int end = (int) (Me * 0.95);
		SimpleRegression reg = new SimpleRegression();

		for(int i = start; i < end; i++)
		{
			reg.addData(ChisqSample[i], XVec.getElement(i));
		}
		System.out.println(reg.getSlope());
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
	
	private double ChiMedianConstant = 0.4549364;
	private double ChiMedian = 0;
	private double lambdaM = 0;
	private double sigma_lambdaM = 0;
	private double pLam = 0;
	
	private double[] ChisqSample;

}
