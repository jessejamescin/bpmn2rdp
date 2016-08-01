package br.upe.dsc.bpel2net;

/**
 * User friendly/Programmer friendly exception class.
 * 
 * @author Cesar A. L. Oliveira
 *         calo@dsc.upe.br
 *
 */
public class AssertException extends Exception {

	private String friendlyMessage = "ocorreu um erro lógico.";
	
	
	public AssertException() {
		super();
	}

	public AssertException(String friendlyMsg, String detailedMsg, Throwable arg1) {
		
		super(detailedMsg, arg1);
		this.friendlyMessage = friendlyMsg;
	}

	public AssertException(String friendlyMsg, String detailedMsg) {
		super(detailedMsg);
		this.friendlyMessage = friendlyMsg;
	}

	public AssertException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @return Returns the friendlyMessage.
	 */
	public String getFriendlyMessage() {
		return friendlyMessage;
	}

	/**
	 * @param friendlyMessage The friendlyMessage to set.
	 */
	public void setFriendlyMessage(String friendlyMessage) {
		this.friendlyMessage = friendlyMessage;
	}
	
	

}
