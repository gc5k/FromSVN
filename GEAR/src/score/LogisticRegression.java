package score;

import org.apache.commons.math.linear.*;

import test.Test;
/**
 * 
 * @author Guo-Bo Chen, chenguobo@gmail.com
 */
public class LogisticRegression {
	double[] Y;
	double[] P;
	double[][] X;
	double[] B;
	boolean intercept;
	double tolerance = 0.0000001;
	int max_iteration = 50;
	double LogLikelihood_old;
	double LogLikelihood_new;

	public LogisticRegression(double[][] y, double[][] x, boolean intercept) {
		Y = new double[y.length];
		for(int i = 0; i < Y.length; i++) {
			Y[i] = y[i][0];
		}
		initial(x, intercept);
	}

	public LogisticRegression(double[] y, double[][] x, boolean intercept) {
		Y = new double[y.length];
		System.arraycopy(y, 0, Y, 0, Y.length);
		initial(x, intercept);
	}

	private void initial(double[][] x, boolean intercept) {
		P = new double[Y.length];
		this.intercept = intercept;
		int u = intercept ? 1:0;
		X = new double[Y.length][];
		for (int i = 0; i < X.length; i++) {
			X[i] = new double[x == null ? u: x[i].length+u];
			if (intercept) {
				X[i][0] = u;
			}
			if(x != null) {
				System.arraycopy(x[i], 0, X[i], u, x[i].length);
			}
		}
		B = new double[X[0].length];
	}

	public void MLE() {
		RealMatrix Matrix_X = new Array2DRowRealMatrix(X);
		RealMatrix Matrix_XT = Matrix_X.transpose();

		int iter = 0;
		RealMatrix B_old = new Array2DRowRealMatrix(B);
		RealMatrix B_new = new Array2DRowRealMatrix(B);
		calculate_P(B);
		LogLikelihood_old = Likelihood();
		RealMatrix Matrix_W = new Array2DRowRealMatrix(getWMatrix());
		RealMatrix Matrix_XT_W = Matrix_XT.multiply(Matrix_W);
		RealMatrix Matrix_XT_W_X = Matrix_XT_W.multiply(Matrix_X);
		if(!(new LUDecompositionImpl(Matrix_XT_W_X).getSolver().isNonSingular())) {
			System.err.println("covariate matrix is singular.");
			Test.LOG.append("covariate matrix is singular.\n");
			Test.printLog();
			System.exit(0);
		}
		RealMatrix Inv_XT_W_X = new LUDecompositionImpl(Matrix_XT_W_X).getSolver().getInverse();
		RealMatrix Inv_XT_W_X_XT = Inv_XT_W_X.multiply(Matrix_XT);
		do {
			B_old = B_new;
			LogLikelihood_old = LogLikelihood_new;
			B = B_old.getColumn(0);
			RealMatrix Vector_Res = new Array2DRowRealMatrix(getResiduals1());
			RealMatrix Vector_H = Inv_XT_W_X_XT.multiply(Vector_Res);
			B_new = B_old.add(Vector_H);
			calculate_P(B_new.getColumn(0));
			LogLikelihood_new = Likelihood();
//			System.out.println(iter + "--->" + B_new);
//			System.out.println("LogLikelihood_old " + LogLikelihood_old + ", LogLikelihood_new " + LogLikelihood_new);
		} while (iter++ < max_iteration && Math.abs(LogLikelihood_old - LogLikelihood_new) > tolerance);
//		System.out.println();
	}

	public void calculate_P(double[] b) {
		for (int i = 0; i < X.length; i++) {
			double s = 0;
			for (int j = 0; j < X[i].length ; j++) {
				s += X[i][j] * b[j];
			}
			P[i] = Math.exp(s)/(1 + Math.exp(s));
		}
	}

	public double[] getResiduals1() {
		double[] res = new double[Y.length];
		for (int i = 0; i < P.length; i++) {
			res[i] = Y[i] - P[i];
		}
		return res;
	}

	public double[][] getResiduals2() {
		double[][] res = new double[Y.length][1];
		for (int i = 0; i < P.length; i++) {
			res[i][0] = Y[i] - P[i];
		}
		return res;
	}

	public double[][] getWMatrix() {
		double[][] W = new double[P.length][P.length];
		for (int i = 0; i < P.length; i++) {
			W[i][i] = P[i] * (1 - P[i]);
		}
		return W;
	}

	public double Likelihood() {
		double L = 0;
		for (int i = 0; i < P.length; i++) {
			L += Y[i] * Math.log10(P[i]) + (1 - Y[i]) * Math.log10(1 - P[i]);
		}
		return L * (-1);
	}

	public static void main(String[] args) {
		double[] Y = { 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
		double[][] X = { { 1, 15, 4 }, { 1, 30, 14 }, { 1, 31, 16 }, { 1, 31, 11 }, { 1, 32, 17 }, { 1, 29, 10 },
				{ 1, 30, 8 }, { 1, 31, 12 }, { 1, 32, 6 }, { 1, 40, 7 } };
		for(int i = 0; i < 10; i++) {
			LogisticRegression LogReg = new LogisticRegression(Y, X, false);
			LogReg.MLE();
		}
/*		
		String filename = "0.phe";
		FileReader r = null;
		try {
			r = new FileReader(filename);
		} catch(IOException E) {
			E.printStackTrace(System.err);
		}
		ArrayList<ArrayList> missingGenotypeInformation = new ArrayList();
		LineNumberReader l = new LineNumberReader(r);
		BufferedReader b = new BufferedReader(l);
		String line;
		try {
			if ((line = b.readLine()) == null) {
			}
		} catch (IOException E) {
			E.printStackTrace(System.err);
		}
		Vector<String> Phe = new Vector<String>();
		Vector<String> Cov = new Vector<String>();
		try {
			while ((line = b.readLine()) != null) {
				String[] fields = line.split("\t");
				Cov.add(fields[2]);
				Phe.add(fields[3]);
			}
		} catch (IOException E) {
			E.printStackTrace(System.err);
		}

		double[] phe = new double[Phe.size()];
		double[][] cov = new double[Cov.size()][1];
		for(int i = 0; i < phe.length; i++) {
			cov[i][0] = (Double.valueOf((String) Cov.get(i))).doubleValue();
			phe[i] = (Double.valueOf(((String) Phe.get(i)))).doubleValue();
		}
		LogisticRegression LogReg1 = new LogisticRegression(phe, cov, true);
		LogReg1.MLE();
		double[] res = LogReg1.getResiduals1();
		for(int i = 0; i < res.length; i++) {
			System.out.println(res[i]);
		}
		*/
	}
}
