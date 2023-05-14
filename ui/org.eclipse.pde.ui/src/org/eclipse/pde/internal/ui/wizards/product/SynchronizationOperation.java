/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;

public class SynchronizationOperation extends ProductDefinitionOperation {

	public SynchronizationOperation(IProduct product, Shell shell, IProject project) {
		super(product, getPluginId(product), getProductId(product), product.getApplication(), shell, project);
	}

	private static String getProductId(IProduct product) {
		String full = product.getProductId();
		int index = full.lastIndexOf('.');
		return index != -1 ? full.substring(index + 1) : full;
	}

	private static String getPluginId(IProduct product) {
		String full = product.getProductId();
		int index = full.lastIndexOf('.');
		return index != -1 ? full.substring(0, index) : full;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IPluginModelBase model = PluginRegistry.findModel(fPluginId);
		if (model == null) {
			String message = PDEUIMessages.SynchronizationOperation_noDefiningPlugin;
			throw new InvocationTargetException(createCoreException(message));
		}

		if (model.getUnderlyingResource() == null) {
			String id = model.getPluginBase().getId();
			String message = PDEUIMessages.SynchronizationOperation_externalPlugin;
			throw new InvocationTargetException(createCoreException(NLS.bind(message, id)));
		}

		super.run(monitor);
	}

	private CoreException createCoreException(String message) {
		return new CoreException(Status.error(message));
	}

}
