package simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math.random.RandomDataImpl;
import org.apache.commons.math.stat.StatUtils;

import family.pedigree.PersonIndex;
import family.pedigree.file.SNP;
import family.pedigree.genotype.BPerson;
import family.plink.PLINKBinaryParser;
import family.plink.PLINKParser;
import family.qc.rowqc.SampleFilter;
import gear.CmdArgs;
import gear.util.FileProcessor;
import gear.util.Logger;
import gear.util.NewIt;
import simulation.qc.rowqc.*;
import simulation.gm.RealDataSimulationGenotypeMatrix;

public class RealDataSimulation
{
	private String casualLociFile = null;
	private int[] casualLociIdx = null;
	private double[] b = null;
	private double[][] bv = null;
	private double[][] y = null;
	private int[][] flag = null;
	private double[] T = null;
	private Random rnd;
	private PLINKParser pp = null;
	private SampleFilter sf = null;
	RealDataSimulationGenotypeMatrix GM;
	private int sampleSize = 0;
	private double accept_cs;
	private double accept_ctrl;

	public RealDataSimulation()
	{
		if (CmdArgs.INSTANCE.getTextDataArgs().isSet())
		{
			pp = new PLINKParser(CmdArgs.INSTANCE.getTextDataArgs()
					.getPedFile(), CmdArgs.INSTANCE.getTextDataArgs()
					.getMapFile());
		} else if (CmdArgs.INSTANCE.getBinaryDataArgs(0).isSet())
		{
			pp = new PLINKBinaryParser(CmdArgs.INSTANCE.getBinaryDataArgs(0)
					.getBedFile(), CmdArgs.INSTANCE.getBinaryDataArgs(0)
					.getBimFile(), CmdArgs.INSTANCE.getBinaryDataArgs(0)
					.getFamFile());
		} else
		{
			Logger.printUserError("No input files.");
			System.exit(1);
		}
		pp.Parse();
		sf = new SampleFilter(pp.getPedigreeData(), pp.getMapData());

	}

	public void GenerateSample()
	{
		rnd = new Random(CmdArgs.INSTANCE.simuSeed);
		if (CmdArgs.INSTANCE.simuCasualLoci != null)
		{
			getCasualLoci();
		} else
		{
			getRandomCasualLoci(CmdArgs.INSTANCE.simuRndCasualLoci);
		}
		RealDataSimulationQC rdSimuQC = new RealDataSimulationQC(
				pp.getPedigreeData(), pp.getMapData(), sf);
		GM = new RealDataSimulationGenotypeMatrix(rdSimuQC);
		sampleSize = GM.getGRow();
		T = new double[CmdArgs.INSTANCE.simuRep];
		flag = new int[CmdArgs.INSTANCE.simuRep][];

		accept_cs = (CmdArgs.INSTANCE.simuCC[0] / (1.0 * sampleSize))
				/ CmdArgs.INSTANCE.simuK;
		accept_ctrl = (CmdArgs.INSTANCE.simuCC[1] / (1.0 * sampleSize))
				/ (1 - CmdArgs.INSTANCE.simuK);

		if (accept_cs > 1 || accept_ctrl > 1)
		{
			Logger.printUserLog("It is impossible to generate the case-control sampel with K = "
					+ CmdArgs.INSTANCE.simuK
					+ " with --simu-cc "
					+ CmdArgs.INSTANCE.simuCC[0]
					+ " "
					+ CmdArgs.INSTANCE.simuCC[1]);
		}

		bv = new double[CmdArgs.INSTANCE.simuRep][sampleSize];
		b = generateAddEffects(casualLociIdx.length);
		y = new double[CmdArgs.INSTANCE.simuRep][];
		int cut_off = (int) (sampleSize * (1 - CmdArgs.INSTANCE.simuK));
		for (int i = 0; i < CmdArgs.INSTANCE.simuRep; i++)
		{
			bv[i] = calculateBV(b);
			double v = StatUtils.variance(bv[i]);
			double se = Math.sqrt(v / (CmdArgs.INSTANCE.simuHsq)
					* (1 - CmdArgs.INSTANCE.simuHsq));
			y[i] = generateY(bv[i], se);
			double[] t = new double[sampleSize];
			System.arraycopy(y[i], 0, t, 0, sampleSize);
			Arrays.sort(t);
			T[i] = t[cut_off];
			flag[i] = sample1(T[i], y[i]);
		}

		StringBuffer sb1 = new StringBuffer(CmdArgs.INSTANCE.out);
		sb1.append(".eff");
		PrintStream ps1 = FileProcessor.CreatePrintStream(sb1.toString());
		ArrayList<SNP> snpList = pp.getMapData().getMarkerList();
		for (int i = 0; i < casualLociIdx.length; i++)
		{
			SNP snp = snpList.get(casualLociIdx[i]);
			ps1.append(snp.getName() + " " + snp.getFirstAllele() + " "
					+ snp.getSecAllele() + " " + b[i] + "\n");
		}
		ps1.close();

		StringBuffer sb2 = new StringBuffer(CmdArgs.INSTANCE.out);
		sb2.append(".phen");
		PrintStream ps2 = FileProcessor.CreatePrintStream(sb2.toString());
		ArrayList<PersonIndex> personTable = rdSimuQC.getSample();

		for (int i = 0; i < sampleSize; i++)
		{
			PersonIndex pi = personTable.get(i);
			ps2.append(pi.getFamilyID() + " " + pi.getIndividualID() + " ");
			for (int j = 0; j < CmdArgs.INSTANCE.simuRep; j++)
			{
				ps2.append(flag[j][i] + " ");
			}
			ps2.append("\n");
		}
		ps2.close();
	}

