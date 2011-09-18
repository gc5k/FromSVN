package admixture;

import java.io.IOException;
import java.util.Arrays;

import admixture.parameter.Parameter;

import family.mdr.AbstractMergeSearch;
import power.SimulationPower;
import family.mdr.HeteroCombinationSearchII;
import family.mdr.arsenal.MDRConstant;
import family.mdr.arsenal.ModelGenerator;
import family.mdr.arsenal.ModelGeneratorII;
import family.mdr.filter.softfilter.SoftSNPFilter;
import family.pedigree.design.hierarchy.ChenInterface;
import family.pedigree.design.hierarchy.SII;
import family.pedigree.design.hierarchy.Unified;
import family.pedigree.design.hierarchy.UnifiedII;
import family.pedigree.design.hierarchy.UnifiedUnrelated;
import family.plink.PLINKParser;
import family.popstat.AlleleFrequency;
import family.popstat.GenotypeMatrix;

/**
 * 
 * @author Guo-Bo Chen, chenguobo@gmail.com
 */

public class UnifiedGMDR {
	public static void main(String[] args) throws IOException {
		Parameter p = new Parameter();
		p.commandListenor(args);

		for (int i = 0; i < p.simu; i++) {
			String PedFile = Integer.toString(i) + "L_ped.txt";
			String PhenoFile = Integer.toString(i) + "score.txt";
			String MapFile = p.map;
			PLINKParser pp = new PLINKParser(PedFile, PhenoFile, MapFile);
			
			long s = p.seed;
			ChenInterface chen = null;
			if (p.mode.compareTo("u") == 0) {
				if(p.unrelated_only) {
					chen = new UnifiedUnrelated(pp.getPedigreeData(), pp.getPhenotypeData(), pp.getMapData(), s, p.response, p.predictor, p.linkfunction);
				} else if (p.permu_fam){
					chen = new UnifiedII(pp.getPedigreeData(), pp.getPhenotypeData(), pp.getMapData(), s, p.response, p.predictor, p.linkfunction);
				} else {
					chen = new Unified(pp.getPedigreeData(), pp.getPhenotypeData(), pp.getMapData(), s, p.response, p.predictor, p.linkfunction);
				}
			} else if (p.mode.compareTo("f") == 0) {
				chen = new SII(pp.getPedigreeData(), pp.getPhenotypeData(), pp.getMapData(), s, p.response, p.predictor, p.linkfunction);
			}

			GenotypeMatrix GM = new GenotypeMatrix(chen);
			AlleleFrequency af = new AlleleFrequency(GM);
			af.CalculateAlleleFrequency();
			pp.setAlleleFrequency(af.getAlleleFrequency());

			SoftSNPFilter snpFilterII = new SoftSNPFilter(pp.getSNPFilter(), af);

			AbstractMergeSearch as;
			ModelGenerator mg;
			if (Parameter.x) {
				mg = new ModelGeneratorII(snpFilterII.getWSeq2(), snpFilterII.getBgSeq());
			} else {
				mg = new ModelGenerator(snpFilterII.getWSeq(), snpFilterII.getBgSeq());
			}
			as = new HeteroCombinationSearchII.Builder(Parameter.cv, chen.getSample(), chen.getMapFile()).
			ModelGenerator(mg).mute(false).build();

			for (int j = p.order; j <= p.order; j++) {

				double[] pv = new double[p.permutation];
				for (int k = 0; k < p.permutation; k++) {
					chen.getPermutedScore(p.permu_scheme);
					as.search(j, 1);
					pv[k] = as.getModelStats()[MDRConstant.TestingBalancedAccuIdx];
				}

				Arrays.sort(pv);
				chen.RecoverScore();
				as.search(j, 1);
				System.out.println(as);

				SimulationPower sp = new SimulationPower(as.getMDRResult(), pv);
				sp.calculatePower();
				System.out.println(sp);
			}
		}
		System.out.println("type I error at alpha=0.05: " + SimulationPower.typeI_005 + "/" + p.simu);
		System.out.println("type I error at alpha=0.01: " + SimulationPower.typeI_001 + "/" + p.simu);
		System.out.println("power at alpha = 0.05: " + SimulationPower.power_005 + "/" + p.simu);
		System.out.println("power at alpha = 0.01: " + SimulationPower.power_001 + "/" + p.simu);
	}
}
