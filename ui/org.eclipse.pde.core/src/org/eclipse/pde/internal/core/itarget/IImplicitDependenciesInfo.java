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

public interface IImplicitDependenciesInfo extends ITargetObject {
	
	public static final String P_IMPLICIT_PLUGINS = "implicit-plugins"; //$NON-NLS-1$
	
	ITargetPlugin[] getPlugins();
	
	public void addPlugin(ITargetPlugin plugin);
	
	public void addPlugins(ITargetPlugin[] plugins);
	
	public void removePlugin(ITargetPlugin plugin);
	
	public void removePlugins(ITargetPlugin[] plugins);
	
	public boolean containsPlugin(String id);

}
