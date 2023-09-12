/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.search.ExtensionPluginSearchScope;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.ISearchQuery;

public class FindDeclarationsAction extends BaseSearchAction {

	private final Object fSelectedObject;

	public FindDeclarationsAction(Object object) {
		super(PDEUIMessages.SearchAction_Declaration);
		setImageDescriptor(PDEPluginImages.DESC_PSEARCH_OBJ);
		this.fSelectedObject = object;
	}

	@Override
	protected ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();
		PluginSearchScope scope = null;

		if (fSelectedObject instanceof IPluginImport) {
			input.setSearchString(((IPluginImport) fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		} else if (fSelectedObject instanceof IPluginExtension) {
			input.setSearchString(((IPluginExtension) fSelectedObject).getPoint());
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			scope = new ExtensionPluginSearchScope(input);
		} else if (fSelectedObject instanceof IPlugin) {
			input.setSearchString(((IPlugin) fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		} else if (fSelectedObject instanceof IFragment) {
			input.setSearchString(((IFragment) fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_FRAGMENT);
		}
		input.setSearchLimit(PluginSearchInput.LIMIT_DECLARATIONS);
		input.setSearchScope((scope == null) ? new PluginSearchScope() : scope);
		return new PluginSearchQuery(input);
	}

}
