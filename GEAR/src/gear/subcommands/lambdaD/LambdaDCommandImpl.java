package gear.subcommands.lambdaD;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.ranking.NaNStrategy;
import org.apache.commons.math.stat.ranking.NaturalRanking;
import org.apache.commons.math.stat.ranking.TiesStrategy;

import gear.gwassummary.GWASReader;
import gear.gwassummary.MetaStat;
import gear.subcommands.CommandArguments;
import gear.subcommands.CommandImpl;
import gear.util.BufferedReader;
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

//generating matrix
		if(lamArgs.isCM())
		{
			readCMFile();
			MetaAnalysis();
		}
		else
		{
			String[] MetaFile = gReader.getMetaFile();
			for (int i=0; i < MetaFile.length-1; i++)
			{
				for (int j = (i+1); j < MetaFile.length; j++)
				{
					Logger.printUserLog("File pair: " + (i+1) + "-" + (j+1));
					if (lamArgs.isQT())
					{
						double[] size = lamArgs.getQTsize();
						Kappa = 2 / ( Math.sqrt(size[i]/size[j]) + Math.sqrt(size[j]/size[i]) );
						Logger.printUserLog("Sample sizes for '" + MetaFile[i] + "': " + size[i]);
						Logger.printUserLog("Sample sizes for '" + MetaFile[j] + "': " +size[j]);
					}
					else
					{
						double[] size = lamArgs.getCCsize();
						R1 = size[i*2]/size[i*2+1];
						R2 = size[j*2]/size[j*2+1];
						double s1 = size[i*2] + size[i*2+1];
						double s2 = size[j*2] + size[j*2+1];
						Kappa = 2 / (Math.sqrt(s1 / s2) + Math.sqrt(s2 / s1));
						Logger.printUserLog("Sample size for '" + MetaFile[i] + "': " + size[i*2] + " cases, " + size[i*2+1] + " controls; R1 = " + R1 + ".");
						Logger.printUserLog("Sample size for '" + MetaFile[j] + "': " + size[j*2] + " cases, " + size[j*2+1] + " controls; R2 = " + R2 + ".");
					}
					Logger.printUserLog("Kappa: " + Kappa);

					calculateLambdaD(i, j);
				}
			}
			WriteMat();
			Logger.printUserLog("=========================================================");
			Logger.printUserLog("Results has been saved in '" + lamArgs.getOutRoot() + ".lmat'.");
			
		}
		
//run mate-analysis
	}

	private void initial()
	{
		gReader = new GWASReader(lamArgs);

		Me = lamArgs.getMe();
		int NumMetaFile = gReader.getNumMetaFile();
		
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

		for(int i = 0; i < NumMetaFile; i++)
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
				olCtrlMat[i][i] = lamArgs.getCCsize()[i*2] + lamArgs.getCCsize()[i*2+1];
				olCsMat[i][i] = lamArgs.getCCsize()[i*2] + lamArgs.getCCsize()[i*2+1];
			}
		}

