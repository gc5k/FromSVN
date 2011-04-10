package mdr;

public interface MDRConstant {
    public static int LinearSearch = 0;
    public static int TreeSearch = 1;
    public static int DynamicSearch = 2;
    public static int GenotypeSearch = 3;

    public static int NumStats = 2;
    public static int TestingBalancedAccuIdx = 0;
    public static int TrainingBalancedAccuIdx = 1;
    
    public static String[] TestStatistic = {"Testing Accuracy", "Training Accuracy", 
    						"Correlation", "CorTestingAccuracy",
    						"Cor_TrA_TA"};

    public static int RandomPartition = 0;
    public static int UnpairedPartition = 1;
    public static int PairedPartition = 2;
	public static Object seperator = ",";
    public static int tieValue = 1;
}
