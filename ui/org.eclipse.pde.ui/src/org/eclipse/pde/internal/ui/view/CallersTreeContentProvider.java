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
package org.eclipse.pde.internal.ui.view;

import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class CallersTreeContentProvider extends CallersContentProvider
		implements ITreeContentProvider {

	/**
	 * Constructor.
	 */
	public CallersTreeContentProvider(DependenciesView view) {
		super(view);
	}

	/**
	 * @see ITreeContentProvider#getChildren(Object) return Object[] of
	 *      IPluginBase
	 */
	public Object[] getChildren(Object parentElement) {
		String id = null;
		if (parentElement instanceof IPluginModelBase) {
			IPluginBase pluginBase = ((IPluginModelBase) parentElement)
					.getPluginBase(false);
			if (pluginBase != null)
				id = pluginBase.getId();
		} else if (parentElement instanceof IPlugin) {
			id = ((IPlugin) parentElement).getId();
		} else if (parentElement instanceof IPluginImport) {
			id = ((IPluginImport) parentElement).getId();
		}
		if (id == null) {
			return new Object[0];
		}
		Set l = findReferences(id);
		return l.toArray();
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 * @return Object[] with 0 or 1 IPluginBase
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
