import java.io.File;
import java.io.FileNotFoundException;
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

public class Tasks2Rdf
{
	static final Set<String> partnerNames = new HashSet<>(Arrays.asList(new String[] {"Athena","ATH","Ontos","Openlink","OpenLink","BROX","Unister","InfAI"}));
	
	public static void main(String[] args) throws XPathExpressionException, IOException
	{
		Model model = ModelFactory.createMemModelMaker().createDefaultModel();
		String prefix = "http://purl.org/research-fp#";
		model.setNsPrefix("",prefix);
		model.setNsPrefix("rdfs",RDFS.getURI());
		model.setNsPrefix("xsd",XSD.getURI());
		
		File f = new File((args.length>0)?args[0]:"../../../tasks.xml");
		if(!f.exists()) throw new AssertionError("file "+f+" doesnt exist");
		XPathFactory xpf = XPathFactory.newInstance();
		InputSource is = new InputSource(new FileReader(f));
		NodeList tasks = (NodeList) xpf.newXPath().evaluate("/tasks/task", is, XPathConstants.NODESET);
		Property partnerProperty = model.createProperty(prefix, "partner");
		Property leadPartnerProperty = model.createProperty(prefix, "leadPartner");
		Property workpackageProperty = model.createProperty(prefix, "workpackage");
		Property taskNumberProperty = model.createProperty(prefix, "taskNumber");
		Property previousProperty = model.createProperty(prefix, "previous");
		Property nextProperty = model.createProperty(prefix, "next");
		
		for (int i = 0; i < tasks.getLength(); ++i)
		{
			Element task = (Element) tasks.item(i);						
			int workpackageNumber = Integer.valueOf(task.getElementsByTagName("workpackageNumber").item(0).getFirstChild().getNodeValue());
			int taskNumber = Integer.valueOf(task.getElementsByTagName("taskNumber").item(0).getFirstChild().getNodeValue());
			
			Resource jenaTask = model.createResource(prefix+"Task"+workpackageNumber+'-'+taskNumber);
						
			jenaTask.addProperty(workpackageProperty, model.createResource(prefix+"wp"+workpackageNumber));
			jenaTask.addLiteral(taskNumberProperty,model.createTypedLiteral(taskNumber,XSD.nonNegativeInteger.getURI()));
			
			// title as rdfs:label
			jenaTask.addLiteral(RDFS.label,model.createLiteral(task.getElementsByTagName("title").item(0).getFirstChild().getNodeValue().trim(),"en"));
			// description as rdfs:comment
			jenaTask.addLiteral(RDFS.comment, model.createLiteral(task.getElementsByTagName("description").item(0).getFirstChild().getNodeValue().trim(),"en"));

			// previous and next links
			if(taskNumber>1)
			{
				Resource previousTask = model.createResource(prefix+"Task"+workpackageNumber+'-'+(taskNumber-1));
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
			
			for(String partnerName : taskPartnerNames)		{jenaTask.addProperty(partnerProperty, model.createResource(prefix+partnerName));}
			for(String partnerName : taskLeadPartnerNames)	{jenaTask.addProperty(leadPartnerProperty, model.createResource(prefix+partnerName));}

//			jenaTask.addLiteral(partnerProperty, model.createResource(prefix+""));
			
//			System.out.println(task.getNodeName() + " -> " + task.getTextContent());
		}
		
		model.write(new FileWriter(new File("../../../tasks.ttl")),"TURTLE",prefix);
//		for(String name : partnerNames) System.out.print("\""+name+"\",");
	}

}