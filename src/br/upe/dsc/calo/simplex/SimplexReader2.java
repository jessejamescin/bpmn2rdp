/* 
 * SimplexReader.java 27/05/2005
 * update 16/06/2006
 */
package br.upe.dsc.calo.simplex;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author César Augusto
 * 			calo@dsc.upe.br
 *
 */
public class SimplexReader2 {

	private InputStream source;
	
	
	private String defaultNamespace = "";
	
	//stacks instantiated at load method
	
	/**
	 * path to the current node
	 */
	private Stack<String> nodePath;
	/**
	 * index of the item being accessed for each node in the path.
	 * for example, for a xml
	 *  <pre>&lt;root&gt;&lt;node&gt;1&lt;/node&gt;&lt;node&gt;2&lt;/node&gt;&lt;/root&gt;</pre>,
	 *  
	 * the xpath for node 2 is /root/node, but with the
	 * indexes the correct path is /root[0]/node[1].
	 */
	private Stack<Integer> pathIndexes;
	
	
	private String currentElement;
	
	
	private int tagIndex = 0;

	private Document doc;
	private XPath xpath;

	/**
	 * stores the information of whether
	 * the node in the path is a list node
	 * or not
	 */
	private Stack<Boolean> isListHistory;
	private boolean isList = false;

	/**
	 * Whether it will consider namespaces or not
	 */
	private boolean namespaceAware = true;
	
	public SimplexReader2(InputStream path) {
		source = path;
	}
	
	
	/**
	 * Carrega o arquivo XML
	 * 
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public void load() throws ParserConfigurationException, IOException, SAXException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(this.namespaceAware ); // never forget this!
		DocumentBuilder db = dbf.newDocumentBuilder();
		doc = db.parse(source);
				
		XPathFactory factory = XPathFactory.newInstance();
	    xpath = factory.newXPath();
						
		nodePath = new Stack<String>();
		pathIndexes = new Stack<Integer>();
		isListHistory = new Stack<Boolean>(); 
		
		currentElement = doc.getDocumentElement().getTagName();
		
		this.setDefaultNamespace(doc.getDocumentElement().getNamespaceURI());
		System.out.println("SIMPLEX: Setting namespace = "+doc.getDocumentElement().getNamespaceURI()+ " default: "+doc.getDocumentElement().isDefaultNamespace(defaultNamespace));
		
		
		//Set the default NameSpace for prefix 'dns'
		xpath.setNamespaceContext(new NamespaceContext() {

			@Override
			public String getNamespaceURI(String prefix) {
				
				if ("dns".equals(prefix))
					return defaultNamespace;
				else
					return null;
			}

			@Override
			public String getPrefix(String namespaceURI) {
				if (defaultNamespace.equals(namespaceURI))
					return "dns";
				else
					return null;
			}

			@Override
			public Iterator getPrefixes(String namespaceURI) {
				if (defaultNamespace.equals(namespaceURI))
				{
					ArrayList<String> prfList = new ArrayList<String>();
					prfList.add("dns");
					return prfList.iterator();
				}
			else
				return new ArrayList<String>().iterator();
			}
			
		});
		
		tagIndex = 1;
		isList = false;
	}
	
	
	public void setDefaultNamespace(String uri)
	{
		this.defaultNamespace = uri;
	}
	
	
	/**
	 *  Entra na primeira TAG encontrada dentro da TAG atual
	 * que possuir o nome igual ao nome dado. 
	 * 	Se a TAG não for encontrada, permanece no mesmo elemento.
	 * 
	 * @param tag Nome da TAG
	 * @return true se a TAG for encontrada.
	 */
	public boolean enter(String tag)
	{
		if (!childExists(tag)) return false;
		
		pushNode(); //still necessary
				
		currentElement = tag;
		tagIndex = 1;
		isList = false;
				
		return true;
	}
	
	/**
	 * Entra numa tag de índice index quando houver
	 * mais de um filho com a mesma tag.
	 * 
	 * @param tag
	 * @param index
	 * @return true se o filho existir
	 */
	public boolean enter(String tag, int index)
	{
		if (!childExists(tag, index)) return false;
		
		pushNode(); //still necessary
		
		currentElement = tag;
		tagIndex = index;
		isList = false; //indexed version does not use this variable
				
		return true;
	}
	


