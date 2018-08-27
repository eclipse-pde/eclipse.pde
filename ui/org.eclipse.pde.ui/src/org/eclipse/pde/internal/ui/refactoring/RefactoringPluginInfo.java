/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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

	@Override
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

	@Override
	public IPluginModelBase getBase() {
		return (fSelection instanceof IPluginModelBase) ? (IPluginModelBase) fSelection : null;
	}

}
