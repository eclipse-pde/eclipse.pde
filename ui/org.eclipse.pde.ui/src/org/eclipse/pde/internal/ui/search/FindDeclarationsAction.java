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


public class FindDeclarationsAction extends BaseSearchAction {
	
	private Object fSelectedObject;

	public FindDeclarationsAction(Object object) {
		super(PDEUIMessages.SearchAction_Declaration);
		this.fSelectedObject = object;
	}
	
	protected ISearchQuery createSearchQuery() {
		PluginSearchInput input = new PluginSearchInput();

		if (fSelectedObject instanceof IPluginImport) {
			input.setSearchString(((IPluginImport) fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		} else if (fSelectedObject instanceof IPluginExtension)  {
			input.setSearchString(((IPluginExtension)fSelectedObject).getPoint());
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
		} else if (fSelectedObject instanceof IPlugin) {
			input.setSearchString(((IPlugin)fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
		} else if (fSelectedObject instanceof IFragment) {
			input.setSearchString(((IFragment)fSelectedObject).getId());
			input.setSearchElement(PluginSearchInput.ELEMENT_FRAGMENT);
		}
		input.setSearchLimit(PluginSearchInput.LIMIT_DECLARATIONS);
		input.setSearchScope(new PluginSearchScope());
		return new PluginSearchQuery(input);
	}

}
