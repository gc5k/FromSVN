package family.RabinowitzLairdAlgorithm;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Rabinowitz3 extends AbstractGenoDistribution {
    public Rabinowitz3(TreeMap children) {
        super(children);
        countChildrenAllele(childrenGenoMap);
        countAllele(childrenGenoMap);
    }

    protected void genotypeParents() {
        // TODO Auto-generated method stub
    }

    public String[] getNontransmitted(final String transmitted) {
        return null;
    }

    private void print(String[] control) {
        for (int i = 0; i < control.length; i++) {
            System.out.print(control[i] + "\t");
        }
        System.out.println();
    }

    public String[] getNontransmitted() {
        String[] control = new String[getChildrenNum()];
        TreeMap controlMap = new TreeMap();
        if (childrenGenoMap.size() == 1) {// situation 1
            // System.err.println("Rabinowitz Table3 s1");

            String[] genopool = new String[1];
            genopool[0] = (String) childrenGenoMap.firstKey();
            double[] freq = new double[1];
            freq[0] = 1.0;
            Produce(control, controlMap, genopool, freq);
        } else if (childrenGenoMap.size() == 2) {
            if (numHomozygous(childrenGenoMap) == 0) {
                if (numChildrenAllele() == 3) {// 5
                    // System.err.println("Rabinowitz Table3 s5");

                    String[] genopool = new String[2];
                    genopool[0] = (String) childrenGenoMap.firstKey();
                    genopool[1] = (String) childrenGenoMap.lastKey();
                    double[] freq = new double[2];
                    freq[0] = 0.5;
                    freq[1] = 1;
                    do {
                        controlMap.clear();
                        Produce(control, controlMap, genopool, freq);
                    } while (!Criteria3_5(controlMap));
                } else {// 7
                    // System.err.println("Rabinowitz Table3 s7");

                    String[] genopool = new String[2];
                    genopool[0] = (String) childrenGenoMap.firstKey();
                    genopool[1] = (String) childrenGenoMap.lastKey();
                    double[] freq = new double[2];
                    freq[0] = 0.5;
                    freq[1] = 1;
                    do {
                        controlMap.clear();
                        Produce(control, controlMap, genopool, freq);
                    } while (!Criteria3_7(controlMap));
                }
            }
            if (numHomozygous(childrenGenoMap) == 1) {
                if (numChildrenAllele() == 2) {// 2
                    // System.err.println("Rabinowitz Table3 s2");

                    shuffle(control);
                } else {// 6-1
                    // System.err.println("Rabinowitz Table3 s6-1");

                    String[] genopool = new String[4];
                    genopool[0] = (String) childrenGenoMap.firstKey();
                    genopool[1] = (String) childrenGenoMap.lastKey();
                    String Deducedgeno1 = new String();
                    String Deducedgeno2 = new String();
                    char allele[][] = new char[2][2];
                    if (genopool[0].charAt(0) <= genopool[1].charAt(0)) {
                        allele[0][0] = genopool[0].charAt(0);
                        allele[0][1] = genopool[1].charAt(0);
                    } else {
                        allele[0][0] = genopool[1].charAt(0);
                        allele[0][1] = genopool[0].charAt(0);
                    }
                    if (genopool[0].charAt(1) <= genopool[1].charAt(1)) {
                        allele[1][0] = genopool[0].charAt(1);
                        allele[1][1] = genopool[1].charAt(1);
                    } else {
                        allele[1][0] = genopool[1].charAt(1);
                        allele[1][1] = genopool[0].charAt(1);
                    }
                    String geno = new String(allele[0]);
                    genopool[2] = geno;
                    geno = new String(allele[1]);
                    genopool[3] = new String(geno);
                    double[] freq = new double[4];
                    freq[0] = 0.25;
                    freq[1] = 0.5;
                    freq[2] = 0.75;
                    freq[3] = 1;
                    do {
                        controlMap.clear();
                        Produce(control, controlMap, genopool, freq);
                    } while (!Criteria3_6(controlMap));
                }
            }
            if (numHomozygous(childrenGenoMap) == 2) {// 3-1
                // System.err.println("Rabinowitz Table3 s3-1");

                String[] genopool = new String[3];
                genopool[0] = (String) childrenGenoMap.firstKey();
                genopool[1] = (String) childrenGenoMap.lastKey();
                char allele[] = new char[2];
                if (genopool[0].charAt(0) <= genopool[1].charAt(0)) {
                    allele[0] = genopool[0].charAt(0);
                    allele[1] = genopool[1].charAt(0);
                } else {
                    allele[0] = genopool[1].charAt(0);
                    allele[1] = genopool[0].charAt(0);
                }
                String geno = new String(allele);
                genopool[2] = geno;
                double[] freq = new double[3];
                freq[0] = 0.25;
                freq[1] = 0.5;
                freq[2] = 1;
                do {
                    controlMap.clear();
                    Produce(control, controlMap, genopool, freq);
                } while (!Criteria3_3(controlMap));
            }
        } else if (childrenGenoMap.size() == 3) {
            if (numHomozygous(childrenGenoMap) == 0) {
                if (numChildrenAllele() == 3) {// 4
                    // System.err.println("Rabinowitz Table3 s4");

                    String[] genopool = new String[3];
                    Set CGSet = childrenGenoMap.keySet();
                    Iterator it = CGSet.iterator();
                    int index = 0;
                    for (; it.hasNext(); index++) {
                        genopool[index] = (String) it.next();
                    }
                    double[] freq = new double[3];
                    freq[0] = 0.33333;
                    freq[1] = 0.66667;
                    freq[2] = 1.0;
                    do {
                        controlMap.clear();
                        Produce(control, controlMap, genopool, freq);
                    } while (!Criteria3_4(controlMap));
                } else {// 8-1
                    // System.err.println("Rabinowitz Table3 s8-1");

                    String[] genopool = new String[4];
                    Set CGSet = childrenGenoMap.keySet();
                    Iterator it = CGSet.iterator();
                    int index = 0;
                    for (; it.hasNext(); index++) {
                        genopool[index] = (String) it.next();
                    }
                    genopool[3] = CompatibleGenotype();
                    double[] freq = new double[4];
                    freq[0] = 0.25;
                    freq[1] = 0.5;
                    freq[2] = 0.75;
                    freq[3] = 1.0;
                    do {
                        controlMap.clear();
                        Produce(control, controlMap, genopool, freq);
                    } while (!Criteria3_8(controlMap));
                }
            }
            if (numHomozygous(childrenGenoMap) == 1) {// 6-2,6-3
                // System.err.println("Rabinowitz Table3 s6-2,6-3");

                String[] genopool = new String[4];
                Set CGSet = childrenGenoMap.keySet();
                Iterator it = CGSet.iterator();
                int index = 0;
                String[] tempgeno = new String[2];
                int ind = 0;
                for (; it.hasNext(); index++) {
                    genopool[index] = (String) it.next();
                    if (isHeterozygous(genopool[index])) {
                        tempgeno[ind++] = genopool[index];
                    }
                }
                String geno = new String(ExtractUniqueAllele2Genotype(
                        tempgeno[0], tempgeno[1]));
                genopool[3] = geno;

                double[] freq = new double[4];
                freq[0] = 0.25;
                freq[1] = 0.5;
                freq[2] = 0.75;
                freq[3] = 1.0;
                do {
                    controlMap.clear();
                    Produce(control, controlMap, genopool, freq);
                } while (!Criteria3_6(controlMap));
            }
            if (numHomozygous(childrenGenoMap) == 2) {// 3-2
                // System.err.println("Rabinowitz Table3 s3-2");

                String[] genopool = new String[3];
                Set CGSet = childrenGenoMap.keySet();
                Iterator it = CGSet.iterator();
                int index = 0;
                for (; it.hasNext(); index++) {
                    genopool[index] = (String) it.next();
                }
                double[] freq = new double[3];
                freq[0] = 0.25;
                freq[1] = 0.5;
                freq[2] = 1.0;
                do {
                    controlMap.clear();
                    Produce(control, controlMap, genopool, freq);
                } while (!Criteria3_3(controlMap));
            }
        } else if (childrenGenoMap.size() == 4) {
            if (numHomozygous(childrenGenoMap) == 1) {// 6-4
                // System.err.println("Rabinowitz Table3 s6-4");

                String[] genopool = new String[4];
                Set CGSet = childrenGenoMap.keySet();
                Iterator it = CGSet.iterator();
                int index = 0;
                for (; it.hasNext(); index++) {
                    genopool[index] = (String) it.next();
                }
                double[] freq = new double[4];
                freq[0] = 0.25;
                freq[1] = 0.5;
                freq[2] = 0.75;
                freq[3] = 1.0;
                do {
                    controlMap.clear();
                    Produce(control, controlMap, genopool, freq);
                } while (!Criteria3_6(controlMap));
            } else {// 8-2
                // System.err.println("Rabinowitz Table3 s8-2");

                String[] genopool = new String[4];
                Set CGSet = childrenGenoMap.keySet();
                Iterator it = CGSet.iterator();
                int index = 0;
                for (; it.hasNext(); index++) {
                    genopool[index] = (String) it.next();
                }
                double[] freq = new double[4];
                freq[0] = 0.25;
                freq[1] = 0.5;
                freq[2] = 0.75;
                freq[3] = 1.0;
                do {
                    controlMap.clear();
                    Produce(control, controlMap, genopool, freq);
                } while (!Criteria3_8(controlMap));
            }
        } else {
            System.err.println("Wrecked in Rabinowitz table 3");
        }
        // print(control);
        return control;
    }

    boolean Criteria3_3(TreeMap controlMap) {
        if (numHomozygous(controlMap) == 2) {
            return true;
        } else {
            return false;
        }
    }

    boolean Criteria3_4(TreeMap controlMap) {
        if (controlMap.size() == 3) {
            return true;
        } else {
            return false;
        }
    }

    boolean Criteria3_5(TreeMap controlMap) {
        if (controlMap.size() == 2) {
            return true;
        } else {
            return false;
        }
    }

    boolean Criteria3_6(TreeMap controlMap) {
        if (numHomozygous(controlMap) > 0) {
            Set controlSet = new TreeSet();
            Set GSet = controlMap.keySet();
            Iterator it = GSet.iterator();
            for (; it.hasNext();) {
                String g = (String) it.next();
                controlSet.add(g.substring(0, 1));
                controlSet.add(g.substring(1, 2));
            }
            if (controlSet.size() == 3) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    boolean Criteria3_7(TreeMap controlMap) {
        if (controlMap.size() == 2) {
            return true;
        } else {
            return false;
        }
    }

    boolean Criteria3_8(TreeMap controlMap) {
        if (controlMap.size() >= 3) {
            return true;
        } else {
            return false;
        }
    }

    void shuffle(String[] control) {
        // print(control);
        Set CGSet = childrenGenoMap.keySet();
        Iterator it = CGSet.iterator();
        int[] CGSetSize = new int[CGSet.size()];
        int index = 0;
        int offset = 0;
        for (; it.hasNext(); index++) {
            String geno = (String) it.next();
            CGSetSize[index] = ((Integer) childrenGenoMap.get(geno)).intValue();
            for (int j = 0; j < CGSetSize[index]; j++) {
                control[j + offset] = geno;
            }
            offset += CGSetSize[index];
        }
        // print(control);
        int N = control.length;
        for (int i = 0; i < N; i++) {
            int Ind = i + (int) (rnd.nextFloat() * (N - i));
            String tmp = control[i];
            control[i] = control[Ind];
            control[Ind] = tmp;
        }
    // print(control);
    }

    String CompatibleGenotype() {
        TreeMap alleleMap = new TreeMap();
        Set GSet = childrenGenoMap.keySet();
        Iterator it = GSet.iterator();
        for (; it.hasNext();) {
            String g = (String) it.next();
            for (int i = 0; i < 2; i++) {
                if (alleleMap.containsKey(g.substring(i + 0, i + 1))) {
                    Integer c = ((Integer) alleleMap.get(g.substring(i + 0,
                            i + 1)));
                    int v = (c.intValue());
                    v++;
                    c = new Integer(v);
                    alleleMap.put(g.substring(i + 0, i + 1), c);
                } else {
                    Integer c = new Integer(1);
                    alleleMap.put(new String(g.substring(i + 0, i + 1)), c);
                }
            }
        }
        String geno = new String();
        Set ASet = alleleMap.keySet();
        it = ASet.iterator();
        for (; it.hasNext();) {
            String allele = (String) it.next();
            if (((Integer) alleleMap.get(allele) == 1)) {
                geno += allele;
            }
        }
        return geno;
    }
}