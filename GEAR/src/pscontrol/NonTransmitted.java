package pscontrol;

import java.util.ArrayList;
import java.util.Iterator;

import pscontrol.hierarchy.AJHG2008;
import pscontrol.write.NonTransWriteBedSNPMajor;
import family.pedigree.PersonIndex;
import family.plink.PLINKBinaryParser;
import family.plink.PLINKParser;
import family.qc.rowqc.SampleFilter;
import gear.Parameter;
import gear.util.Logger;
import gear.util.NewIt;

public class NonTransmitted
{

	private PLINKParser pp = null;
	private SampleFilter sf = null;

	public NonTransmitted()
	{
		Logger.printUserLog("--nontrans procedure.");

		if (Parameter.INSTANCE.getBfileParameter(0).isSet())
		{
			pp = new PLINKBinaryParser(Parameter.INSTANCE.getBfileParameter(0)
					.getBedFile(), Parameter.INSTANCE.getBfileParameter(0)
					.getBimFile(), Parameter.INSTANCE.getBfileParameter(0)
					.getFamFile());
		} else
		{
			Logger.printUserError("--bfile is not set.");
			System.exit(1);
		}
		pp.Parse();
		sf = new SampleFilter(pp.getPedigreeData(), pp.getMapData());
	}

	public void GenerateNonTransmitted()
	{
		AJHG2008 ajhg2008 = new AJHG2008(pp.getPedigreeData(), pp.getMapData());
		ajhg2008.setSeed(Parameter.INSTANCE.nontransSeed);
		ajhg2008.RevvingUp(sf.getSample());

		ArrayList<PersonIndex> sample = ajhg2008.getSample();
		ArrayList<PersonIndex> ps = NewIt.newArrayList();

		Logger.printUserLog("Sample size: " + sample.size());
		for (Iterator<PersonIndex> e = sample.iterator(); e.hasNext();)
		{
			PersonIndex pi = e.next();
			if (pi.isPseudo())
			{
				if (Parameter.INSTANCE.nontranscasesFlag)
				{
					if (pi.getPerson().getAffectedStatus().compareTo("2") != 0)
					{
						continue;
					}
				}
				if (Parameter.INSTANCE.nontranscontrolsFlag)
				{
					if (pi.getPerson().getAffectedStatus().compareTo("1") != 0)
					{
						continue;
					}
				}
				ps.add(pi);
			}
		}

		NonTransWriteBedSNPMajor writeSNP = new NonTransWriteBedSNPMajor(ps,
				ajhg2008.getMapFile().getMarkerList());
		StringBuilder out = new StringBuilder();
		out.append(Parameter.INSTANCE.out);
		out.append(".nt");
		writeSNP.WriteFile(out.toString());
	}
}
