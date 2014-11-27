package gear.gwassummary;

import gear.ConstValues;
import gear.util.BufferedReader;
import gear.util.Logger;
import gear.util.NewIt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;

public class GWASReader
{

	public GWASReader(String[] MetaFile, boolean[] FileKeep, String[] field, boolean isQT, boolean isGZ)
	{
		this.MetaFile = MetaFile;
		this.field = field;
		this.isQT = isQT;
		this.isGZ = isGZ;

		workingMetaFile = NewIt.newArrayList();

		Cohort = 0;
		for (int i = 0; i < FileKeep.length; i++)
		{
			if(FileKeep[i])
			{
				workingMetaFile.add(MetaFile[i]);
				Cohort++;
			}
		}

		if (Cohort == 0)
		{
			Logger.printUserLog("No cohort left. GEAR quitted.");
			System.exit(0);
		}
		else 
		{
			Logger.printUserLog(Cohort + " cohorts are remained for analysis.");
		}

		gc = new double[Cohort];
		Arrays.fill(gc, 1);
		logit = new boolean[Cohort];
		Arrays.fill(logit, false);

		KeyIdx = new int[Cohort][8];
		for (int i = 0; i < KeyIdx.length; i++)
		{
			Arrays.fill (KeyIdx[i], -1);
		}

		int cnt = 0;
		for (int i = 0; i < MetaFile.length; i++)
		{
			if (FileKeep[i])
			{
				HashMap<String, MetaStat> m = readMeta(cnt++);
				MetaStat.add(m);
			}
		}
	}

	public int getCohortNum()
	{
		return Cohort;
	}
	
	public String[] getMetaFile()
	{
		return MetaFile;
	}

	public int[][] getKeyIndex()
	{
		return KeyIdx;
	}

	public int getNumMetaFile()
	{
		return MetaFile.length;
	}

	public ArrayList<HashMap<String, MetaStat>> getMetaStat()
	{
		return MetaStat;
	}

	public ArrayList<ArrayList<String>> getMetaSNPArray() 
	{
		return MetaSNPArray;
	}

	public HashMap<String, ArrayList<Integer>> getMetaSNPTable()
	{
		return MetaSNPTable;
	}

