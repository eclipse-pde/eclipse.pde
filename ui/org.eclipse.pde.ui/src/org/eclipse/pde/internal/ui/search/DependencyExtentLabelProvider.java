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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.swt.graphics.Image;


public class DependencyExtentLabelProvider extends JavaElementLabelProvider {

	public Image getImage(Object element) {
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry = (ISearchResultViewEntry) element;
			element = entry.getGroupByKey();
		}
		if (element instanceof IPluginExtensionPoint)
			return PDEPlugin.getDefault().getLabelProvider().getImage(
				(IPluginExtensionPoint) element);

		return super.getImage(element);
	}

	public String getText(Object element) {
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry = (ISearchResultViewEntry) element;
			element = entry.getGroupByKey();
		}
		if (element instanceof IPluginExtensionPoint) {
			return ((IPluginExtensionPoint) element)
				.getPluginModel()
				.getPluginBase()
				.getId()
				+ "."
				+ ((IPluginExtensionPoint) element).getId();
		} else if (element instanceof IJavaElement) {
			IJavaElement javaElement = (IJavaElement) element;
			String text =
				super.getText(javaElement)
					+ " - "
					+ javaElement
						.getAncestor(IJavaElement.PACKAGE_FRAGMENT)
						.getElementName();
			if (!(javaElement instanceof IType)) {
				IJavaElement ancestor = javaElement.getAncestor(IJavaElement.TYPE);
				if (ancestor == null)
					ancestor = javaElement.getAncestor(IJavaElement.CLASS_FILE);
				if (ancestor == null)
					ancestor = javaElement.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (ancestor != null)
					text += "." + ancestor.getElementName();
			}
			return text;
		}
		return super.getText(element);
	}
}