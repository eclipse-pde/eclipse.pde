/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
