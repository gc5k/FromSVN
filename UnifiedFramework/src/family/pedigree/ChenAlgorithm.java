package family.pedigree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import family.pedigree.genotype.FamilyStruct;
import family.pedigree.genotype.Person;
import family.pedigree.phenotype.FamilyUnit;
import family.pedigree.phenotype.Subject;

import publicAccess.PublicData;

import score.LinearRegression;
import score.LogisticRegression;

import util.NewIt;
import util.Sample;

import org.apache.commons.lang3.ArrayUtils;

public class ChenAlgorithm {

	public MDRPed PedData;
	public GMDRPhenoFile PhenoData;

	private byte[][] genotype;
	private int numUnrelated;
	private int numSibs;
	private int[] numSib;
	private byte[] status;
	private double[] score;
	private double[] permuted_score;

	private ArrayList<ArrayList<String>> CovariateTable;

	private String[] scoreName = new String[1];
	private ArrayList<PersonIndex> PersonTable;// The indexing file records the

	private int[] subsetMarker;
	public static Random rnd;

	/**
	 * PersonIndex class is developed to construct a unique key for each
	 * individual.
	 * 
	 * @author Guo-Bo Chen
	 */
	public static class PersonIndex {

		String FamilyID;
		String IndividualID;
		String Key;

		/**
		 * Constructs an individual index. Key = family_id+"->"individual_in
		 * within a family.
		 * 
		 * @param fid
		 *            ID of the family where the individual comes from.
		 * @param id
		 *            ID of the individual where the individual is in a family.
		 */
		PersonIndex(String fid, String id) {
			FamilyID = new String(fid);
			IndividualID = new String(id);
			Key = new String(fid + "->" + id);
		}

		/**
		 * get the family ID of the individual
		 * 
		 * @return String
		 */
		public String getFamilyID() {
			return FamilyID;
		}

		/**
		 * get the individual ID
		 * 
		 * @return String
		 */
		public String getIndividualID() {
			return IndividualID;
		}

		/**
		 * get the key of the indivudal. Key=family_id+"->"individual_ID.
		 * 
		 * @return
		 */
		public String getKey() {
			return Key;
		}
	}

	/**
	 * Constructing the GMDR with specialized filetype
	 * 
	 * @param filetype
	 */

	public ChenAlgorithm() {
		PhenoData = new GMDRPhenoFile();
		PedData = new MDRPed();
		CovariateTable = NewIt.newArrayList();
		PersonTable = NewIt.newArrayList();
	}

	/**
	 * Build score with a selected response and predictor(s).
	 * 
	 * @param Adjust
	 *            a boolean variable. If it equals false, don't adjust the model
	 *            with predictor.
	 * @throws CalEngineException
	 */
	public void buildScore(int PIndex, int[] CIndex, boolean adjust, int method, boolean includeFounder, boolean includeChildren) {
		// method: 0 for regression, 1 for logistic
		score = new double[PersonTable.size()];
		PersonIndex PI;
		ArrayList<Double> T = NewIt.newArrayList();
		ArrayList<ArrayList<Double>> C = NewIt.newArrayList();
		ArrayList<PersonIndex> P = NewIt.newArrayList();

		for (int i = 0; i < PersonTable.size(); i++) {
			PI = PersonTable.get(i);
			if (!includeFounder) {
				FamilyStruct FamStr = PedData.getFamilyStruct(PI.getFamilyID());
				if (!FamStr.hasAncestor(PI.getIndividualID())) {
					continue;
				}
			}
			if (!includeChildren) {
				FamilyStruct FamStr = PedData.getFamilyStruct(PI.getFamilyID());
				if (FamStr.hasAncestor(PI.getIndividualID())) {
					continue;
				}
			}
			boolean flag = true;
			double t = 0;
			ArrayList<Double> c = NewIt.newArrayList();
			ArrayList<String> tempc = CovariateTable.get(i);
			if (PIndex == -1) {// using affecting status as phenotype
				if (status[i] == 0) {
					flag = false;
					continue;
				} else {// after converting, 0 for unaffected; 1 for affected;
					// -1 for unknown;
					t = status[i] - 1;
				}
			} else {
				if (((String) tempc.get(PIndex)).compareTo(PublicData.MissingValue) == 0) {
					flag = false;
					continue;
				} else {
					t = Double.parseDouble((String) tempc.get(PIndex));
				}
			}
			if (adjust) {
				for (int j = 0; j < CIndex.length; j++) {
					if (((String) tempc.get(CIndex[j])).compareTo(PublicData.MissingValue) == 0) {
						flag = false;
						break;
					}
					c.add((Double.parseDouble((String) tempc.get(CIndex[j]))));
				}
				if (!flag) {
					continue;
				}
				C.add(c);
			}
			P.add(PersonTable.get(i));
			T.add(new Double(t));
		}

		double[][] X = null;
		if (adjust) {
			X = new double[C.size()][CIndex.length];
			for (int i = 0; i < C.size(); i++) {
				ArrayList<Double> c = C.get(i);
				for (int j = 0; j < c.size(); j++) {
					X[i][j] = ((Double) c.get(j)).doubleValue();
				}
			}
		}

		double[][] Y = new double[T.size()][1];
		for (int i = 0; i < T.size(); i++) {
			Y[i][0] = ((Double) T.get(i)).doubleValue();
		}

		double[] r = null;
		if (method == 0) {
			LinearRegression LReg = new LinearRegression(Y, X, true);
			LReg.MLE();
			r = LReg.getResiduals1();
		} else {
			LogisticRegression LogReg1 = new LogisticRegression(Y, X, true);
			LogReg1.MLE();
			r = LogReg1.getResiduals1();
		}

		System.arraycopy(r, 0, score, 0, r.length);
		nameScore(PIndex, CIndex, adjust, method, includeFounder);
	}

