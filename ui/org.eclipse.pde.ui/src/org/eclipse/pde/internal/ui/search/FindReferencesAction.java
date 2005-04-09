/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.search.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.search.ui.*;


public class FindReferencesAction extends BaseSearchAction {
	
	private Object fSelectedObject;
	
	public FindReferencesAction(Object object) {
		super(PDEUIMessages.SearchAction_references);
		fSelectedObject = object;
	}
	
	protected ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();
		if (fSelectedObject instanceof IPlugin) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IPlugin) fSelectedObject).getId());
		} else if (fSelectedObject instanceof IPluginExtensionPoint) {
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			IPluginModelBase model = ((IPluginExtensionPoint) fSelectedObject).getPluginModel();
			String id = model.getPluginBase().getId();
			if (id == null || id.trim().length() == 0)
				id = "*"; //$NON-NLS-1$
			input.setSearchString(
						id
						+ "." //$NON-NLS-1$
						+ ((IPluginExtensionPoint) fSelectedObject).getId());
		} else if (fSelectedObject instanceof IPluginImport) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IPluginImport) fSelectedObject).getId());
		}
		input.setSearchLimit(PluginSearchInput.LIMIT_REFERENCES);
		input.setSearchScope(new PluginSearchScope());
		return new PluginSearchQuery(input);
	}

}
