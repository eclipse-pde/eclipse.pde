package org.eclipse.pde.internal;

import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.*;
/**
 * Insert the type's description here.
 * Creation date: (12/13/2000 6:20:05 PM)
 * @author: Dejan Glozic
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
