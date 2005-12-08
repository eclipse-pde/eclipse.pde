package org.eclipse.pde.internal.core.itarget;

public interface ILocationInfo extends ITargetObject {
	
	final String P_LOC = "location"; //$NON-NLS-1$
	
	public boolean useDefault();
	
	public String getPath();
	
	public void setPath(String path);

}
