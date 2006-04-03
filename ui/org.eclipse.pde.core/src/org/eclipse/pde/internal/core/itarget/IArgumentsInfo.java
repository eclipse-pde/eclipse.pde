/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.itarget;

public interface IArgumentsInfo extends ITargetObject {
	
	public static final String P_PROG_ARGS = "programArgs"; //$NON-NLS-1$
	public static final String P_VM_ARGS = "vmArgs"; //$NON-NLS-1$
	
	public String getProgramArguments();
	
	public String getVMArguments();
	
	public void setProgramArguments(String args);
	
	public void setVMArguments(String args);

}
