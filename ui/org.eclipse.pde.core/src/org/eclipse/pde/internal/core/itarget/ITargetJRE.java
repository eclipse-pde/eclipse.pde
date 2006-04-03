/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.itarget;

public interface ITargetJRE extends ITargetObject {
	
	public final static int TYPE_DEFAULT = 0;
	public final static int TYPE_NAMED = 1;
	public final static int TYPE_EXECUTION_ENV = 2;
	
	public static final String P_TARGET_JRE = "targetJRE"; //$NON-NLS-1$
	
	/**
	 * Returns the JRE type (TYPE_DEFAULT or TYPE_NAMED or TYPE_EXECUTION_ENV)
	 * 
	 * @return the int representing the predefined type of the JRE defined
	 */
	public int getJREType();
	
	public String getJREName();
	
	public void setNamedJRE(String name);
	
	public void setExecutionEnvJRE(String name);
	
	public void setDefaultJRE();
	
	public String getCompatibleJRE();

}
