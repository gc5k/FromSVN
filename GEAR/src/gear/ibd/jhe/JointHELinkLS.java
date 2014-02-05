package gear.ibd.jhe;

import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;

import gear.CmdArgs;
import gear.data.PhenotypeFile;
import gear.data.SubjectID;
import gear.util.BufferedReader;
import gear.util.FileUtil;
import gear.util.Logger;
import gear.util.NewIt;

public class JointHELinkLS
{
	private String ibdFile;
	private String pheFile;
	private int pheIdx = 0;
	private ArrayList<SubjectID> ibdID1 = NewIt.newArrayList();
	private ArrayList<SubjectID> ibdID2 = NewIt.newArrayList();
	private double[][] pibd;
	private double[][] mibd;

	private ArrayList<Integer> keepIBD = NewIt.newArrayList();
	
	// PhenotypeFile is used here instead of InputDataSet,
	// because the IBD file format is not consistent with the design
	// purpose of InputDataSet and cannot be moved into InputDataSet.
	private PhenotypeFile phe;
	private double[] Y;

	public JointHELinkLS ()
	{
		pheFile = CmdArgs.INSTANCE.getHEArgs().getPheno();
		ibdFile = CmdArgs.INSTANCE.ibdFile;
		pheIdx = CmdArgs.INSTANCE.getHEArgs().getTargetTraitOptionValue() -1;
		initial();
	}

	public void initial()
	{
		readPhenotypes();
		readIBD();
		lineup();
	}

	private void lineup()
	{
		//need to take care of the missing data
		ArrayList<Double> HEphe = NewIt.newArrayList();
		for (int i = 0; i < ibdID1.size(); i++)
		{
			int idx1 = phe.getSubjectIndex(ibdID1.get(i));
			int idx2 = phe.getSubjectIndex(ibdID2.get(i));
			if (!phe.isMissing(idx1, pheIdx) && !phe.isMissing(idx2, pheIdx))
			{
				double d = phe.getPhenotype(idx1, pheIdx) - phe.getPhenotype(idx2, pheIdx);
				HEphe.add(d*d);
				keepIBD.add(i);
			}
		}

		//make ibd
		double[][] tpibd = new double[keepIBD.size()][];
		double[][] tmibd = new double[keepIBD.size()][];
		
		for(int originalIdx : keepIBD)
		{
			int newIdx = keepIBD.get(originalIdx).intValue();
			tpibd[newIdx] = pibd[newIdx];
			tmibd[newIdx] = mibd[newIdx];
		}
		pibd = tpibd;
		mibd = tmibd;

		//make Y
		Y = new double[HEphe.size()];
		for (int i = 0; i < Y.length; i++)
		{
			Y[i] = HEphe.get(i);
		}

		Logger.printUserLog(keepIBD.size() + " phenotypes matched with IBD scores.");
	}

	public void JHE()
	{
		JHEpm();
	}

	private void JHEAve()
	{
		
	}

	private void JHEpm()
	{
		String outFileName = CmdArgs.INSTANCE.out + ".helink";
		PrintStream ps = FileUtil.CreatePrintStream(outFileName);
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		Logger.printUserLog("Started scannning...");
		ps.println("mean\tb1(paternal)\tb2(maternal)");
		for (int i = 0; i < pibd[0].length; i++)
		{
			double [][] x = new double[Y.length][2];
			for (int j = 0; j < pibd.length; j++)
			{
				x[j][0] = pibd[j][i];
				x[j][1] = mibd[j][i];
			}
			regression.newSampleData(Y, x);
			double b[] = regression.estimateRegressionParameters();
			
			for (int j = 0; j < b.length; j++)
			{
				ps.print(b[j] + " ");
			}
			ps.println();
		}
		ps.close();
		Logger.printUserLog("Finished HE scanning. The result has been saved in " + outFileName);
	}
	
	
	private void readPhenotypes()
	{
		phe = new PhenotypeFile(pheFile);
		if (phe.getNumberOfTraits() < pheIdx)
		{
			Logger.printUserError("the index for the selected phenotype is too large! Only " + phe.getNumberOfTraits() + " phenotypes.");
			System.exit(1);
		}
	}
	
	private void readIBD()
	{
		BufferedReader reader = BufferedReader.openTextFile(ibdFile, "ibd");

		String[] tokens1 = reader.readTokensAtLeast(4), tokens2;
		
		int numCols = tokens1.length;
		if (numCols % 2 != 0)
		{
			reader.errorPreviousLine("Odd number of columns (" + numCols + " columns).");
		}
		
		ArrayList<double[]> pibd = new ArrayList<double[]>();
		ArrayList<double[]> mibd = new ArrayList<double[]>();

		do
		{
			double[] ibdThisDad = new double[numCols - 4];
			for (int j = 4; j < numCols; ++j)
			{
				try
				{
					ibdThisDad[j - 4] = Double.parseDouble(tokens1[j]);
				}
				catch (NumberFormatException e)
				{
					reader.errorPreviousLine("'" + tokens1[j] + "' is not a valid floating point number.");
				}
			}
			pibd.add(ibdThisDad);

			tokens2 = reader.readTokens(numCols);

			SubjectID id11 = new SubjectID(tokens1[0], tokens1[1]);
			SubjectID id12 = new SubjectID(tokens1[2], tokens1[3]);
			SubjectID id21 = new SubjectID(tokens2[0], tokens2[1]);
			SubjectID id22 = new SubjectID(tokens2[2], tokens2[3]);

			if (id11 != id21 || id12 != id22)
			{
				reader.errorPreviousLine("The IDs in this line and the above line do not match.");
			}

			ibdID1.add(id11);
			ibdID2.add(id12);

			double[] ibdThisMom = new double[numCols - 4];
			for (int j = 4; j < numCols; ++j)
			{
				try
				{
					ibdThisMom[j - 4] = Double.parseDouble(tokens2[j]);
				}
				catch (NumberFormatException e)
				{
					reader.errorPreviousLine("'" + tokens2[j] + "' is not a valid floating point number.");
				}
			}
			mibd.add(ibdThisMom);
		} while ((tokens1 = reader.readTokens(numCols)) != null);

		this.pibd = pibd.toArray(new double[0][]);
		this.mibd = mibd.toArray(new double[0][]);

		Logger.printUserLog("Read " + ibdID1.size() + " pairs in " + ibdFile);
	}
}
