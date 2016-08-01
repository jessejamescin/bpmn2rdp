/**
 * 
 */
package br.upe.dsc.bpel2net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import br.upe.dsc.calo.simplex.SimplexReader2;
import br.upe.dsc.calo.simplex.SimplexWriter2;

/**
 * @author Thiago Andre
 *
 */
public class BPELCompiler {

	
	private SimplexReader2 reader;
	
	private DataReader dataReader;
	

	//Lista de todos os papéis neste processo
	private ArrayList<String> rolesList = new ArrayList<String>();
	
	//Guarda cada invoke e seu respectivo nó (em DOM)
	// a chave é o atributo name do nó
	private HashMap<String, Node> invokeTable = new HashMap<String, Node>();
	private ArrayList<Place> placeList = new ArrayList<Place>();	
	private ArrayList<ExponentialTransition> transitionList = new ArrayList<ExponentialTransition>();
	private ArrayList<ImmediateTransition> immTransitionList = new ArrayList<ImmediateTransition>();
	private ArrayList<Arc> arcList = new ArrayList<Arc>();
	
	/*
	 *Contador para evitar
	 * atividades de mesmo nome.
	 * (Toda atividade receberá
	 * um sufixo numérico diferente).
	 */
	private int activityCounter = 0;
	
	//contador para geração de ids
	private int nextId = 1;
	
	
	//soma de probabilidades
	private double probSum = 0.0;

	//coordenada X para o pr�ximo elemento gerado
	private int posX = 50;

	//coordenada Y para o pr�ximo elemento gerado
	private int posY = 50;
	
	//flag para fazer com que o elemento seja gerado
	// hora 30 pixels acima, hora 30 pixels abaixo
	// da coordenada posY
	private int yFlag = 30;
	
