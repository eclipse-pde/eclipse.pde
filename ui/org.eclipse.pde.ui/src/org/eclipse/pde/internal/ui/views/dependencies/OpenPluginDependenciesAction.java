/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;

public class OpenPluginDependenciesAction extends Action {

	private IPluginModelBase fModel = null;

	public OpenPluginDependenciesAction(IPluginModelBase base) {
		fModel = base;
	}

	public void run() {
		try {
			IViewPart view = PDEPlugin.getActivePage().showView(IPDEUIConstants.DEPENDENCIES_VIEW_ID);
			((DependenciesView) view).openCalleesFor(fModel);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

}
