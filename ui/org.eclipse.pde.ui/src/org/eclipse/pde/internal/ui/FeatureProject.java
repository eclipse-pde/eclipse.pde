package org.eclipse.pde.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.ui.*;

public class FeatureProject extends BaseProject {

public FeatureProject() {
	super();
}
public void configure() throws CoreException {
	addToBuildSpec(PDEPlugin.FEATURE_BUILDER_ID);
}
public void deconfigure() throws CoreException {
	removeFromBuildSpec(PDEPlugin.FEATURE_BUILDER_ID);
}
}
