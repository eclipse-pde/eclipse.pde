/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - bug 201342
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;

public class JavaSearchOperation implements IRunnableWithProgress {

	private IPluginModelBase[] fModels;
	private boolean fAdd;

	public JavaSearchOperation(IPluginModelBase[] models, boolean add) {
		fModels = models;
		fAdd = add;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException {
		try {
			SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
			if (fAdd)
				manager.addToJavaSearch(fModels);
			else
				manager.removeFromJavaSearch(fModels);
		} finally {
			monitor.done();
		}
	}

}
