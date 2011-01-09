package score;
import org.apache.commons.math.linear.*;
public class LinearRegression {
	double[] Y;
	double[] P;
	double[][] X;
	double[][] Fisher;
	double[] T_statistic;
	double[] B;

	boolean intercept;
	double[] fitted;
	double[] res;
	
	public LinearRegression (double[][] y, double[][] x, boolean intercept) {
		Y = new double[y.length];
		for (int i = 0; i < Y.length; i++) {
			Y[i] = y[i][0];
		}
		initial(x, intercept);
	}
	
	public LinearRegression (double[] y, double[][] x, boolean intercept) {
		Y = new double[y.length];
		System.arraycopy(y, 0, Y, 0, Y.length);
		initial(x, intercept);
	}
	
	private void initial(double[][] x, boolean intercept) {
		fitted = new double[x.length];
		res = new double[x.length];
		X = new double[x.length][];
		this.intercept = intercept;
		int u = intercept ? 1:0;
		for (int i = 0; i < x.length; i++) {
			X[i] = new double[x[i].length+u];
			if (intercept) {
				X[i][0] = u;
			}
			System.arraycopy(x[i], 0, X[i], u, x[i].length);
		}
		T_statistic = new double[X[0].length];
	}
	
	public void MLE() {
		RealMatrix Matrix_Y = new RealMatrixImpl(Y);
		RealMatrix Matrix_X = new RealMatrixImpl(X);
		RealMatrix Matrix_XT = Matrix_X.transpose();
		RealMatrix Matrix_XT_X = Matrix_XT.multiply(Matrix_X);
		if (Matrix_XT_X.isSingular()) {
			throw new IllegalArgumentException(
					"The covariate matrix is singular.");
		}
		RealMatrix Matrix_XT_X_Ivt = Matrix_XT_X.inverse();
		RealMatrix Matrix_XT_X_Ivt_XT = Matrix_XT_X_Ivt.multiply(Matrix_XT);
		RealMatrix b = Matrix_XT_X_Ivt_XT.multiply(Matrix_Y);
		RealMatrix Fitted = Matrix_X.multiply(b);
		RealMatrix Res = Matrix_Y.subtract(Fitted);
		RealMatrix Res_T = Res.transpose();
		RealMatrix SSE = Res_T.multiply(Res);
		double mse = SSE.getEntry(0, 0)/(Matrix_Y.getRowDimension() - Matrix_X.getColumnDimension());
		RealMatrix Matrix_Fisher = Matrix_XT_X_Ivt.scalarMultiply(mse);
		Fisher = Matrix_Fisher.getData();
		for (int i = 0; i < T_statistic.length; i++) {
			T_statistic[i] = b.getEntry(i, 0)/Math.sqrt(Fisher[i][i]);
		}
		res = Res.getColumn(0);
		fitted = Fitted.getColumn(0);
		B = b.getColumn(0);
	}

	public double[] getResiduals1() {
		return res;
	}
	
	public double[][] getResiduals2() {
		double[][] res2 = new double[res.length][1];
		for(int i = 0; i < res2.length; i++) {
			res2[i][0] = res[i];
		}
		return res2;
	}

	public static void main(String[] args) {
		double[] Y = { 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
		double[][] X = { { 1, 15, 4 }, { 1, 30, 14 }, { 1, 31, 16 }, { 1, 31, 11 }, { 1, 32, 17 }, { 1, 29, 10 },
				{ 1, 30, 8 }, { 1, 31, 12 }, { 1, 32, 6 }, { 1, 40, 7 } };
		LinearRegression LR = new LinearRegression(Y, X, false);
		LR.MLE();
		double[][] r = LR.getResiduals2();
		for(int i = 0; i < r.length; i++) {
			System.out.println(r[i][0]);
		}
		System.out.println();
	}
}
