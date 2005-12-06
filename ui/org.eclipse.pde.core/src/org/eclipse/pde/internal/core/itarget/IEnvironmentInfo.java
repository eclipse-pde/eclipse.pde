package org.eclipse.pde.internal.core.itarget;

public interface IEnvironmentInfo extends ITargetObject{
	
	public static final String P_OS = "os"; //$NON-NLS-1$
	public static final String P_WS = "ws"; //$NON-NLS-1$
	public static final String P_ARCH = "arch"; //$NON-NLS-1$
	public static final String P_NL = "nl"; //$NON-NLS-1$
	
	public String getOS();
	
	public String getWS();
	
	public String getArch();
	
	public String getNL();
	
	public void setOS(String os);
	
	public void setWS(String ws);
	
	public void setArch(String arch);
	
	public void setNL(String nl);

}
