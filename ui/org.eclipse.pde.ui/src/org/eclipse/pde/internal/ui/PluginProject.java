package org.eclipse.pde.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.ui.*;
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
	addToBuildSpec(PDEPlugin.MANIFEST_BUILDER_ID);
	addToBuildSpec(PDEPlugin.SCHEMA_BUILDER_ID);
}

public void deconfigure() throws CoreException {
	removeFromBuildSpec(PDEPlugin.MANIFEST_BUILDER_ID);
	removeFromBuildSpec(PDEPlugin.SCHEMA_BUILDER_ID);
}
}
