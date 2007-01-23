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
package org.eclipse.pde.core.plugin;

import java.util.ArrayList;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class ModelEntry extends PlatformObject {
	
	private String fId;
	protected ArrayList fWorkspaceEntries = new ArrayList(1);
	protected ArrayList fExternalEntries = new ArrayList(1);

	public ModelEntry(String id) {
		fId = id;
	}
	
	public IPluginModelBase[] getWorkspaceModels() {
		return (IPluginModelBase[])fWorkspaceEntries.toArray(new IPluginModelBase[fWorkspaceEntries.size()]);
	}
	
	public IPluginModelBase[] getExternalModels() {
		return (IPluginModelBase[])fExternalEntries.toArray(new IPluginModelBase[fExternalEntries.size()]);
	}
	
	public IPluginModelBase getModel() {
		IPluginModelBase model = getBestCandidate(getWorkspaceModels());
		if (model == null)
			model = getBestCandidate(getExternalModels());
		return model;
	}
	
	private IPluginModelBase getBestCandidate(IPluginModelBase[] models) {
		IPluginModelBase model = null;
		for (int i = 0; i < models.length; i++) {
			if (model == null) {
				model = models[i];
				continue;
			}
			
			if (!model.isEnabled() && models[i].isEnabled()) {
				model = models[i];
				continue;
			}
			
			BundleDescription current = model.getBundleDescription();
			BundleDescription candidate = models[i].getBundleDescription();
			if (!current.isResolved() && candidate.isResolved()) {
				model = models[i];
				continue;
			}
			
			if (current.getVersion().compareTo(candidate.getVersion()) < 0) {
				model = models[i];
			}
		}
		return model;
	}
	
	public IPluginModelBase[] getActiveModels() {
		if (fWorkspaceEntries.size() > 0)
			return getWorkspaceModels();
		
		if (fExternalEntries.size() > 0) {
			ArrayList list = new ArrayList(fExternalEntries.size());
			for (int i = 0; i < fExternalEntries.size(); i++) {
				IPluginModelBase model = (IPluginModelBase)fExternalEntries.get(i);
				if (model.isEnabled())
					list.add(model);
			}
			return (IPluginModelBase[])list.toArray(new IPluginModelBase[list.size()]);
		}	
		return new IPluginModelBase[0];
	}
	
	public String getId() {
		return fId;
	}
	
	public IPluginModelBase getModel(BundleDescription desc) {
		long bundleId = desc.getBundleId();
		for (int i = 0; i < fWorkspaceEntries.size(); i++) {
			IPluginModelBase model = (IPluginModelBase)fWorkspaceEntries.get(i);
			if (model.getBundleDescription().getBundleId() == bundleId)
				return model;
		}
		for (int i = 0; i < fExternalEntries.size(); i++) {
			IPluginModelBase model = (IPluginModelBase)fExternalEntries.get(i);
			if (model.getBundleDescription().getBundleId() == bundleId)
				return model;
		}
		return null;			
	}
	
	public boolean hasWorkspaceModels() {
		return !fWorkspaceEntries.isEmpty();
	}
	
	public boolean hasExternalModels() {
		return !fExternalEntries.isEmpty();
	}
	
}
