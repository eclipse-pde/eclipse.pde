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

	@Override
	public IPluginBase createPluginBase() {
		Fragment fragment = new Fragment(!isEditable());
		fragment.setModel(this);
		return fragment;
	}

	@Override
	public IFragment getFragment() {
		return (IFragment) getPluginBase();
	}

	@Override
	public boolean isFragmentModel() {
		return true;
	}
}