	private void nameScore(int PIndex, int[] CIndex, boolean adjust, int method, boolean includeFounder) {
		StringBuilder ln = new StringBuilder();
		if(method == 1) {
			ln.append("linear(" );
		} else {
			ln.append("logistic(");
		}
		if(PIndex == -1) {
			ln.append("status->");
		} else {
			ln.append(PhenoData.getTraitAtI(PIndex));
		}
		ln.append("->");

		for (int i = 0; i < CIndex.length; i++) {
			ln.append(PhenoData.getTraitAtI(CIndex[i]) + ",");
		}
		
		if(includeFounder) {
			ln.append("included founders)");
		} else {
			ln.append("excluded founders)");
		}
		scoreName[0] = ln.toString();
	}

	/**
	 * When score has alread been built outside PedGMDR, it can be imported
	 * directly
	 * 
	 * @param score_index
	 *            Index of the score in the phenotype file
	 * @return
	 */
	public boolean fetchScore(int score_index, boolean includeFounder) {
		ArrayList<Integer> mi = NewIt.newArrayList();
		double sum = 0;
		score = new double[PersonTable.size()];
		for (int i = 0; i < PersonTable.size(); i++) {
			if (score_index == -1) {
				if (status[i] == PublicData.MissingAffection) {
					score[i] = rnd.nextInt(2);
				} else {
					score[i] = status[i] - 1;
				}
			} else {
				ArrayList<String> v = CovariateTable.get(i);
				String s = (String) v.get(score_index);
				if (s.compareTo(PublicData.MissingValue) == 0) {
					mi.add(new Integer(i));
					continue;
				}
				score[i] = Double.parseDouble(s);
				sum += score[i];
			}
		}
		sum /= (score.length - mi.size());
		for (Integer i : mi) {
			score[i.intValue()] = sum;
		}
		if (score_index == -1) {
			scoreName[0] = new String("status");
		} else {
			scoreName[0] = new String(PhenoData.getTraitAtI(score_index));
		}
		return true;
	}

	public double[] getPermutationScore() {
		permuted_score = new double[score.length];
		int[] idx = Sample.SampleIndex(0, score.length - 1, score.length);
		for (int i = 0; i < idx.length; i++) {
			permuted_score[i] = score[idx[i]];
		}
		return permuted_score;
	}

	public double[] getNestedPermutedScore() {
		permuted_score = new double[score.length];
		int[] un_related = Sample.SampleIndex(0, numUnrelated - 1, numUnrelated);
		for (int i = 0; i < un_related.length; i++) {
			permuted_score[i] = score[un_related[i]];
		}
		int c = numUnrelated;
		for (int i = 0; i < numSib.length; i++) {
			int[] si = Sample.SampleIndex(0, numSib[i] - 1, numSib[i]);
			for (int j = 0; j < si.length; j++) {
				permuted_score[numUnrelated + j] = score[c+si[j]];
			}
			c += si.length;
		}
		return permuted_score;
	}

	public ArrayList<ArrayList<String>> getCovariateTable() {
		return CovariateTable;
	}

	/**
	 * Initialize basic implementation of the genotype file.
	 * 
	 * @param Ped
	 *            the name of the pedigree file
	 * @param Pheno
	 *            the name of the phenotype file
	 * @throws IOException
	 */
	public void Initial(File Ped, File Pheno) throws IOException {
		try {
			PedData.Initial(Ped);
			PedData.parseLinkage();
			PhenoData.Initial(Pheno);
			PhenoData.parsePhenotype();
		} catch (MDRPedFileException e) {
			System.err.println("PedFile initialization exception.");
			e.printStackTrace(System.err);
		} catch (GMDRPhenoFileException e) {
			System.err.println("PhenoFile initialization exception.");
			e.printStackTrace(System.err);
		} catch (IOException e) {
			System.err.println("GMDRData initial exception.");
			e.printStackTrace(System.err);
		}
	}

