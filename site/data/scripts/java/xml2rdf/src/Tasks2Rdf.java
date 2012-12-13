import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * Converts the tasks from a custom xml format (see the input file) RDF Turtle adhering to the research project ontology.
 * http://purl.org/research-fp#. Prefixes not included.
 */

public class Tasks2Rdf
{
	static final Set<String> partnerNames = new HashSet<>(Arrays.asList(new String[] {"Athena","ATH","Ontos","Openlink","OpenLink","BROX","Unister","InfAI"}));
	
	public static void main(String[] args) throws XPathExpressionException, IOException
	{
		Model model = ModelFactory.createMemModelMaker().createDefaultModel();
		String fp = "http://purl.org/research-fp#";
		String gk = "http://geoknow.eu/";
		model.setNsPrefix("fp",fp);
		model.setNsPrefix("gk",gk);
		model.setNsPrefix("rdfs",RDFS.getURI());
		model.setNsPrefix("xsd",XSD.getURI());
		
		File f = new File((args.length>0)?args[0]:"../../../tasks.xml");
		if(!f.exists()) throw new AssertionError("file "+f+" doesnt exist");
		XPathFactory xpf = XPathFactory.newInstance();
		InputSource is = new InputSource(new FileReader(f));
		NodeList tasks = (NodeList) xpf.newXPath().evaluate("/tasks/task", is, XPathConstants.NODESET);
		Property partnerProperty = model.createProperty(fp, "partner");
		Property leadPartnerProperty = model.createProperty(fp, "leadPartner");
		Property workpackageProperty = model.createProperty(fp, "workpackage");
		Property taskNumberProperty = model.createProperty(fp, "taskNumber");
		Property previousProperty = model.createProperty(fp, "previous");
		Property nextProperty = model.createProperty(fp, "next");
		Property identifier = model.createProperty(gk, "identifier");
		
		for (int i = 0; i < tasks.getLength(); ++i)
		{
			Element task = (Element) tasks.item(i);						
			int workpackageNumber = Integer.valueOf(task.getElementsByTagName("workpackageNumber").item(0).getFirstChild().getNodeValue());
			int taskNumber = Integer.valueOf(task.getElementsByTagName("taskNumber").item(0).getFirstChild().getNodeValue());			
			Resource jenaTask = model.createResource(gk+"Task"+workpackageNumber+'-'+taskNumber);
						
			jenaTask.addProperty(identifier, model.createLiteral("T"+workpackageNumber+'-'+taskNumber));
			jenaTask.addProperty(workpackageProperty, model.createResource(fp+"wp"+workpackageNumber));
			jenaTask.addLiteral(taskNumberProperty,model.createTypedLiteral(taskNumber,XSD.nonNegativeInteger.getURI()));
			
			// title as rdfs:label
			jenaTask.addLiteral(RDFS.label,model.createLiteral(task.getElementsByTagName("title").item(0).getFirstChild().getNodeValue().trim(),"en"));
			// description as rdfs:comment
			jenaTask.addLiteral(RDFS.comment, model.createLiteral(task.getElementsByTagName("description").item(0).getFirstChild().getNodeValue().trim(),"en"));

			// previous and next links
			if(taskNumber>1)
			{
				Resource previousTask = model.createResource(fp+"Task"+workpackageNumber+'-'+(taskNumber-1));
				previousTask.addProperty(nextProperty,jenaTask);
				jenaTask.addProperty(previousProperty,previousTask);
			}
			
			NodeList partners = ((Element)task.getElementsByTagName("partners").item(0)).getElementsByTagName("partner");
			
			Set<String> taskPartnerNames = new HashSet<>(); 
			Set<String> taskLeadPartnerNames = new HashSet<>();
			for(int j=0;j<partners.getLength();j++)
			{
				 Element partner = (Element) partners.item(j);
				 String partnerName = partner.getFirstChild().getNodeValue();
				 taskPartnerNames.add(partnerName);
			}
			
			if(taskPartnerNames.contains("all partners")||taskPartnerNames.contains("All partners"))
			{
				taskPartnerNames.remove("all partners");
				taskPartnerNames.remove("All partners");
				taskLeadPartnerNames.addAll(taskPartnerNames);	// explicitly named partners when "all partners" is present are marked as lead partners...
				taskPartnerNames.addAll(partnerNames);			// ...but they are still marked as partner even though leadPartner is a subProperty of partner because of the ease of doing SPARQL queries with it
			}
			
			for(String partnerName : taskPartnerNames)		{jenaTask.addProperty(partnerProperty, model.createResource(gk+partnerName));}
			for(String partnerName : taskLeadPartnerNames)	{jenaTask.addProperty(leadPartnerProperty, model.createResource(gk+partnerName));}

//			jenaTask.addLiteral(partnerProperty, model.createResource(prefix+""));
			
//			System.out.println(task.getNodeName() + " -> " + task.getTextContent());
		}
		
		model.write(new FileWriter(new File("../../../tasks.ttl")),"TURTLE",gk);
//		for(String name : partnerNames) System.out.print("\""+name+"\",");
	}

}