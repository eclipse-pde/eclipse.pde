/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.natures;

import org.eclipse.core.runtime.CoreException;

public class FeatureProject extends BaseProject {

	public FeatureProject() {
		super();
	}

	@Override
	public void configure() throws CoreException {
		addToBuildSpec(PDE.FEATURE_BUILDER_ID);
	}

	@Override
	public void deconfigure() throws CoreException {
		removeFromBuildSpec(PDE.FEATURE_BUILDER_ID);
	}
}
