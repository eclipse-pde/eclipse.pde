package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
	addToBuildSpec(PDECore.MANIFEST_BUILDER_ID);
	addToBuildSpec(PDECore.SCHEMA_BUILDER_ID);
}

public void deconfigure() throws CoreException {
	removeFromBuildSpec(PDECore.MANIFEST_BUILDER_ID);
	removeFromBuildSpec(PDECore.SCHEMA_BUILDER_ID);
}
}