	private int[] sample1(double T, double[] y)
	{
		int[] indicator = new int[y.length];

		int filler = Integer.parseInt(CmdArgs.INSTANCE.missing_phenotype);

		Arrays.fill(indicator, filler);
		int cs = 0;
		int ctrl = 0;
		int i = 0;
		while (cs < CmdArgs.INSTANCE.simuCC[0]
				&& ctrl < CmdArgs.INSTANCE.simuCC[1])
		{
			if (i == y.length)
				i = 0;

			if (indicator[i] == filler)
			{
				double r = rnd.nextFloat();
				if (y[i] < T)
				{
					if (r < accept_ctrl)
					{
						indicator[i] = 1;
						ctrl++;
					}
				} else
				{
					if (r < accept_cs)
					{
						indicator[i] = 2;
						cs++;
					}
				}
			}
			i++;
		}
		return indicator;
	}

	private double[] generateY(double[] bv, double se)
	{
		double[] phe = new double[bv.length];
		for (int i = 0; i < phe.length; i++)
		{
			phe[i] += bv[i] + rnd.nextGaussian() * se;
		}
		return phe;
	}

	private double[] calculateBV(double[] ae)
	{
		double[] bv = new double[sampleSize];
		for (int i = 0; i < bv.length; i++)
		{
			for (int j = 0; j < casualLociIdx.length; j++)
			{
				int idx = casualLociIdx[j];
				int g = GM.getGenotypeScore(i, idx);

				if (g == BPerson.MissingGenotypeCode)
					continue; // leave it alone if it is missing

				bv[i] += ae[j] * GM.getGenotypeScore(i, idx);
			}
		}
		return bv;
	}

	private double[] generateAddEffects(int len)
	{
		double[] effect = new double[len];
		for (int i = 0; i < effect.length; i++)
		{
			effect[i] = rnd.nextGaussian();
		}
		return effect;
	}

	private void getRandomCasualLoci(int num)
	{
		ArrayList<SNP> snpList = pp.getMapData().getMarkerList();

		RandomDataImpl rd = new RandomDataImpl();
		rd.reSeed(CmdArgs.INSTANCE.simuSeed);
		casualLociIdx = rd.nextPermutation(snpList.size(), num);
	}

	private void getCasualLoci()
	{
		ArrayList<String> cl = NewIt.newArrayList();
		ArrayList<SNP> snpList = pp.getMapData().getMarkerList();

		if (CmdArgs.INSTANCE.simuCasualLoci != null)
		{
			casualLociFile = CmdArgs.INSTANCE.simuCasualLoci;
			BufferedReader reader = FileProcessor.FileOpen(casualLociFile);
			String line = null;
			try
			{
				while ((line = reader.readLine()) != null)
				{
					String[] l = line.split("\\s+");
					if (l.length < 1)
						continue;
					cl.add(l[0]);
				}
			} catch (IOException e)
			{
				Logger.handleException(e,
						"An exception occurred when reading the casual-loci file '"
								+ casualLociFile + "'.");
			}
		}

		if (cl.size() == 0)
		{
			casualLociIdx = new int[snpList.size()];
			for (int i = 0; i < casualLociIdx.length; i++)
				casualLociIdx[i] = i;
		} else
		{
			ArrayList<Integer> Idx = NewIt.newArrayList();
			HashSet<String> SS = NewIt.newHashSet();
			for (int i = 0; i < cl.size(); i++)
			{
				SS.add(cl.get(i));
			}
			for (int i = 0; i < snpList.size(); i++)
			{
				SNP snp = snpList.get(i);
				String rs = snp.getName();
				if (SS.contains(rs))
				{
					Idx.add(i);
				}
			}
			Integer[] A = Idx.toArray(new Integer[0]);
			casualLociIdx = ArrayUtils.toPrimitive(A);
		}
	}

}
