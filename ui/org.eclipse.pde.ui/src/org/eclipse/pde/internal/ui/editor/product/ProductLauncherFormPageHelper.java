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
package org.eclipse.pde.internal.ui.editor.product;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ILauncherFormPageHelper;
import org.eclipse.pde.internal.ui.editor.PDELauncherFormEditor;
import org.eclipse.pde.internal.ui.wizards.product.SynchronizationOperation;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class ProductLauncherFormPageHelper implements ILauncherFormPageHelper {
	PDELauncherFormEditor fEditor;

	public ProductLauncherFormPageHelper(PDELauncherFormEditor editor) {
		fEditor = editor;
	}

	public Object getLaunchObject() {
		Object file = fEditor.getEditorInput().getAdapter(IFile.class);
		if (file != null)
			return file;
		return ((IProductModel) fEditor.getAggregateModel()).getUnderlyingResource();
	}

	public boolean isOSGi() {
		return false;
	}

	public void preLaunch() {
		handleSynchronize(false);
	}

	public void handleSynchronize(boolean alert) {
		try {
			IProgressService service = PlatformUI.getWorkbench().getProgressService();
			IProject project = fEditor.getCommonProject();
			SynchronizationOperation op = new SynchronizationOperation(getProduct(), fEditor.getSite().getShell(), project);
			service.runInUI(service, op, PDEPlugin.getWorkspace().getRoot());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			if (alert)
				MessageDialog.openError(fEditor.getSite().getShell(), "Synchronize", e.getTargetException().getMessage()); //$NON-NLS-1$
		}
	}

	public IProduct getProduct() {
		IBaseModel model = fEditor.getAggregateModel();
		return ((IProductModel) model).getProduct();
	}
}
