package org.eclipse.pde.internal.core.itarget;

public interface ITargetPlugin extends ITargetObject {
	
	String getId();
	
	void setId(String id);
	
	boolean isOptional();
	
	void setOptional(boolean optional);

}
