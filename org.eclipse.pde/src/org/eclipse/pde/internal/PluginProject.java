package org.eclipse.pde.internal;
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
	addToBuildSpec(PDE.MANIFEST_BUILDER_ID);
	addToBuildSpec(PDE.SCHEMA_BUILDER_ID);
}

public void deconfigure() throws CoreException {
	removeFromBuildSpec(PDE.MANIFEST_BUILDER_ID);
	removeFromBuildSpec(PDE.SCHEMA_BUILDER_ID);
}
}
