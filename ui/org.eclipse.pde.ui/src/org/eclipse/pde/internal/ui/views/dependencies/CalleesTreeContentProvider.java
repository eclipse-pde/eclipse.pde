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
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class CalleesTreeContentProvider extends CalleesContentProvider implements ITreeContentProvider {

	/**
	 * Constructor.
	 */
	public CalleesTreeContentProvider(DependenciesView view) {
		super(view);
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IPluginBase) {
			parentElement = ((IPluginBase) parentElement).getModel();
		}
		if (parentElement instanceof IPluginModelBase) {
			return findCallees(((IPluginModelBase) parentElement));
		}
		if (parentElement instanceof BundleSpecification) {
			parentElement = ((BundleSpecification) parentElement).getSupplier();
		}
		if (parentElement instanceof ImportPackageSpecification) {
			parentElement = ((ExportPackageDescription) (((ImportPackageSpecification) parentElement).getSupplier())).getExporter();
		}
		if (parentElement instanceof BundleDescription) {
			return findCallees((BundleDescription) parentElement);
		}
		return new Object[0];
	}

	/**
	 * @see IStructuredContentProvider#getElements(Object)
	 * @return Object[] of IPluginBase
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IPluginModelBase) {
			// need to use PluginBase.  If we use BundleDescription, whenever the Manifest is update the tree refreshes and collapses
			// If we use IPluginModelBase, it confuses the Tree since we return the same object as our input
			return new Object[] {((IPluginModelBase) inputElement).getPluginBase()};
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
