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

public interface ITarget extends ITargetObject {
	
	final String P_ID = "id"; //$NON-NLS-1$
	final String P_NAME = "name"; //$NON-NLS-1$
	final String P_ALL_PLUGINS = "useAllPlugins"; //$NON-NLS-1$
	
	void reset();
	
	String getId();
	
	void setId(String id);
	
	String getName();
	
	void setName(String name);
	
	IArgumentsInfo getArguments();
	
	void setArguments(IArgumentsInfo info);
	
	IEnvironmentInfo getEnvironment();
	
	void setEnvironment(IEnvironmentInfo info);
	
	IRuntimeInfo getTargetJREInfo();
	
	void setTargetJREInfo(IRuntimeInfo info);
	
	ILocationInfo getLocationInfo();
	
	void setImplicitPluginsInfo(IImplicitDependenciesInfo info);
	
	IImplicitDependenciesInfo getImplicitPluginsInfo();
	
	void setLocationInfo(ILocationInfo info);
		
	void addPlugin(ITargetPlugin plugin);
	
	void addPlugins(ITargetPlugin[] plugins);
	
	void addFeature(ITargetFeature feature);
	
	void addFeatures(ITargetFeature features[]);
	
	void removePlugin(ITargetPlugin plugin);
	
	void removePlugins(ITargetPlugin[] plugins);
	
	void removeFeature(ITargetFeature feature);
	
	void removeFeatures(ITargetFeature[] features);
	
	ITargetPlugin[] getPlugins();
	
	ITargetFeature[] getFeatures();
	
	boolean containsPlugin(String id);
	
	boolean containsFeature(String id);
	
	boolean useAllPlugins();
	
	void setUseAllPlugins(boolean value);

}
