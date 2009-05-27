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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.search.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.ISearchQuery;

public class FindReferencesAction extends BaseSearchAction {

	private Object fSelectedObject;
	private String fPluginID;

	public FindReferencesAction(Object object) {
		this(object, null);
	}

	public FindReferencesAction(Object object, String pluginID) {
		super(PDEUIMessages.SearchAction_references);
		fSelectedObject = object;
		fPluginID = pluginID;
		initialize();
	}

	private void initialize() {
		setImageDescriptor(PDEPluginImages.DESC_PSEARCH_OBJ);
	}

	protected ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();
		PluginSearchScope scope = null;
		if (fSelectedObject instanceof IPlugin) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IPlugin) fSelectedObject).getId());
		} else if (fSelectedObject instanceof IPluginExtensionPoint) {
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			IPluginModelBase model = ((IPluginExtensionPoint) fSelectedObject).getPluginModel();
			String id = model.getPluginBase().getId();
			if (id == null || id.trim().length() == 0)
				id = fPluginID;
			if (id == null || id.trim().length() == 0)
				id = "*"; //$NON-NLS-1$
			input.setSearchString(id + "." //$NON-NLS-1$
					+ ((IPluginExtensionPoint) fSelectedObject).getId());
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
