package org.eclipse.pde.internal.ui.model;

/**
 * @author melhem
 *
 */
public interface IDocumentNode {
	
	
	IDocumentNode getParentNode();	
	void setParentNode(IDocumentNode node);
	
	void addChildNode(IDocumentNode child);
	void addChildNode(IDocumentNode child, int position);
	IDocumentNode[] getChildNodes();

	IDocumentNode getPreviousSibling();
	void setPreviousSibling(IDocumentNode sibling);
	
	void setXMLTagName(String tag);
	
	String getXMLTagName();
	
	void setXMLAttribute(IDocumentAttribute attribute);	
	void setXMLAttribute(String name, String value);	

	String getXMLAttributeValue(String name);	
	IDocumentAttribute getDocumentAttribute(String name);
	IDocumentAttribute[] getNodeAttributes();
	

	boolean isErrorNode();	
	void setIsErrorNode(boolean isErrorNode);
	
	void setOffset(int offset);
	void setLength(int length);
	
	int getOffset();
	int getLength();
	
	void setLineIndent(int indent);
	int getLineIndent();
	
	String write(boolean indent);
	String writeShallow(boolean terminate);
	
}