	private HashMap<String, MetaStat> readMeta(int metaIdx)
	{
		ArrayList<Double> pArray = NewIt.newArrayList();
		BufferedReader reader = null;
		if (isGZ)
		{
			reader = BufferedReader.openGZipFile(MetaFile[metaIdx], "Summary Statistic file");
		}
		else
		{
			reader = BufferedReader.openTextFile(MetaFile[metaIdx], "Summary Statistic file");
		}

		String[] tokens = reader.readTokens();
		int tokenLen = tokens.length;

		for(int i = 0; i < tokens.length; i++)
		{
			if (tokens[i].equalsIgnoreCase(field[GWASConstant.SNP]))
			{
				KeyIdx[metaIdx][SNP] = i;
			}
			if (tokens[i].equalsIgnoreCase(field[GWASConstant.CHR]))
			{
				KeyIdx[metaIdx][CHR] = i;
			}
			if (tokens[i].equalsIgnoreCase(field[GWASConstant.BP]))
			{
				KeyIdx[metaIdx][BP] = i;
			}
			if (isQT)
			{
				if (tokens[i].equalsIgnoreCase(field[GWASConstant.BETA]))
				{
					KeyIdx[metaIdx][BETA] = i;
				}
			}
			else
			{
				if(tokens[i].equalsIgnoreCase(field[GWASConstant.BETA]))
				{
					KeyIdx[metaIdx][BETA] = i;
					logit[metaIdx] = false;
				}
				else if (tokens[i].equalsIgnoreCase(field[GWASConstant.OR]))
				{
					KeyIdx[metaIdx][OR] = i;
					logit[metaIdx] = true;
				}
			}
			if (tokens[i].equalsIgnoreCase(field[GWASConstant.SE]))
			{
				KeyIdx[metaIdx][SE] = i;
			}
			if (tokens[i].equalsIgnoreCase(field[GWASConstant.P]))
			{
				KeyIdx[metaIdx][P] = i;
			}
			if (tokens[i].equalsIgnoreCase(field[GWASConstant.A1]))
			{
				KeyIdx[metaIdx][A1] = i;
			}
			if (tokens[i].equalsIgnoreCase(field[GWASConstant.A2]))
            {
				KeyIdx[metaIdx][A2] = i;
			}
		}

		boolean qFlag = false;

		if (KeyIdx[metaIdx][SNP] == -1)
		{
			Logger.printUserLog("Cannot find the snp column in " + MetaFile[metaIdx]);
			qFlag = true;
		}
/*
		if (KeyIdx[metaIdx][CHR] == -1)
		{
			Logger.printUserLog("Cannot find the chr column in " + MetaFile[metaIdx]);
			qFlag = true;
		}
		if (KeyIdx[metaIdx][BP] == -1)
		{
			Logger.printUserLog("Cannot find the bp column in " + MetaFile[metaIdx]);
			qFlag = true;
		}
*/
		if (KeyIdx[metaIdx][BETA] == -1)
		{
			Logger.printUserLog("Cannot find the beta/or column in " + MetaFile[metaIdx]);
		}		
		if (KeyIdx[metaIdx][SE] == -1)
		{
			Logger.printUserLog("Cannot find the se value column in " + MetaFile[metaIdx]);
		}
		if (KeyIdx[metaIdx][P] == -1)
		{
			Logger.printUserLog("Cannot find the p value column in " + MetaFile[metaIdx]);
		}
		if (KeyIdx[metaIdx][A1] == -1)
		{
			Logger.printUserLog("Cannot find the allele 1 column in " + MetaFile[metaIdx]);
		}
//		if (KeyIdx[metaIdx][7] == -1)
//		{
//			Logger.printUserLog("Cannot find the allele 2 in " + MetaFile[metaIdx]);
//		}

		if (qFlag)
		{
			Logger.printUserLog("GEAR quitted.");
			System.exit(0);
		}

		HashMap<String, MetaStat> sumstat = NewIt.newHashMap();
		ArrayList<String> snpArray = NewIt.newArrayList();
		int total = 0;
		int cnt = 0;
		int cntBadChr = 0;
		int cntBadBp = 0;
		int cntBadBeta = 0;
		int cntBadP = 0;
		int cntBadSE = 0;
		int cntBadA1 = 0;
		int cntBadA2 = 0;
		
		while( (tokens = reader.readTokens(tokenLen)) != null)
		{
			total++;
			if (KeyIdx[metaIdx][CHR] != -1 && ConstValues.isNA(tokens[KeyIdx[metaIdx][CHR]]))
			{
				cntBadChr++;
				continue;
			}
			if (KeyIdx[metaIdx][BP] != -1 && ConstValues.isNA(tokens[KeyIdx[metaIdx][BP]]))
			{
				cntBadBp++;
				continue;
			}
			if (ConstValues.isNA(tokens[KeyIdx[metaIdx][BETA]]))
			{
				cntBadBeta++;
				continue;
			}
			if (ConstValues.isNA(tokens[KeyIdx[metaIdx][SE]]))
			{
				cntBadSE++;
				continue;
			}
			if (Float.parseFloat(tokens[KeyIdx[metaIdx][SE]]) <= 0)
			{
				cntBadSE++;
				continue;
			}
			if (ConstValues.isNA(tokens[KeyIdx[metaIdx][P]]))
			{
				cntBadP++;
				continue;
			}
			else
			{
				pArray.add(new Double(Double.parseDouble(tokens[KeyIdx[metaIdx][P]])));
			}
			if (tokens[KeyIdx[metaIdx][A1]].length() != 1)
			{
				cntBadA1++;
				continue;
			}
			if (KeyIdx[metaIdx][A2] != -1 && tokens[KeyIdx[metaIdx][A2]].length() != 1)
			{
				cntBadA2++;
				continue;
			}

			MetaStat ms = null;
			ms = new MetaStat(tokens[KeyIdx[metaIdx][SNP]], Float.parseFloat(tokens[KeyIdx[metaIdx][BETA]]), Float.parseFloat(tokens[KeyIdx[metaIdx][SE]]), Double.parseDouble(tokens[KeyIdx[metaIdx][P]]), tokens[KeyIdx[metaIdx][A1]].charAt(0), logit[metaIdx]);
			if (KeyIdx[metaIdx][CHR] != -1)
			{
				if(tokens[KeyIdx[metaIdx][CHR]].equalsIgnoreCase("X"))
				{
					ms.setChr(23);
				}
				else if(tokens[KeyIdx[metaIdx][CHR]].equalsIgnoreCase("Y"))
				{
					ms.setChr(24);
				}
				else if(tokens[KeyIdx[metaIdx][CHR]].equalsIgnoreCase("XY"))
				{
					ms.setChr(25);
				}				
				else if(tokens[KeyIdx[metaIdx][CHR]].equalsIgnoreCase("MT"))
				{
					ms.setChr(26);
				}
				else 
				{
					int chr = 0;
					try
					{
						chr = Integer.parseInt(tokens[KeyIdx[metaIdx][CHR]]);						
					}
					catch (NumberFormatException e)
					{
						Logger.printUserLog(e.toString() + " in line " + total + " in '" + MetaFile[metaIdx] + ".'");
						continue;
					}
					ms.setChr(chr);					
				}

			}
			if (KeyIdx[metaIdx][BP] != -1)
			{
				int bp = 0;
				try
				{
					bp = Integer.parseInt(tokens[KeyIdx[metaIdx][BP]]);	
				} 
				catch (NumberFormatException e)
				{
					Logger.printUserError(e.toString() + " in line " + total + " in '" + MetaFile[metaIdx] + ".'");
					continue;
				}

				ms.setBP(bp);
			}
			if (KeyIdx[metaIdx][A2] != -1)
			{
				ms.setA2(tokens[KeyIdx[metaIdx][A2]].charAt(0));
			}

			sumstat.put(ms.getSNP(), ms);
			snpArray.add(ms.getSNP());

			if ( MetaSNPTable.containsKey(ms.getSNP()) )
			{
				ArrayList<Integer> snpCnt = MetaSNPTable.get(ms.getSNP());
				snpCnt.set(metaIdx, 1);
				Integer Int = snpCnt.get(snpCnt.size()-1);
				Int++;
				snpCnt.set(snpCnt.size()-1, Int);
			}
			else
			{
				ArrayList<Integer> snpCnt = NewIt.newArrayList();
				snpCnt.ensureCapacity(MetaFile.length+1);
				for(int ii = 0; ii < MetaFile.length + 1; ii++)
				{
					snpCnt.add(0);
				}
				snpCnt.set(metaIdx, 1);
				snpCnt.set(snpCnt.size()-1, 1);
				MetaSNPTable.put(ms.getSNP(), snpCnt);
			}
			cnt++;
		}

		if(cnt == 0)
		{
			Logger.printUserLog("Did not find any summary statistics from '" + MetaFile[metaIdx] + ".'");
			System.exit(0);
		}
		else
		{
			Logger.printUserLog("Read " + cnt +" (of " + total + ")" + " summary statistics from '" + MetaFile[metaIdx] + ".'");			
		}

		if (cntBadChr > 0)
		{
			if (cntBadChr == 1)
			{
				Logger.printUserLog("Removed " + cntBadChr + " locus due to incorrect Chr.");				
			}
			else
			{
				Logger.printUserLog("Removed " + cntBadChr + " loci due to incorrect Chr.");				
			}
		}

		if (cntBadBp > 0)
		{
			if (cntBadBp == 1)
			{
				Logger.printUserLog("Removed " + cntBadBp + " locus due to incorrect Bp.");				
			}
			else
			{
				Logger.printUserLog("Removed " + cntBadChr + " loci due to incorrect Bp.");				
			}
		}

		if (cntBadBeta > 0)
		{
			if (cntBadBeta == 1)
			{
				Logger.printUserLog("Removed " + cntBadBeta + " locus due to incorrect effect.");				
			}
			else
			{
				Logger.printUserLog("Removed " + cntBadBeta + " loci due to incorrect effect.");				
			}
		}

		if (cntBadSE > 0)
		{
			if (cntBadSE == 1)
			{
				Logger.printUserLog("Removed " + cntBadSE + " locus due to incorrect se.");				
			}
			else
			{
				Logger.printUserLog("Removed " + cntBadSE + " loci due to incorrect se.");
			}
		}

		if (cntBadP > 0)
		{
			if (cntBadP == 1)
			{
				Logger.printUserLog("Removed " + cntBadP + " locus due to incorrect p values.");				
			}
			else
			{
				Logger.printUserLog("Removed " + cntBadP + " loci due to incorrect p values.");
			}
		}

		if (cntBadA1 > 0)
		{
			if (cntBadA1 == 1)
			{
				Logger.printUserLog("Removed " + cntBadA1 + " locus due to bad a1 allele.");				
			}
			else
			{
				Logger.printUserLog("Removed " + cntBadA1 + " loci due to bad a1 allele.");
			}
		}
		if(cntBadA2 > 0)
		{
			if (cntBadA2 == 1)
			{
				Logger.printUserLog("Removed " + cntBadA2 + " locus due to bad a2 allele.");
			}
			else
			{
				Logger.printUserLog("Removed " + cntBadA2 + " loci due to bad a2 allele.");				
			}
		}

		MetaSNPArray.add(snpArray);
		gc[metaIdx] = getGC(pArray);

		return sumstat;
	}

