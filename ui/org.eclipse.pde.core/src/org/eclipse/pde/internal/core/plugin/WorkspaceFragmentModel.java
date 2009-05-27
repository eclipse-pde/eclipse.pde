/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;

public class WorkspaceFragmentModel extends WorkspacePluginModelBase implements IFragmentModel {

	private static final long serialVersionUID = 1L;

	public WorkspaceFragmentModel(IFile file, boolean abbreviated) {
		super(file, abbreviated);
	}

	public IPluginBase createPluginBase() {
		Fragment fragment = new Fragment(!isEditable());
		fragment.setModel(this);
		return fragment;
	}

	public IFragment getFragment() {
		return (IFragment) getPluginBase();
	}

	public boolean isFragmentModel() {
		return true;
	}
}
