package org.eclipse.pde.internal.ui.model;

public interface IDocumentTextNode {
	
	void setEnclosingElement(IDocumentNode node);	
	IDocumentNode getEnclosingElement();

	void setText(String text);
	String getText();
	
	void setOffset(int offset);
	int getOffset();
	
	void setLength(int length);
	int getLength();
	
}
