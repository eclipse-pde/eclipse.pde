/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.model.PluginModel;

/**
 * Generates build.xml script for plug-ins.
 */
public class PluginBuildScriptGenerator extends ModelBuildScriptGenerator {
	public PluginBuildScriptGenerator() {

	}
	/**
	 * @see ModelBuildScriptGenerator#getModel(String)
	 */
	protected PluginModel getModel(String modelId) throws CoreException {
		return getSite(false).getPluginRegistry().getPlugin(modelId);
	}

	/**
	 * @see ModelBuildScriptGenerator#getModelTypeName()
	 */
	protected String getModelTypeName() {
		return "plugin"; //$NON-NLS-1$
	}

}
