package gear.subcommands.metawatchdog.powercalculator;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import gear.subcommands.CommandArguments;
import gear.subcommands.CommandImpl;
import gear.subcommands.metawatchdog.MetaWatchdogConstant;
import gear.util.Logger;

public class DogPowerCommandImpl extends CommandImpl
{
	private DogPowerCommandArguments dogpowerArgs;
	private int method = MetaWatchdogConstant.Chisq;
	@Override
	public void execute(CommandArguments cmdArgs)
	{
		dogpowerArgs = (DogPowerCommandArguments) cmdArgs;

		if(dogpowerArgs.getChisqFlag())
		{
			method = MetaWatchdogConstant.Chisq;
			chisq();
		}
		else if (dogpowerArgs.getRegressionFlag())
		{
			method = MetaWatchdogConstant.Reg;
			regression();
		}
		else
		{
			Logger.printUserLog("No method has been specified. GEAR quitted.");
			System.exit(0);
		}

		Logger.printUserLog("Parameters have been saved into '" + dogpowerArgs.getOutRoot() + ".encode'.");
	}

	private void chisq()
	{
		ChiSquaredDistributionImpl chiDis = new ChiSquaredDistributionImpl(1);
		double constFactor = 8;
		double alpha = dogpowerArgs.getAlpha();
		long tests = dogpowerArgs.getTests();
		double logP = -1 * Math.log10(alpha/(1.0*tests));

		double q = 1 * constFactor * dogpowerArgs.getMissingRate();
		double logChisqP = 0;
		try
		{
			logChisqP = -1 * Math.log10(chiDis.cumulativeProbability(q));
		}
		catch (MathException e)
		{
			Logger.handleException(e, "error in getting p value for chisq distribution.");
		}
		double dif = logChisqP - logP;
		double k = 1;

		while (dif < 0)
		{
			k = k + 1;
			chiDis = new ChiSquaredDistributionImpl(k);
			q = k * constFactor * dogpowerArgs.getMissingRate();
			try
			{
				logChisqP = -1 * Math.log10(chiDis.cumulativeProbability(q));
			}
			catch (MathException e)
			{
				Logger.handleException(e, "error in getting p value for chisq distribution.");
			}
//			System.out.println(q+ " " + logChisqP + " " + logP);
			dif = logChisqP - logP;
		}
		double[][] RsqTab = Rsq(k);

		Logger.printUserLog("Loci missing rate: " + dogpowerArgs.getMissingRate());
		Logger.printUserLog("Seed: " + dogpowerArgs.getSeed());
		Logger.printUserLog("Alpha (Type I error rate): " + alpha);
		Logger.printUserLog("Tests: " + tests);
		Logger.printUserLog("Method: chisq");
		Logger.printUserLog("Q value (threshold for chisq): " + q);
		Logger.printUserLog("Number of pseudo profile scores to archive type I error rate at alpha = " + alpha + " is " + k);
		Logger.printUserLog("Rsq chart==============");
		for (int i = 0; i < RsqTab.length; i++)
		{
			Logger.printUserLog("Keep Rsq < " + RsqTab[i][0] + ", " + RsqTab[i][1] + " markers are required");
		}

		DataOutputStream os = null;
		try
		{
			os = new DataOutputStream(new FileOutputStream(dogpowerArgs.getOutRoot() + ".encode"));
		}
		catch (FileNotFoundException e)
		{
			Logger.printUserError("Cannot create file '" + dogpowerArgs.getOutRoot()
					+ "'.");
			Logger.printUserError("Exception Message: " + e.getMessage());
			System.exit(1);
		}
		try
		{
			os.writeDouble(k);
			os.writeLong(dogpowerArgs.getSeed());
			os.writeDouble(dogpowerArgs.getAlpha());
			os.writeLong(dogpowerArgs.getTests());

			os.writeDouble(dogpowerArgs.getBeta());
			os.writeDouble(dogpowerArgs.getB());
			os.writeDouble(q);
			os.writeInt(method);
			os.close();
		}
		catch (IOException e)
		{
			Logger.handleException(e, "An I/O exception occurred when writing ecode.");
		}
	}

	private void regression()
	{
		NormalDistribution nd = new NormalDistributionImpl();

		double alpha = dogpowerArgs.getAlpha();
		double beta = dogpowerArgs.getBeta();
		long tests = dogpowerArgs.getTests();

		double za = 0;
		double zb = 0;
		try
		{
			za = nd.inverseCumulativeProbability(1 - alpha / tests);
			zb = nd.inverseCumulativeProbability(1 - beta);
		}
		catch (MathException e)
		{
			Logger.handleException(e, "A math exception occurred when calculating inverse cumulative probability.");
		}

		double b = dogpowerArgs.getB();

		double k = (zb * Math.sqrt(1-b * b) + za) / b;
		k *= k;
		double[][] RsqTab = Rsq(k);
		Logger.printUserLog("Loci missing rate: " + dogpowerArgs.getMissingRate());
		Logger.printUserLog("Seed: " + dogpowerArgs.getSeed());
		Logger.printUserLog("Alpha (Type I error rate): " + alpha);
		Logger.printUserLog("Beta (Type II error rate; power = 1-beta): " + beta);
		Logger.printUserLog("Tests: " + tests);
		Logger.printUserLog("Method: regression");
		Logger.printUserLog("Regression coefficient (threshold for regression): " + b);
		Logger.printUserLog("Number of pseudo profile scores to archive type I error rate at alpha = " + alpha + " and power = " + (1-beta) + " is " + k);

		Logger.printUserLog("Rsq chart==============");
		for (int i = 0; i < RsqTab.length; i++)
		{
			Logger.printUserLog("Keep Rsq < " + RsqTab[i][0] + ", " + RsqTab[i][1] + " markers are required");
		}
		DataOutputStream os = null;
		try
		{
			os = new DataOutputStream(new FileOutputStream(dogpowerArgs.getOutRoot() + ".encode"));
		}
		catch (FileNotFoundException e)
		{
			Logger.printUserError("Cannot create file '" + dogpowerArgs.getOutRoot()
					+ "'.");
			Logger.printUserError("Exception Message: " + e.getMessage());
			System.exit(1);
		}
		try
		{
			os.writeDouble(k);
			os.writeLong(dogpowerArgs.getSeed());
			os.writeDouble(dogpowerArgs.getAlpha());
			os.writeLong(dogpowerArgs.getTests());

			os.writeDouble(dogpowerArgs.getBeta());
			os.writeDouble(dogpowerArgs.getB());
			double q = dogpowerArgs.getMissingRate() * k;
			os.writeDouble(q);
			os.writeInt(method);
			os.close();
		}
		catch (IOException e)
		{
			Logger.handleException(e, "An I/O exception occurred when writing ecode.");
		}
	}
	
	private double[][] Rsq(double k)
	{
		double lambda=0.6;
		double[][] RsqTab = new double[3][2];
		RsqTab[0][0] = 0.01; RsqTab[0][1] = Math.ceil(k * (1 - RsqTab[0][0])/RsqTab[0][0] * lambda);
		RsqTab[1][0] = 0.05; RsqTab[1][1] = Math.ceil(k * (1 - RsqTab[1][0])/RsqTab[1][0] * lambda);
		RsqTab[2][0] = 0.1; RsqTab[2][1] = Math.ceil(k * (1 - RsqTab[2][0])/RsqTab[2][0] * lambda);
		return RsqTab;
	}
}
