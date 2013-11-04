package gear.ancestry;

import gear.CmdArgs;
import gear.ConstValues;
import gear.family.pedigree.file.MapFile;
import gear.family.pedigree.file.SNP;
import gear.family.plink.PLINKBinaryParser;
import gear.family.plink.PLINKParser;
import gear.family.popstat.GenotypeMatrix;
import gear.family.qc.rowqc.SampleFilter;
import gear.he.SubjectID;
import gear.sumstat.qc.rowqc.SumStatQC;
import gear.util.BinaryInputFile;
import gear.util.FileUtil;
import gear.util.Logger;
import gear.util.pop.PopStat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;

public class BLUPPCA
{
	
	private final String delim = "\\s+";

	private boolean[] flag;
	private String phenoFile;
	private double[][] phe;
	private double[][] A;

	private HashMap<SubjectID, Integer> id2Idx;

	private MapFile mapFile;
	private SampleFilter sf;
	private SumStatQC ssQC;
	private GenotypeMatrix gm;
	
	public BLUPPCA()
	{

		//read grm
		id2Idx = new HashMap<SubjectID, Integer>();
		readGrmIds(id2Idx);

		if (CmdArgs.INSTANCE.getHEArgs().isGrmBinary())
		{
			readGRMbin();
		}
		else if (CmdArgs.INSTANCE.getHEArgs().isGrm())
		{
			readGRMgz();
		}

		flag = new boolean[id2Idx.size()];
		Arrays.fill(flag, false);

		//read pheno
		phenoFile = CmdArgs.INSTANCE.getHEArgs().getPheno();
		readPhenotypes(id2Idx);

		PLINKParser pp = null;
		if (CmdArgs.INSTANCE.getFileArgs().isSet())
		{
			pp = new PLINKParser(CmdArgs.INSTANCE.getFileArgs()
					.getPed(), CmdArgs.INSTANCE.getFileArgs()
					.getMap());
		}
		else if (CmdArgs.INSTANCE.getBFileArgs(0).isSet())
		{
			pp = new PLINKBinaryParser(CmdArgs.INSTANCE.getBFileArgs(0)
					.getBed(), CmdArgs.INSTANCE.getBFileArgs(0)
					.getBim(), CmdArgs.INSTANCE.getBFileArgs(0)
					.getFam());
		}
		else
		{
			Logger.printUserError("No input files.");
			System.exit(1);
		}
		pp.Parse();

		sf = new SampleFilter(pp.getPedigreeData(),
				pp.getMapData());
		ssQC = new SumStatQC(pp.getPedigreeData(), pp.getMapData(),
				sf);
		mapFile = ssQC.getMapFile();
		gm = new GenotypeMatrix(ssQC.getSample());
		
		System.out.println("geno test:" + gm.getBiAlleleGenotype(0, 0)[0] + gm.getBiAlleleGenotype(0, 0)[1]);
		System.out.println("geno test:" + gm.getBiAlleleGenotype(0, 1)[0] + gm.getBiAlleleGenotype(0, 1)[1]);
		System.out.println("geno test:" + gm.getBiAlleleGenotype(0, 2)[0] + gm.getBiAlleleGenotype(0, 2)[1]);

		System.out.println("geno test:" + gm.getBiAlleleGenotype(1, 0)[0] + gm.getBiAlleleGenotype(1, 0)[1]);
		System.out.println("geno test:" + gm.getBiAlleleGenotype(1, 1)[0] + gm.getBiAlleleGenotype(1, 1)[1]);
		System.out.println("geno test:" + gm.getBiAlleleGenotype(1, 2)[0] + gm.getBiAlleleGenotype(1, 2)[1]);

		//impute
		PopStat.Imputation(gm);
	}

	public void BLUPit()
	{

		double[][] genoMat = new double[gm.getNumIndivdial()][gm.getNumMarker()];
		for(int i = 0; i < genoMat.length; i++)
		{
			for(int j = 0; j < genoMat[i].length; j++)
			{
				genoMat[i][j] = gm.getAdditiveScoreOnFirstAllele(i, j);
			}
		}

		double[][] blupPC = new double[gm.getNumMarker()][phe[0].length];

		RealMatrix grm = new Array2DRowRealMatrix(A);
		RealMatrix grm_Inv = (new LUDecompositionImpl(grm)).getSolver().getInverse();
		
		Logger.printUserLog("Revving up the BLUP machine...");
		RealMatrix tmp = (new Array2DRowRealMatrix(genoMat)).transpose().multiply(grm_Inv);

		for(int i = 0; i < phe[0].length; i++)
		{
			Logger.printUserLog("Calculating blup vector[" + (i+1) + "].");

			double[] Y = new double[phe.length];
			for(int j = 0; j < phe.length; j++)
			{
				Y[j] = phe[j][i];
			}
			RealMatrix B = tmp.multiply(new Array2DRowRealMatrix(Y));

			Logger.printUserLog("Rescaling the snp effects...");
			for(int j = 0; j < B.getRowDimension(); j++)
			{
				blupPC[j][i] = B.getEntry(j, 0);
			}
		}

		StringBuilder fsb = new StringBuilder();
		fsb.append(CmdArgs.INSTANCE.out);
		fsb.append(".blup");
		PrintStream predictorFile = FileUtil.CreatePrintStream(fsb.toString());

		// Title Line
		ArrayList<SNP> snpList = mapFile.getMarkerList();

		predictorFile.print("SNP\tRefAllele");
		for(int i = 0; i < phe[0].length; i++)
		{
			if (i == (phe[0].length - 1)) 
			{
				predictorFile.println("\tBLUP" + (i+1));				
			}
			else
			{
				predictorFile.print("\tBLUP" + (i+1));
			}
		}

		for(int i = 0; i < gm.getNumMarker(); i++)
		{
			SNP snp = snpList.get(i);
			predictorFile.print(snp.getName() + "\t" + snp.getFirstAllele() + "\t");
			for(int j = 0; j < blupPC[i].length; j++)
			{
				if (j == (blupPC[i].length - 1))
				{
					predictorFile.println(blupPC[i][j]);
				}
				else
				{
					predictorFile.print(blupPC[i][j]+"\t");
				}
			}
		}
		predictorFile.close();
	}

