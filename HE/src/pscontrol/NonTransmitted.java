package pscontrol;

import java.util.ArrayList;
import java.util.Iterator;

import parameter.Parameter;
import pscontrol.hierarchy.AJHG2008;
import pscontrol.write.NonTransWriteBedSNPMajor;
import test.Test;
import util.NewIt;
import family.pedigree.PersonIndex;
import family.plink.PLINKBinaryParser;
import family.plink.PLINKParser;
import family.qc.rowqc.SampleFilter;

public class NonTransmitted {
	private String casualLociFile = null;
	private int[] casualLociIdx = null;
	private PLINKParser pp = null;
	private SampleFilter sf = null;
	private NonTransmitted QC = null;
	Parameter par;

	public NonTransmitted(Parameter p) {
		System.err.print(Parameter.version);
		par = p;

		if (Parameter.fileOption) {
			pp = new PLINKParser(Parameter.pedfile, Parameter.mapfile);
		}
		if (Parameter.bfileOption) {
			pp = new PLINKBinaryParser(Parameter.bedfile, Parameter.bimfile,
					Parameter.famfile);
		} else {
			System.err.println("did not specify files.");
			Test.LOG.append("did not specify files.\n");
			Test.printLog();
			System.exit(0);
		}
		pp.Parse();
		sf = new SampleFilter(pp.getPedigreeData(), pp.getMapData());
		
	}

	public void GenerateNonTransmitted() {
		AJHG2008 ajhg2008 = new AJHG2008(pp.getPedigreeData(), pp.getMapData());
		ajhg2008.setSeed(par.nontransSeed);
		ajhg2008.RevvingUp(sf.getSample());

		ArrayList<PersonIndex> sample = ajhg2008.getSample();
		ArrayList<PersonIndex> ps = NewIt.newArrayList();

		for (Iterator<PersonIndex> e = sample.iterator(); e.hasNext();) {
			PersonIndex pi = e.next();
			if (pi.isPseudo()) {
				if (par.nontranscasesFlag) {
					if (pi.getPerson().getAffectedStatus().compareTo("2") != 0) {
						continue;
					}
				}
				if (par.nontranscontrolsFlag) {
					if (pi.getPerson().getAffectedStatus().compareTo("1") != 0) {
						continue;
					}
				}
				ps.add(pi);
			}
		}

		NonTransWriteBedSNPMajor writeSNP = new NonTransWriteBedSNPMajor(ps, ajhg2008
				.getMapFile().getMarkerList());
		StringBuilder out = new StringBuilder(); 
		out.append(par.out);
		out.append(".nt");
		writeSNP.WriteFile(out.toString());
	}
}
