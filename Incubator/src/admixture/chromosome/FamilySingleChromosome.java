package admixture.chromosome;

public class FamilySingleChromosome {
	private int[][][] p_g; // parental chromosomes;
	// p_g[0] for dad, p_g[1] for mom, [2][haploid=2][num of locus per chr]
	private int[][][] o_g; // offspring chromosomes; [kid number][haploid=2][num of locus per chr]

	private double[][][] ancestry_haploid_p; //[2][haploid=2][ancestry=num of ancetral populations]
	private double[][][] ancestry_haploid_o; //[kid number][haploid=2][ancestry=num of ancetral populations]

	private double[][] ancestry_diploid_p; //[2][ancestry=num of ancetral populations]
	private double[][] ancestry_diploid_o; //[kin number][ancestry=num of ancetral populations]

	public FamilySingleChromosome(int[][][] p, int[][][] o) {
		p_g = p;
		o_g = o;
	}

	public FamilySingleChromosome(int id, int num_kid) {
		p_g = new int[2][][];
		o_g = new int[num_kid][][];
	}

	public void AddFatherChr(int[][] g) {
		p_g[0] = g;
	}

	public void AddMotherChr(int[][] g) {
		p_g[1] = g;
	}

	public void AscertainParentSingleChromosomeAncestry(double[][][] post_snp_ancestry) {
		ancestry_haploid_p = new double[2][2][];
		for (int i = 0; i < p_g.length; i++) {
			for (int j = 0; j < p_g[i].length; j++) {
				ancestry_haploid_p[i][j] = AscertainHaploidAncestry(p_g[i][j], post_snp_ancestry);
			}
		}
		
		ancestry_diploid_p = new double[2][ancestry_haploid_p[0][0].length];
		for (int i = 0; i < ancestry_haploid_p.length; i++) {
			for (int j = 0; j < ancestry_haploid_p[i][0].length; j++) {
				ancestry_diploid_p[i][j] = (ancestry_haploid_p[i][0][j] + ancestry_haploid_p[i][1][j])/2;
			}
		}
	}

	public void AscertainOffspringSingleChromosomeAncestry(double[][][] post_snp_ancestry) {
		ancestry_haploid_o = new double[2][2][];
		for (int i = 0; i < o_g.length; i++) {
			for (int j = 0; j < o_g[i].length; j++) {
				ancestry_haploid_o[i][j] = AscertainHaploidAncestry(o_g[i][j], post_snp_ancestry);
			}
		}

		ancestry_diploid_o = new double[ancestry_haploid_o.length][ancestry_haploid_o[0][0].length];
		for (int i = 0; i < ancestry_haploid_o.length; i++) {
			for (int j = 0; j < ancestry_haploid_o[i][0].length; j++) {
				ancestry_diploid_o[i][j] = (ancestry_haploid_o[i][0][j] + ancestry_haploid_o[i][1][j])/2;
			}
		}
	}

	private double[] AscertainHaploidAncestry(int[] g, double[][][] post_snp_ancestry) {
		double[] ancestry = new double[post_snp_ancestry[0][0].length];
		for (int i = 0; i < g.length; i++) {//allele
			for (int j = 0; j < post_snp_ancestry[i][g[i]].length; j++) {//ancestry
				ancestry[j] += post_snp_ancestry[i][g[i]][j];
			}
		}
		for (int i = 0; i < ancestry.length; i++) {
			ancestry[i] /= g.length;
		}
		return ancestry;
	}

	public double[][] getParentChromosomeAncestry() {
		return ancestry_diploid_p;
	}

	public double[][] getOffspringChromosomeAncestry() {
		return ancestry_diploid_o;
	}
	
	public int[] ParentGenotype(int pi, int loci ) {
		int[] d = new int[2];
		d[0] = p_g[pi][0][loci];
		d[1] = p_g[pi][1][loci];
		return d;
	}
	
	public int[] OffspringGenotype(int oi, int loci ) {
		int[] d = new int[2];
		d[0] = o_g[oi][0][loci];
		d[1] = o_g[oi][1][loci];
		return d;
	}

}
