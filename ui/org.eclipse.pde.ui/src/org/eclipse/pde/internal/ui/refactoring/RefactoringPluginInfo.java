/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class RefactoringPluginInfo extends RefactoringInfo {

	private boolean fRenameProject;

	public boolean isRenameProject() {
		return fRenameProject;
	}

	public void setRenameProject(boolean renameProject) {
		fRenameProject = renameProject;
	}

	public String getCurrentValue() {
		IPluginModelBase base = getBase();
		if (base == null)
			return null;
		BundleDescription desc = base.getBundleDescription();
		if (desc != null)
			return desc.getSymbolicName();
		IPluginBase pb = base.getPluginBase();
		return pb.getId();
	}

	public IPluginModelBase getBase() {
		return (fSelection instanceof IPluginModelBase) ? (IPluginModelBase) fSelection : null;
	}

}