//reading meta files
	}

	private void MetaAnalysis()
	{
		int NumMetaFile = gReader.getNumMetaFile();

		double[][] Mx = new double[NumMetaFile][NumMetaFile];
		for (int i = 0; i < Mx.length; i++)
		{
			for (int j = 0; j < i + 1; j++)
			{
				Mx[i][j] = zMat[i][j];
				Mx[j][i] = zMat[i][j];
			}
		}

		RealMatrix gg = new Array2DRowRealMatrix(Mx);
		RealMatrix gg_Inv = (new LUDecompositionImpl(gg)).getSolver().getInverse();
//		InvMx = gg_Inv.getData();
		RealMatrix Unit = new Array2DRowRealMatrix(NumMetaFile, 1);
		for (int i = 0; i < Unit.getRowDimension(); i++)
		{
			Unit.setEntry(i, 0, 1);
		}
		RealMatrix tmp = Unit.transpose().multiply(gg_Inv);
		RealMatrix tmp1 = tmp.multiply(Unit);
		RealMatrix W = tmp.scalarMultiply(1/tmp1.getEntry(0, 0));

		Set<String> keys = gReader.getMetaSNPTable().keySet();
		for (Iterator<String> e=keys.iterator(); e.hasNext();)
		{
			String key = e.next();
			ArrayList<Integer> Int = gReader.getMetaSNPTable().get(key);
			//common snp only
			if(Int.get(Int.size()-1) == NumMetaFile)
			{
				MetaCommon(key, Mx, W, Int);
			}
			else
			{
				MetaSNP(key, Mx, Int);
			}
		}
	}

	private void MetaSNP(String key, double[][] Mx, ArrayList<Integer> Int)
	{
		int[] idx = new int[Int.get(Int.size()-1)];
		
		int cnt = 0;
		for(int i = 0; i < Int.size() - 1; i++)
		{
			if(Int.get(i) == 0) continue;
			idx[cnt++] = i;
		}

		double[][] mx = new double[idx.length][idx.length];
		for(int i = 0; i < idx.length; i++)
		{
			for(int j = 0; j < idx.length; j++)
			{
				mx[i][j] = Mx[idx[i]][idx[j]];
			}
		}

		RealMatrix gg = new Array2DRowRealMatrix(mx);
		RealMatrix gg_Inv = (new LUDecompositionImpl(gg)).getSolver().getInverse();
//		double[][] Invmx = gg_Inv.getData();
		RealMatrix Unit = new Array2DRowRealMatrix(mx.length, 1);
		for (int i = 0; i < Unit.getRowDimension(); i++)
		{
			Unit.setEntry(i, 0, 1);
		}
		RealMatrix tmp = Unit.transpose().multiply(gg_Inv);
		RealMatrix tmp1 = tmp.multiply(Unit);
		RealMatrix W = tmp.scalarMultiply(1/tmp1.getEntry(0, 0));

		double gb = 0;
		double gse = 0;
		double[] se = new double[W.getColumnDimension()];
		
		HashMap<String, MetaStat> m1 = gReader.getMetaStat().get(idx[0]);
		MetaStat ms1 = m1.get(key);
		gb += ms1.getEffect() * W.getEntry(0, 0);
		se[0] = ms1.getSE();

		for (int i = 1; i < idx.length; i++)
		{
			HashMap<String, MetaStat> m2 = gReader.getMetaStat().get(idx[i]);
			MetaStat ms2 = m2.get(key); 
			se[i] = ms2.getSE();

			if (ms1.getA1() == ms2.getA1() || ms1.getA1() == SNPMatch.Flip(ms2.getA1())) //match A1 in the second meta
			{
				gb += ms2.getEffect() * W.getEntry(0, i);
			}
			else if (ms1.getA1() == ms2.getA2() || ms1.getA1() == SNPMatch.Flip(ms2.getA2())) //match A2 in the second meta
			{
				gb += (-1) * ms2.getEffect() * W.getEntry(0, i);
			}
		}

		for (int i = 0; i < W.getRowDimension(); i++)
		{
			for (int j = 0; j < W.getRowDimension(); j++)
			{
				gse += W.getEntry(0, i) * W.getEntry(0, j) * se[i] * se[j] * Mx[i][j];
			}
		}
		gse = Math.sqrt(gse);
		System.out.println(key + " " +gb + " " + gse + " " + Int.toString());
	}

	private void MetaCommon(String key, double[][] Mx, RealMatrix W, ArrayList<Integer> Int)
	{
		double gb = 0;
		double gse = 0;
		double[] se = new double[W.getColumnDimension()];
		
		HashMap<String, MetaStat> m1 = gReader.getMetaStat().get(0);
		MetaStat ms1 = m1.get(key);
		gb += ms1.getEffect() * W.getEntry(0, 0);
		se[0] = ms1.getSE();
			
		for(int i = 1; i < gReader.getMetaStat().size(); i++)
		{
			HashMap<String, MetaStat> m2 = gReader.getMetaStat().get(i);
			MetaStat ms2 = m2.get(key); 
			se[i] = ms2.getSE();

			if (ms1.getA1() == ms2.getA1() || ms1.getA1() == SNPMatch.Flip(ms2.getA1())) //match A1 in the second meta
			{
				gb += ms2.getEffect() * W.getEntry(0, i);
			}
			else if (ms1.getA1() == ms2.getA2() || ms1.getA1() == SNPMatch.Flip(ms2.getA2())) //match A2 in the second meta
			{
				gb += (-1) * ms2.getEffect() * W.getEntry(0, i);
			}
		}

		for(int i = 0; i < W.getRowDimension(); i++)
		{
			for(int j = 0; j < W.getRowDimension(); j++)
			{
				gse += W.getEntry(0, i) * W.getEntry(0, j) * se[i] * se[j] * Mx[i][j];
			}
		}
		gse = Math.sqrt(gse);
		System.out.println(key + " " +gb + " " + gse + " " + Int.toString());
	}

	private void calculateLambdaD(int idx1, int idx2)
	{
		ArrayList<LamUnit> LamArray = NewIt.newArrayList();
    	DescriptiveStatistics T0 = new DescriptiveStatistics();

		int cntAmbiguous = 0;
		HashMap<String, MetaStat> SumStat1 = gReader.getMetaStat().get(idx1);
		HashMap<String, MetaStat> SumStat2 = gReader.getMetaStat().get(idx2);

		ArrayList<String> snpArray = gReader.getMetaSNPArray().get(idx1);
		
		int[][] KeyIdx = gReader.getKeyIndex();
		for(String snp : snpArray)
		{
			if (!SumStat2.containsKey(snp) || !SumStat1.containsKey(snp))
			{
				continue;
			}
			MetaStat ms1 = SumStat1.get(snp);
			MetaStat ms2 = SumStat2.get(snp);
			double d = 0;
			
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

			if (ms1.getA1() == ms2.getA1() || ms1.getA1() == SNPMatch.Flip(ms2.getA1())) //match A1 in the second meta
			{
				d = (ms1.getEffect() - ms2.getEffect()) * (ms1.getEffect() - ms2.getEffect()) / (ms1.getSE() * ms1.getSE() + ms2.getSE() * ms2.getSE());
			}
			else if (ms1.getA1() == ms2.getA2() || ms1.getA1() == SNPMatch.Flip(ms2.getA2())) //match A2 in the second meta
			{
				d = ((-1) * ms1.getEffect() - ms2.getEffect()) * ((-1) * ms1.getEffect() - ms2.getEffect()) / (ms1.getSE() * ms1.getSE() + ms2.getSE() * ms2.getSE());
			}
			else
			{
				cntAmbiguous++;
				continue;
			}
			T0.addValue(d);
			LamArray.add(new LamUnit(d, ms1, ms2));
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
		Logger.printUserLog("Lambda is calculated based on " + T0.getN() + " summary statistics between two files.");

//select independent snps
		double[] sortLD = T0.getSortedValues();
		double[] DesStat = null;
		int[] selIdx = null;
		
		if (Me < 0)
		{//use all 
			DesStat = new double[sortLD.length];
			System.arraycopy(sortLD, 0, DesStat, 0, sortLD.length);
			selIdx = new int[sortLD.length];
			for (int i = 0; i < sortLD.length; i++)
			{
				selIdx[i] = i;
			}	
		}
		else if (sortLD.length <= Me)
		{//use available ones
			DesStat = new double[sortLD.length];
			System.arraycopy(sortLD, 0, DesStat, 0, sortLD.length);
			selIdx = new int[sortLD.length];
			for (int i = 0; i < sortLD.length; i++)
			{
				selIdx[i] = i;
			}
		}
		else
		{//use Me
			DesStat = new double[(int) Math.ceil(Me)];
			selIdx = new int[(int) Math.ceil(Me)];
			for (int i = 0; i < DesStat.length; i++)
			{
				selIdx[i] = (int) Math.floor( (i*1.0 + 1)/Me * sortLD.length ) -1;
				DesStat[i] = sortLD[selIdx[i]];
			}
		}

		if (lamArgs.isQT())
		{
			double[] qtSize = lamArgs.getQTsize();
			XTest et = new XTest(DesStat, qtSize[idx1], qtSize[idx2]);

			olCtrlMat[idx1][idx2] = olCsMat[idx1][idx2] = et.getN12();
			lamMat[idx1][idx2] = lamMat[idx2][idx1] = et.getLambda();
			zMat[idx2][idx1] = et.getRho();
			zMat[idx1][idx2] = et.getZ();

			kMat[idx1][idx2] = kMat[idx2][idx1] = Kappa;

			et.PrintQT();
		}
		else
		{
			double[] ccSize = lamArgs.getCCsize();
			XTest et = new XTest(DesStat, ccSize[idx1*2], ccSize[idx1*2+1], ccSize[idx2*2], ccSize[idx2*2+1]);

			olCtrlMat[idx1][idx2] = olCsMat[idx1][idx2] = et.getN12();
			lamMat[idx1][idx2] = lamMat[idx2][idx1] = et.getLambda();
			zMat[idx2][idx1] = et.getRho();
			zMat[idx1][idx2] = et.getZ();

			kMat[idx1][idx2] = kMat[idx2][idx1] = Kappa;

			et.PrintCC();

			olCtrlMat[idx2][idx1] = et.getN12cl();
			olCsMat[idx2][idx1] = et.getN12cs();
		}

		if (lamArgs.isVerboseGZ())
		{
			VerboseGZ(LamArray, T0.getValues(), idx1, idx2);
		}
		else if (lamArgs.isVerbose())
		{
			Verbose(LamArray, T0.getValues(), idx1, idx2);
		}
		else
		{
			NotVerbose(LamArray, T0.getValues(), idx1, idx2, selIdx);
		}
	}

	private void NotVerbose(ArrayList<LamUnit> LamArray, double[] ld, int idx1, int idx2, int[] selIdx)
	{
		Collections.sort(LamArray);
		ChiSquaredDistributionImpl chiDis = new ChiSquaredDistributionImpl(1);

        PrintWriter writer = null;
        try
        {
        	writer = new PrintWriter(new BufferedWriter(new FileWriter(lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam")));
        	Logger.printUserLog("Writting detailed test statistics into '"+lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam" + ".'\n");
        }
		catch (IOException e)
		{
			Logger.handleException(e, "An I/O exception occurred when writing '" + lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam" + "'.\n");
		}

        FileUtil.CreatePrintStream(new String(lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam"));
       	writer.write(titleLine);

        for (int i = 0; i < selIdx.length; i++)
        {
        	LamUnit lu = LamArray.get(selIdx[i]);
        	MetaStat ms1 = lu.getMS1();
        	MetaStat ms2 = lu.getMS2();
        	double chi0 = 0;
			try
			{
				chi0 = chiDis.inverseCumulativeProbability((i+1)/(selIdx.length+0.05));;
			}
			catch (MathException e)
			{
				e.printStackTrace();
			}
			double lambda = lu.getChi1()/chi0;
        	writer.write(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1.getBP() + "\t" + ms1.getA1() + "\t" + ms1.getEffect() + "\t"  + ms1.getSE() + "\t" + ms1.getP() + "\t"+ ms2.getEffect() + "\t" + ms2.getSE() + "\t" + ms2.getP() + "\t" +lu.getChi1() + "\t" + chi0 + "\t" + lambda + "\n");
        }
        writer.close();
	}

	private void Verbose(ArrayList<LamUnit> LamArray, double[] ld, int idx1, int idx2)
	{
		ChiSquaredDistributionImpl chiDis = new ChiSquaredDistributionImpl(1);

		NaturalRanking ranking = new NaturalRanking(NaNStrategy.MINIMAL, TiesStrategy.SEQUENTIAL);
        double[] ranks = ranking.rank(ld);

        PrintWriter writer = null;
        try
        {
        	writer = new PrintWriter(new BufferedWriter(new FileWriter(lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam")));
        	Logger.printUserLog("Writting detailed test statistics into '" + lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam.'\n");
        }
		catch (IOException e)
		{
			Logger.handleException(e, "An I/O exception occurred when writing '" + lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam" + "'.");
		}

        FileUtil.CreatePrintStream(new String(lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam"));
       	writer.write(titleLine);

        for (int i = 0; i < ranks.length; i++)
        {
        	LamUnit lu = LamArray.get(i);
        	MetaStat ms1 = lu.getMS1();
        	MetaStat ms2 = lu.getMS2();
        	double chi0 = 0;
			try
			{
				chi0 = chiDis.inverseCumulativeProbability(ranks[i]/(ranks.length+1));
			}
			catch (MathException e)
			{
				e.printStackTrace();
			}
			double lambda = lu.getChi1()/chi0;
        	writer.write(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1.getBP() + "\t" + ms1.getA1() + "\t" + ms1.getEffect() + "\t"  + ms1.getSE() + "\t" + ms1.getP() + "\t"+ ms2.getEffect() + "\t" + ms2.getSE() + "\t" + ms2.getP() + "\t" +lu.getChi1() + "\t" + chi0 + "\t" + lambda + "\n");
        }
        writer.close();
	}

	private void VerboseGZ(ArrayList<LamUnit> LamArray, double[] ld, int idx1, int idx2)
	{
		ChiSquaredDistributionImpl chiDis = new ChiSquaredDistributionImpl(1);

		NaturalRanking ranking = new NaturalRanking(NaNStrategy.MINIMAL, TiesStrategy.SEQUENTIAL);
        double[] ranks = ranking.rank(ld);
		BufferedWriter GZ = null;
		GZ = FileUtil.ZipFileWriter(new String(lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam.gz"));
    	Logger.printUserLog("Writting detailed test statistics into '"+lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam.gz.'\n");

       	try
		{
			GZ.append(titleLine);
		}
		catch (IOException e)
		{
			Logger.handleException(e, "error in writing " + new String(lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam.gz"));
		}

        for (int i = 0; i < ranks.length; i++)
        {
        	LamUnit lu = LamArray.get(i);
        	MetaStat ms1 = lu.getMS1();
        	MetaStat ms2 = lu.getMS2();
        	double chi0 = 0;
			try
			{
				chi0 = chiDis.inverseCumulativeProbability(ranks[i]/(ranks.length+1));
			}
			catch (MathException e)
			{
				e.printStackTrace();
			}
			double lambda = lu.getChi1()/chi0;
        	try
			{
				GZ.write(ms1.getSNP() + "\t" + ms1.getChr() + "\t" + ms1.getBP() + "\t" + ms1.getA1() + "\t" + ms1.getEffect() + "\t"  + ms1.getSE() + "\t" + ms1.getP() + "\t"+ ms2.getEffect() + "\t" + ms2.getSE() + "\t" + ms2.getP() + "\t" +lu.getChi1() + "\t" + chi0 + "\t" + lambda + "\n");
			}
			catch (IOException e)
			{
				Logger.handleException(e, "error in writing " + new String(lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam.gz"));
			}
        }
        try
		{
			GZ.close();
		}
		catch (IOException e)
		{
			Logger.handleException(e, "error in writing " + new String(lamArgs.getOutRoot() + "." + (idx1+1) + "-" + (idx2+1) + ".lam"));
		}
	}

	private void WriteMat()
	{
		PrintWriter cwriter = null;
		try 
		{
			cwriter = new PrintWriter(new BufferedWriter(new FileWriter(lamArgs.getOutRoot() + ".cm")));
		}
		catch (IOException e)
		{
			Logger.handleException(e, "An I/O exception occurred when writing '" + lamArgs.getOutRoot() + ".cm" + "'.");
		}

		for (int i = 0; i < zMat.length; i++)
		{
			for (int j = 0; j < zMat[i].length; j++)
			{
				cwriter.print(String.format("%.4f", zMat[i][j]) + " ");
			}
			cwriter.println();
		}
		cwriter.close();

		PrintWriter writer = null;
		try 
		{
			writer = new PrintWriter(new BufferedWriter(new FileWriter(lamArgs.getOutRoot() + ".lmat")));
		}
		catch (IOException e)
		{
			Logger.handleException(e, "An I/O exception occurred when writing '" + lamArgs.getOutRoot() + ".lmat" + "'.");
		}


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

		writer.println("Kappa:");
		for (int i = 0; i < kMat.length; i++)
		{
			for (int j = 0; j < kMat[i].length; j++)
			{
				writer.print(String.format("%.4f", kMat[i][j]) + " ");
			}
			writer.println();
		}

		writer.close();
	}

	private void readCMFile()
	{
		int NumMetaFile = gReader.getNumMetaFile();
		BufferedReader bf = BufferedReader.openTextFile(lamArgs.getCMFile(), "cm file.");
		Logger.printUserLog("Reading '" + lamArgs.getCMFile() + "'.");
		zMat = new double[NumMetaFile][NumMetaFile];
		String[] d = null;
		int cnt=0;
		while ( (d = bf.readTokens())!= null ){
			if (d.length != NumMetaFile )
			{
				Logger.printUserError("incorrect '" + lamArgs.getCMFile() + "'.");
				System.exit(0);
			}
			for(int i = 0; i < d.length; i++)
			{
				zMat[cnt][i] = Double.parseDouble(d[i]);
			}
			cnt++;
		}
		
		for(int i = 0; i < zMat.length; i++)
		{
			for(int j = 0; j < zMat[i].length; j++)
			{
				System.out.print(zMat[i][j] + " ");
			}
			System.out.println();
		}
	}

//	private int M = 100;
	private double Me = 30000;

	private double R1 = 1;
	private double R2 = 1;
	private double Kappa = 1;
	private LambdaDCommandArguments lamArgs;

	private String titleLine= "SNP\tChr\tBp\tA1\tBETA1\tSE1\tP1\tBETA2\tSE2\tP2\tChiObs\tChiExp\tLambdaD\n";
//	private int[][] KeyIdx; //snp, chr, bp, beta, se, p, a1, a2

	private GWASReader gReader;
//	private ArrayList<HashMap<String, MetaStat>> meta = NewIt.newArrayList();
//	private ArrayList<ArrayList<String>> SNPArray = NewIt.newArrayList();
//	private HashMap<String, ArrayList<Integer>> MetaSNPTable = NewIt.newHashMap();

	private boolean[] logit;
	private double[][] lamMat;
	private double[][] zMat;
	private double[][] olCtrlMat;
	private double[][] olCsMat;
	private double[][] kMat;	
}
