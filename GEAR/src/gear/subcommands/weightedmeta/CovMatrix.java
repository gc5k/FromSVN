package gear.subcommands.weightedmeta;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;

import gear.gwassummary.GWASReader;
import gear.gwassummary.MetaStat;

public class CovMatrix
{
	public CovMatrix(String snp, ArrayList<Integer> Int, double[][] corMat, GWASReader gReader, boolean isGC)
	{
		this.snp = snp;
		this.cohort = Int.get(Int.size() -1).intValue();
		this.cohortIdx = new int[cohort];
		this.isGC = isGC;
		this.gc = new double[cohort];

		int cnt = 0;
		for(int i = 0; i < Int.size()-1; i++)
		{
			if(Int.get(i) == 0) continue;
			cohortIdx[cnt++] = i;
		}

		if(this.isGC)
		{
			double[] Egc = gReader.GetGC();
			for(int i = 0; i < cohortIdx.length; i++)
			{
				gc[i] = Egc[cohortIdx[i]];
				if(this.isGC)
				{
					gc[i] = gc[i] > 1 ? gc[i]:1;
				}
			}
		}
		else
		{
			Arrays.fill(this.gc, 1);
		}

//		if(this.cohort == (Int.size() - 1))
//		{
//			System.out.println(this.cohort);
//			RealMatrix gt = new Array2DRowRealMatrix(corMat);
//
//			boolean isNonSingular = (new LUDecompositionImpl(gt)).getSolver().isNonSingular();
//			System.out.println(isNonSingular);
//			double[] eigent = (new EigenDecompositionImpl(gt, 0.00000001)).getRealEigenvalues();
//			System.out.println("test" + eigent);
//			for(int i = 0; i < eigent.length; i++)
//			{
//				System.out.print("[" + (i+1) + "]"+eigent[i] + " ");
//			}
//			System.out.println("====");
//			double dett = new LUDecompositionImpl(gt).getDeterminant();
//			System.out.println("test det=" + dett);
//
//			System.exit(0);
//		}

		double[][] covMat = new double[cohort][cohort];
		for(int i = 0; i < cohortIdx.length; i++)
		{
			MetaStat ms1 = gReader.getMetaStat().get(cohortIdx[i]).get(snp);
			for(int j = 0; j < cohortIdx.length; j++)
			{
				MetaStat ms2 = gReader.getMetaStat().get(cohortIdx[j]).get(snp);
				covMat[i][j] = corMat[cohortIdx[i]][cohortIdx[j]] * ms1.getSE() * ms2.getSE() * Math.sqrt(gc[i]) *  Math.sqrt(gc[j]);
			}
		}

		RealMatrix gg = new Array2DRowRealMatrix(covMat);

		boolean isNonSingular = (new LUDecompositionImpl(gg)).getSolver().isNonSingular();
		System.out.println(isNonSingular);
		EigenDecompositionImpl EI= new EigenDecompositionImpl(gg, 0.00000001);
		for(int i = 0; i < this.cohort; i++)
		{
			System.out.print(EI.getRealEigenvalue(i) + " ");
		}
		System.out.println();


		double det = new LUDecompositionImpl(gg).getDeterminant();
		System.out.println("det=" + det);

		RealMatrix gg_Inv = (new LUDecompositionImpl(gg)).getSolver().getInverse();
		RealMatrix Unit = new Array2DRowRealMatrix(covMat.length, 1);
		for(int i = 0; i < Unit.getRowDimension(); i++)
		{
			Unit.setEntry(i, 0, 1);
		}
		RealMatrix tmp = Unit.transpose().multiply(gg_Inv);
		RealMatrix tmp1 = tmp.multiply(Unit);
		RealMatrix W = tmp.scalarMultiply(1/tmp1.getEntry(0, 0));

		double gse1 = 1/tmp1.getEntry(0, 0);
		for (int i = 0; i < covMat.length; i++)
		{
			for (int j = 0; j < covMat[i].length; j++)
			{
//				gse += covMat[i][j];
				gse += W.getEntry(0, i) * W.getEntry(0, j) * covMat[i][j];
			}
		}
		System.out.println("V: " + gse + " V1:" + gse1);
		gse = Math.sqrt(gse);
		Weight = W.getRow(0);
//		System.out.println(W);
	}

	public double[][] getCovMatrix()
	{
		return covMat;
	}

	public double[] getWeights()
	{
		return Weight;
	}

	public double getGSE()
	{
		return gse;
	}

	public int[] getCohortIdx()
	{
		return cohortIdx;
	}

	public String getSNP()
	{
		return snp;
	}

	private boolean isGC;
	private double[] gc;
	private String snp;
	private double[][] covMat;
	private double gse = 0;
	private int cohort;
	private double[] Weight;
	private int[] cohortIdx;
}
