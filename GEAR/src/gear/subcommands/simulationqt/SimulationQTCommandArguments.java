package gear.subcommands.simulationqt;

import gear.subcommands.CommandArguments;
import gear.util.FileUtil;
import gear.util.Logger;

public class SimulationQTCommandArguments extends CommandArguments {

	public void setRep(String rep) {
		this.rep = Integer.parseInt(rep);
		if (this.rep < 1) {
			Logger.printUserLog("Replication should be greater than 0. GEAR quit.");
			System.exit(0);
		}
	}

	public int getRep() {
		return this.rep;
	}

	public void setSampleSize(String n) {
		N = Integer.parseInt(n);
		if (N < 1) {
			Logger.printUserLog("Sample size " + N + " is too small. GEAR quit.");
			System.exit(0);
		}
	}

	public int getSampleSize() {
		return N;
	}

	public void setMarkerNum(String m) {
		M = Integer.parseInt(m);
		if (M < 1) {
			Logger.printUserLog("Marker number " + M + " is too small. GEAR quit.");
			System.exit(0);
		}
	}

	public int getMarkerNum() {
		return M;
	}

	public void setNullMarkerNum(String nm) {
		nullM = Integer.parseInt(nm);
		if (nullM < 0) {
			Logger.printUserLog("Null marker number " + nullM + " is negative. It is set to zero.");
			nullM = 0;
		}
		if (nullM >= M) {
			Logger.printUserLog("Null marker number " + nullM + " should less than the number of merkers (" + M
					+ ").\n GEAR quittte.");
			System.exit(0);
		}

	}

	public int getNullMarkerNum() {
		return nullM;
	}

	// add effect
	public void setPlainEffect(double e) {
		polyEffect = e;
		isPlainEffect = true;
		isPolyEffect = false;
		isPolyEffectSort = false;
		isPolyEffectFile = false;
	}

	public boolean isPlainEffect() {
		return isPlainEffect;
	}

	public double getPolyEffect() {
		return polyEffect;
	}

	public void setPolyEffect() {
		isPlainEffect = false;
		isPolyEffect = true;
		isPolyEffectSort = false;
		isPolyEffectFile = false;
	}

	public boolean isPolyEffect() {
		return isPolyEffect;
	}

	public void setPolyEffectSort() {
		isPlainEffect = false;
		isPolyEffect = false;
		isPolyEffectSort = true;
		isPolyEffectFile = false;
	}

	public boolean isPolyEffectSort() {
		return isPolyEffectSort;
	}

	public void setPolyEffectFile(String f) {
		FileUtil.exists(f);
		polyEffectFile = f;

		isPlainEffect = false;
		isPolyEffect = false;
		isPolyEffectSort = false;
		isPolyEffectFile = true;
	}

	public boolean isPolyEffectFile() {
		return isPolyEffectFile;
	}

	public String getPolyEffectFile() {
		return polyEffectFile;
	}

	// dom effect
	public void setPlainDomEffect(double e) {
		polyDomEffect = e;
		isPlainDomEffect = true;
		isPolyDomEffect = false;
		isPolyDomEffectSort = false;
		isPolyDomEffectFile = false;
	}

	public boolean isPlainDomEffect() {
		return isPlainDomEffect;
	}

	public double getPolyDomEffect() {
		return polyDomEffect;
	}

	public void setPolyDomEffect() {
		isPlainDomEffect = false;
		isPolyDomEffect = true;
		isPolyDomEffectSort = false;
		isPolyDomEffectFile = false;
	}

	public boolean isPolyDomEffect() {
		return isPolyDomEffect;
	}

	public void setPolyDomEffectSort() {
		isPlainDomEffect = false;
		isPolyDomEffect = false;
		isPolyDomEffectSort = true;
		isPolyDomEffectFile = false;
	}

	public boolean isPolyDomEffectSort() {
		return isPolyDomEffectSort;
	}

	public void setPolyDomEffectFile(String f) {
		FileUtil.exists(f);
		polyDomEffectFile = f;

		isPlainDomEffect = false;
		isPolyDomEffect = false;
		isPolyDomEffectSort = false;
		isPolyDomEffectFile = true;
	}

	public boolean isPolyDomEffectFile() {
		return isPolyDomEffectFile;
	}

	public String getPolyDomEffectFile() {
		return polyDomEffectFile;
	}

	// Freq
	public void setFreq(double freq) {
		this.freq = freq;
		if (this.freq < 0.01) {
			Logger.printUserLog("Frequecy " + this.freq + " is too small. Should be greater than 0.01. GEAR quit.");
			System.exit(0);
		}
		isPlainFreq = true;
		isUnifFreq = false;
		isFreqFile = false;
	}

	public boolean isPlainFreq() {
		return isPlainFreq;
	}

	public double getFreq() {
		return freq;
	}

	public void setUnifFreq() {
		isPlainFreq = false;
		isUnifFreq = true;
		isFreqFile = false;
	}

	public boolean isUnifFreq() {
		return isUnifFreq;
	}

