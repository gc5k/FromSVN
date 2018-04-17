package gear.subcommands.ebatchgwas;

import gear.subcommands.CommandArguments;
import gear.subcommands.CommandImpl;
import gear.subcommands.eigengwas.EigenGWASArguments;
import gear.subcommands.eigengwas.EigenGWASImpl;
import gear.subcommands.eigengwasdom.EigenGWASDomCommandArguments;
import gear.subcommands.eigengwasdom.EigenGWASDomImpl;
import gear.subcommands.eigengwasepi.EigenGWASEpiCommandArguments;
import gear.subcommands.grm.GRMArguments;
import gear.subcommands.grm.GRMImpl;
import gear.subcommands.qpca.QPCACommandArguments;
import gear.subcommands.qpca.QPCACommandImpl;
import gear.util.Logger;

public class EbatchGWASImpl extends CommandImpl 
{
	@Override
	public void execute(CommandArguments cmdArgs) 
	{
		EbatchGWASArguments eArgs = (EbatchGWASArguments) cmdArgs;

		GRMArguments gArgs = new GRMArguments();
		gArgs.setBFile(eArgs.getBFile());
		gArgs.setGZ();
		gArgs.setOutRoot(eArgs.getOutRoot());
		if (eArgs.isInbred())
		{
			gArgs.setAdjVar();
		}

		if (eArgs.getExtractFile() != null) {
			gArgs.setExtractFile(eArgs.getExtractFile());
		} else if (eArgs.getExcludeFile() != null) {
			gArgs.setExcludeFile(eArgs.getExcludeFile());
		} else if (eArgs.getChr() != null) {
			gArgs.setChr(eArgs.getChr().toArray(new String[0]));
		} else if (eArgs.getNotChr() != null) {
			gArgs.setNotChr(eArgs.getNotChr().toArray(new String[0]));			
		}

		if (eArgs.getKeepFile() != null) {
			gArgs.setKeepFile(eArgs.getKeepFile());
		} else if (eArgs.getRemoveFile() != null) {
			gArgs.setRemoveFile(eArgs.getRemoveFile());
		}

		GRMImpl gImpl = new GRMImpl();
		gImpl.execute(gArgs);

		Logger.printUserLog("\n---------Generating eigenvectors---------");

		QPCACommandArguments qpcaArgs = new QPCACommandArguments();
		qpcaArgs.setGrmGZ(eArgs.getOutRoot() + ".grm.gz");
		qpcaArgs.setGrmID(eArgs.getOutRoot() + ".grm.id");
		qpcaArgs.setEV(Integer.toString(eArgs.getEV()));
		qpcaArgs.setOutRoot(eArgs.getOutRoot());
		QPCACommandImpl qpcaImpl = new QPCACommandImpl();
		qpcaImpl.execute(qpcaArgs);
		Logger.printUserLog("Saving the top "+ eArgs.getEV() + " eigenvectors in '" + eArgs.getOutRoot() + ".eigenvec'.");

		if (!eArgs.isDom() && !eArgs.isEpi())
		{
			for(int i = 1; i <= eArgs.getEV(); i++)
			{
				Logger.printUserLog("\n---------Running EigenGWAS (Additive model) for the "+i+"th eigenvector.");
				EigenGWASArguments eigenArgs = new EigenGWASArguments();
				eigenArgs.setBFile(eArgs.getBFile());
				eigenArgs.setPhenotypeFile(eArgs.getOutRoot()+".eigenvec");
				eigenArgs.setPhentypeIndex(i);
				eigenArgs.setOutRoot(eArgs.getOutRoot() + "." + i);


				if (eArgs.getExtractFile() != null) {
					eigenArgs.setExtractFile(eArgs.getExtractFile());
				} else if (eArgs.getExcludeFile() != null) {
					eigenArgs.setExcludeFile(eArgs.getExcludeFile());
				} else if (eArgs.getChr() != null) {
					eigenArgs.setChr(eArgs.getChr().toArray(new String[0]));
				} else if (eArgs.getNotChr() != null) {
					eigenArgs.setNotChr(eArgs.getNotChr().toArray(new String[0]));			
				}

				if (eArgs.getKeepFile() != null) {
					eigenArgs.setKeepFile(eArgs.getKeepFile());
				} else if (eArgs.getRemoveFile() != null) {
					eigenArgs.setRemoveFile(eArgs.getRemoveFile());
				}

				EigenGWASImpl eigenImpl = new EigenGWASImpl();
				eigenImpl.execute(eigenArgs);
				Logger.printUserLog("Saved EigenGWAS results in '"+eigenArgs.getOutRoot() + ".egwas'.");
			}
		}
//		else if (eArgs.isEpi())
//		{
//			for(int i = 1; i <= eArgs.getEV(); i++)
//			{
//				Logger.printUserLog("\n---------Running EigenGWAS (Add+Dom+AA model) for the "+i+"th eigenvector.");
//				EigenGWASEpiCommandArguments eigenEpiArgs = new EigenGWASEpiCommandArguments();
//				eigenEpiArgs.setBFile(eArgs.getBFile());
//				eigenEpiArgs.setPhenotypeFile(eArgs.getOutRoot()+".eigenvec");
//				eigenEpiArgs.setPhentypeIndex(i);
//				eigenEpiArgs.setOutRoot(eArgs.getOutRoot() + "." + i);
//				if (eArgs.isInbred())
//				{
//					eigenEpiArgs.setInbred();
//				}
//
//				EigenGWASDomImpl eigenDomImpl = new EigenGWASDomImpl();
//				eigenDomImpl.execute(eigenEpiArgs);
//				Logger.printUserLog("Saved EigenGWAS results in '"+eigenEpiArgs.getOutRoot() + ".egwasepi'.");
//			}			
//		}
//		else if (eArgs.isDom())
//		{
//			for(int i = 1; i <= eArgs.getEV(); i++)
//			{
//				Logger.printUserLog("\n---------Running EigenGWAS (Additive+Dominance model) for the "+i+"th eigenvector.");
//				EigenGWASDomCommandArguments eigenDomArgs = new EigenGWASDomCommandArguments();
//				eigenDomArgs.setBFile(eArgs.getBFile());
//				eigenDomArgs.setPhenotypeFile(eArgs.getOutRoot()+".eigenvec");
//				eigenDomArgs.setPhentypeIndex(i);
//				eigenDomArgs.setOutRoot(eArgs.getOutRoot() + "." + i);
//				
//				EigenGWASDomImpl eigenDomImpl = new EigenGWASDomImpl();
//				eigenDomImpl.execute(eigenDomArgs);
//				Logger.printUserLog("Saved EigenGWAS results in '"+eigenDomArgs.getOutRoot() + ".egwasd'.");
//			}
//		}
	}
}
