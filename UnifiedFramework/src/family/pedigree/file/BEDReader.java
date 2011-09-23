package family.pedigree.file;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import util.NewIt;

import family.pedigree.genotype.BFamilyStruct;
import family.pedigree.genotype.BPerson;

public class BEDReader extends PedigreeFile {
	public String FamFile;
	private int n_individual = 0;
	private ArrayList<String> Famid;
	private ArrayList<String> Individualid;
	private MapFile mapData;

	public BEDReader(String famF, int numMark, MapFile mapdata) {
		super();
		FamFile = famF;
		this.num_marker = numMark;
		this.mapData = mapdata;
	}

	@Override
	public void initial() throws IOException {
		Famid = NewIt.newArrayList();
		Individualid = NewIt.newArrayList();

		BufferedReader reader = new BufferedReader(new FileReader(FamFile));
		AlleleSet = new char[num_marker][];
		AlleleFreq = new short[num_marker][2];
		for (int i = 0; i < mapData.snpList.size(); i++) {
			SNP snp = mapData.snpList.get(i);
			AlleleSet[i] = snp.getSNP();
		}
		String line;

		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split("\\s+");

			BPerson per = new BPerson(num_marker);
			Famid.add(tokens[0]);
			Individualid.add(tokens[1]);
			per.setFamilyID(tokens[0]);
			per.setPersonID(tokens[1]);
			per.setDadID(tokens[2]);
			per.setMomID(tokens[3]);
			per.setGender(Integer.parseInt(tokens[4]));
			per.setAffectedStatus(Integer.parseInt(tokens[5]));

			BFamilyStruct fam = familystructure.get(tokens[0]);
			if (fam == null) {
				fam = new BFamilyStruct(tokens[0]);
				familystructure.put(tokens[0], fam);
			}
			fam.addPerson(per);
			n_individual++;
		}

	}

	@Override
	public void parseLinkage(String infile, int numMarkerInFile, int[] WSNP) throws IOException {
		initial();
		pedfile = infile;
		BufferedInputStream in = null;

		try {
			in = new BufferedInputStream(new FileInputStream(new File(pedfile)));
		} catch (FileNotFoundException e) {
			System.err.println("cannot open pedigree file.");
		}
		byte[] magic = new byte[3];
		int n = in.read(magic, 0, 3);
		if (magic[2] == 1) {
			snp_major(in, numMarkerInFile, WSNP);
		} else {
			individual_major(in, numMarkerInFile, WSNP);
		}
		in.close();
	}

	private void individual_major(BufferedInputStream in, int numMarkerInFile, int[] WSNP) throws IOException {
		int L = 0;
		if (numMarkerInFile % 4 == 0) {
			L = numMarkerInFile / 4;
		} else {
			L = numMarkerInFile / 4 + 1;
		}
		int exL = 0;
		if (WSNP.length % 4 == 0) {
			exL = WSNP.length / 4;
		} else {
			exL = WSNP.length / 4 + 1;
		}
		byte[] geno = new byte[L];
		byte[] extract_geno = new byte[exL];
		for (int i = 0; i < n_individual; i++) {
			int n = in.read(geno, 0, L);
			BFamilyStruct bf = familystructure.get(Famid.get(i));
			BPerson per = bf.getPerson(Individualid.get(i));
			extract_geno = extractGenotype(geno, numMarkerInFile, WSNP);
			per.addAllMarker(extract_geno);			
		}
		Famid = null;
		Individualid = null;
	}

	private void snp_major(BufferedInputStream in, int numMarkerInFile, int[] WSNP) throws IOException {
		int L = 0;
		if (n_individual % 4 == 0) {
			L = n_individual / 4;
		} else {
			L = n_individual / 4 + 1;
		}
		byte[] g = new byte[L];

		int c = 0;
		for (int i = 0; i < numMarkerInFile; i++) {
			int n = in.read(g, 0, L);
			int idx = Arrays.binarySearch(WSNP, i);
			if (idx < 0)
				continue;
			for (int j = 0; j < n_individual; j++) {
				BFamilyStruct bf = familystructure.get(Famid.get(j));
				BPerson per = bf.getPerson(Individualid.get(j));
				int posByte = j >> 2;
				int posBite = (j - (j >> 2 << 2)) << 1;
				int g1 = (g[posByte] >> posBite) & 3;
				if (g1 == 0) {
					AlleleFreq[c][0] += 2;
				} else if (g1 == 2) {
					AlleleFreq[c][0]++;
					AlleleFreq[c][1]++;
				} else if (g1 == 3) {
					AlleleFreq[c][1] += 2;
				}
				per.addByteGenotype(g1, c);
			}
			c++;
		}
	}

	private byte[] extractGenotype(byte[] g, int numMarkerInFile, int[] WSNP) {

		int exL = 0;
		if (WSNP.length % 4 == 0) {
			exL = WSNP.length / 4;
		} else {
			exL = WSNP.length / 4 + 1;
		}
		byte[] Exg = new byte[exL];
		int c = 0;
		for (int i = 0; i < numMarkerInFile; i++) {
			int idx = Arrays.binarySearch(WSNP, i);
			if (idx < 0)
				continue;
			int posByte = i >> 2;
			int posBite = (i - (i >> 2 << 2)) << 1;
			int g1 = (g[posByte] >> posBite) & 3;

			int ExposByte = c >> 2;
			int ExposBite = (c - (c >> 2 << 2)) << 1;
			Exg[ExposByte] |= g1 << ExposBite;
			if (g1 == 0) {
				AlleleFreq[c][0] += 2;
			} else if (g1 == 2) {
				AlleleFreq[c][0]++;
				AlleleFreq[c][1]++;
			} else if (g1 == 3) {
				AlleleFreq[c][1] += 2;
			}
			c++;
		}
		return Exg;
	}
}