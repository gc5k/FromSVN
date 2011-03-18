package admixture.population;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import org.apache.commons.lang3.ArrayUtils;


/**
*
* @author Guo-Bo Chen, chenguobo@gmail.com
*/
public class AlleleFrequencyReader {

	private double[][] AlleleFreq;
	private String[] population;
	private String[] snpNames;

	public AlleleFrequencyReader(String f) {

		String delim = "\\s+";
		FileReader r1 = null;
		FileReader r2 = null;
		try {
			r1 = new FileReader(f);
			r2 = new FileReader(f);
		} catch (IOException E) {
			System.err.println("Could not find " + f);
			E.printStackTrace(System.err);
			System.exit(0);
		}


		LineNumberReader l2 = new LineNumberReader(r2);

		String line = null;
		try {
			while((line = l2.readLine()) != null) {};
		}catch (IOException E) {
			E.printStackTrace(System.err);
		}

		LineNumberReader l = new LineNumberReader(r1);
		BufferedReader b = new BufferedReader(l);

		try {
			if ((line = b.readLine()) != null) {
				population = line.split(delim);
			}
		} catch (IOException E) {
			E.printStackTrace(System.err);
		}

		population = ArrayUtils.removeElement(population, population[0]);
		AlleleFreq = new double[population.length][l2.getLineNumber() - 1];
		snpNames = new String[l2.getLineNumber() - 1];

		int c = 0;
		try {
			while ((line = b.readLine()) != null) {
				String[] fields = line.split(delim);
				snpNames[c] = fields[0]; 
				for (int i = 1; i < fields.length; i++) {
					AlleleFreq[i - 1][c] = Double.parseDouble(fields[i]);
				}
				c++;
			}
		} catch (IOException E) {
			E.printStackTrace(System.err);
		}
	}

	public double[][] getAlleleFreq() {
		return AlleleFreq;
	}

	public int getNumberPopulation() {
		return AlleleFreq.length;
	}

	public String[] getSNPName() {
		return snpNames;
	}

	public int getNumberSNP() {
		return AlleleFreq[0].length;
	}

	public static void main(String[] args) {
		String f = "allele_freq_chr1.txt";
		AlleleFrequencyReader afr = new AlleleFrequencyReader(f);
		double[][] af = afr.getAlleleFreq();
		System.out.println(af[0][0] + " " + af[1][0] + " " +af[0][af[0].length-1] + " " + af[1][af[0].length-1]);
	}
}
