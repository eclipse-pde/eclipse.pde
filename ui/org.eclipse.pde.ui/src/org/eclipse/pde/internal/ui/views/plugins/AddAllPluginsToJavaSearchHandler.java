/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <zx@eclipsesource.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.commands.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.PlatformUI;

public class AddAllPluginsToJavaSearchHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IPluginModelBase[] models = PluginRegistry.getExternalModels();
		IRunnableWithProgress op = new JavaSearchOperation(models, true);
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}

}
