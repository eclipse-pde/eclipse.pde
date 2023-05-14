/*******************************************************************************
 *  Copyright (c) 2007, 2019 IBM Corporation and others.
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
package org.eclipse.pde.core.plugin;

import java.util.ArrayList;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * A ModelEntry object has an ID and keeps track of all workspace plug-ins and target
 * plug-ins that have that ID.
 * <p>
 * This class is not meant to be extended or instantiated by clients.
 * </p>
 *
 * @since 3.3
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ModelEntry extends PlatformObject {

	private final String fId;

	/**
	 * The list of workspace models with the same entry ID
	 */
	protected ArrayList<IPluginModelBase> fWorkspaceEntries = new ArrayList<>(1);

	/**
	 * The list of external models with the same entry ID
	 */
	protected ArrayList<IPluginModelBase> fExternalEntries = new ArrayList<>(1);

	/**
	 * Constructor
	 *
	 * @param id the entry ID
	 */
	public ModelEntry(String id) {
		fId = id;
	}

	/**
	 * Returns all the workspace plug-ins that have the model entry ID
	 *
	 * @return an array of workspace plug-ins that have the model entry ID
	 */
	public IPluginModelBase[] getWorkspaceModels() {
		return fWorkspaceEntries.toArray(new IPluginModelBase[fWorkspaceEntries.size()]);
	}

	/**
	 * Returns all plug-ins in the target platform that have the model entry ID.
	 * The returned result contains both plug-ins that are enabled (ie. checked
	 * on the <b>Plug-in Development &gt; Target Platform</b> preference page)
	 * and disabled.
	 *
	 * @return an array of plug-ins in the target platform that have the model
	 *         entry ID
	 */
	public IPluginModelBase[] getExternalModels() {
		return fExternalEntries.toArray(new IPluginModelBase[fExternalEntries.size()]);
	}

	/**
	 * Returns the plug-in model for the best match plug-in with the given ID.
	 * A null value is returned if no such bundle is found in the workspace or target platform.
	 * <p>
	 * A workspace plug-in is always preferably returned over a target plug-in.
	 * A plug-in that is checked/enabled on the Target Platform preference page is always
	 * preferably returned over a target plug-in that is unchecked/disabled.
	 * </p>
	 * <p>
	 * In the case of a tie among workspace plug-ins or among target plug-ins,
	 * the plug-in with the highest version is returned.
	 * </p>
	 * <p>
	 * In the case of a tie among more than one suitable plug-in that have the same version,
	 * one of those plug-ins is randomly returned.
	 * </p>
	 *
	 * @return the plug-in model for the best match plug-in with the given ID
	 */
	public IPluginModelBase getModel() {
		IPluginModelBase model = getBestCandidate(getWorkspaceModels());
		if (model == null) {
			model = getBestCandidate(getExternalModels());
		}
		return model;
	}

	private IPluginModelBase getBestCandidate(IPluginModelBase[] models) {
		IPluginModelBase result = null;
		for (IPluginModelBase model : models) {
			if (model.getBundleDescription() == null) {
				continue;
			}

			if (result == null) {
				result = model;
				continue;
			}

			if (!result.isEnabled() && model.isEnabled()) {
				result = model;
				continue;
			}

			BundleDescription current = result.getBundleDescription();
			BundleDescription candidate = model.getBundleDescription();
			if (!current.isResolved() && candidate.isResolved()) {
				result = model;
				continue;
			}

			if (current.getVersion().compareTo(candidate.getVersion()) < 0) {
				result = model;
			}
		}
		return result;
	}

	/**
	 * Returns all the plug-ins, with the model entry ID, that are currently
	 * active.
	 * <p>
	 * Workspace plug-ins are always active. Target plug-ins are only active if:
	 * </p>
	 * <ul>
	 * <li>they are checked on the <b>Plug-in Development &gt; Target
	 * Platform</b> preference page</li>
	 * <li>there does not exist a workspace plug-in that has the same ID</li>
	 * </ul>
	 *
	 * @return an array of the currently active plug-ins with the model entry ID
	 */
	public IPluginModelBase[] getActiveModels() {
		if (!fWorkspaceEntries.isEmpty()) {
			return getWorkspaceModels();
		}

		if (!fExternalEntries.isEmpty()) {
			ArrayList<IPluginModelBase> list = new ArrayList<>(fExternalEntries.size());
			for (int i = 0; i < fExternalEntries.size(); i++) {
				IPluginModelBase model = fExternalEntries.get(i);
				if (model.isEnabled()) {
					list.add(model);
				}
			}
			return list.toArray(new IPluginModelBase[list.size()]);
		}
		return new IPluginModelBase[0];
	}

	/**
	 * Returns the model entry ID
	 *
	 * @return the model entry ID
	 */
	public String getId() {
		return fId;
	}

	/**
	 * Return the plug-in model associated with the given bundle description or
	 * <code>null</code> if none is found.
	 *
	 * @param desc  the given bundle description
	 *
	 * @return the plug-in model associated with the given bundle description if such a
	 * model exists.
	 */
	public IPluginModelBase getModel(BundleDescription desc) {
		if (desc == null) {
			return null;
		}

		for (int i = 0; i < fWorkspaceEntries.size(); i++) {
			IPluginModelBase model = fWorkspaceEntries.get(i);
			if (desc.equals(model.getBundleDescription())) {
				return model;
			}
		}
		for (int i = 0; i < fExternalEntries.size(); i++) {
			IPluginModelBase model = fExternalEntries.get(i);
			if (desc.equals(model.getBundleDescription())) {
				return model;
			}
		}
		return null;
	}

	/**
	 * Returns <code>true</code> if there are workspace plug-ins associated with the ID
	 * of this model entry; <code>false</code>otherwise.
	 *
	 * @return <code>true</code> if there are workspace plug-ins associated with the ID
	 * of this model entry; <code>false</code>otherwise.
	 */
	public boolean hasWorkspaceModels() {
		return !fWorkspaceEntries.isEmpty();
	}

	/**
	 * Returns <code>true</code> if there are target plug-ins associated with the ID
	 * of this model entry; <code>false</code>otherwise.
	 *
	 * @return <code>true</code> if there are target plug-ins associated with the ID
	 * of this model entry; <code>false</code>otherwise.
	 */
	public boolean hasExternalModels() {
		return !fExternalEntries.isEmpty();
	}

}
