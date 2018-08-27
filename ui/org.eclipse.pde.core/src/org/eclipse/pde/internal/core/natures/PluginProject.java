/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

	@Override
	public void configure() throws CoreException {
		addToBuildSpec(PDE.MANIFEST_BUILDER_ID);
		addToBuildSpec(PDE.SCHEMA_BUILDER_ID);
	}

	@Override
	public void deconfigure() throws CoreException {
		removeFromBuildSpec(PDE.MANIFEST_BUILDER_ID);
		removeFromBuildSpec(PDE.SCHEMA_BUILDER_ID);
	}
}
