package gear.subcommands.oath.nss;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import gear.subcommands.Command;
import gear.subcommands.CommandArgumentException;
import gear.subcommands.CommandArguments;
import gear.subcommands.CommandImpl;

public class NSSCommand extends Command 
{

	@Override
	public String getName() 
	{
		return "nss";
	}

	@Override
	public String getDescription() 
	{
		return "Generates naive summary statistics.";
	}

	@SuppressWarnings("static-access")
	@Override
	public void prepareOptions(Options options)
	{
		options.addOption(OptionBuilder.withDescription(OPT_BFILE_DESC).withLongOpt(OPT_BFILE_LONG).hasArg().isRequired().create());
		options.addOption(OptionBuilder.withDescription(OPT_PHE_DESC).hasArg().isRequired().create(OPT_PHE));
		options.addOption(OptionBuilder.withDescription(OPT_MPHE_DESC).hasArg().isRequired().create(OPT_MPHE));
		options.addOption(OptionBuilder.withDescription(OPT_COVAR_DESC).hasArg().isRequired().create(OPT_COVAR));
		options.addOption(OptionBuilder.withDescription(OPT_COVAR_NUMBER_DESC).withLongOpt(OPT_COVAR_NUMBER).hasArgs().create());
		options.addOption(OptionBuilder.withDescription(OPT_CHR_DESC).hasArg().create(OPT_CHR));
		options.addOption(OptionBuilder.withDescription(OPT_KEEP_DESC).hasArg().create(OPT_KEEP));
	}

	@Override
	public CommandArguments parse(CommandLine cmdLine) throws CommandArgumentException
	{
		NSSCommandArguments nssArgs = new NSSCommandArguments();
		parseFileArguments(nssArgs, cmdLine);
		nssArgs.setPhenotypeFile(cmdLine.getOptionValue(OPT_PHE));

		if (cmdLine.hasOption(OPT_MPHE))
		{
			nssArgs.setPhentypeIndex(cmdLine.getOptionValue(OPT_MPHE));
		}

		nssArgs.setCovFile(cmdLine.getOptionValue(OPT_COVAR));

		if (cmdLine.hasOption(OPT_COVAR_NUMBER))
		{
			nssArgs.setCovNumber(cmdLine.getOptionValues(OPT_COVAR_NUMBER));
		}

		if (cmdLine.hasOption(OPT_KEEP))
		{
			nssArgs.setKeeFile(cmdLine.getOptionValue(OPT_KEEP));
		}
		
		if (cmdLine.hasOption(OPT_CHR))
		{
			nssArgs.setChr(cmdLine.getOptionValue(OPT_CHR));
		}

		return nssArgs;
	}

	private void parseFileArguments(NSSCommandArguments nssArgs, CommandLine cmdLine) throws CommandArgumentException
	{
		String bfile = cmdLine.getOptionValue("bfile");
		String file = cmdLine.getOptionValue("file");

		if ((bfile == null) && (file == null))
		{
			throw new CommandArgumentException("No genotypes are provided. Either --bfile or --file must be set.");
		}

		if ((bfile != null) && (file != null))
		{
			throw new CommandArgumentException("--bfile and --file cannot be set together.");
		}

		nssArgs.setBFile(bfile);
		nssArgs.setFile(file);

		if (cmdLine.hasOption(OPT_CHR))
		{
			nssArgs.setChr(cmdLine.getOptionValue(OPT_CHR));
		}
	}

	@Override
	protected CommandImpl createCommandImpl()
	{
		return new NSSCommandImpl();
	}

	private static final String OPT_PHE = "pheno";
	private static final String OPT_PHE_DESC = "Specify the phenotype file (individual eigenvector)";

	private static final String OPT_MPHE = "mpheno";
	private static final String OPT_MPHE_DESC = "Specify the phenotype indicex";

	private final static String OPT_COVAR = "covar";
	private final static String OPT_COVAR_DESC = "Specify the covariate file";

	private final static String OPT_COVAR_NUMBER = "covar-number";
	private final static String OPT_COVAR_NUMBER_DESC = "Specify the indices for covariate file";

	private static final String OPT_CHR = "chr";
	private static final String OPT_CHR_DESC = "Specify the chromosomes for analysis";

	private static final String OPT_KEEP = "keep";
	private static final String OPT_KEEP_DESC = "Specify the samples for the analysis";

}
