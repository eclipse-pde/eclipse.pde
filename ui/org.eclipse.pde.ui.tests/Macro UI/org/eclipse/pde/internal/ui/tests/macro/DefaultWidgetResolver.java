/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPluginContribution;

public class DefaultWidgetResolver implements IWidgetResolver {
	public String getUniqueId(Widget widget) {
		Object data = widget.getData();

		// direct resolution (widget-independent)
		if (data instanceof IPluginContribution)
			return ((IPluginContribution) data).getLocalId();

		// widget-specific resolution
		if (widget instanceof TreeItem || widget instanceof TableItem) {
			if (data instanceof IJavaElement)
				return ((IJavaElement) data).getPath().toString();
			if (data instanceof IResource)
				return ((IResource) data).getFullPath().toString();
			if (data instanceof IClasspathContainer)
				return ((IClasspathContainer) data).getPath().toString();
			if (data instanceof IPluginModelBase)
				return ((IPluginModelBase)data).getPluginBase().getId();
			if (data instanceof IFeatureModel)
				return ((IFeatureModel)data).getFeature().getId();
			if (data instanceof IIdentifiable)
				return ((IIdentifiable)data).getId();
		}
		if (widget instanceof Button) {
			if (data instanceof Integer)
				return "ButtonId=" + ((Integer) data).intValue();
		}
		if (widget instanceof TabFolder || widget instanceof CTabFolder) {
		}
		return null;
	}
}