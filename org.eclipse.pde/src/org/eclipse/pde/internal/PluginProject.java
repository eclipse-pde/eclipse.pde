/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal;

import org.eclipse.core.runtime.*;
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
