package org.eclipse.pde.internal.ui.model;

/**
 * @author melhem
 *
 */
public interface IDocumentAttribute {
	
	void setEnclosingElement(IDocumentNode node);	
	IDocumentNode getEnclosingElement();
	
	void setNameOffset(int offset);
	int getNameOffset();
	
	void setNameLength(int length);
	int getNameLength();
	
	void setValueOffset(int offset);
	int getValueOffset();
	
	void setValueLength(int length);
	int getValueLength();
	
	String getAttributeName();
	String getAttributeValue();
	
	
}
