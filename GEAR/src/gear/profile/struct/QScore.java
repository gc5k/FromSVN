package gear.profile.struct;

import gear.Parameter;
import gear.util.BufferedReader;
import gear.util.Logger;

public class QScore {

	private String SNP;
	private double qscore;
	private boolean isMissing;

	public QScore(String snp) {
		SNP = snp;
		isMissing = true;
	}
	
	public QScore(String snp, double qScore) {
		this.SNP = snp;
		this.qscore = qScore;
		isMissing = false;
	}

	public String getSNP() {
		return SNP;
	}

	public double getQScore() {
		return qscore;
	}

	public boolean isMissing() {
		return isMissing;
	}
	
	public static QScore getNextQScore(BufferedReader reader) {
		QScore qScore = null;
		
		while (true) {
			String tokens[] = reader.readTokens();
			
			if (tokens == null) {
				break;
			}
			
			if (tokens.length != 2) {
				Logger.printUserError(
					"The format of the q-score file '" + reader.getFileName() + "' is incorrect: " +
					"A q-score file should consists of 2 columns, but line " + reader.getCurLineNum() + " contains " + tokens.length + " column(s)."
				);
				System.exit(1);
			}
			
			if (Parameter.INSTANCE.isNA(tokens[1])) {
				qScore = new QScore(/* SNP = */ tokens[0]);
			}
			else {
				try {
					qScore = new QScore(/* SNP = */ tokens[0], /* q-score = */ Double.parseDouble(tokens[1]));
				}
				catch (NumberFormatException e) {
					Logger.handleException(e,
						"Line " + reader.getCurLineNum() + " of the q-score file '" + reader.getFileName() + "' " +
					    "contains an error: '" + tokens[1] + "' is not a valid q-score."
					);
				}
			}
			
			break;
		}
		
		return qScore;
	}

}
