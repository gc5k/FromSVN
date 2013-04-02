package family.pedigree.file;

import gear.util.NewIt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import parameter.Parameter;

import test.Test;


public class MapFile {

	protected static final String DELIMITER = "\\s+";
	protected ArrayList<SNP> snpList = NewIt.newArrayList();
	protected HashMap<String, Integer> chrSNPCount = NewIt.newHashMap();
	protected ArrayList<Integer> badline;
	protected int[] WSNP;
	protected int numMarkerOriginal;
	protected String mf = null;
	protected File mapfile = null;

	public MapFile(String m) {
		mf = m;
	}

	public void setWSNP(int[] WSNP) {
		this.WSNP = WSNP;
		ArrayList<SNP> filterSNP = NewIt.newArrayList();
		filterSNP.ensureCapacity(WSNP.length);
		for(int i = 0; i < WSNP.length; i++) {
			SNP snp = snpList.get(WSNP[i]);
			filterSNP.add(snp);
		}
		snpList = null;
		snpList = filterSNP;
	}

	public void parseMap() {
		mapfile = new File(mf);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(mapfile));
		} catch (IOException E) {
			System.err.println("could not open map file.");
			Test.LOG.append("could not open map file.\n");
			Test.printLog();
			System.exit(0);
		}

		String line = null;
		try {
			int c = 0;
			while ((line = reader.readLine()) != null) {
				c++;
				String[] tokens = line.split(DELIMITER);
				if (tokens.length < 4) {
					if (badline == null)
						badline = NewIt.newArrayList();
					badline.add(new Integer(c));
				}
				String chr = tokens[0];
				String name = tokens[1];
				// Skip genetic distance field at tokens[2].
				float dis = Float.parseFloat(tokens[2]);
				int pos = Integer.parseInt(tokens[3]);
				addSNP(chr, name, dis, pos);
			}
			reader.close();
		} catch (IOException E) {
			System.err.println("bad map file.");
		}

		if(badline != null) {
			System.err.println( "problems with the lines below:");
			for(Integer i: badline) {
				System.err.print( i + ",");
				Test.LOG.append(i + ",");
			}
			Test.printLog();
			System.exit(0);
		}
		numMarkerOriginal = snpList.size();
	}

	public HashMap<String, Integer> getChrSNPCount() {
		return chrSNPCount;
	}

	private void count(String chr) {
		if(chrSNPCount.containsKey(chr)) {
			Integer c = chrSNPCount.get(chr);
			c++;
			chrSNPCount.put(chr, c);
		} else {
			chrSNPCount.put(chr, new Integer(1));
		}
	}

	public void addSNP(String chr, String name, float dis, int pos) {
		count(chr);
		snpList.add(new SNP(chr, name, dis, pos));
	}

	public void addSNP(String chr, String name, float dis, int pos, char a1, char a2) {
		count(chr);
		snpList.add(new SNP(chr, name, dis, pos, a1, a2));
	}

	public void setMarker(int l) {
		snpList.ensureCapacity(l);
		for(int i = 0; i < l; i++ ) {
			StringBuilder sb = new StringBuilder();
			sb.append("snp");
			sb.append(i);
			addSNP(sb.toString(), "-1", -1f, -1);
		}
	}
	
	public ArrayList<SNP> getMarkerList() {
		return snpList;
	}
	
	public String getMarkerName(int i) {
		return snpList.get(i).getName();
	}
	
	public int getMarkerNumberOriginal() {
		return numMarkerOriginal;
	}

	public int getMarkerNumber() {
		return snpList.size();
	}
	
	public SNP getSNP(int i) {
		return snpList.get(i);
	}
	
	public void setPolymorphism(char[][] p, short[][] freq) {
		if(p.length != snpList.size()) {
			System.err.println("map file and the pedigree file do not match.");
			Test.LOG.append("map file and the pedigree file do not match.\n");
			Test.printLog();
			System.exit(0);
		} else {
			for(int i = 0; i < p.length; i++) {
				SNP snp = snpList.get(i);
				snp.setAllele(p[i], freq[i]);
			}
		}
	}

	public void setAlleleFrequency(double[][] freq) {
		for(int i = 0; i < freq.length; i++) {
			SNP snp = snpList.get(i);
			snp.setAllele(freq[i]);
		}
	}

	public void setPolymorphismMarker(char[][] p) {
		for(int i = 0; i < p.length; i++) {
			SNP snp = snpList.get(i);
			snp.setAllelePolymorphism(p[i]);
		}
	}
}
