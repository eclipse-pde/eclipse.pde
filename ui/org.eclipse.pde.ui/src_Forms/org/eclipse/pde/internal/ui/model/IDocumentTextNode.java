package org.eclipse.pde.internal.ui.model;

public interface IDocumentTextNode {
	
	void setEnclosingElement(IDocumentNode node);	
	IDocumentNode getEnclosingElement();

	void setText(String text);
	String getText();
	
	void setOffset(int offset);
	int getOffset();
	
	void setTopOffset(int offset);
	int getTopOffset();
	
	void setLength(int length);
	int getLength();
	
	void setFullLength(int length);
	int getFullLength();
	
}
