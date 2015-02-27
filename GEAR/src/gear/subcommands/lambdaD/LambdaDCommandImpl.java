package gear.subcommands.lambdaD;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;

import gear.gwassummary.GWASReader;
import gear.gwassummary.MetaStat;
import gear.subcommands.CommandArguments;
import gear.subcommands.CommandImpl;
import gear.util.FileUtil;
import gear.util.Logger;
import gear.util.NewIt;
import gear.util.SNPMatch;

public class LambdaDCommandImpl extends CommandImpl
{

	@Override
	public void execute(CommandArguments cmdArgs)
	{
		lamArgs = (LambdaDCommandArguments) cmdArgs;

		if (lamArgs.isQT())
		{
			Logger.printUserLog("Analysing summary statistics analysis for quantitative traits.\n");
		}
		else
		{
			Logger.printUserLog("Analysing summary statistics analysis for case-contrl studies.\n");
		}

		initial();

		// generating matrix
		String[] MetaFile = gReader.getMetaFile();
		int FileSize = lamArgs.getTop() > 0 ? lamArgs.getTop()
				: (MetaFile.length - 1);
		for (int i = 0; i < FileSize; i++)
		{
			for (int j = (i + 1); j < MetaFile.length; j++)
			{
				Logger.printUserLog("");
				Logger.printUserLog("File pair: " + (i + 1) + "-" + (j + 1));
				if (lamArgs.isQT())
				{
					double[] size = lamArgs.getQTsize();
					Kappa = 2 / (Math.sqrt(size[i] / size[j]) + Math
							.sqrt(size[j] / size[i]));
					Logger.printUserLog("Sample sizes for '" + MetaFile[i] + "': " + size[i]);
					Logger.printUserLog("Sample sizes for '" + MetaFile[j] + "': " + size[j]);
				}
				else
				{
					double[] size = lamArgs.getCCsize();
					R1 = size[i * 2] / size[i * 2 + 1];
					R2 = size[j * 2] / size[j * 2 + 1];
					double s1 = size[i * 2] + size[i * 2 + 1];
					double s2 = size[j * 2] + size[j * 2 + 1];
					Kappa = 2 / (Math.sqrt(s1 / s2) + Math.sqrt(s2 / s1));
					Logger.printUserLog("Sample size for '" + MetaFile[i] + "': " + size[i * 2] + " cases, " + size[i * 2 + 1] + " controls; R1 = " + R1 + ".");
					Logger.printUserLog("Sample size for '" + MetaFile[j] + "': " + size[j * 2] + " cases, " + size[j * 2 + 1] + " controls; R2 = " + R2 + ".");
				}
				Logger.printUserLog("Kappa: " + Kappa);

				calculateLambdaD(i, j);
			}
		}
		WriteMat();
		if (lamArgs.isFrq())
		{
			WriteFstMat();
		}
		Logger.printUserLog("=========================================================");
		Logger.printUserLog("Results has been saved in '" + lamArgs
				.getOutRoot() + ".lmat'.");
	}

	private void initial()
	{
		boolean[] FileKeep = new boolean[lamArgs.getMetaFile().length];
		Arrays.fill(FileKeep, true);
		gReader = new GWASReader(lamArgs.getMetaFile(), FileKeep,
				lamArgs.getKeys(), lamArgs.isQT(), lamArgs.isGZ(),
				lamArgs.isChr(), lamArgs.getChr());
		gReader.Start(lamArgs.isFrq());

		Me = lamArgs.getMe();
		if (Me > 0)
		{
			Logger.printUserLog("Set the effective number of marker: " + Me);
		}
		else
		{
			Logger.printUserLog("Using all markers.");
		}

		if (lamArgs.isBeta())
		{
			Logger.printUserLog("Calculating genetic effects difference.");
		}
		else if (lamArgs.isFrq())
		{
			Logger.printUserLog("Calculating allele frequency difference, and Fst.");
		}

		int NumMetaFile = lamArgs.getMetaFile().length;

		if (NumMetaFile < 2)
		{
			Logger.printUserError("At least two summary statistic files should be specified.\n");
			Logger.printUserError("GEAR quitted.\n");
			System.exit(0);
		}

		lamMat = new double[NumMetaFile][NumMetaFile];
		zMat = new double[NumMetaFile][NumMetaFile];
		olCtrlMat = new double[NumMetaFile][NumMetaFile];
		olCsMat = new double[NumMetaFile][NumMetaFile];

		kMat = new double[NumMetaFile][NumMetaFile];

		fstMat = new double[NumMetaFile][NumMetaFile];
		for (int i = 0; i < NumMetaFile; i++)
		{
			Arrays.fill(lamMat[i], 1);
			Arrays.fill(zMat[i], 1);
			Arrays.fill(kMat[i], 1);
			if (lamArgs.isQT())
			{
				olCtrlMat[i][i] = lamArgs.getQTsize()[i];
				olCsMat[i][i] = lamArgs.getQTsize()[i];
			}
			else
			{
				olCtrlMat[i][i] = lamArgs.getCCsize()[i * 2] + lamArgs
						.getCCsize()[i * 2 + 1];
				olCsMat[i][i] = lamArgs.getCCsize()[i * 2] + lamArgs
						.getCCsize()[i * 2 + 1];
			}
		}

		// reading meta files
	}

