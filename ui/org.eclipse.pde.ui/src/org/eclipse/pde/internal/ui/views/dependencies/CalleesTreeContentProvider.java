/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class CalleesTreeContentProvider extends CalleesContentProvider
		implements ITreeContentProvider {

	/**
	 * Constructor.
	 */
	public CalleesTreeContentProvider(DependenciesView view) {
		super(view);
	}

	/**
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IPluginBase) {
			IPluginBase plugin = (IPluginBase) parentElement;
			return findCallees(plugin);
		} else if (parentElement instanceof IPluginImport) {
			IPluginImport pluginImport = (IPluginImport) parentElement;
			String id = pluginImport.getId();
			IPluginModelBase importedPlugin = PluginRegistry.findModel(id);
			if (importedPlugin != null)
				return findCallees(importedPlugin.getPluginBase());

		}
		return new Object[0];
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 * @return Object[] of IPluginBase
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IPluginModelBase) {
			return new Object[] { ((IPluginModelBase) inputElement)
					.getPluginBase() };
		}
		return new Object[0];
	}

	/**
	 * @see ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		return null;
	}

	/**
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

}
