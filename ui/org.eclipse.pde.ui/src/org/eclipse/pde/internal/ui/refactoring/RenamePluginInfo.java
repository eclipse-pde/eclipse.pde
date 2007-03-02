/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class RenamePluginInfo {
	
	private IPluginModelBase fBase;
	
	private boolean fRenameProject;
	
	private boolean fUpdateReferences = true;
	
	private String fNewID;

	public IPluginModelBase getBase() {
		return fBase;
	}

	public void setBase(IPluginModelBase base) {
		fBase = base;
	}

	public boolean isRenameProject() {
		return fRenameProject;
	}

	public void setRenameProject(boolean renameProject) {
		fRenameProject = renameProject;
	}

	public boolean isUpdateReferences() {
		return fUpdateReferences;
	}

	public void setUpdateReferences(boolean updateReferences) {
		fUpdateReferences = updateReferences;
	}

	public String getNewID() {
		return fNewID;
	}

	public void setNewID(String newName) {
		fNewID = newName;
	}
	
	public String getCurrentID() {
		BundleDescription desc = fBase.getBundleDescription();
		if (desc != null)
			return desc.getSymbolicName();
		IPluginBase pb = fBase.getPluginBase();
		return pb.getId();
	}

}
