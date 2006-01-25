package org.eclipse.pde.internal.core.itarget;

public interface IAdditionalLocation extends ITargetObject {
	
	public static final String P_PATH = "path"; //$NON-NLS-1$
	String getPath();
	void setPath(String path);

}
