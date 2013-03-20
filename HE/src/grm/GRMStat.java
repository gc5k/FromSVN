package grm;

import he.endian.LittleEndianDataInputStream;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import parameter.Parameter;
import util.FileProcessor;

public class GRMStat {

	private final String delim = "\\s+";

	private double Nt=0;
	private double N = 0;
	private double mean = 0;
	private double prod = 0;
	private double v = 0;
	private double Ne = 0;
	private double Me = 0;

	StringBuffer sb = new StringBuffer();
	
	public GRMStat() {
		if(Parameter.INSTANCE.getHEParameter().isGrmBinary()) {
			BinaryGRM();
		} else {
			gzGRM();
		}

		double s = Nt - N;

		sb.append("grm file: " + Parameter.INSTANCE.getHEParameter().getGrm() + "\n");

		sb.append("Total lines in grm: " + Nt + "\n");
		sb.append("Read " + N + " lines." + "\n");
		sb.append("Individuals: " + s + "\n");
		sb.append("Mean is " + mean + "\n");
		sb.append("Variance " + v + "\n");
		sb.append("The effective sample size is " + Ne + "\n");
		sb.append("Effective number of markers is " + Me + "\n");

		System.out.print(sb);

		StringBuilder fsb = new StringBuilder();
		fsb.append(Parameter.INSTANCE.out);
		fsb.append(".gs");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(fsb.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		pw.append(sb);
		pw.close();
	}
	
	private void BinaryGRM() {
		
		BufferedReader reader = FileProcessor.FileOpen(Parameter.INSTANCE.getHEParameter().getGrmId());

		String line = null;
		int size = 0;
		try {
			while ((line = reader.readLine()) != null) {
				size++;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// *************************************read grm file
		FileInputStream fileStream = null;
		try {
			fileStream = new FileInputStream(Parameter.INSTANCE.getHEParameter().getGrm());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataInputStream bigEndianDataStream = new DataInputStream(fileStream);
		LittleEndianDataInputStream littleEndianDataStream = new LittleEndianDataInputStream(bigEndianDataStream, Float.SIZE);

		for (int i = 0; i < size; i++) {
			for (int j = 0; j <= i; j++) {
				Nt++;
				double g = 0;
				try {
					if (littleEndianDataStream.available()>0) {
						g = littleEndianDataStream.readFloat();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				int id1 = i;
				int id2 = j;
				if (id1 == id2)
					continue;

				mean += g;
				prod += g*g;
				N++;
			}
		}

		getEffectiveNumber();
	}
	
	private void gzGRM() {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(Parameter.INSTANCE.getHEParameter().getGrm());
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		GZIPInputStream gzis = null;
		try {
			gzis = new GZIPInputStream(fin);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		InputStreamReader xover = new InputStreamReader(gzis);

		BufferedReader grmFile = new BufferedReader(xover);

		String line;
		try {
			while ((line = grmFile.readLine()) != null) {
				Nt++;
				String[] s = line.split(delim);
				int id1 = Integer.parseInt(s[0]) - 1;
				int id2 = Integer.parseInt(s[1]) - 1;

				if (id1 == id2)//exclude diagonal if it is required
					continue;
				double g = Double.parseDouble(s[3]);
				mean += g;
				prod += g*g;
				N++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		getEffectiveNumber();
	}

	private void getEffectiveNumber() {
		mean /= N;
		prod /= N;
		v = prod - mean * mean;
		Ne = -1/mean + 1;
		Me = 1/v;
	}
}
