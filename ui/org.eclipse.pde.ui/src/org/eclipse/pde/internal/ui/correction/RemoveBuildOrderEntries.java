/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.ui.model.bundle.BundleModel;

public class RemoveBuildOrderEntries extends ManifestHeaderErrorResolution {

	public RemoveBuildOrderEntries(int type) {
		super(type);
	}

	public String getDescription() {
		return ".project file contains potentially harmfull <project> entries.";
	}

	public String getLabel() {
		return "remove all <project> entries";
	}

	protected void createChange(BundleModel model) {
		try {
			IProjectDescription projDesc = model.getUnderlyingResource().getProject().getDescription();
			projDesc.setReferencedProjects(new IProject[0]);
		} catch (CoreException e) {
		}
	}
}