	public void setFreqRange(String[] rf) {
		FreqRangeLow = Double.parseDouble(rf[0]);
		FreqRangeHigh = Double.parseDouble(rf[1]);
		if (FreqRangeLow <= 0 || FreqRangeHigh >= 1) {
			Logger.printUserError("Allele frequency is out of range: " + FreqRangeLow + "--" + FreqRangeHigh);
		}
		if (FreqRangeLow > FreqRangeHigh) {
			double t = FreqRangeLow;
			FreqRangeLow = FreqRangeHigh;
			FreqRangeLow = t;
		}
		setUnifFreq();
	}

	public double getFreqRangeLow() {
		return FreqRangeLow;
	}

	public double getFreqRangeHigh() {
		return FreqRangeHigh;
	}

	public void setFreqFile(String ff) {
		FileUtil.exists(ff);
		freqFile = ff;
		isPlainFreq = false;
		isUnifFreq = false;
		isFreqFile = true;
	}

	public boolean isFreqFile() {
		return isFreqFile;
	}

	public String getFreqFile() {
		return freqFile;
	}

	public void setLD(double ld) {
		this.ld = ld;
		if (this.ld < -1 || this.ld > 1) {
			Logger.printUserLog("LD should be between -1 and 1. GEAR quit.");
		}
		isPlainLD = true;
		isRandLD = false;
		isLDfile = false;
	}

	public void setLDRange(String[] ld) {
		ldRangeLow = Double.parseDouble(ld[0]);
		ldRangeHigh = Double.parseDouble(ld[1]);
		if (ldRangeLow <= -1 || ldRangeHigh >= 1) {
			Logger.printUserError("LD (Lewontin's) frequency is out of range: " + ldRangeLow + "--" + ldRangeHigh);
		}
		if (ldRangeLow > ldRangeHigh) {
			double t = FreqRangeLow;
			FreqRangeLow = FreqRangeHigh;
			FreqRangeLow = t;
		}
		setUnifFreq();
		isPlainLD = false;
		isRandLD = true;
		isLDfile = false;
	}

	public double getLDRangeLow() {
		return ldRangeLow;
	}

	public double getLDRangeHigh() {
		return ldRangeHigh;
	}

	public boolean isPlainLD() {
		return isPlainLD;
	}

	public double getLD() {
		return ld;
	}

	public void setRandLD() {
		isPlainLD = false;
		isRandLD = true;
		isLDfile = false;
	}

	public boolean isRandLD() {
		return isRandLD;
	}

	public void setHsq(double h) {
		hsq = h;
		if (hsq < 0 || hsq > 0.99) {
			Logger.printUserLog("hsq should be between 0 ~ 1. GEAR quit.");
		}
	}

	public double getHsq() {
		return hsq;
	}

	public void setHsqDom(String hsqD) {
		double hd = Double.parseDouble(hsqD);
		if (hd < 0 || hd > 0.99) {
			Logger.printUserLog("hsq should be between 0 ~ 1. GEAR quit.");
		}
		if ((hd + hsq) > 1) {
			Logger.printUserLog("The total heritability (hsq+hsq_dom) should be between 0 ~ 1. GEAR quit.");
		}
		hsqDom = hd;
	}

	public double getHsqDom() {
		return hsqDom;
	}

	public void setMakeBed() {
		isMakeBed = true;
	}

	public boolean isMakeBed() {
		return isMakeBed;
	}

	public void setLDFile(String file) {
		FileUtil.exists(file);
		ldFile = file;
		isPlainLD = false;
		isRandLD = false;
		isLDfile = true;
	}

	public String getLDFile() {
		return ldFile;
	}

	public boolean isLDFile() {
		return isLDfile;
	}

	public void setFamIDPrefix(String fid) {
		FID_Prefix = fid;
		isFID = true;
	}

	public String getFamIDPrefix() {
		return FID_Prefix;
	}

	public boolean isFID() {
		return isFID;
	}

	private int N = 100;
	private int M = 100;
	private int nullM = 0;

	private double polyEffect = 0.5;
	private boolean isPlainEffect = true;
	private boolean isPolyEffect = false;
	private boolean isPolyEffectSort = false;
	private boolean isPolyEffectFile = false;
	private String polyEffectFile = null;

	private double polyDomEffect = 0.5 * polyEffect;
	private boolean isPlainDomEffect = true;
	private boolean isPolyDomEffect = false;
	private boolean isPolyDomEffectSort = false;
	private boolean isPolyDomEffectFile = false;
	private String polyDomEffectFile = null;

	private double freq = 0.5;
	private boolean isPlainFreq = true;
	private boolean isUnifFreq = false;
	private boolean isFreqFile = false;
	private String freqFile = null;

	private double FreqRangeLow = 0.01;
	private double FreqRangeHigh = 0.5;

	private double ld = 0;
	private boolean isPlainLD = true;
	private boolean isRandLD = false;
	private boolean isLDfile = false;

	private double ldRangeLow = -1;
	private double ldRangeHigh = 1;

	private String ldFile = null;

	private double hsq = 0.5;
	private double hsqDom = 0;
	private boolean isMakeBed = false;

	private String FID_Prefix = "";
	private boolean isFID = false;

	private int rep = 1;

}
