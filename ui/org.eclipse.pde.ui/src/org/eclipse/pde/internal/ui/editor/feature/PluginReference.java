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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;

public class PluginReference {
	private IFeaturePlugin reference;
	private IPluginModelBase model;
	private boolean fragment;

	public PluginReference(IFeaturePlugin reference, IPluginModelBase model) {
		this.reference = reference;
		this.model = model;
	}

	public IPluginModelBase getModel() {
		return model;
	}

	public IFeaturePlugin getReference() {
		return reference;
	}

	public boolean isFragment() {
		return fragment;
	}

	public boolean isInSync() {
		if (model == null)
			return false;
		if (reference == null)
			return true;
		if (!reference.getId().equals(model.getPluginBase().getId()))
			return false;
		if (!reference.getVersion().equals(model.getPluginBase().getVersion()))
			return false;
		return true;
	}

	public boolean isUnresolved() {
		return false;
	}

	public void setFragment(boolean newFragment) {
		fragment = newFragment;
	}

	public void setModel(IPluginModelBase newModel) {
		model = newModel;
	}

	public void setReference(IFeaturePlugin newReference) {
		reference = newReference;
	}

	public String toString() {
		String name = model.getPluginBase().getName();
		return model.getResourceString(name);
	}
}
