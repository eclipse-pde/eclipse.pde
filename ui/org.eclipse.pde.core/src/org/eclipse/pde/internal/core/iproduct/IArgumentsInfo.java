package org.eclipse.pde.internal.core.iproduct;

public interface IArgumentsInfo extends IProductObject {
	
	public static final String P_PROG_ARGS = "programArgs"; //$NON-NLS-1$
	public static final String P_VM_ARGS = "vmArgs"; //$NON-NLS-1$
	
	void setProgramArguments(String args);
	
	String getProgramArguments();
	
	void setVMArguments(String args);
	
	String getVMArguments();

}
