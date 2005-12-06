package org.eclipse.pde.internal.core.itarget;

public interface IRuntimeInfo extends ITargetObject {
	
	public final static int TYPE_DEFAULT = 0;
	public final static int TYPE_NAMED = 1;
	public final static int TYPE_EXECUTION_ENV = 2;
	
	public static final String P_TARGET_JRE = "targetJRE"; //$NON-NLS-1$
	
	public int getJREType();
	
	public String getJREName();
	
	public void setNamedJRE(String name);
	
	public void setExecutionEnvJRE(String name);
	
	public void setDefaultJRE();

}