	private void calculateLambdaD(int idx1, int idx2)
	{
		ArrayList<LamUnit> LamArray = NewIt.newArrayList();

		// DescriptiveStatistics T0 = new DescriptiveStatistics();

		int cntAmbiguous = 0;
		HashMap<String, MetaStat> SumStat1 = gReader.getMetaStat().get(idx1);
		HashMap<String, MetaStat> SumStat2 = gReader.getMetaStat().get(idx2);

		ArrayList<String> snpArray = gReader.getMetaSNPArray().get(idx1);

		int[][] KeyIdx = gReader.getKeyIndex();
		for (String snp : snpArray)
		{
			if (!SumStat2.containsKey(snp) || !SumStat1.containsKey(snp))
			{
				continue;
			}
			MetaStat ms1 = SumStat1.get(snp);
			MetaStat ms2 = SumStat2.get(snp);

			if (KeyIdx[idx1][GWASReader.SE] != -1)
			{
				if (SNPMatch.isAmbiguous(ms1.getA1(), ms1.getA2()))
				{
					cntAmbiguous++;
					continue;
				}
			}
			if (KeyIdx[idx2][GWASReader.SE] != -1)
			{
				if (SNPMatch.isAmbiguous(ms2.getA1(), ms2.getA2()))
				{
					cntAmbiguous++;
					continue;
				}
			}

			boolean lineup = true;
			if (ms1.getA1() == ms2.getA1() || ms1.getA1() == SNPMatch.Flip(ms2
					.getA1())) // match A1 in the second meta
			{
			}
			else if (ms1.getA1() == ms2.getA2() || ms1.getA1() == SNPMatch
					.Flip(ms2.getA2())) // match A2 in the second meta
			{
				lineup = false;
			}
			else
			{
				cntAmbiguous++;
				continue;
			}

			double s1, s2;
			if (lamArgs.isQT())
			{
				s1 = lamArgs.getQTsize()[idx1];
				s2 = lamArgs.getQTsize()[idx2];
			}
			else
			{
				s1 = lamArgs.getCCsize()[idx1 * 2] + lamArgs.getCCsize()[idx1 * 2 + 1];
				s2 = lamArgs.getCCsize()[idx2 * 2] + lamArgs.getCCsize()[idx2 * 2 + 1];
			}

			LamArray.add(new LamUnit(ms1, ms2, lamArgs.getMode(), lineup, s1,
					s2));
		}

		if (cntAmbiguous > 0)
		{
			if (cntAmbiguous == 1)
			{
				Logger.printUserLog("Removed " + cntAmbiguous + " ambiguous locus (AT/GC).");
			}
			else
			{
				Logger.printUserLog("Removed " + cntAmbiguous + " ambiguous loci (AT/GC).");
			}
		}
		Logger.printUserLog("Found " + LamArray.size() + " consensus summary statistics between these two files.");
		
		if (LamArray.size() < ((int) (Me * 0.2)))
		{
			Logger.printUserLog("Too few overlapping snps, skip this pair of files");
			return;
		}

		// select independent snps
		Collections.sort(LamArray);

		int[] selIdx = null;
		if (Me < 0)
		{// use all
			selIdx = new int[LamArray.size()];
			for (int i = 0; i < selIdx.length; i++)
				selIdx[i] = i;
		}
		else if (LamArray.size() <= Me)
		{// use available ones
			selIdx = new int[LamArray.size()];
			for (int i = 0; i < selIdx.length; i++)
				selIdx[i] = i;
		}
		else
		{// use Me
			selIdx = new int[(int) Math.ceil(Me)];
			for (int i = 0; i < Me; i++)
				selIdx[i] = (int) Math.floor((i * 1.0 + 1) / Me * LamArray
						.size()) - 1;
		}

		BVec Bvec = new BVec();
		double[] DesStat = new double[selIdx.length];
		double fst = 0;

		for (int i = 0; i < selIdx.length; i++)
		{
			LamUnit lu = LamArray.get(selIdx[i]);
			DesStat[i] = lu.getIndicateStat(lamArgs.getMode());
			fst += lu.getFstBW()/selIdx.length;
			Bvec.addStats(lu.getB1(), lu.getB2(), lu.getSE1(), lu.getSE2());
		}

		if (lamArgs.isBeta())
		{
			Bvec.setSelected();
			Bvec.CalCorrelation();
			Bvec.printOut();
		}
		else
		{
			Logger.printUserLog("Fst is " + fst);
			fstMat[idx2][idx1] = fstMat[idx1][idx2] = fst;
		}

		if (lamArgs.isQT())
		{
			double[] qtSize = lamArgs.getQTsize();
			XTest et = new XTest(DesStat, qtSize[idx1], qtSize[idx2]);

			olCtrlMat[idx2][idx1] = olCsMat[idx1][idx2] = et.getN12();
			lamMat[idx1][idx2] = lamMat[idx2][idx1] = et.getLambda();
			zMat[idx2][idx1] = et.getRho();
			zMat[idx1][idx2] = et.getZ();

			kMat[idx1][idx2] = et.getX();
			kMat[idx2][idx1] = Kappa;

			et.PrintQT();
		}
		else
		{
			double[] ccSize = lamArgs.getCCsize();
			XTest et = new XTest(DesStat, ccSize[idx1 * 2],
					ccSize[idx1 * 2 + 1], ccSize[idx2 * 2],
					ccSize[idx2 * 2 + 1]);

			olCtrlMat[idx1][idx2] = olCsMat[idx1][idx2] = et.getN12();
			lamMat[idx1][idx2] = lamMat[idx2][idx1] = et.getLambda();
			zMat[idx2][idx1] = et.getRho();
			zMat[idx1][idx2] = et.getZ();

			kMat[idx1][idx2] = Kappa;
			kMat[idx2][idx1] = et.getX();

			et.PrintCC();

			olCtrlMat[idx2][idx1] = et.getN12cl();
			olCsMat[idx2][idx1] = et.getN12cs();
		}

		if (!lamArgs.isClean())
		{
			if (lamArgs.isVerboseGZ())
			{
				VerboseGZ(LamArray, idx1, idx2);
			}
			else if (lamArgs.isVerbose())
			{
				Verbose(LamArray, idx1, idx2);
			}
			else
			{
				NotVerbose(LamArray, idx1, idx2, selIdx);
			}			
		}
	}

