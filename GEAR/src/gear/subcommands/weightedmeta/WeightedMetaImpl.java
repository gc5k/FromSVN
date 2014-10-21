package gear.subcommands.weightedmeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;

import gear.gwassummary.GWASReader;
import gear.gwassummary.MetaStat;
import gear.subcommands.CommandArguments;
import gear.subcommands.CommandImpl;
import gear.subcommands.lambdaD.LambdaDCommandArguments;
import gear.util.BufferedReader;
import gear.util.Logger;
import gear.util.SNPMatch;

public class WeightedMetaImpl extends CommandImpl
{

	@Override
	public void execute(CommandArguments cmdArgs)
	{
		// TODO Auto-generated method stub
		readCMFile();
		MetaAnalysis();

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
