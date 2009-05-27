/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.natures;

import org.eclipse.core.runtime.CoreException;

/**
 */
public class PluginProject extends BaseProject {
	/**
	 * PluginProject constructor comment.
	 */
	public PluginProject() {
		super();
	}

	public void configure() throws CoreException {
		addToBuildSpec(PDE.MANIFEST_BUILDER_ID);
		addToBuildSpec(PDE.SCHEMA_BUILDER_ID);
	}

	public void deconfigure() throws CoreException {
		removeFromBuildSpec(PDE.MANIFEST_BUILDER_ID);
		removeFromBuildSpec(PDE.SCHEMA_BUILDER_ID);
	}
}