	private void NotVerbose(ArrayList<LamUnit> LamArray, int idx1, int idx2,
			int[] selIdx)
	{
		double[] ChiExp = sampleChisq(selIdx.length, 1);
		PrintStream writer = FileUtil.CreatePrintStream(new String(lamArgs
				.getOutRoot() + "." + (idx1 + 1) + "-" + (idx2 + 1) + tail[lamArgs.getMode()]));
		writer.print(titleLine[lamArgs.getMode()]);

		for (int i = 0; i < selIdx.length; i++)
		{
			LamUnit lu = LamArray.get(selIdx[i]);
			MetaStat ms1 = lu.getMetaStat1();
			MetaStat ms2 = lu.getMetaStat2();
			double lambda = lu.getIndicateStat(lamArgs.getMode()) / (ChiExp[i]);
			if (lamArgs.getMode() == LambdaDCommandArguments.BETA)
			{
				writer.print(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1
						.getBP() + "\t" + ms1.getA1() + "\t" + lu.getB1() + "\t" + ms1
						.getSE() + "\t" + ms1.getP() + "\t" + lu.getB2() + "\t" + ms2
						.getSE() + "\t" + ms2.getP() + "\t" + lu
						.getIndicateStat(lamArgs.getMode()) + "\t" + ChiExp[i] + "\t" + lambda + "\n");
			}
			else
			{
				writer.print(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1
						.getBP() + "\t" + ms1.getA1() + "\t" + lu.getB1() + "\t" + ms1
						.getSE() + "\t" + ms1.getP() + "\t" + lu.getB2() + "\t" + ms2
						.getSE() + "\t" + ms2.getP() + "\t" + lu.getFstBW() + "\t" + lu.getFstChi() + "\t" + lu
						.getIndicateStat(lamArgs.getMode()) + "\t" + ChiExp[i] + "\t" + lambda + "\n");
			}
		}
		writer.close();
	}

