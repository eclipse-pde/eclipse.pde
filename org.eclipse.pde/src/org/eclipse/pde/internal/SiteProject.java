package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;

public class SiteProject extends BaseProject {

public SiteProject() {
	super();
}
public void configure() throws CoreException {
	addToBuildSpec(PDE.SITE_BUILDER_ID);
}
public void deconfigure() throws CoreException {
	removeFromBuildSpec(PDE.SITE_BUILDER_ID);
}
}