	private double getGC(ArrayList<Double> pArray)
	{
		ChiSquaredDistributionImpl chiDis = new ChiSquaredDistributionImpl(1);		

		Collections.sort(pArray);
		double p = 0.5;
		if (pArray.size() % 2 == 0)
		{
			p =1 - (pArray.get(pArray.size()/2) + pArray.get(pArray.size()/2 + 1))/2;
		}
		else
		{
			p =1 - pArray.get((pArray.size()+1)/2);
		}
		double lambda = 1;
		try
		{
			lambda = chiDis.inverseCumulativeProbability(p) / ChiMedianConstant;
		}
		catch (MathException e)
		{
			Logger.printUserError(e.toString());
		}
		Logger.printUserLog("Genomic control factor is: " + lambda);
		return lambda;
	}

	public double[] GetGC()
	{
		return gc;
	}

	public ArrayList<String> getWorkingMetaFile()
	{
		return workingMetaFile;
	}

	private String[] field;
	private boolean isQT;
	private boolean isGZ;
	private boolean[] logit;
	public static int SNP = 0, CHR=1, BP=2, BETA=3, OR=3, SE=4, P=5, A1=6, A2=7;
	private int[][] KeyIdx; //snp, chr, bp, beta, se, p, a1, a2
	private String[] MetaFile;
	private ArrayList<String> workingMetaFile;
	private ArrayList<HashMap<String, MetaStat>> MetaStat = NewIt.newArrayList();
	private ArrayList<ArrayList<String>> MetaSNPArray = NewIt.newArrayList();
	private HashMap<String, ArrayList<Integer>> MetaSNPTable = NewIt.newHashMap();

	private double[] gc;
	private double ChiMedianConstant = 0.4549364;
	private int Cohort;

}
