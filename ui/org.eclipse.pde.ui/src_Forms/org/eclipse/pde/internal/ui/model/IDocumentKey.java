package org.eclipse.pde.internal.ui.model;

import org.eclipse.pde.core.*;

public interface IDocumentKey extends IWritable {
	void setName(String name);
	String getName();
	
	void setValue(String value);
	String getValue();
	
	void setOffset(int offset);
	int getOffset();
	
	void setLineSpan(int span);
	int getLineSpan();
	
}
