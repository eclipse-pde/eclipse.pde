package org.eclipse.pde.internal.core.ibundle;

public interface IBundle {
	
	void setHeader(String key, String value);
	
	String getHeader(String key);
}
