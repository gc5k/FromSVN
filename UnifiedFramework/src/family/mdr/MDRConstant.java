package family.mdr;

/**
 * 
 * @author Guo-Bo Chen, chenguobo@gmail.com
 */
public interface MDRConstant {
    public static int LinearSearch = 0;
    public static int TreeSearch = 1;
    public static int DynamicSearch = 2;
    public static int GenotypeSearch = 3;

    public static int NumStats = 2;
    public static int TestingBalancedAccuIdx = 0;
    public static int TrainingBalancedAccuIdx = 1;
    
    public static String[] TestStatistic = {"BTA (Balanced Testing Accuracy)", "BTrA (Balanced Training Accuracy)"};

    public static int RandomPartition = 0;
    public static int UnpairedPartition = 1;
    public static int PairedPartition = 2;
	public static Object seperator = ",";
    public static int tieValue = 1;
    public static String missingGenotype = "44";
}
