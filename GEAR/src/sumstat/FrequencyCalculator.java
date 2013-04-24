package sumstat;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.inference.ChiSquareTestImpl;

import sumstat.qc.rowqc.SumStatQC;

import family.pedigree.file.MapFile;
import family.pedigree.file.SNP;
import family.plink.PLINKBinaryParser;
import family.plink.PLINKParser;
import family.popstat.GenotypeMatrix;
import family.qc.rowqc.SampleFilter;
import gear.CmdArgs;
import gear.util.Logger;
import gear.util.stat.FastFisherExactTest;

public class FrequencyCalculator
{
	private GenotypeMatrix G;
	private int numMarker;
	private double[][] allelefreq;
	private double[][] genotypefreq;
	private MapFile snpMap;

	private double[][] hw;
	private int[][] N;

	public FrequencyCalculator()
	{
		PLINKParser pp = null;
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
		SampleFilter sf = new SampleFilter(pp.getPedigreeData(),
				pp.getMapData());
		SumStatQC ssQC = new SumStatQC(pp.getPedigreeData(), pp.getMapData(),
				sf);
		snpMap = pp.getMapData();
		GenotypeMatrix gm = new GenotypeMatrix(ssQC.getSample());
		setup(gm);
	}

	public void setup(GenotypeMatrix gm)
	{
		G = gm;
		numMarker = G.getNumMarker();
		allelefreq = new double[numMarker][3];
		genotypefreq = new double[numMarker][4];
		hw = new double[numMarker][2];
		N = new int[numMarker][4];
	}

	public void CalculateAlleleFrequency()
	{
		int[][] g = G.getG();
		for (int i = 0; i < g.length; i++)
		{
			for (int j = 0; j < numMarker; j++)
			{
				int[] c = G.getBiAlleleGenotype(i, j);
				allelefreq[j][c[0]]++;
				allelefreq[j][c[1]]++;
				int idx = G.getAdditiveScore(i, j);
				genotypefreq[j][idx]++;
				if (idx == 0)
				{
					N[j][0] += 2;
					N[j][3]++;
				} else if (idx == 1)
				{
					N[j][0] += 1;
					N[j][1] += 1;
					N[j][2] += 1;
					N[j][3]++;
				} else if (idx == 2)
				{
					N[j][2] += 1;
					N[j][3]++;
				}
			}
		}

		for (int i = 0; i < numMarker; i++)
		{

			double wa = allelefreq[i][0] + allelefreq[i][1];
			double a = allelefreq[i][0] + allelefreq[i][1] + allelefreq[i][2];
			if (wa > 0)
			{
				for (int j = 0; j < allelefreq[i].length - 1; j++)
				{
					allelefreq[i][j] /= wa;
				}
				allelefreq[i][2] /= a;
			} else
			{
				allelefreq[i][2] = 1;
			}

			double wb = genotypefreq[i][0] + genotypefreq[i][1]
					+ genotypefreq[i][2];
			double b = genotypefreq[i][0] + genotypefreq[i][1]
					+ genotypefreq[i][2] + genotypefreq[i][3];
			if (wb > 0)
			{
				for (int j = 0; j < genotypefreq[i].length - 1; j++)
				{
					genotypefreq[i][j] /= wb;
				}
				genotypefreq[i][3] /= b;
			} else
			{
				genotypefreq[i][3] = 1;
			}

			FastFisherExactTest ff = new FastFisherExactTest(N[i][3], N[i][1],
					N[i][0]);
			hw[i][0] = ff.HDP();
			hw[i][1] = chiHWE(N[i][3], N[i][1], N[i][0]);

		}
	}

	public double chiHWE(int N, int NAB, int NA)
	{
		int n = N;
		int na = NA;
		int nb = 2 * N - na;

		long[] O = { (na - NAB) / 2, NAB, (nb - NAB) / 2 };
		double n_d = n * 1.0;
		double na_d = na * 1.0;
		double nb_d = nb * 1.0;
		double[] E = { na_d * na_d / (4 * n_d), na_d * nb_d * 2 / (4 * n_d),
				nb_d * nb_d / (4 * n_d) };

		ChiSquareTestImpl chi = new ChiSquareTestImpl();
		double p = 0;
		try
		{
			p = chi.chiSquareTest(E, O);
		} catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		} catch (MathException e)
		{
			e.printStackTrace();
		}
		return p;
	}

	public double[][] getAlleleFrequency()
	{
		return allelefreq;
	}

	public String toString()
	{
		NumberFormat fmt = new DecimalFormat(".###E0");

		StringBuffer sb = new StringBuffer();
		if (CmdArgs.INSTANCE.freqFlag)
		{
			sb.append("chr\tsnp\tA1\tA2\tfrq(A1)\tfrq(A2)\tMiss\tNChr");
		} else
		{
			sb.append("chr\tsnp\tA1\tA2\tfrq(A1A1)\tfrq(A1A2)\tfrq(A2A2)\tMiss\tNChr\tp(Fisher)");
		}
		sb.append(System.getProperty("line.separator"));
		for (int i = 0; i < allelefreq.length; i++)
		{
			SNP snp = snpMap.getSNP(i);
			sb.append(snp.getChromosome() + "\t" + snp.getName() + "\t"
					+ snp.getFirstAllele() + "\t" + snp.getSecAllele() + "\t");
			if (CmdArgs.INSTANCE.freqFlag)
			{
				sb.append(fmt.format(allelefreq[i][0]) + "\t"
						+ fmt.format(allelefreq[i][1]) + "\t"
						+ fmt.format(allelefreq[i][2]) + "\t" + N[i][3] * 2);
			} else if (CmdArgs.INSTANCE.genoFreqFlag)
			{
				sb.append(fmt.format(genotypefreq[i][0]) + "\t"
						+ fmt.format(genotypefreq[i][1]) + "\t"
						+ fmt.format(genotypefreq[i][2]) + "\t" + N[i][3] * 2
						+ "\t" + hw[i][0]);
			}
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}
}