	/**
	 * Volta para a TAG em que estava antes.
	 *
	 */
	public void exit()
	{
		if (nodePath.size() > 0)
		{
			popNode(); //still necessary
		}
	}
	
	
	/**
	 * Salva a posição (nó) atual na pilha
	 *
	 */
	@Deprecated
	public void pushNode()
	{
		nodePath.push(currentElement);
		pathIndexes.push(tagIndex);
		isListHistory.push(isList);
	}
	
	/**
	 * Recupera a posição (nó) da pilha
	 *
	 */
	@Deprecated
	public void popNode()
	{
		currentElement = nodePath.pop();
		tagIndex = pathIndexes.pop();
		isList = isListHistory.pop();
	}
	
	/**
	 * Cria uma lista dos nós que existem dentro
	 * da tag atual que possuam o nome dado.
	 * Após este comando é preciso obrigatoriamente
	 * chamar um dos métodos enterList ou exitList.
	 * 
	 * @param tag Nome das tags a listar
	 * @return o número de nós encontrados
	 */
	@Deprecated
	public void listNodes(String tag)
	{
		isListHistory.push(isList); //records if previous node was a list
		isList = false;
		
		//check if such node exists
		if (!childExists(tag)) return;
				
		//backups the current element
		nodePath.push(currentElement);
		pathIndexes.push(tagIndex);
		//list info already stored
		
		//go to the iterating tag
		currentElement = tag;
		tagIndex = 1; //first element of the xpath list
		isList = true;
		
		return;
	}
	
	/**
	 * Returns true if the tag specified is a child of the current node.
	 * @param tag
	 * @return
	 */
	public boolean childExists(String tag) {
		
		Boolean result;
		
		try {
			String expression = currentNodeXPathExpr()+"/"+tag+"[1]";
			result = (Boolean) xpath.evaluate(expression, doc, XPathConstants.BOOLEAN);
		} catch (XPathExpressionException e) {
			return false;
		}
		
		return result;
		
	}
	
	/**
	 * Retorna true se existir uma tag filho de índice index
	 * @param tag
	 * @param index
	 * @return
	 */
	public boolean childExists(String tag, int index) {
		
		Boolean result;
		
		try {
			result = (Boolean) xpath.evaluate(currentNodeXPathExpr()+"/"+tag+"["+index+"]", doc, XPathConstants.BOOLEAN);
		} catch (XPathExpressionException e) {
			return false;
		}
		
		return result;
		
	}


	/**
	 * Entra no primeiro nó da lista de nós gerados
	 * através de listNodes.<br>
	 *  O método exitList deve ser chamado quando as operações
	 *  com a lista forem encerradas.
	 *  
	 * @return true se existir algum nó
	 * @deprecated Do not use enterList to iterate nodes. Use countNodes and the indexed version of
	 * getNodeValue and getNodeAttribute instead.
	 */
	@Deprecated
	public boolean enterList()
	{
		return isList;
		
	}
		
	/**
	 * Vai para o próximo nó da lista criada através
	 * do comando listNodes.<br>
	 * Deve-se entrar na lista antes através de enterList.
	 * 
	 * @return true se existir um próximo nó
	 * @deprecated Do not use enterList to iterate nodes. Use countNodes and the indexed version of
	 * getNodeValue and getNodeAttribute instead.
	 */
	@Deprecated
	public boolean gotoNext()
	{
		tagIndex++;
		
		//check if the next node exists
		// notice that the index is updated, so
		// the 'current' is already the next one.
		// notice also that "." refers to the current
		// node rather than a child
		return childExists(".");
	}
	
	/**
	 * Sai da lista em que entrou através de enterList.
	 * @deprecated Do not use enterList to iterate nodes. Use countNodes and the indexed version of
	 * getNodeValue and getNodeAttribute instead.
	 */
	@Deprecated
	public void exitList()
	{
		if (!isList)
		{
			isList = isListHistory.pop(); //restores previous list information
			//does not need to restore other node information
			return;
		}
		//restores complete previous node information
		popNode();
		
	}
	
	
	public List<String> getChildren()
	{
		List<String> children = new ArrayList<String>();
		
		HashMap<String, Integer> countElements = new HashMap<String, Integer>();
		
		org.w3c.dom.NodeList result;
		
		try {
			
			String expression = currentNodeXPathExpr()+"/*";
			result = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
			int n;
			
			for (int i = 0; i < result.getLength(); i++)
			{
				String name = result.item(i).getNodeName();
				
				if (countElements.containsKey(name))
				{
					n = countElements.get(name);
					countElements.put(name, n+1);
					n = n+1;
				}
				else
				{
					n = 1;
					countElements.put(name, 1);
				}
				
				children.add(name + "["+n+"]");
			}
				
			
			
		} catch (XPathExpressionException e) {
			return null;
		}
		
		return children;
	}
	
