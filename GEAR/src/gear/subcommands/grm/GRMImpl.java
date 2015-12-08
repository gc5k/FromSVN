package gear.subcommands.grm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import gear.data.Person;
import gear.family.pedigree.Hukou;
import gear.family.pedigree.file.MapFile;
import gear.family.pedigree.file.PedigreeFile;
import gear.family.plink.PLINKParser;
import gear.family.popstat.GenotypeMatrix;
import gear.family.qc.rowqc.SampleFilter;
import gear.subcommands.CommandArguments;
import gear.subcommands.CommandImpl;
import gear.sumstat.qc.rowqc.SumStatQC;
import gear.util.FileUtil;
import gear.util.Logger;
import gear.util.pop.PopStat;

public class GRMImpl extends CommandImpl
{
	private double maf_threshold = 1e-5;

	private GenotypeMatrix G;
	private int numMarker;
	private double[][] allelefreq;
	private double[] allelevar;
	private PedigreeFile pf;
	private GRMArguments grmArgs;
	
	@Override
	public void execute(CommandArguments cmdArgs) 
	{
		grmArgs = (GRMArguments) cmdArgs;

		PLINKParser pp = PLINKParser.parse(grmArgs);
//		pp.Parse();
		pf = pp.getPedigreeData();
		SampleFilter sf = new SampleFilter(pp.getPedigreeData(),
				pp.getMapData());
		SumStatQC ssQC = new SumStatQC(pp.getPedigreeData(), pp.getMapData(),
				sf);
		GenotypeMatrix gm = new GenotypeMatrix(ssQC.getSample());
		G = gm;
		numMarker = G.getNumMarker();
		allelefreq = new double[numMarker][3];

		makeGeneticRelationshipScore();
	}

	public void makeGeneticRelationshipScore()
	{
		prepareMAF();
		StringBuffer sb = new StringBuffer();
		sb.append(grmArgs.getOutRoot());
		PrintStream grm = null;
		BufferedWriter grmGZ = null;
		if (grmArgs.isGZ())
		{
			sb.append(".grm.gz");
			grmGZ = FileUtil.ZipFileWriter(sb.toString());
		}
		else
		{
			sb.append(".grm.txt");
			grm = FileUtil.CreatePrintStream(sb.toString());
		}

		for (int i = 0; i < G.getGRow(); i++)
		{
			for (int j = 0; j <= i; j++)
			{
				double[] s = GRMScore(i, j);
				if (grmArgs.isGZ())
				{
					try
					{
						grmGZ.append((i + 1) + "\t" + (j + 1) + "\t" + s[0]
								+ "\t" + s[1] + "\n");
					} 
					catch (IOException e)
					{
						Logger.handleException(e,
								"error in writing '" + sb.toString() + "' for "
										+ (i + 1) + " " + (j + 1) + ".");
					}
				} 
				else
				{
					grm.println((i + 1) + "\t" + (j + 1) + "\t" + s[0] + "\t"
							+ s[1]);
				}
			}
		}

		if (grmArgs.isGZ())
		{
			try
			{
				grmGZ.close();
			} 
			catch (IOException e)
			{
				Logger.handleException(e, " error in closing '" + sb.toString()
						+ "'.");
			}
		} 
		else
		{
			grm.close();
		}
		Logger.printUserLog("Writing GRM scores into '" + sb.toString() + "'.");
		StringBuffer sb_id = new StringBuffer();
		sb_id.append(grmArgs.getOutRoot());
		PrintStream grm_id = null;
		sb_id.append(".grm.id");
		grm_id = FileUtil.CreatePrintStream(sb_id.toString());

		ArrayList<Hukou> H = pf.getHukouBook();
		for (int i = 0; i < H.size(); i++)
		{
			Hukou h = H.get(i);
			grm_id.println(h.getFamilyID() + "\t" + h.getIndividualID());
		}
		grm_id.close();
		Logger.printUserLog("Writing individual information into '"
				+ sb_id.toString() + "'.");
	}

//	public void makeGeneticRelationshipScore(int n0, int n1)
//	{
//		if (n0 < 1 || n1 > (G.getGRow() * G.getGRow()) / 2)
//		{
//			Logger.printUserError("incorrect range for grm pairs : " + n0 + ","
//					+ n1);
//			System.exit(0);
//		} 
//		else
//		{
//			Logger.printUserLog("generating grm scores for the pairs from "
//					+ n0 + " to " + n1);
//		}
//
//		prepareMAF();
//		StringBuffer sb = new StringBuffer();
//		sb.append(CmdArgs.INSTANCE.out);
//		PrintStream grm = null;
//		BufferedWriter grmGZ = null;
//		if (CmdArgs.INSTANCE.makeGRMTXTFlag)
//		{
//			sb.append(".grm.txt");
//			grm = FileUtil.CreatePrintStream(sb.toString());
//		} 
//		else if (CmdArgs.INSTANCE.makeGRMFlag)
//		{
//			sb.append(".grm.gz");
//			grmGZ = FileUtil.ZipFileWriter(sb.toString());
//		}
//
//		int i = 0, j = 0;
//		for (; i < G.getGRow(); i++)
//		{
//			if ((i + 1) * (i + 1 + 1) / 2 >= n0)
//				break;
//		}
//		j = n0 - i * (i + 1) / 2 - 1;
//		int c = n0;
//
//		while (c <= n1)
//		{
//			double[] s = GRMScore(i, j);
//			if (CmdArgs.INSTANCE.makeGRMTXTFlag)
//			{
//				grm.println((i + 1) + "\t" + (j + 1) + "\t" + s[0] + "\t"
//						+ s[1]);
//			} 
//			else
//			{
//				try
//				{
//					grmGZ.append((i + 1) + "\t" + (j + 1) + "\t" + s[0] + "\t"
//							+ s[1] + "\n");
//				} 
//				catch (IOException e)
//				{
//					Logger.handleException(e,
//							"error in writing '" + sb.toString() + "' for "
//									+ (i + 1) + " " + (j + 1) + ".");
//				}
//			}
//			if (j < i)
//			{
//				j++;
//			} 
//			else
//			{
//				i++;
//				j = 0;
//			}
//			c++;
//		}
//
//		if (CmdArgs.INSTANCE.makeGRMTXTFlag)
//		{
//			grm.close();
//		} 
//		else
//		{
//			try
//			{
//				grmGZ.close();
//			} 
//			catch (IOException e)
//			{
//				Logger.handleException(e, " error in closing '" + sb.toString()
//						+ "'.");
//			}
//		}
//		Logger.printUserLog("Writing GRM scores into '" + sb.toString() + "'.");
//		StringBuffer sb_id = new StringBuffer();
//		sb_id.append(CmdArgs.INSTANCE.out);
//		PrintStream grm_id = null;
//		sb_id.append(".grm.id");
//		grm_id = FileUtil.CreatePrintStream(sb_id.toString());
//
//		ArrayList<Hukou> H = pf.getHukouBook();
//		for (int k = 0; k < H.size(); k++)
//		{
//			Hukou h = H.get(k);
//			grm_id.println(h.getFamilyID() + "\t" + h.getIndividualID());
//		}
//		grm_id.close();
//		Logger.printUserLog("Writing individual information into '"
//				+ sb_id.toString() + "'.");
//	}

