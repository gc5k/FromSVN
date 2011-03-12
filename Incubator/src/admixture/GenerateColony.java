package admixture;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import admixture.chromosome.FamilyGenome;
import admixture.chromosome.FamilySingleChromosome;
import admixture.phenotype.FamilyPhenotype;
import admixture.phenotype.PhenotypeGenerator;
import admixture.phenotype.QualityControl;

public class GenerateColony {

	private int N_phe;
	private long seed;
	private PhenotypeGenerator pg;
	private HotSpot hs;
	private Habitat FamHab;
	private Habitat CaseControlHab;
	private ArrayList<DNAStirrer> DNAPool;
	private ArrayList<ChromosomeGenerator> ChrGenerator;
	private int disease_chr;
	private double[] disease_rate;
	private boolean recombination_free;
	private int CurrFam;
	public GenerateColony (int np, long s, int dc, double[] dr, HotSpot h, ArrayList<DNAStirrer> dp, ArrayList<ChromosomeGenerator> cg,
			PhenotypeGenerator p, boolean rf) {
		N_phe = np;
		seed = s;
		disease_chr = dc;
		disease_rate = dr;
		hs = h;
		DNAPool = dp;
		ChrGenerator = cg;
		pg = p;
		recombination_free = rf;

		hs.setSeed(seed);
		pg.setSeed(seed);
	}

	public void GenerateNewFamHab(int N_Fam, int N_Kid, QualityControl qc) {
		FamHab = new Habitat();
		generateFamilies(FamHab, N_Fam, N_Kid, qc);
	}
	
	public void GenerateCCHab(int N_Fam, int N_Kid, QualityControl qc) {
		CaseControlHab = new Habitat();
		generateFamilies(CaseControlHab, N_Fam, N_Kid, qc);
	}
	
	private void generateFamilies(Habitat hab, int N_Fam, int N_Kid, QualityControl qc) {
		for (int i = 0; i < N_Fam; i++) {
			FamilyGenome fg = new FamilyGenome(i+1, N_Kid);
			FamilyPhenotype fp;
			int r = 0;
			do {
				for (int j = 0; j < DNAPool.size(); j++) {
					if (r > 0 && disease_chr != j) {
						continue;
					}
					int chrID = j;
					DNAStirrer ds = DNAPool.get(j);
					ChromosomeGenerator cg = ChrGenerator.get(j);
					hs.rev(ds.NumberOfSNP());
					hs.GenerateRecombination(AdmixtureConstant.free_recombination);
					int[] f_hotspot = hs.getHotSpot();
					hs.GenerateRecombination(AdmixtureConstant.free_recombination);
					int[] m_hotspot = hs.getHotSpot();
					if (r == 0) {
						fg.addFamilyChromosome(cg.generateFamilySingleChromosome(chrID, N_Kid, f_hotspot, m_hotspot,
								ds.PostSNPAncestralProb(), disease_chr == j));
					} else {
						fg.setFamilyChromosome(j, cg.generateFamilySingleChromosome(chrID, N_Kid, f_hotspot,
								m_hotspot, ds.PostSNPAncestralProb(), disease_chr == j));
					}
				}
				if(disease_rate == null) {
					fp = pg.getGeneratePhenotypeLogistic(fg);
				} else {
					fp = pg.getGeneratePhenotypeAncestry(fg, disease_rate);
				}
				r++;
			} while (!qc.Accept(fp));
			hab.AddFamilyGenome(fg);
			hab.AddFamilyPhenotype(fp);
		}
		CurrFam += N_Fam;
	}

