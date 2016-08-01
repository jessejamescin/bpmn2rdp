package br.upe.dsc.calo.simplex;

import java.io.OutputStream;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import br.upe.dsc.bpel2net.AssertException;

/**
 * 
 * @author	César Oliveira calo@dsc.upe.br
 *
 *
 */
public class SimplexWriter2 {

/*
 * Using Sketch
 * -------------------------------------------
 * writer.create("listName", "<list></list>");
 * for (Object myObject : myList)
 *  writer.appendChild("listName",
 *             writer.create(<node>myObject.content()</node>));
 * 
 *  writer.appendChild("certainNode", "otherNode");
 * 
 * writer.create("root", "
 *   <root name="myObject.getSomething()">
 *    <child id="Integer.toString(myObject.getInt())">
 *      <node>myObject.getText()</node>
 *    </child>
 *    {writer.writeElement("listName")}
 *   </root>
 *   ");    
 * 
 */	
	
	private Document doc;
	
	
	/**
	 * Stores the nodes created,
	 * identified by a string key
	 */
	private HashMap<String, Node> nodeTable;
	
	public SimplexWriter2(){
		nodeTable = new HashMap<String, Node>();
	}
	
	/**
	 * Creates the document instance
	 * @return
	 */
	public Document createDocument()
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
          DocumentBuilder builder = factory.newDocumentBuilder();
          doc = builder.newDocument();  // Create from whole cloth

          nodeTable.put("root", doc);
          
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
        }
        
        return doc;
	}
	
	/**
	 * 
	 * @param id non-empty, non-null node identifier
	 * @param tag non-empty, non-null tag name
	 * @param attributes table containing attributes to be set to the node
	 */
	public void create(String id, String tag, HashMap<String, String> attributes)
	{
		Element e = doc.createElement(tag);
		
		if (attributes != null)
		  for (String att : attributes.keySet())
			  e.setAttribute(att, attributes.get(att));
		
		nodeTable.put(id, e);
	}
	
	/**
	 * 
	 * @param id non-empty, non-null node identifier
	 * @param tag non-empty, non-null tag name
	 */
	public void create(String id, String tag)
	{
		Element e = doc.createElement(tag);
		nodeTable.put(id, e);
	}
	
	/**
	 * Returns a stored node
	 * @param id
	 */
	public Node get(String id)
	{
		return nodeTable.get(id);
	}
	
	/**
	 * Set the attribute
	 * 
	 * @param nodeId
	 * @param attrName
	 * @param value
	 */
	public void setAttribute(String nodeId, String attrName, String value)
	{
		Element e = (Element) get(nodeId);
		
		if (e != null) e.setAttribute(attrName, value);
	}
	
	/**
	 * 
	 * @param parentId non-empty, non-null id of the parent node
	 * @param childId non-empty, non-null id of the node to be appended to the parent
	 */
	public void appendChild(String parentId, String childId)
	{
		Node parent = nodeTable.get(parentId);
		Node child = nodeTable.get(childId);
		
		parent.appendChild(child);
	}
	
	/**
	 * Set text contents to the node identified by id.
	 * @param id
	 * @param text
	 */
	public void appendText(String id, String text)
	{
		Node textNode = doc.createTextNode(text);
		
		Node n = nodeTable.get(id);
		
		n.appendChild(textNode);
	}
	
	

	/**
	 * Writes the XML code to the given stream.
	 * TODO: translate error message
	 * 
	 * @param stream
	 * @throws AssertException
	 */
	public void saveToStream(OutputStream stream) throws AssertException
	{
		try {
			
			doc.normalizeDocument();
			//Use a Transformer for output
			TransformerFactory tFactory =
			       TransformerFactory.newInstance();
			tFactory.setAttribute("indent-number", new Integer(4));
			Transformer transformer = tFactory.newTransformer();
			
			transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(stream);
			transformer.transform(source, result);
			   
		} catch (TransformerConfigurationException e) {

			throw new AssertException("ocorreu uma exceção na geração da descrição XML.", "TransformerConfigurationException.");
			
		} catch (TransformerFactoryConfigurationError e) {
			
			throw new AssertException("ocorreu uma exceção na geração da descrição XML.", "TransformerFactoryConfigurationError.");
			
		} catch (TransformerException e) {
			
			throw new AssertException("ocorreu uma exceção na geração da descrição XML.", "TransformerException.");
		}
	}
}
