package org.eclipse.pde.internal.ui.model;

/**
 * @author melhem
 *
 */
public interface IDocumentNode {
	
	IDocumentNode[] getChildNodes();
	
	IDocumentNode getParentNode();
	
	void setParentNode(IDocumentNode node);
	
	void addChildNode(IDocumentNode child);
	
	void setXMLAttribute(IDocumentAttribute attribute);
	
	void setXMLAttribute(String name, String value);
	
	String getXMLAttributeValue(String name);
	
	boolean isErrorNode();
	
	void setIsErrorNode(boolean isErrorNode);
	
	void setOffset(int offset);
	void setLength(int length);
	
	int getOffset();
	int getLength();
	
}