	private void Verbose(ArrayList<LamUnit> LamArray, int idx1, int idx2)
	{
		double[] ChiExp = sampleChisq(LamArray.size(), 1);
		PrintStream writer = FileUtil.CreatePrintStream(new String(lamArgs
				.getOutRoot() + "." + (idx1 + 1) + "-" + (idx2 + 1) + tail[lamArgs.getMode()]));
		writer.print(titleLine[lamArgs.getMode()]);

		for (int i = 0; i < LamArray.size(); i++)
		{
			LamUnit lu = LamArray.get(i);
			MetaStat ms1 = lu.getMetaStat1();
			MetaStat ms2 = lu.getMetaStat2();
			double lambda = lu.getIndicateStat(lamArgs.getMode()) / (ChiExp[i]);
			if (lamArgs.getMode() == LambdaDCommandArguments.BETA)
			{
				writer.print(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1
						.getBP() + "\t" + ms1.getA1() + "\t" + lu.getB1() + "\t" + ms1
						.getSE() + "\t" + ms1.getP() + "\t" + lu.getB2() + "\t" + ms2
						.getSE() + "\t" + ms2.getP() + "\t" + lu
						.getIndicateStat(lamArgs.getMode()) + "\t" + ChiExp[i] + "\t" + lambda);
			}
			else
			{
				writer.print(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1
						.getBP() + "\t" + ms1.getA1() + "\t" + lu.getB1() + "\t" + ms1
						.getSE() + "\t" + ms1.getP() + "\t" + lu.getB2() + "\t" + ms2
						.getSE() + "\t" + ms2.getP() + "\t" + lu.getFstBW() + "\t" + lu.getFstChi() + "\t" + lu
						.getIndicateStat(lamArgs.getMode()) + "\t" + ChiExp[i] + "\t" + lambda);
			}
		}
		writer.close();
	}

	private void VerboseGZ(ArrayList<LamUnit> LamArray, int idx1, int idx2)
	{
		double[] ChiExp = sampleChisq(LamArray.size(), 1);
		BufferedWriter GZ = FileUtil
				.ZipFileWriter(new String(
						lamArgs.getOutRoot() + "." + (idx1 + 1) + "-" + (idx2 + 1) + tail[lamArgs.getMode()] + ".gz"));

		try
		{
			GZ.append(titleLine[lamArgs.getMode()]);
		}
		catch (IOException e)
		{
			Logger.handleException(	e,
									"error in writing " + new String(
											lamArgs.getOutRoot() + "." + (idx1 + 1) + "-" + (idx2 + 1) + tail[lamArgs.getMode()] + ".gz"));
		}

		for (int i = 0; i < LamArray.size(); i++)
		{
			LamUnit lu = LamArray.get(i);
			MetaStat ms1 = lu.getMetaStat1();
			MetaStat ms2 = lu.getMetaStat2();
			double lambda = lu.getIndicateStat(lamArgs.getMode()) / (ChiExp[i]);
			try
			{
				if (lamArgs.getMode() == LambdaDCommandArguments.BETA)
				{
					GZ.write(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1
							.getBP() + "\t" + ms1.getA1() + "\t" + lu.getB1() + "\t" + ms1
							.getSE() + "\t" + ms1.getP() + "\t" + lu.getB2() + "\t" + ms2
							.getSE() + "\t" + ms2.getP() + "\t" + lu
							.getIndicateStat(lamArgs.getMode()) + "\t" + ChiExp[i] + "\t" + lambda + "\n");
				}
				else
				{
					GZ.write(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1
							.getBP() + "\t" + ms1.getA1() + "\t" + lu.getB1() + "\t" + ms1
							.getSE() + "\t" + ms1.getP() + "\t" + lu.getB2() + "\t" + ms2
							.getSE() + "\t" + ms2.getP() + "\t" + lu.getFstBW() + "\t" + lu.getFstChi() + "\t" + lu
							.getIndicateStat(lamArgs.getMode()) + "\t" + ChiExp[i] + "\t" + lambda + "\n");
				}
			}
			catch (IOException e)
			{
				Logger.handleException(	e,
										"error in writing " + new String(
												lamArgs.getOutRoot() + "." + (idx1 + 1) + "-" + (idx2 + 1) + ".lam.gz"));
			}
		}
		try
		{
			GZ.close();
		}
		catch (IOException e)
		{
			Logger.handleException(	e,
									"error in writing " + new String(
											lamArgs.getOutRoot() + "." + (idx1 + 1) + "-" + (idx2 + 1) + ".lam.gz"));
		}
	}

