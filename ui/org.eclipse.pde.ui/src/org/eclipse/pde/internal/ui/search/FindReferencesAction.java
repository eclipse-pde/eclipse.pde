/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.search.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.ISearchQuery;
import org.osgi.framework.Version;

/**
 * A find references action to find references to a given object. The object should
 * be an instance of {@link IPlugin}, {@link IPluginExtensionPoint}, {@link IPluginImport}, or
 * {@link IPluginExtension}.  The pluginID will be used to qualify an extension point ID.  If
 * <code>null</code> is passed, the extension point object will be queried for the plug-in id.
 * If no id is found, the search scope will prefix the extension point ID with <code>*</code>
 *
 **/
public class FindReferencesAction extends BaseSearchAction {

	private Object fSelectedObject;
	private String fPluginID;

	/**
	 * Creates a new find references action to find references to the given object. The object should
	 * be an instance of {@link IPlugin}, {@link IPluginExtensionPoint}, {@link IPluginImport}, or
	 * {@link IPluginExtension}.  The pluginID will be used to qualify an extension point ID.  If
	 * <code>null</code> is passed, the extension point object will be queried for the plug-in id.
	 * If no id is found, the search scope will prefix the extension point ID with <code>*</code>
	 *
	 * @param object the object to search for references to
	 * @param pluginID plug-in id to prefix extension point id's with
	 */
	public FindReferencesAction(Object object, String pluginID) {
		super(PDEUIMessages.SearchAction_references);
		fSelectedObject = object;
		fPluginID = pluginID;
		initialize();
	}

	private void initialize() {
		setImageDescriptor(PDEPluginImages.DESC_PSEARCH_OBJ);
	}

	@Override
	protected ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();
		PluginSearchScope scope = null;
		if (fSelectedObject instanceof IPlugin) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IPlugin) fSelectedObject).getId());
		} else if (fSelectedObject instanceof IPluginExtensionPoint) {
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			String extensionID = ((IPluginExtensionPoint) fSelectedObject).getId();
			IPluginModelBase model = ((IPluginExtensionPoint) fSelectedObject).getPluginModel();

			// Only plug-in xmls created with 3.2 or later support fully qualified names, assume no file version means a > 3.2 version
			String schemaVersion = model.getPluginBase().getSchemaVersion();
			Version fileVersion = schemaVersion != null ? new Version(schemaVersion) : null;
			if ((fileVersion == null || fileVersion.compareTo(new Version("3.2")) >= 0) && extensionID.indexOf('.') >= 0) { //$NON-NLS-1$
				// Fully qualified extension point, don't prefix with plug-in id
				input.setSearchString(extensionID);
			} else {
				String id = model.getPluginBase().getId();
				if (id == null || id.trim().length() == 0)
					id = fPluginID;
				if (id == null || id.trim().length() == 0)
					id = "*"; //$NON-NLS-1$
				input.setSearchString(id + "." + extensionID); //$NON-NLS-1$
			}
			scope = new ExtensionPluginSearchScope(input);
		} else if (fSelectedObject instanceof IPluginImport) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IPluginImport) fSelectedObject).getId());
		} else if (fSelectedObject instanceof IPluginExtension) {
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			input.setSearchString(((IPluginExtension) fSelectedObject).getPoint());
			input.setSearchScope(new ExtensionPluginSearchScope(input));
		}
		input.setSearchLimit(PluginSearchInput.LIMIT_REFERENCES);
		input.setSearchScope((scope == null) ? new PluginSearchScope() : scope);
		return new PluginSearchQuery(input);
	}
}
