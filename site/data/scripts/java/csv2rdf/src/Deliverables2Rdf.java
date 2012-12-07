import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Converts the deliverables from a csv tables to RDF Turtle adhering to the research project ontology
 * http://purl.org/research-fp#. Prefixes not included. Columns are expected to be [Del. No, Deliverable name, WP no., Nature, Diss. level, Delivery  date].
 * Needs Java 7 (else you have to trivially rewrite the program by removing the try-with-resource). Output goes to standard out and standard err.
 */
public class Deliverables2Rdf
{
	static Map<String,String> deliverableMap = null;
	static {
		deliverableMap = new HashMap<>();
		deliverableMap.put("R",":Report");
		deliverableMap.put("P",":Prototype");
		deliverableMap.put("D",":Demonstrator");
		deliverableMap.put("O",":Other");		
	}

	static void printlnProperty(PrintStream out,String property, int object, boolean last)
	{
		out.println('\t'+property+" \""+object+"\"^^xsd:nonNegativeInteger"+(last?'.':';'));		
	}

	static void printlnProperty(PrintStream out,String property, String object, boolean last)
	{
		out.println('\t'+property+' '+object+(last?'.':';'));		
	}
	
	public static void main(String[] args) throws IOException
	{		
		File f = new File((args.length>0)?args[0]:"input/deliverables.csv");
		PrintStream out = System.out;
		
		try(CSVReader reader = new CSVReader(new FileReader(f)))
		{
			boolean first = true;
			for(String[] row : reader.readAll())
			{								
				try
				{
					String[] deliverableIdentifier = row[0].substring(1).split("\\.");
					// does not represent a deliverable record, could be a title row or empty												
					int workpackageNumber	= Integer.parseInt(deliverableIdentifier[0]);
					int taskNumber			= Integer.parseInt(deliverableIdentifier[1]);
					int deliverableNumber	= Integer.parseInt(deliverableIdentifier[2]);
					String uri = "gk:D-"+workpackageNumber+'-'+taskNumber+'-'+deliverableNumber;

					String label = "\""+row[1].trim()+"\"@en";												
					String nature = deliverableMap.get(row[3].trim());					
					if(nature==null) throw new AssertionError("nature is null");
					String disseminationLevel = ":"+row[4].trim();
					String deliveryDate = "\""+row[5].trim()+"\"^^xsd:nonNegativeInteger";					
					
					if(!first) {out.println();}
					first=false;
					out.println(uri+" a fp:Deliverable;");
					printlnProperty(out,"rdfs:label",label,false);
					printlnProperty(out,"fp:deliverableIdentifier","\""+row[0].trim()+"\"",false);
//					printlnProperty(out,":workpackageNumber",workpackageNumber,false);
					printlnProperty(out,"fp:task",":Task"+workpackageNumber+"-"+taskNumber,false);
					printlnProperty(out,"fp:deliverableNature",nature,false);
					printlnProperty(out,"fp:deliverableNumber",deliverableNumber,false);
					printlnProperty(out,"fp:disseminationLevel",disseminationLevel,false);
					printlnProperty(out,"fp:deliveryDate",deliveryDate,true);
				} 
				catch(Throwable t) {System.err.println("Error with line :"+Arrays.toString(row)+": "+t);continue;}
			}			
		}
	}
}