	private void WriteMat()
	{
		// cm matrix
		PrintStream cwriter = FileUtil.CreatePrintStream(new String(lamArgs.getOutRoot() + ".cm"));

		for (int i = 0; i < zMat.length; i++)
		{
			for (int j = 0; j < zMat[i].length; j++)
			{
				cwriter.print(String.format("%.4f", zMat[i][j]) + " ");
			}
			cwriter.println();
		}
		cwriter.close();

		// Xmatrix
		PrintStream xwriter = FileUtil.CreatePrintStream(new String(lamArgs.getOutRoot() + ".xm"));

		for (int i = 0; i < kMat.length; i++)
		{
			for (int j = 0; j < kMat[i].length; j++)
			{
				xwriter.print(String.format("%.4f", kMat[i][j]) + " ");
			}
			xwriter.println();
		}
		xwriter.close();

		PrintStream writer = FileUtil.CreatePrintStream(new String(lamArgs.getOutRoot() + ".lmat"));

		writer.println("LambdaMeta:");
		for (int i = 0; i < lamMat.length; i++)
		{
			for (int j = 0; j < lamMat[i].length; j++)
			{
				writer.print(String.format("%.4f", lamMat[i][j]) + " ");
			}
			writer.println();
		}

		if (!lamArgs.isQT())
		{
			writer.println("Overlapping controls (lower triangle) vs overlapping samples (upper triangle):");
			for (int i = 0; i < olCtrlMat.length; i++)
			{
				for (int j = 0; j < olCtrlMat[i].length; j++)
				{
					writer.print(String.format("%.4f", olCtrlMat[i][j]) + " ");
				}
				writer.println();
			}

			writer.println("Overlapping cases (lower triangle) vs Overlapping samples (upper triangle):");
			for (int i = 0; i < olCsMat.length; i++)
			{
				for (int j = 0; j < olCsMat[i].length; j++)
				{
					writer.print(String.format("%.4f", olCsMat[i][j]) + " ");
				}
				writer.println();
			}
		}
		else
		{
			writer.println("Overlapping samples (lower triangle)");
			for (int i = 0; i < olCtrlMat.length; i++)
			{
				for (int j = 0; j < olCtrlMat[i].length; j++)
				{
					writer.print(String.format("%.4f", olCtrlMat[i][j]) + " ");
				}
				writer.println();
			}
		}

		writer.close();
	}

	private double[] sampleChisq(int Len, int df)
	{
		ChiSquaredDistributionImpl chiDis = new ChiSquaredDistributionImpl(df);
		double[] ChiExp = new double[Len];
		for (int i = 0; i < Len; i++)
		{
			try
			{
				ChiExp[i] = chiDis
						.inverseCumulativeProbability((i + 1) / (Len + 0.05));
				;
			}
			catch (MathException e)
			{
				e.printStackTrace();
			}
		}
		return ChiExp;
	}

	private void WriteFstMat()
	{
		PrintStream FstWriter = FileUtil.CreatePrintStream(new String(lamArgs.getOutRoot() + ".fst"));
		for (int i = 0; i < fstMat.length; i++)
		{
			for (int j = 0; j < fstMat[i].length; j++)
			{
				FstWriter.print(String.format("%.5f", fstMat[i][j]) + " ");
			}
			FstWriter.println();
		}
		FstWriter.close();
	}

	private double Me = 30000;

	private double R1 = 1;
	private double R2 = 1;
	private double Kappa = 1;
	private LambdaDCommandArguments lamArgs;

	private String[] titleLine = {
			"SNP\tChr\tBp\tA1\tBeta1\tSE1\tP1\tBeta2\tSE2\tP2\tChiObs(beta)\tChiExp\tLambdaD\n",
			"SNP\tChr\tBp\tA1\tFrq1\tSE1\tP1\tFrq2\tSE2\tP2\tFst\tChiObs(Fst)\tChiObs(Frq)\tChiExp\tLambdaD\n" };

	private String[] tail = {".lamB", ".lamF"};

	private GWASReader gReader;

	private double[][] lamMat;
	private double[][] zMat;
	private double[][] olCtrlMat;
	private double[][] olCsMat;
	private double[][] kMat;
	private double[][] fstMat;

}
