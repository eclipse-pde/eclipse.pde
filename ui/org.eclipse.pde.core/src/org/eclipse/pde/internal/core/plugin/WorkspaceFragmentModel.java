/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.pde.core.plugin.*;

public class WorkspaceFragmentModel extends WorkspacePluginModelBase implements
		IFragmentModel {

	private static final long serialVersionUID = 1L;

	public WorkspaceFragmentModel(org.eclipse.core.resources.IFile file) {
		super(file);
	}

	public IPluginBase createPluginBase() {
		Fragment fragment = new Fragment();
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