	public void printAllele2file(String ped, String phe) throws IOException {
		PrintWriter pedout = new PrintWriter(new File(ped));
		PrintWriter pheout = new PrintWriter(new File(phe));
		ArrayList<FamilyGenome> FamG = FamHab.getFamilyGenome();
		ArrayList<FamilyPhenotype> FamP = FamHab.getFamilyPhenotype();
		pedout.print("FID ID FA MO SEX Affection ");
		for(int i = 0; i < DNAPool.size(); i++) {
			DNAStirrer ds = DNAPool.get(i);
			String[] SN = ds.getSNPNames();
			for(int j = 0; j < SN.length; j++) {
				pedout.print(SN[j] + " ");
			}
		}
		pedout.println();
		
		pheout.print("FID ID ");
		for(int i = 0; i < N_phe; i++) {
			pheout.print("phe" + i + " ");
		}
		pheout.println();
		for(int f = 0; f < FamP.size(); f++) {
			//print phenotype
			FamilyPhenotype fp = FamP.get(f);
			StringBuffer[] sp = new StringBuffer[2 + fp.getNumberOffspring()];

			for(int i = 0; i < sp.length; i++) {
				sp[i] = new StringBuffer();
				sp[i].append(fp.getFamilyID() + " " + fp.getFamilyID() * 10000 + i);
			}
			sp[0].append(fp.getStringParentPhenotype(0));
			sp[1].append(fp.getStringParentPhenotype(1));

			for(int i = 0; i < fp.getNumberOffspring(); i++) {
				sp[i + 2].append(fp.getStringOffspringPhenotype(i));
			}
			for(int i = 0; i < sp.length; i++) {
				pheout.println(sp[i].toString());
			}		

			//print genotype
			FamilyGenome fg = FamG.get(f);
			StringBuffer[] sb = new StringBuffer[2 + fg.getNumberOffspring()];
			for(int i = 0; i < sb.length; i++) {
				sb[i] = new StringBuffer();
				sb[i].append(fg.getFamilyID() + " " + fg.getIndividualID(i) + " ");
			}
			sb[0].append(0 + " " + 0 + " " + 1 + " " + fp.getParentStatus(0) + " ");
			sb[1].append(0 + " " + 0 + " " + 2 + " " + fp.getParentStatus(1) + " ");
			
			for(int i = 0; i < fp.getNumberOffspring(); i++) {
				sb[2+i].append(fg.getFatherID() + " " + fg.getMotherID() + " " + 1 + " " + fp.getOffspringStatus(i) + " ");
			}

			for(FamilySingleChromosome fsc:fg) {
				sb[0].append(fsc.getStringParentChromosome(0));
				sb[1].append(fsc.getStringParentChromosome(1));
				for(int i = 0; i < fg.getNumberOffspring(); i++)
				sb[i+2].append(fsc.getStringOffspringChromosome(i));
			}
			
			for(int i = 0; i < sb.length; i++) {
				pedout.println(sb[i].toString());
			}
		}
		pedout.close();
		pheout.close();
	}
	
	public void printGenotype2file(String ped, String phe) throws IOException {
		PrintWriter pedout = new PrintWriter(new File(ped));
		PrintWriter pheout = new PrintWriter(new File(phe));
		ArrayList<FamilyGenome> FamG = FamHab.getFamilyGenome();
		ArrayList<FamilyPhenotype> FamP = FamHab.getFamilyPhenotype();
		
		pedout.print("FID ID FA MO SEX Affection ");
		for(int i = 0; i < DNAPool.size(); i++) {
			DNAStirrer ds = DNAPool.get(i);
			String[] SN = ds.getSNPNames();
			for(int j = 0; j < SN.length; j++) {
				pedout.print(SN[j] + " ");
			}
		}
		pedout.println();
		
		pheout.print("FID ID ");
		for(int i = 0; i < N_phe; i++) {
			pheout.print("phe" + i + " ");
		}
		pheout.println();
		for(int f = 0; f < FamP.size(); f++) {
			//print phenotype
			FamilyPhenotype fp = FamP.get(f);
			StringBuffer[] sp = new StringBuffer[2 + fp.getNumberOffspring()];

			for(int i = 0; i < sp.length; i++) {
				sp[i] = new StringBuffer();
				sp[i].append(fp.getFamilyID() + " " + fp.getFamilyID() * 10000 + i);
			}
			sp[0].append(fp.getStringParentPhenotype(0));
			sp[1].append(fp.getStringParentPhenotype(1));

			for(int i = 0; i < fp.getNumberOffspring(); i++) {
				sp[i + 2].append(fp.getStringOffspringPhenotype(i));
			}
			for(int i = 0; i < sp.length; i++) {
				pheout.println(sp[i].toString());
			}		

			//print genotype
			FamilyGenome fg = FamG.get(f);
			StringBuffer[] sb = new StringBuffer[2 + fg.getNumberOffspring()];
			for(int i = 0; i < sb.length; i++) {
				sb[i] = new StringBuffer();
				sb[i].append(fg.getFamilyID() + " " + fg.getIndividualID(i) + " ");
			}
			sb[0].append(0 + " " + 0 + " " + 1 + " " + fp.getParentStatus(0) + " ");
			sb[1].append(0 + " " + 0 + " " + 2 + " " + fp.getParentStatus(1) + " ");
			
			for(int i = 0; i < fp.getNumberOffspring(); i++) {
				sb[2+i].append(fg.getFatherID() + " " + fg.getMotherID() + " " + 1 + " " + fp.getOffspringStatus(i) + " ");
			}

			for(FamilySingleChromosome fsc:fg) {
				sb[0].append(fsc.getGenotypeStringParentChromosome(0));
				sb[1].append(fsc.getGenotypeStringParentChromosome(1));
				for(int i = 0; i < fg.getNumberOffspring(); i++)
				sb[i+2].append(fsc.getGenotypeStringOffspringChromosome(i));
			}
			
			for(int i = 0; i < sb.length; i++) {
				pedout.println(sb[i].toString());
			}
		}
		pedout.close();
		pheout.close();	
	}	
}
