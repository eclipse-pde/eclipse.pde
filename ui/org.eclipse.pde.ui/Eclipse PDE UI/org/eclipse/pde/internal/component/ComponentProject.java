package org.eclipse.pde.internal.component;

import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.*;

public class ComponentProject extends BaseProject {

public ComponentProject() {
	super();
}
public void configure() throws CoreException {
	addToBuildSpec(PDEPlugin.COMPONENT_BUILDER_ID);
}
public void deconfigure() throws CoreException {
	removeFromBuildSpec(PDEPlugin.COMPONENT_BUILDER_ID);
}
}
