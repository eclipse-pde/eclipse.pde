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
package org.eclipse.pde.internal.ui.search;

import java.util.HashSet;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

/**
 * @author wassimm
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ReferencesInPluginAction extends Action {
	
	private static final String KEY_REFERENCES = "DependencyExtent.references"; //$NON-NLS-1$
	
	ISearchResultViewEntry entry;
	
	
	public ReferencesInPluginAction(ISearchResultViewEntry entry) {
		this.entry = entry;
		setText(PDEPlugin.getResourceString(KEY_REFERENCES) + " " + entry.getResource().getName()); //$NON-NLS-1$
	}
	
	public void run() {
		try {
			SearchUI.activateSearchResultView();
			IWorkspaceRunnable operation = null;
			Object object = entry.getGroupByKey();
			if (object instanceof IJavaElement) {
				operation = new JavaSearchOperation((IJavaElement)object, (IProject)entry.getResource());
			} else {
				operation =
					new PluginSearchUIOperation(
						getPluginSearchInput((IPluginExtensionPoint) object),
						new PluginSearchResultCollector());
			}
			PDEPlugin.getWorkspace().run(operation, null, IWorkspace.AVOID_UPDATE, new NullProgressMonitor());
		} catch (CoreException e) {
		}
	}
	
	private PluginSearchInput getPluginSearchInput(IPluginExtensionPoint object) {
		PluginSearchInput input = new PluginSearchInput();
		input.setSearchElement(PluginSearchInput.ELEMENT_EXTENSION_POINT);
		input.setSearchString(
			((IPluginExtensionPoint) object).getPluginBase().getId()
				+ "." //$NON-NLS-1$
				+ ((IPluginExtensionPoint) object).getId());
		input.setSearchLimit(PluginSearchInput.LIMIT_REFERENCES);
		HashSet set = new HashSet();
		IResource resource = ((IProject)entry.getResource()).getFile("plugin.xml"); //$NON-NLS-1$
		if (!resource.exists())
			resource = ((IProject)entry.getResource()).getFile("fragment.xml"); //$NON-NLS-1$
			
		set.add(resource);
		input.setSearchScope(
			new PluginSearchScope(
				PluginSearchScope.SCOPE_SELECTION,
				PluginSearchScope.EXTERNAL_SCOPE_NONE,
				set));
		return input;
	}


}
