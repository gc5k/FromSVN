package gear.bluppca;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import gear.Command;
import gear.CommandArgumentException;
import gear.CommandArguments;
import gear.CommandImpl;

public class BlupPCACommand extends Command
{
	@Override
	public String getName()
	{
		return "bluppca";
	}

	@Override
	public String getDescription()
	{
		return "Calculate the SNP effects with BLUP-PCA";
	}

	@SuppressWarnings("static-access")
	@Override
	protected void prepareOptions(Options options)
	{
		options.addOption(OptionBuilder.withDescription(OPT_GRM_BIN_DESC).withLongOpt(OPT_GRM_BIN_LONG).hasArg().create());
		options.addOption(OptionBuilder.withDescription(OPT_GRM_TEXT_DESC).withLongOpt(OPT_GRM_TEXT_LONG).hasArg().create());
		options.addOption(OptionBuilder.withDescription(OPT_GRM_GZ_DESC).withLongOpt(OPT_GRM_GZ_LONG).hasArg().create());
		options.addOption(OptionBuilder.withDescription(OPT_FILE_DESC).withLongOpt(OPT_FILE_LONG).hasArg().create());
		options.addOption(OptionBuilder.withDescription(OPT_BFILE_DESC).withLongOpt(OPT_BFILE_LONG).hasArg().create());
		options.addOption(OptionBuilder.withDescription(OPT_PHE_DESC).withLongOpt(OPT_PHE_LONG).hasArg().isRequired().create());
		options.addOption(OptionBuilder.withDescription(OPT_OUT_DESC).withLongOpt(OPT_OUT_LONG).hasArg().create(OPT_OUT));
	}

	@Override
	protected CommandArguments parse(CommandLine cmdLine) throws CommandArgumentException
	{
		BlupPCACommandArguments blupArgs = new BlupPCACommandArguments();
		parseGRMArguments(blupArgs, cmdLine);
		parseFileArguments(blupArgs, cmdLine);
		blupArgs.setPhenotypeFile(cmdLine.getOptionValue(OPT_PHE_LONG));
		blupArgs.setOutRoot(cmdLine.getOptionValue(OPT_OUT_LONG, OPT_OUT_DEFAULT));
		return blupArgs;
	}
	
	private void parseGRMArguments(BlupPCACommandArguments blupArgs, CommandLine cmdLine) throws CommandArgumentException
	{
		String grmBin = cmdLine.getOptionValue(OPT_GRM_BIN_LONG);
		String grmText = cmdLine.getOptionValue(OPT_GRM_TEXT_LONG);
		String grmGZ = cmdLine.getOptionValue(OPT_GRM_GZ_LONG);
		
		int numFiles = 0;
		
		if (grmBin != null)
		{
			blupArgs.setGRMBin(grmBin + ".grm.bin");
			blupArgs.setGRM_ID(grmBin + ".grm.id");
			++numFiles;
		}
		
		if (grmText != null)
		{
			blupArgs.setGRMText(grmText + ".grm");
			blupArgs.setGRM_ID(grmText + ".grm.id");
			++numFiles;
		}
		
		if (grmGZ != null)
		{
			blupArgs.setGRM_GZ(grmGZ + ".grm.gz");
			blupArgs.setGRM_ID(grmGZ + ".grm.id");
			++numFiles;
		}
		
		if (numFiles == 0)
		{
			throw new CommandArgumentException("No GRM is provided. One of --" + OPT_GRM_BIN_LONG + ", " + OPT_GRM_TEXT_LONG + " or --" + OPT_GRM_GZ_LONG + " must be set.");
		}
		
		if (numFiles > 1)
		{
			throw new CommandArgumentException("At most one of --" + OPT_GRM_BIN_LONG + ", --" + OPT_GRM_TEXT_LONG + " and --" + OPT_GRM_GZ_LONG + " can be set.");
		}
	}
	
	private void parseFileArguments(BlupPCACommandArguments blupArgs, CommandLine cmdLine) throws CommandArgumentException
	{
		String bfile = cmdLine.getOptionValue(OPT_BFILE_LONG);
		String file = cmdLine.getOptionValue(OPT_FILE_LONG);
		
		if (bfile == null && file == null)
		{
			throw new CommandArgumentException("No genotypes are provided. Either --" + OPT_BFILE_LONG + " or --" + OPT_FILE_LONG + " must be set.");
		}
		
		if (bfile != null && file != null)
		{
			throw new CommandArgumentException("--" + OPT_BFILE_LONG + " and --" + OPT_FILE_LONG + " cannot be set together.");
		}
		
		blupArgs.setBFile(bfile);
		blupArgs.setFile(file);
	}

	@Override
	protected CommandImpl createCommandImpl()
	{
		return new BlupPCACommandImpl();
	}
	
	private final static String OPT_GRM_BIN_LONG = "grm-bin";
	private final static String OPT_GRM_BIN_DESC = "Specify the .grm.bin and .grm.id files";
	
	private final static String OPT_GRM_TEXT_LONG = "grm";
	private final static String OPT_GRM_TEXT_DESC = "Specify the .grm and .grm.id files";
	
	private final static String OPT_GRM_GZ_LONG = "grm-gz";
	private final static String OPT_GRM_GZ_DESC = "Specify the .grm.gz and .grm.id files";
	
	private final static String OPT_PHE_LONG = "phe";
	private final static String OPT_PHE_DESC = "Specify the phenotype file (individual eigenvector)";
}
