package statistics.FisherExactTest;

import java.util.Map.Entry;

import admixture.parameter.Parameter;
import family.mdr.result.Combination;
import family.mdr.result.Suite;

public class MDRTruncatedExactTest {

	private Combination model;
	private double T;
	private int H; // number of high risk groups
	private int L; // number of low risk groups
	private int HPos; // positive individuals in high groups
	private int HNeg; // negative individuals in high groups;
	private int LPos; // positive individuals in low groups;
	private int LNeg; // negative individuals in low groups;
	private int[][] HSub;
	private int[][] LSub;

	private double pbase;
	private double ptruncated;
	private double pobs;
	private double pInt;
	private double pOneTail;

	private int[][] confusion = { { 0, 0 }, { 0, 0 } };

	// definition of the confusion table
	// | Hgroup | Lgroup
	// --------------------------
	// HGeno | a (HPos) | b (LPos)|
	// --------------------------
	// LGeno | c (HNeg) | d (LNeg)|
	// --------------------------

	public MDRTruncatedExactTest(Combination model) {
		this.model = model;
		initial();
		ExactTest();
	}

	private void initial() {
		for (Entry<String, Suite> entry : model.entrySet()) {
			Suite s = entry.getValue();
			if (Suite.Ascertainment(s.getPositiveSubjects(), s.getNegativeSubjects()) == 1) {
				H++;
				HPos += s.getPositiveSubjects();
				HNeg += s.getNegativeSubjects();
			} else if (Suite.Ascertainment(s.getPositiveSubjects(), s.getNegativeSubjects()) == 0) {
				L++;
				LPos += s.getPositiveSubjects();
				LNeg += s.getNegativeSubjects();
			} else {

			}
		}
		HSub = new int[H][2];
		LSub = new int[L][2];

		int c1 = 0;
		int c2 = 0;
		for (Entry<String, Suite> entry : model.entrySet()) {
			Suite s = entry.getValue();
			if (Suite.Ascertainment(s.getPositiveSubjects(), s.getNegativeSubjects()) == 1) {
				HSub[c1][0] = s.getPositiveSubjects();
				HSub[c1][1] = s.getNegativeSubjects();
				c1++;
			} else if (Suite.Ascertainment(s.getPositiveSubjects(), s.getNegativeSubjects()) == 0) {
				LSub[c2][0] = s.getPositiveSubjects();
				LSub[c2][1] = s.getNegativeSubjects();
				c2++;
			}
		}
		T = 1.0 * (HPos + LPos) / (HNeg + LNeg);
		if (H >= L) {
			for (int i = 0; i < HSub.length; i++) {
				confusion[0][0] += getNumSubject(true, HSub[i][0], HSub[i][1], T/(1 + T));
			}
			confusion[1][0] = (HPos + HNeg) - confusion[0][0];
			confusion[0][1] = (HPos + LPos) - confusion[0][0];
			confusion[1][1] = (HNeg + LNeg) - confusion[1][0];
		} else {
			for (int i = 0; i < LSub.length; i++) {
				confusion[1][1] += getNumSubject(false, LSub[i][0], LSub[i][1], 1.0/(1+ T));
			}
			confusion[1][0] = (HNeg + LNeg) - confusion[1][1];
			confusion[0][1] = (LPos + LNeg) - confusion[1][1];
			confusion[0][0] = (HPos + HNeg) - confusion[1][0];
		}
	}

	private int getNumSubject(boolean isHigh, int PosSubs, int NegSubs, double T) {
		int n1;
		if (isHigh) {
			n1 = (int) Math.ceil((PosSubs + NegSubs) * T);
			int n2 = PosSubs + NegSubs - n1;
				
			if (n2 != 0 && (n1 / n2) * 1.0 == T && Parameter.tie == 0) {
				n1++;
				n2--;
			}
		} else {
			n1 = (int) Math.ceil((PosSubs + NegSubs) * T);
			int n2 = PosSubs + NegSubs - n1;
			
			if (n2 != 0 && (n1 / n2) * 1.0 == T && Parameter.tie == 1) {
				n1--;
				n2++;
			}
		}
		return n1;
	}

	private void base() {
		double p = 0;
		for (int i = 1; i <= HPos + HNeg; i++) {
			p += Math.log(i);
		}
		for (int i = 1; i <= HPos + LPos; i++) {
			p += Math.log(i);
		}
		for (int i = 1; i <= LNeg + HNeg; i++) {
			p += Math.log(i);
		}
		for (int i = 1; i <= LNeg + LPos; i++) {
			p += Math.log(i);
		}

		for (int i = 1; i <= HPos; i++) {
			p -= Math.log(i);
		}
		for (int i = 1; i <= HNeg; i++) {
			p -= Math.log(i);
		}
		for (int i = 1; i <= LPos; i++) {
			p -= Math.log(i);
		}
		for (int i = 1; i <= LNeg; i++) {
			p -= Math.log(i);
		}
		for (int i = 1; i <= HPos + HNeg + LPos + LNeg; i++) {
			p -= Math.log(i);
		}

		pbase = Math.exp(p);
	}

	public double getOneTailP() {
		return pOneTail;
	}

	public void ExactTest() {
		base();
		int upper = confusion[0][1] < confusion[1][0] ? confusion[0][1] : confusion[1][0];
		double p = pbase;
		ptruncated += pbase;
		if (HPos <= confusion[0][0]) {
			pInt += pbase;
		}
		for (int i = confusion[0][0]; i <= confusion[0][0] + upper - 1; i++) {
			int a = i;
			int b = confusion[0][0] + confusion[0][1] - a;
			int c = confusion[0][0] + confusion[1][0] - a;
			int d = confusion[1][1] + confusion[0][1] - b;
			p = p * b * c / ((a + 1) * (d + 1));
			if (i < HPos) {
				pInt += p;
			}
			if (i == HPos - 1) {
				pobs = p;
			}
			ptruncated += p;
		}
		pOneTail = 1 - pInt / ptruncated;
		pobs = pobs / ptruncated;
	}
}