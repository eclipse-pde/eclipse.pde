package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;

public class FeatureProject extends BaseProject {

public FeatureProject() {
	super();
}
public void configure() throws CoreException {
	addToBuildSpec(PDE.FEATURE_BUILDER_ID);
}
public void deconfigure() throws CoreException {
	removeFromBuildSpec(PDE.FEATURE_BUILDER_ID);
}
}