	private double[] GRMScore(int idx1, int idx2)
	{
		double[] s = { 0, 0 };

		for (int i = 0; i < allelefreq.length; i++)
		{

			if(allelefreq[i][1] == Double.NaN)
			{
				continue;
			}
			int g1 = G.getAdditiveScore(idx1, i);
			int g2 = G.getAdditiveScore(idx2, i);
			double m = allelefreq[i][1];
			if (g1 == Person.MissingGenotypeCode
					|| g2 == Person.MissingGenotypeCode)
			{
				continue;
			}
			else
			{
				if (m < maf_threshold || m > (1-maf_threshold))
				{// ignor too little maf
					continue;
				}
				else
				{
					s[0]++;
					if (grmArgs.isVar())
					{
						s[1] += (g1 - 2 * m) * (g2 - 2 * m) / (allelevar[i]);						
						
					}
					else
					{
						s[1] += (g1 - 2 * m) * (g2 - 2 * m) / (2 * m * (1 - m));						
					}
				}
			}
		}

		if (s[0] > 0)
		{
			s[1] /= s[0];
		}
		else 
		{
			s[0] = 0;
			s[1] = 0;
		}

		return s;
	}

	private void prepareMAF()
	{
//		if (CmdArgs.INSTANCE.ref_freq != null)
//		{
//			getRefFreq();
//		} 
//		else
//		{
		allelefreq = PopStat.calAlleleFrequency(G, numMarker);
		allelevar = PopStat.calGenoVariance(G, numMarker);
//		}
	}

//	private void getRefFreq()
//	{
//		HashMap<String, MAF> refMap = NewIt.newHashMap();
//		gear.util.BufferedReader reader = gear.util.BufferedReader.openTextFile(CmdArgs.INSTANCE.ref_freq, "reference-frequency");
//		reader.readNonEmptyLine();
//		MAF maf;
//		int numSnpsRead = 0;
//		while ((maf = MAF.next(reader)) != null)
//		{
//			refMap.put(maf.getSNP(), maf);
//			++numSnpsRead;
//		}
//		Logger.printUserLog("Read " + numSnpsRead + " SNPs in '" + CmdArgs.INSTANCE.ref_freq + "'.\n");
//
//		ArrayList<SNP> snpList = snpMap.getMarkerList();
//		int c = 0;
//		for (int i = 0; i < snpList.size(); i++)
//		{
//			SNP snp = snpList.get(i);
//			String snpName = snp.getName();
//			double f = 0;
//			if (refMap.containsKey(snpName))
//			{
//				f = refMap.get(snpName).getMAF();
//			}
//			if (allelefreq[i][1] <= 0.5)
//			{
//				if (allelefreq[i][1] < CmdArgs.INSTANCE.maf_range[0]
//						|| allelefreq[i][1] > CmdArgs.INSTANCE.maf_range[1])
//				{
//					allelefreq[i][1] = 0;
//				} 
//				else
//				{
//					allelefreq[i][1] = f;
//				}
//			} 
//			else
//			{
//				if ((1 - allelefreq[i][1]) < CmdArgs.INSTANCE.maf_range[0]
//						|| (1 - allelefreq[i][1]) > CmdArgs.INSTANCE.maf_range[1])
//				{
//					allelefreq[i][1] = 0;
//				} 
//				else
//				{
//					allelefreq[i][1] = f;
//				}
//			}
//
//			c++;
//		}
//		Logger.printUserLog("Got " + c + " matched reference alleles.\n");
//	}

}
