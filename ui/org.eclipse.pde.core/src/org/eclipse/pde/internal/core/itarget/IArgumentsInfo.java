package org.eclipse.pde.internal.core.itarget;

public interface IArgumentsInfo extends ITargetObject {
	
	public static final String P_PROG_ARGS = "programArgs"; //$NON-NLS-1$
	public static final String P_VM_ARGS = "vmArgs"; //$NON-NLS-1$
	
	public String getProgramArguments();
	
	public String getVMArguments();
	
	public void setProgramArguments(String args);
	
	public void setVMArguments(String args);

}
