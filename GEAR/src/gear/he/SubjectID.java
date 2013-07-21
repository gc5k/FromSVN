package gear.he;

public class SubjectID
{
	public SubjectID(String famID, String indID)
	{
		this.famID = famID;
		this.indID = indID;
	}
	
	public String getFamilyID()
	{
		return famID;
	}
	
	public String getIndividualID()
	{
		return indID;
	}
	
	public boolean equals(Object obj)
	{
		return obj != null &&
			   obj instanceof SubjectID &&
			   ((SubjectID)obj).getFamilyID().equals(famID) &&
			   ((SubjectID)obj).getIndividualID().equals(indID);
	}
	
	public String toString()
	{
		return "[Family ID: " + famID + ", Individual ID: " + indID + "]";
	}
	
	private String famID;
	private String indID;
}