	/**
	 * Pega o valor do nó.
	 * 
	 * @return o valor encontrado
	 */
	public String getNodeValue()
	{
		return evaluateRelative("text()");
	}

	
	/**
	 * Returns the value of an attribute of the current node.
	 * 
	 * @param attribute
	 * @return
	 */
	public String getNodeAttribute(String attribute)
	{
		return evaluateRelative("@"+attribute);
	}
	

	/**
	 * 
	 * 
	 * @param index índice do nó a ser consultado
	 * @return o valor encontrado
	 */
	public String getNodeValue(int index)
	{
		String result;
		
		int i = tagIndex;
		
		tagIndex = index;
		
		result = getNodeValue();
		
		tagIndex = i;
		
		return result;
	}

	
	/**
	 * 
	 * 
	 * @param index índice do nó a ser consultado
	 * @param attribute
	 * @return
	 */
	public String getNodeAttribute(int index, String attribute)
	{
		String result;
		
		int i = tagIndex;
		
		tagIndex = index;
		
		result = getNodeAttribute(attribute);
		
		tagIndex = i;
		
		return result;
	}
	
	/**
	 * Evaluates an xpath expression relative to the current node.
	 * @param expr
	 * @return
	 */
	public String get(String expr)
	{
		return evaluateRelative(expr);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getCurrentTagName()
	{
		return currentElement;
	}
	
	
	/**
	 * Returns an XPath expression that
	 * points to the current node.
	 * @return
	 */
	public String currentNodeXPathExpr()
	{
		
		StringBuffer expr = new StringBuffer("/");
		
		for (int i=0; i < nodePath.size(); i++)
		{
			int index = pathIndexes.get(i);
			String indexStr = Integer.toString(index);
			String nodeName = nodePath.get(i);
			
			expr.append(nodeName).append("[").append(indexStr).append("]").append("/");
		}
		
		//append the current node
		expr.append(currentElement).append("[").append(tagIndex).append("]");
		
		
		return expr.toString();
	}
	
	/**
	 * 
	 * @param expr
	 * @return
	 */
	private String evaluateRelative(String expr)
	{
		String result;
		
		String xpathCommand = currentNodeXPathExpr()+"/"+expr;
		
		try {
			result = (String) xpath.evaluate(xpathCommand, doc, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			
			e.printStackTrace();
			
			return null;
		}
		
		//System.out.println(xpathCommand);
		//System.out.println(result);
		
		return result;
	}
	
	/**
	 * Evaluates the given xpath expression without changing the internal
	 * state.
	 * 
	 * @param xpathExpr
	 * @return
	 */
	public String evaluate(String xpathExpr) throws XPathExpressionException
	{
		String result;
		
		result = (String) xpath.evaluate(xpathExpr, doc, XPathConstants.STRING);
		
		
		return result;
	}
	
	
	/**
	 * Counts the number of child nodes with the given tag
	 * @param tag
	 * @return the number of nodes found
	 */
	public int countNodes(String tag)
	{
		int n = 0;
		
		org.w3c.dom.NodeList result;
		
		String xpathCommand = currentNodeXPathExpr()+"/"+tag;
		
		try {
			result = (org.w3c.dom.NodeList) xpath.evaluate(xpathCommand, doc, XPathConstants.NODESET);
			n = result.getLength();
			
		} catch (XPathExpressionException e) {
			
			e.printStackTrace();
			
			return 0;
		}
		
		return n;
	}
	
	/**
	 * Counts the number of child nodes with the given tag
	 * in relation to their parent node with the given index,
	 * for use when iterating over a list of nodes.
	 * @param index
	 * @param tag
	 * @return
	 */
	public int countNodes(int index, String tag)
	{
		int n = 0;
		
		int i = tagIndex;
		
		tagIndex = index;
		
		org.w3c.dom.NameList result;
		
		String xpathCommand = currentNodeXPathExpr()+"/"+tag;
		
		try {
			result = (org.w3c.dom.NameList) xpath.evaluate(xpathCommand, doc, XPathConstants.NODESET);
			n = result.getLength();
			
		} catch (XPathExpressionException e) {
			
			e.printStackTrace();
			
			tagIndex = i;
			
			return 0;
		}
		tagIndex = i;
		return n;
	}


	public boolean isNamespaceAware() {
		return namespaceAware;
	}

	/**
	 * Setting names
	 * @param namespaceAware
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}
	
}
