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
	
	void setXMLTagName(String tag);
	
	String getXMLTagName();
	
	void setXMLAttribute(IDocumentAttribute attribute);
	
	void setXMLAttribute(String name, String value);
	
	String getXMLAttributeValue(String name);
	
	IDocumentAttribute getDocumentAttribute(String name);
	
	boolean isErrorNode();
	
	void setIsErrorNode(boolean isErrorNode);
	
	void setOffset(int offset);
	void setLength(int length);
	
	int getOffset();
	int getLength();
	
	int getRecommendedOffset(IDocumentAttribute attribute);
	
}
