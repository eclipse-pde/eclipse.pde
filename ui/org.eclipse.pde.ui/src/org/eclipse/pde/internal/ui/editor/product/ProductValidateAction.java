/*******************************************************************************
 * Copyright (c) 2009, 2022 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.Set;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.launching.launcher.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.LaunchAction;
import org.eclipse.swt.SWT;

public class ProductValidateAction extends Action {

	IProduct fProduct;

	public ProductValidateAction(IProduct product) {
		super(PDEUIMessages.ProductValidateAction_validate, IAction.AS_PUSH_BUTTON);
		setImageDescriptor(PDEPluginImages.DESC_VALIDATE_TOOL);
		fProduct = product;
	}

	@Override
	public void run() {
		Set<IPluginModelBase> launchPlugins = LaunchAction.getModels(fProduct);
		try {
			LaunchValidationOperation operation = new ProductValidationOperation(launchPlugins);
			LaunchPluginValidator.runValidationOperation(operation, new NullProgressMonitor());
			if (!operation.hasErrors()) {
				MessageDialog.open(SWT.ICON_INFORMATION, PDEPlugin.getActiveWorkbenchShell(),
						PDEUIMessages.PluginStatusDialog_pluginValidation,
						PDEUIMessages.AbstractLauncherToolbar_noProblems, 0, IDialogConstants.CLOSE_LABEL);
			}
		} catch (CoreException e) {
			if (e.getStatus().getSeverity() == IStatus.CANCEL) {
				return;
			}
			PDEPlugin.logException(e);
		}
	}
}
