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
package org.eclipse.pde.internal.ui.view;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;

public class CalleesListContentProvider extends
		DependenciesViewPageContentProvider implements
		IStructuredContentProvider {

	public CalleesListContentProvider(DependenciesView view) {
		super(view);
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		// input either IPluginModelBase or ImportObject
		if (inputElement instanceof IPluginModelBase) {
			IPluginBase pluginBase = ((IPluginModelBase) inputElement)
					.getPluginBase();
			Map elements = new Hashtable();
			Set candidates = new HashSet();
			candidates.addAll(Arrays.asList(pluginBase.getImports()));

			while (!candidates.isEmpty()) {
				Set newCandidates = new HashSet();
				for (Iterator it = candidates.iterator(); it.hasNext();) {
					IPluginImport pluginImport = (IPluginImport) it.next();
					String id = pluginImport.getId();
					IPlugin importedPlugin = PDECore.getDefault()
							.findPlugin(id);
					it.remove();
					if (!elements.containsKey(id)) {
						elements.put(id, pluginImport);
						if (importedPlugin != null) {
							newCandidates.addAll(Arrays.asList(importedPlugin
									.getImports()));
						}
					}
				}
				candidates = newCandidates;

			}
			return elements.values().toArray();
		}
		return new Object[0];
	}
}
