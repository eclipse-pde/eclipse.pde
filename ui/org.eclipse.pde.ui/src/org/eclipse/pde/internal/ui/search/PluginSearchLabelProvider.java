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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.swt.graphics.Image;


public class PluginSearchLabelProvider extends LabelProvider {
	
	public PluginSearchLabelProvider() {
		// Increment reference count for the global label provider
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	public void dispose() {
		// Allow global label provider to release shared images, if needed.
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
	
	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry = (ISearchResultViewEntry)element;
			return PDEPlugin.getDefault().getLabelProvider().getImage((IPluginObject)entry.getGroupByKey());
		}
		return super.getImage(element);
	}

	
	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry = (ISearchResultViewEntry) element;

			IPluginObject object = (IPluginObject)entry.getGroupByKey();
			
			if (object instanceof IPluginBase) {
				return ((IPluginBase)object).getId();
			}
			
			if (object instanceof IPluginImport) {
				return ((IPluginImport)object).getId() 
					+ " - "
					+ object.getPluginModel().getPluginBase().getId();
			} 
			
			if (object instanceof IPluginExtension) {
				return ((IPluginExtension)object).getPoint()
					 + " - "
					+ object.getPluginModel().getPluginBase().getId();
			}
			
			if (object instanceof IPluginExtensionPoint) {
				return ((IPluginExtensionPoint)object).getFullId();
			}
		}
		
		return super.getText(element);
	}
	

}