	private void readGrmIds(HashMap<SubjectID, Integer> id2Idx)
	{

		gear.util.BufferedReader reader = gear.util.BufferedReader.openTextFile(CmdArgs.INSTANCE.getHEArgs().getGrmId(), "GRM-ID");
		int idx = 0;
		String[] tokens;
		while ((tokens = reader.readTokens(2)) != null)
		{
			id2Idx.put(new SubjectID(tokens[0], tokens[1]), idx++);
		}
		Logger.printUserLog("individuals in grm id file: " + id2Idx.size());
		reader.close();
	}

	private void readGRMbin()
	{
		BinaryInputFile grmBin = new BinaryInputFile(CmdArgs.INSTANCE.getHEArgs().getGrm(), "GRM");
		grmBin.setLittleEndian(true);
		A = new double[id2Idx.size()][id2Idx.size()];
		Logger.printUserLog("Constructing A matrix: a " + id2Idx.size() + " X " + id2Idx.size() + " matrix.");
		for (int i = 0; i < A.length; i++) 
		{
			for (int j = 0; j <= i; j++)
			{
				if (grmBin.available() >= ConstValues.FLOAT_SIZE)
				{
					A[i][j] = A[j][i] = grmBin.readFloat();
				}
			}
		}
	}

	private void readGRMgz()
	{
		A = new double[id2Idx.size()][id2Idx.size()];
		FileInputStream fin = null;
		try
		{
			fin = new FileInputStream(CmdArgs.INSTANCE.getHEArgs()
					.getGrm());
		}
		catch (FileNotFoundException e)
		{
			Logger.handleException(e, "Cannot open the GRM file '"
					+ CmdArgs.INSTANCE.getHEArgs().getGrm() + "'.");
		}

		GZIPInputStream gzis = null;
		try
		{
			gzis = new GZIPInputStream(fin);
		} 
		catch (IOException e)
		{
			Logger.handleException(e, "Cannot open the GRM archive '"
					+ CmdArgs.INSTANCE.getHEArgs().getGrm() + "'.");
		}
		InputStreamReader xover = new InputStreamReader(gzis);

		BufferedReader grmFile = new BufferedReader(xover);

		String line;
		try
		{
			for (int i = 0; i < A.length; i++)
			{
				for (int j = 0; j <= i; j++)
				{
					if ((line = grmFile.readLine()) != null) 
					{
						String[] s = line.split(delim);
						A[i][j] = A[j][i] = Double.parseDouble(s[3]);
					}
				}
			}
		} 
		catch (IOException e)
		{
			Logger.handleException(e,
					"An exception occurred when reading the GRM archive '"
							+ CmdArgs.INSTANCE.getHEArgs().getGrm()
							+ "'.");
		}

	}

	private void readPhenotypes(HashMap<SubjectID, Integer> id2Idx)
	{
		Logger.printUserLog("reading phentoypes from '" + phenoFile + "'");
		gear.util.BufferedReader reader = gear.util.BufferedReader.openTextFile(phenoFile, "phenotype");
				
		@SuppressWarnings("unchecked")
		HashMap<SubjectID, Integer> subjectsUnread = (HashMap<SubjectID, Integer>)id2Idx.clone();

		HashSet<SubjectID> subjectsRead = new HashSet<SubjectID>();

		String[] tokens = null;

		int c = 0;
		while ((tokens = reader.readTokens()) != null)
		{

			if (tokens.length < 3)
			{
				reader.errorPreviousLine("There should be at least " + 3 + " columns.");
			}
			if (c == 0) 
			{
				phe = new double[id2Idx.size()][tokens.length - 2];
				c++;
				for( int i = 0; i < id2Idx.size(); i++)
				{
					Arrays.fill(phe[i], -9);
				}
			}

			SubjectID subID = new SubjectID(/*famID*/tokens[0], /*indID*/tokens[1]);

			int ii = 0;
			if (subjectsUnread.containsKey(subID))
			{
				ii = subjectsUnread.get(subID);
				boolean f = true;
				String pheValStr = null;
				try
				{
					for( int i = 0; i < phe[ii].length; i++)
					{
						pheValStr = tokens[2 + i];
						if (ConstValues.isNA(pheValStr))
						{
							f = false;
							break;
						}
						phe[ii][i] = Double.parseDouble(pheValStr);							
					}
				}
				catch (NumberFormatException e)
				{
					reader.errorPreviousLine("'" + pheValStr + "' is not a valid phenotype value. It should be a floating point number.");
				}
				flag[ii] = f;

				subjectsUnread.remove(subID);
				subjectsRead.add(subID);
			}
			else if (subjectsRead.contains(subID))
			{
				reader.errorPreviousLine("Individual " + subID + " is repeated.");
			}
			else
			{
				flag[ii] = false;
//				reader.errorPreviousLine("Individual " + subID + " appears in the phenotype file but not in the grm id file(s).");
			}
		}
		reader.close();

		if (!subjectsUnread.isEmpty())
		{
			String msg = "";
			msg += subjectsUnread.size() + " individual(s) (e.g. " + subjectsUnread.keySet().iterator().next();
			msg += ") appear in the grm id file(s) but not in the phenotype file";
			Logger.printUserError(msg);
		}
	}
}