	public void RevvingUp(String ped, String phe) {
		ParsePedFile(ped);
		ParsePhenoFile(phe);
		int[] m = new int[PedData.getNumMarkers()];
		for (int i = 0; i < m.length; i++) {
			m[i] = i;
		}
		SetChosenMarker(m);

		Hashtable<String, FamilyStruct> Fam = PedData.getFamilyStruct();

		for (String fi : Fam.keySet()) {
			FamilyStruct fs = Fam.get(fi);
			numUnrelated += fs.getNumFounders();
			numSibs += fs.getNumSibs();
		}

		PersonTable.ensureCapacity(numUnrelated + numSibs);
		CovariateTable.ensureCapacity(numUnrelated + numSibs);
		genotype = new byte[numUnrelated + numSibs][];
		status = new byte[numUnrelated + numSibs];

		int n_unrelated = 0;
		int n_sib = 0;

		ArrayList<PersonIndex> u_P = NewIt.newArrayList();
		ArrayList<ArrayList<String>> u_C = NewIt.newArrayList();

		ArrayList<PersonIndex> s_P = NewIt.newArrayList();
		ArrayList<ArrayList<String>> s_C = NewIt.newArrayList();

		ArrayList<Integer> SibIdx = NewIt.newArrayList();

		for (String fi : PedData.getFamListSorted()) {
			FamilyStruct fs = Fam.get(fi);
			FamilyUnit FamUnit = PhenoData.getFamilyUnit(fi);
			String[] pi = fs.getPersonListSorted();
			int si = 0;
			for (int i = 0; i < pi.length; i++) {
				Person per = fs.getPerson(pi[i]);
				Subject sub = FamUnit.getSubject(pi[i]);
				if (fs.hasAncestor(per)) {
					si++;
					s_P.add(new PersonIndex(fs.getFamilyStructName(), pi[i]));
					genotype[n_sib + numUnrelated] = per.getGenotypeScore();
					status[n_sib + numUnrelated] = (byte) per.getAffectedStatus();
					s_C.add(sub.getTraits());
					n_sib++;
				} else {
					u_P.add(new PersonIndex(fs.getFamilyStructName(), pi[i]));
					genotype[n_unrelated] = per.getGenotypeScore();
					status[n_unrelated] = (byte) per.getAffectedStatus();
					u_C.add(sub.getTraits());
					n_unrelated++;
				}
			}
			if (si != 0)
				SibIdx.add(new Integer(si));
		}
		PersonTable.addAll(u_P);
		PersonTable.addAll(s_P);
		CovariateTable.addAll(u_C);
		CovariateTable.addAll(s_C);

		numSib = ArrayUtils.toPrimitive(SibIdx.toArray(new Integer[0]));
		
		RLDriver RLD = new RLDriver();
		RLD.TDT(Fam, PedData.getMarkerInformation(), m);
	}

	/**
	 * Initialize basic implementation of the genotype file.
	 * 
	 * @param Ped
	 *            the name of the pedigree file
	 * @throws IOException
	 */
	public void ParsePedFile(String ped) {
		File PedFile = new File(ped);
		try {
			PedData.Initial(PedFile);
			PedData.parseLinkage();
		} catch (MDRPedFileException e) {
			System.err.println("PedFile initial exception.");
			e.printStackTrace(System.err);
		} catch (IOException e) {
			System.err.println("Pedgree file initialization exception.");
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Initialize basic implementation of the phenotype file.
	 * 
	 * @param Pheno
	 *            the name of the pedigree file
	 * @throws IOException
	 */
	public void ParsePhenoFile(String pheno) {
		File PheFile = new File(pheno);
		try {
			PhenoData.Initial(PheFile);
			PhenoData.parsePhenotype();
		} catch (GMDRPhenoFileException e) {
			System.err.println("Pheno file initialization exception.");
			e.printStackTrace(System.err);
		} catch (IOException e) {
			System.err.println("Pheno file initialization exception.");
			e.printStackTrace(System.err);
		}
	}

	public String[] getMarkerName() {
		return PedData.getMarkerInformation(subsetMarker).toArray(new String[0]);
	}

	public ArrayList<String> getTraitName() {
		return PhenoData.getTraitName();
	}

	public void SetChosenMarker(int[] mi) {
		subsetMarker = new int[mi.length];
		for (int i = 0; i < mi.length; i++) {
			subsetMarker[i] = mi[i];
		}
	}

	public byte[][] getGenotype() {
		return genotype;
	}
	
	public byte[] getStatue() {
		return status;
	}
	
	public double[] getScore() {
		return score;
	}
	
	public String[] getScoreName() {
		return scoreName;
	}

	public double[][] getScore2() {
		double[][] s = new double[score.length][1];
		for(int i = 0; i < score.length; i++) {
			s[i][0] = score[i];
		}
		return s;
	}

	public static void main(String[] args) {

	}
}
