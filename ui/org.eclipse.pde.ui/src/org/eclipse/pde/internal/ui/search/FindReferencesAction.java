/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.SearchUI;


public class FindReferencesAction extends Action {
	
	private static final String KEY_REFERENCES = "SearchAction.references";
	private Object object;
	
	public FindReferencesAction(Object object) {
		this.object = object;
		setText(PDEPlugin.getResourceString(KEY_REFERENCES));
	}
	
	public void run() {
		PluginSearchInput input = new PluginSearchInput();
		if (object instanceof IPlugin) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IPlugin) object).getId());
		} else if (object instanceof IPluginExtensionPoint) {
			input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
			input
				.setSearchString(
					((IPluginExtensionPoint) object)
						.getPluginModel()
						.getPluginBase()
						.getId()
						+ "."
						+ ((IPluginExtensionPoint) object).getId());
		} else if (object instanceof IPluginImport) {
			input.setSearchElement(PluginSearchInput.ELEMENT_PLUGIN);
			input.setSearchString(((IPluginImport) object).getId());
		}
		input.setSearchLimit(PluginSearchInput.LIMIT_REFERENCES);
		input.setSearchScope(new PluginSearchScope());
		try {
			SearchUI.activateSearchResultView();
			ProgressMonitorDialog pmd =
				new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			PluginSearchUIOperation op =
				new PluginSearchUIOperation(
					input,
					new PluginSearchResultCollector());
			pmd.run(true, true, op);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}


}