	public BPELCompiler(){}
	
	
	/**
	 * Compila o arquivo BPEL
	 * @param f
	 */
	public void compile(File f)
	{
		try {
			
			//-1) Cria o escritor do XML
			SimplexWriter2 writer = new SimplexWriter2();
			writer.createDocument();
			
			//0) Cria o leitor
			this.reader = new SimplexReader2(new FileInputStream(f));
			//0.1) Cria o leitor para o arquivo com os dados estatísticos (nome do arquivo + .data)
			this.dataReader = new DataReader(new File(f.getAbsoluteFile()+".data"));
			
			//0.2) Lê o arquivo de dados e coloca tudo na memória
			this.dataReader.loadData();
			
			//1) Lê o arquivo BPEL e carrega na memória
			reader.setNamespaceAware(false); //ignora o namespace do BPEL
			reader.load();
			
			//reader.setDefaultNamespace("http://ode/bpel/unit-test");
			
			//System.out.println(reader.currentNodeXPathExpr());
						
			
			//3) Procura pelos Roles
			if (reader.enter("partnerLinks"))
			{//<partnerLinks>
				
				System.out.println(reader.currentNodeXPathExpr());
				
				//varre a lista de partners
				int numberOfRoles = reader.countNodes("partnerLink");
				
				for (int i = 1; i <= numberOfRoles; i++)
				{
					reader.enter("partnerLink", i);
					
					System.out.println(reader.currentNodeXPathExpr());
					
					//<partnerLink>
					String name = reader.getNodeAttribute("name");
					//guarda o nome deste papel na lista
					rolesList.add(name);
					
					//</partnerLink>
					reader.exit();
				}
				System.out.println(reader.currentNodeXPathExpr());
				//</partnerLinks>
				reader.exit();
			}
			
			
			//------------------------------
			//---- LÊ O PROCESSO e GERA A REDE DE PETRI ----
			
			//4.0) Cria a tag net
			writer.create("net", "net");
			writer.appendChild("root", "net");
			writer.setAttribute("net", "id", "0");
			writer.setAttribute("net", "netclass", "eDSPN");
			writer.setAttribute("net", "xmlns", "http://pdv.cs.tu-berlin.de/TimeNET/schema/eDSPN");
			writer.setAttribute("net", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.setAttribute("net", "xsi:schemaLocation", "http://pdv.cs.tu-berlin.de/TimeNET/schema/eDSPN etc/schemas/eDSPN.xsd");
			
			
			//createExponentialTransitionXML(writer, "arrival", false);
			transitionList.add(new ExponentialTransition("arrival", dataReader.getArrivalDelay(), false));
			
			//Cria transi��o departure
			immTransitionList.add(new ImmediateTransition("departure", 1.0));
			placeList.add(new Place("P_D", "0"));
			arcList.add(new Arc("PD-departure", "P_D", "departure"));
			
			
			//4.1) Gera os pap�is
			for (String currentRole : rolesList)
			{	
				int numOfResources = dataReader.getNumberOfResources(currentRole);
				//createPlaceXML(writer, currentRole);
				placeList.add(new Place(currentRole, Integer.toString(numOfResources)));
				
			}
			
			//4.2) Gera os processos
			Subprocess workflow = null;
			//procurando in�cio do processo
			if (reader.childExists("sequence"))
			{
				//chama o processo de compilação principal
				workflow = compileProcess("sequence", 1);
			}
			else
			if (reader.childExists("switch"))
			{
				//chama o processo de compilação principal
				workflow = compileProcess("switch", 1);
			}
			else
			if (reader.childExists("if"))
			{
				//chama o processo de compilaçõo principal
				workflow = compileProcess("if", 1);
			}
			else
			if (reader.childExists("flow"))
			{
				//chama o processo de compilação principal
				workflow = compileProcess("flow", 1);
			}
			else
			if (reader.childExists("while"))
			{
				//chama o processo de compilação principal
				workflow = compileProcess("while", 1);
			}
			else
			if (reader.childExists("repeatUntil"))
			{
				//chama o processo de compilação principal
				workflow = compileProcess("repeatUntil", 1);
			}
			else
				System.out.println("Processo não suportado");
			
			
			//Finaliza o modelo com os arcos
			// para arrival e departure
			if (workflow != null)
			{
				//arrival
				arcList.add(new Arc("a"+getNewId(), "arrival", workflow.getStartingPlace()));

				//departure
				for (String transition : workflow.getDeptTransitions())
					arcList.add(new Arc("a"+getNewId(), transition, "P_D"));
				
			}
			
			//--------- GERA o XML
			
			//GERA todos os lugares
			for (Place p : placeList)
				createPlaceXML(writer, p);
			//Gera todas as transições exponenciais
			for (ExponentialTransition t : transitionList)
				createExponentialTransitionXML(writer, t);
			//Gera todas as transicoes imediatas
			for (ImmediateTransition it : immTransitionList)
				createImmediateTransitionXML(writer, it);
			//Gera todos os arcos
			for (Arc a : arcList)
				createArcXML(writer, a);
			
			
			//GERA O ARQUIVO XML
			FileOutputStream out = new FileOutputStream(new File(f.getAbsolutePath()+".xml"));
			writer.saveToStream(out);	
			
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AssertException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**
	 * Compila o processo a partir da tag fornecida
	 * 
	 * @param tag
	 * @param reader
	 */
	private Subprocess compileProcess(String tag, int index) {
			
		ArrayList<Subprocess> childProcesses = new ArrayList<Subprocess>();
		
		Subprocess resultProc = null;
		
		//entra na raiz
		reader.enter(tag, index);
		
		
		
				
		//---- checa se é uma folha------------------
		if ("invoke".equals(tag)) //invoke = ATIVIDADE
		{
			//compila a atividade
			String name = reader.getNodeAttribute("operation");
			String role = reader.getNodeAttribute("partnerLink");
			
			//TODO obter o delay correto
			Subprocess result = createActivityNet(name, role, dataReader.getDelay(name));
						
			System.out.println(result);
			
			reader.exit();
			//retorna
			return result;
		}
		else
		if ("assign".equals(tag) || "receive".equals(tag)
				|| "reply".equals(tag))
		{
			reader.exit();
			return null; //ignora estas tags
		}
		
		//--------------------------------------------
		
		// Operações (não folhas)
		
		//lista os nós filhos
		List<String> children = reader.getChildren();
		
		//------ compilar os filhos ------------------
		for (String s : children)
		{
				System.out.println("Compilar "+s);
				
				//compilar filho s -----
				
				//separar nome do nó e índice nomen�[i]
				String nodeName = getNodeName(s);
				int nodeIndex = getNodeIndex(s);
				
				//chama recursivamente ==>
				Subprocess nodeCompiled = compileProcess(nodeName, nodeIndex); //<====
				
				//adiciona o filho compilado � lista de filhos compilados
				if (nodeCompiled != null)
					childProcesses.add(nodeCompiled);
				
				
		}
		//--------------------------------------------
		
		//compila o pai (faz a ligação entre os filhos)
		
//---------------------- SEQUENCE ----------------------------------------------		
		if ("sequence".equals(tag))
		{
			//cria um arco ligando cada filho ao seguinte
			System.out.println("Criar sequencia");
			
			for (int i = 0; i < childProcesses.size()-1; i++)
			{
				Subprocess s1 = childProcesses.get(i);
				Subprocess s2 = childProcesses.get(i+1);
				
				System.out.println("s1: "+s1);
				
				for (String transition : s1.getDeptTransitions())
					//System.out.println("gera arco: "+transition +"--->"+ s2.getStartingPlace()); //CHAMAR MÉTODO REAL
					arcList.add(new Arc(getNewId(), transition, s2.getStartingPlace()));
			}
			//cria o sub-processo de retorno
			resultProc = new Subprocess();
			resultProc.setStartingPlace(childProcesses.get(0).getStartingPlace());
			resultProc.getDeptTransitions().addAll(childProcesses.get(childProcesses.size()-1).getDeptTransitions());
			
			//adiciona o arco da transicao inicial "arrival" para o primeiro lugar do processo
			//arcList.add(new Arc("arrival-P","arrival", resultProc.getStartingPlace()));
			
			//pega a ultima transicao
				/*		
				String fim= "";
				for(String t : resultProc.getDeptTransitions() ){
					fim+= t;
				}*/
			//System.out.println(fim);	
			
			
			//adiciona o arco da transicao do subprocesso final para o lugar final P_D 
				//arcList.add(new Arc(fim+" to P_D",fim, "P_D"));
			
			
			//System.out.println("SEQ: "+resultProc.toString());
		
		}
		else
//---------------------- FLOW ----------------------------------------------			
			if ("flow".equals(tag))
			{
				String id = getNewId();
				//Cria o lugar do paralelismo P_AND
				placeList.add(new Place("P_AND"+id, "0"));
				
				//Cria a transi��o imediata qAND
				immTransitionList.add(new ImmediateTransition("qAND"+id, 1.0));
				
				//Cria um arco do P_AND para o qAND
				arcList.add(new Arc("pAND-qAND"+id, "P_AND"+id, "qAND"+id)); 
					
				//cria a transi��o imediata qSync responsavel pelo sincronismo
				immTransitionList.add(new ImmediateTransition("qSync"+id, 1.0));
				
				//cria um arco ligando cada filho ao seguinte
				System.out.println("Criar paralelismo");
				
				for (int i = 0; i < childProcesses.size(); i++)
				{
					//pega o filho
					Subprocess s1 = childProcesses.get(i);
					
					//cria uma variavel para guardar as id  
					String cont= getNewId();
					
					//cria um arco de qAND para s1.getStartingPlace()
					arcList.add(new Arc(cont+"a", "qAND"+id, s1.getStartingPlace()));
					
					//CRIA o lugar Z
					String z = "Z_s"+cont;
					placeList.add(new Place(z, "0"));
					
					//para cada transi��o de sa�da de s1					
					for (String transition : s1.getDeptTransitions())
					{//System.out.println("gera arco: "+transition +"--->"+ s2.getStartingPlace()); //CHAMAR M�TODO REAL
						arcList.add(new Arc(cont+"b", transition, z));
					
					}
					//cria um arco deste Z para o qSync
					arcList.add(new Arc(cont+"c", z, "qSync"+id));
				}
				
				//cria o sub-processo de retorno
				
				resultProc = new Subprocess();
				resultProc.setStartingPlace("P_AND"+id);
				resultProc.getDeptTransitions().add("qSync"+id);
				//resultProc.getDeptTransitions().addAll(childProcesses.get(childProcesses.size()-1).getDeptTransitions());
				
				//adiciona o arco da transicao inicial "arrival" para o primeiro lugar do processo
				//arcList.add(new Arc("arrival-P","arrival", resultProc.getStartingPlace()));
				
				//pega a ultima transicao
				/*
				String fim= "";
				for(String t : resultProc.getDeptTransitions() ){
					fim+= t;
				}
								
				//adiciona o arco da transicao final do subprocesso para o lugar final P_D 
				arcList.add(new Arc(fim+" to P_D",fim, "P_D"));
								
				System.out.println("PARALEL: "+resultProc.toString());*/
			}
			else
//---------------------- CASE (switch) ----------------------------------------------				
		if ("case".equalsIgnoreCase(tag) || "otherwise".equalsIgnoreCase(tag))
		{
			//compilando uma condição de um switch
			
			String condition = reader.getNodeAttribute("condition");
			
			//cria a transiçãoo imediata "to_[stplace-name]"
			if (childProcesses.size() > 0)
			{
				//deve haver apenas 1 elemento filho da tag case, que �
				// o subprocesso que queremos
				Subprocess branch = childProcesses.get(0);
				
				String stplaceName = branch.getStartingPlace();
				
				//obter o peso correto a partir do arquivo de dados estat�sticos
				immTransitionList.add(new ImmediateTransition("to_"+stplaceName, dataReader.getProbability(condition)));
			

				//retorna o pr�prio filho como sendo o subprocesso
				resultProc = branch;
			}
			//se n�o h� nenhum filho no n�, retorna nulo
			resultProc = null;
			
		}
		else
//---------------------- SWITCH ----------------------------------------------			
		if ("switch".equals(tag))
		{
			//compilando o switch
			// as condi��es (case e otherwise) j� foram compiladas antes deste ponto
			
			//cada subprocesso filho � um caminho

			//cria o lugar Pxor
			String pxorId = "pxor"+getNewId();

			placeList.add(new Place(pxorId, "0"));
			
			resultProc = new Subprocess();
			resultProc.setStartingPlace(pxorId);
			
			// As transi��es imediatas j� foram criadas,
			// criam-se agora os arcos.
			for (Subprocess s : childProcesses)
			{
				String stplace = s.getStartingPlace();
				
				arcList.add(new Arc(getNewId(), pxorId, "to_"+stplace));
				arcList.add(new Arc(getNewId(), "to_"+stplace, stplace));
				
				//todas as transi��es de partida de cada filho s�o transi��es de partida deste subprocesso
				resultProc.getDeptTransitions().addAll(s.getDeptTransitions());
			}
			
		}
		else
//---------------------- ELSIF (if) ----------------------------------------------				
			if ("elseif".equalsIgnoreCase(tag))
			{
				//compilando uma condi��o de um if
				
				String condition;
				//pega a condi�ao (BPEL 2.0)
				if (reader.enter("condition"))
				{
					condition = reader.getNodeValue();
					reader.exit();
				}
				else condition="";
				
				
				//cria a transi��o imediata "to_[stplace-name]"
				if (childProcesses.size() > 0)
				{
					//deve haver apenas 1 elemento filho da tag case, que �
					// o subprocesso que queremos
					Subprocess branch = childProcesses.get(0);
					
					String stplaceName = branch.getStartingPlace();
					
					//obter o peso correto a partir do arquivo de dados estat�sticos
					double branchProbability = dataReader.getProbability(condition);
					probSum += branchProbability; //acumula probabilidade
					
					immTransitionList.add(new ImmediateTransition("to_"+stplaceName, branchProbability));
				

					//retorna o pr�prio filho como sendo o subprocesso
					resultProc = branch;
				}
				//se n�o h� nenhum filho no n�, retorna nulo
				resultProc = null;
				
			}
			else
//---------------------- ELSE (if) ----------------------------------------------				
			if ("else".equalsIgnoreCase(tag))
			{
				//simplesmente compila a atividade filha e a retorna
								
				//cria a transi��o imediata "to_[stplace-name]"
				if (childProcesses.size() > 0)
				{
					//deve haver apenas 1 elemento filho da tag case, que �
					// o subprocesso que queremos
					Subprocess branch = childProcesses.get(0);
					
					//retorna o pr�prio filho como sendo o subprocesso
					resultProc = branch;
				}
				else
				//se n�o h� nenhum filho no n�, retorna nulo
				resultProc = null;
				
			}
			else
//---------------------- IF ----------------------------------------------
		if("if".equalsIgnoreCase(tag))
		{
			String condition;
			
			//pega a condi�ao (BPEL 2.0)
			if (reader.enter("condition"))
			{
				condition = reader.getNodeValue();
				reader.exit();
			}
			else condition="";
			
			
			//O MESMO QUE compilando o switch
			// as condi��es if, elseif j� foram compiladas antes deste ponto
			
			//cada subprocesso filho � um caminho

			//cria o lugar Pxor
			String pxorId = "pxor"+getNewId();

			placeList.add(new Place(pxorId, "0"));
			
			resultProc = new Subprocess();
			resultProc.setStartingPlace(pxorId);
			
			//� necess�rio criar uma transi��o imediata
			// para o primeiro caminho (da condi��o If)
			Subprocess branch = childProcesses.get(0);
			
			String ifActivityStartPlace = branch.getStartingPlace();
			
			//obter o peso correto a partir do arquivo de dados estat�sticos
			double ifProb = dataReader.getProbability(condition);
			probSum += ifProb; //acumula probabilidade
			
			immTransitionList.add(new ImmediateTransition("to_"+ifActivityStartPlace, ifProb));
					
			//� preciso tamb�m criar uma transi��o imediata para o ELSE, caso haja um.
			// esta transi��o ter� probabilidade igual a 1 - probSum
			if (reader.childExists("else"))
			{
				//ATEN��O: o ELSE � necessariamente o �ltimo elemento no if 
				Subprocess elseBranch = childProcesses.get(childProcesses.size()-1);
				
				String elseActivityStartPlace = elseBranch.getStartingPlace();
				
				//calcula o peso
				double elseProb = 1 - probSum;
				
				
				System.out.println("else "+elseActivityStartPlace+" "+elseProb);
				
				immTransitionList.add(new ImmediateTransition("to_"+elseActivityStartPlace, elseProb));
			}
			else
			{
				//caso n�o exista o ELSE, deve haver uma transi��o
				//imediata representando um Skip, no qual nada � executado
				double elseProb = 1 - probSum;
				
				
				System.out.println("else skip "+elseProb);
				
				immTransitionList.add(new ImmediateTransition("to_skip_"+pxorId, elseProb));
				arcList.add(new Arc("a"+getNewId(), pxorId, "to_skip_"+pxorId));
				
				resultProc.getDeptTransitions().add("to_skip_"+pxorId);
			}
			
			probSum = 0.0; //reseta probSum
			
			// As outras transi��es imediatas (elseif) j� foram criadas,
			// criam-se agora os arcos.
			for (Subprocess s : childProcesses)
			{
				String thisActivityStartPlace = s.getStartingPlace();
								
				System.out.println("arcs to "+thisActivityStartPlace);
				
				arcList.add(new Arc("a"+getNewId(), pxorId, "to_"+thisActivityStartPlace));
				arcList.add(new Arc("a"+getNewId(), "to_"+thisActivityStartPlace, thisActivityStartPlace));
				
				//todas as transi��es de partida de cada filho s�o transi��es de partida deste subprocesso
				resultProc.getDeptTransitions().addAll(s.getDeptTransitions());
			}
		}
		else
//---------------------- REPEAT UNTIL ----------------------------------------------
			if("repeatUntil".equalsIgnoreCase(tag))
			{
				//	System.out.println("Ignorando tag: "+tag);
				String id = getNewId();
				
				//cria o lugar do LOOPING Prepeat
				placeList.add(new Place ("Prepeat"+id,"0"));
		
				//cria a transicao imediata que liga o lugar Pwhile ao
				//lugar inical do subprocesso
				immTransitionList.add(new ImmediateTransition("q_"+id, 1.0));
				
				//cria o arco do lugar Pwhile para a transicao q
				arcList.add(new Arc("Prepeat-q"+id,"Prepeat"+id,"q_"+id));
				
				
					//pega o filho
					Subprocess s1 = childProcesses.get(0);
					
					//cria uma variavel para guardar as id  
					String cont= getNewId();
					
					//separa o lugar inicial do subprocesso-filho
					String stPlace = s1.getStartingPlace();
					
					//cria um arco de q_  para stPlace
					arcList.add(new Arc(cont+"a", "q_"+id, stPlace));
					
					//CRIA o lugar P_z
					String zId = "P_z"+cont;
					placeList.add(new Place(zId, "0"));
					
					//para cada transi��o de sa�da de s1					
					for (String transition : s1.getDeptTransitions())
					{//System.out.println("gera arco: "+transition +"--->"+ s2.getStartingPlace()); //CHAMAR M�TODO REAL
						arcList.add(new Arc("a1_"+getNewId(), transition, zId));
					
					}
					//compilando uma condicao para a transicao "qw"
					//String condition = reader.getNodeAttribute("condition");
					String condition;
					
					//pega a condi�ao (BPEL 2.0)
					if (reader.enter("condition"))
					{
						condition = reader.getNodeValue();
						reader.exit();
					}
					else condition="";
					
					
					//obter o peso correto a partir do arquivo de dados estat�sticos
					immTransitionList.add(new ImmediateTransition("qw"+id, dataReader.getProbability(condition)));
					
					//cria um arco deste Z para o "qw"
					arcList.add(new Arc("a2_"+cont, zId, "qw"+id));
					
					//cria a transicao imediata "1-qwhile" 
					immTransitionList.add(new ImmediateTransition("1-qw"+id, 1 - dataReader.getProbability(condition)));
					
					//cria o arco do lugar Pz para a transicao "1-qwhile"
					arcList.add(new Arc("a3_"+cont, zId, "1-qw"+id));
					
					//cria o arco da transicao "1-qw"+id para o lugar Pw
					arcList.add(new Arc("looping"+id, "1-qw"+id, "Prepeat"+id));
										
				
					resultProc = new Subprocess();
					resultProc.setStartingPlace("Prepeat"+id);
					resultProc.getDeptTransitions().add("qw"+id);
					
			}
			else
//---------------------- WHILE ----------------------------------------------			
				if("while".equals(tag))
				{
					String id = getNewId();
					
					
					String condition;
					
					
					//pega a condi�ao (BPEL 2.0)
					if (reader.enter("condition"))
					{
						condition = reader.getNodeValue();
						reader.exit();
					}
					else  //caso nao encontre, tenta seguindo a nota�ao 1.1
						condition = reader.getNodeAttribute("condition");
					
					//cria o lugar do LOOPING Pwhile
					placeList.add(new Place ("Pwhile"+id,"0"));
					
					immTransitionList.add(new ImmediateTransition("qw_in"+id, dataReader.getProbability(condition)));
					
					//cria um arco deste Z para o "qw"
					arcList.add(new Arc("a1_"+id, "Pwhile"+id, "qw_in"+id));
					
					//cria a transicao imediata de sa�da
					immTransitionList.add(new ImmediateTransition("qw_out"+id, 1 - dataReader.getProbability(condition)));
					
					//cria o arco do lugar Pz para a transicao "1-qwhile"
					arcList.add(new Arc("a2_"+id, "Pwhile"+id, "qw_out"+id));
					
					
					
						//pega o filho
						Subprocess s1 = childProcesses.get(0);
						
						//cria uma variavel para guardar as id  
						//String child= getNewId();
						
						//separa o lugar inicial do subprocesso-filho
						String stPlace = s1.getStartingPlace();
						
						//cria um arco de q_  para stPlace
						arcList.add(new Arc("a3_"+id, "qw_in"+id, stPlace));
						
						
						
						//para cada transi��o de sa�da de s1					
						for (String transition : s1.getDeptTransitions())
						{
							arcList.add(new Arc("a"+getNewId(), transition, "Pwhile"+id));
						
						}
				
					resultProc = new Subprocess();
					resultProc.setStartingPlace("Pwhile"+id);
					resultProc.getDeptTransitions().add("qw_out"+id);
					
					
				}
		
		
		
		
		//sai do n� onde est�
		reader.exit();
		
		
						
		return resultProc;
	}
	
	


	/**
	 * faz a convers�o:
	 * nodename[i] --> i
	 * 
	 * @param xpathName
	 * @return
	 */
	private int getNodeIndex(String xpathName) {

		int start = xpathName.indexOf('[');
		int end = xpathName.indexOf(']');
		
		String indexStr = xpathName.substring(start+1, end);
		
		return Integer.parseInt(indexStr);
	}


	/**
	 * faz a convers�o:
	 * nodename[i] --> nodename
	 * @param xpathName
	 * @return
	 */
	private String getNodeName(String xpathName) {

		int end = xpathName.indexOf('[');
		
		return xpathName.substring(0, end);
	}


	/**
	 * @param writer
	 * @param placeName
	 */
	private void createPlaceXML(SimplexWriter2 writer, Place place) {
		
		String placeName = place.getId();
		
		writer.create(placeName, "place");
		writer.setAttribute(placeName, "id", placeName);
		writer.setAttribute(placeName, "initialMarking", place.getInitialMarking());
		writer.setAttribute(placeName, "type", "node");
		//adiciona � rede
		writer.appendChild("net", placeName);
		
		//gera os elementos gr�ficos
		writer.create(placeName+"g", "graphics");
		writer.setAttribute(placeName+"g", "orientation", "0");
		writer.setAttribute(placeName+"g", "x", newXpos());
		writer.setAttribute(placeName+"g", "y", newYpos());
		//adiciona ao lugar
		writer.appendChild(placeName, placeName+"g");
		
		writer.create(placeName+"l", "label");
		writer.setAttribute(placeName+"l", "id", placeName+".0");
		writer.setAttribute(placeName+"l", "text", placeName);
		writer.setAttribute(placeName+"l", "type", "text");
		//adiciona ao lugar
		writer.appendChild(placeName, placeName+"l");
		
		writer.create(placeName+"lg", "graphics");
		writer.setAttribute(placeName+"lg", "x", "5");
		writer.setAttribute(placeName+"lg", "y", "5");
		//adiciona ao label
		writer.appendChild(placeName+"l", placeName+"lg");
	}
	
	

	/**
	 * @param writer
	 * @param transitionName
	 */
	private void createExponentialTransitionXML(SimplexWriter2 writer, ExponentialTransition transition) {

		String transitionName = transition.getId();
		boolean isInfinity = transition.isInfinity();
		
		writer.create(transitionName, "exponentialTransition");
		writer.setAttribute(transitionName, "id", transitionName);
		writer.setAttribute(transitionName, "DTSPNpriority", "1");
		writer.setAttribute(transitionName, "type", "node");
		writer.setAttribute(transitionName, "delay", Double.toString(transition.getDelay()));
		writer.setAttribute(transitionName, "preemptionPolicy", "PRD");
		writer.setAttribute(transitionName, "serverType", isInfinity ? "InfiniteServer" : "ExclusiveServer");
		//adiciona � rede
		writer.appendChild("net", transitionName);
		
		//gera os elementos gr�ficos
		writer.create(transitionName+"g", "graphics");
		writer.setAttribute(transitionName+"g", "orientation", "0");
		writer.setAttribute(transitionName+"g", "x", newXpos());
		writer.setAttribute(transitionName+"g", "y", newYpos());
		//adiciona ao lugar
		writer.appendChild(transitionName, transitionName+"g");
		
		writer.create(transitionName+"l", "label");
		writer.setAttribute(transitionName+"l", "id", transitionName+".0");
		writer.setAttribute(transitionName+"l", "text", transitionName);
		writer.setAttribute(transitionName+"l", "type", "text");
		//adiciona ao lugar
		writer.appendChild(transitionName, transitionName+"l");
		
		writer.create(transitionName+"lg", "graphics");
		writer.setAttribute(transitionName+"lg", "x", "-10");
		writer.setAttribute(transitionName+"lg", "y", "-10");
		//adiciona ao label
		writer.appendChild(transitionName+"l", transitionName+"lg");
	}
	
	
	

	/**
	 * @param writer
	 * @param transitionName
	 */
	private void createImmediateTransitionXML(SimplexWriter2 writer, ImmediateTransition transition) {

		String transitionName = transition.getId();
		
		
		writer.create(transitionName, "immediateTransition");
		writer.setAttribute(transitionName, "id", transitionName);
		writer.setAttribute(transitionName, "priority", "1");
		writer.setAttribute(transitionName, "type", "node");
		writer.setAttribute(transitionName, "weight", Double.toString(transition.getWeight()));
		writer.setAttribute(transitionName, "enablingFunction", "");
		//adiciona � rede
		writer.appendChild("net", transitionName);
		
		//gera os elementos gr�ficos
		writer.create(transitionName+"g", "graphics");
		writer.setAttribute(transitionName+"g", "orientation", "0");
		writer.setAttribute(transitionName+"g", "x", newXpos());
		writer.setAttribute(transitionName+"g", "y", newYpos());
		//adiciona ao lugar
		writer.appendChild(transitionName, transitionName+"g");
		
		writer.create(transitionName+"l", "label");
		writer.setAttribute(transitionName+"l", "id", transitionName+".0");
		writer.setAttribute(transitionName+"l", "text", transitionName);
		writer.setAttribute(transitionName+"l", "type", "text");
		//adiciona ao lugar
		writer.appendChild(transitionName, transitionName+"l");
		
		writer.create(transitionName+"lg", "graphics");
		writer.setAttribute(transitionName+"lg", "x", "-10");
		writer.setAttribute(transitionName+"lg", "y", "-10");
		//adiciona ao label
		writer.appendChild(transitionName+"l", transitionName+"lg");
	}
	
	

	/**
	 * @param writer
	 * @param transitionName
	 */
	private void createArcXML(SimplexWriter2 writer, Arc arc) {

		String arcName = arc.getId();
		
		
		writer.create(arcName, "arc");
		writer.setAttribute(arcName, "id", arcName);
		writer.setAttribute(arcName, "fromNode", arc.getFromId());
		writer.setAttribute(arcName, "toNode", arc.getToId());
		writer.setAttribute(arcName, "type", "connector");
		
		//adiciona � rede
		writer.appendChild("net", arcName);
		
		//gera os elementos gr�ficos
	
		
		writer.create(arcName+"l", "inscription");
		writer.setAttribute(arcName+"l", "id", arcName+".0");
		writer.setAttribute(arcName+"l", "text", "1");
		writer.setAttribute(arcName+"l", "type", "inscriptionText");
		//adiciona ao lugar
		writer.appendChild(arcName, arcName+"l");
		
		writer.create(arcName+"lg", "graphics");
		writer.setAttribute(arcName+"lg", "x", "-10");
		writer.setAttribute(arcName+"lg", "y", "-10");
		//adiciona ao label
		writer.appendChild(arcName+"l", arcName+"lg");
	}
	
	
	/**
	 * Cria a atividade.
	 * 
	 * @param activityName
	 * @param roleName
	 * @param delay
	 * @return
	 */
	private Subprocess createActivityNet(String activityName, String roleName, double delay)
	{
		Subprocess result = null;
		
		activityName = activityName + activityCounter;
		
		placeList.add(new Place("W_"+activityName, "0"));
		placeList.add(new Place("S_"+activityName, "0"));
		
		immTransitionList.add(new ImmediateTransition("q_"+activityName, 1.0));
		
		transitionList.add(new ExponentialTransition("T_"+activityName, delay, true));
		
		arcList.add(new Arc("w-q_"+activityName, "W_"+activityName, "q_"+activityName));
		arcList.add(new Arc("q-S_"+activityName, "q_"+activityName, "S_"+activityName));
		arcList.add(new Arc("S-T_"+activityName, "S_"+activityName, "T_"+activityName));
		
		arcList.add(new Arc("r-q_"+activityName, roleName, "q_"+activityName));
		arcList.add(new Arc("T-r_"+activityName, "T_"+activityName, roleName));
		
		//Cria o subprocesso indicando o lugar inicial e a transicao final
		result = new Subprocess();
		result.setStartingPlace("W_"+activityName);
		result.getDeptTransitions().add("T_"+activityName);
		
		activityCounter++;
		
		return result;
	}
	
	
	/**
	 * Gera um novo id no formato "id#"
	 * @return
	 */
	public String getNewId()
	{
		String id = "_"+nextId;
		
		nextId++;
		
		return id;
	}
	
//----- Objects position

	private String newXpos() {
		
		String posXstr = Integer.toString(posX);
		
		if (posX > 1000)
		{
			posX = 50;
			posY += 100;
		}
		else
			posX += 50;
		
		return posXstr;
	}


	private String newYpos() {

		String posYstr = Integer.toString(posY);
		
		posY = posY + yFlag;
		
		yFlag = -yFlag;
		
		return posYstr;
		
	}
	
}
