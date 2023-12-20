/*******************************************************************************
 * Copyright (c) 2009, 2015 eXXcellent solutions gmbh, EclipseSource Corporation
 * and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Achim Demelt, eXXcellent solutions gmbh - initial API and implementation
 *     EclipseSource - initial API and implementation, ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.launching.PDEMessages;
import org.eclipse.pde.internal.launching.launcher.EclipsePluginValidationOperation;
import org.eclipse.pde.internal.launching.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class PluginValidationStatusHandler implements IStatusHandler {
	@Override
	public Object handleStatus(IStatus status, Object source) throws CoreException {
		if (status.getCode() == EclipsePluginValidationOperation.CREATE_EXTENSION_ERROR_CODE)
			return createExtensionError((String) source);
		else if (status.getCode() == LaunchPluginValidator.DISPLAY_VALIDATION_ERROR_CODE)
			displayValidationError((LaunchValidationOperation) source);
		return null;
	}

	private Object createExtensionError(String bundleID) {
		String name = NLS.bind(PDEMessages.EclipsePluginValidationOperation_pluginMissing, bundleID);
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		Image image = provider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
		return new NamedElement(name, image);
	}

	private void displayValidationError(final LaunchValidationOperation op) throws CoreException {
		final int[] result = new int[1];
		final Display display = LauncherUtilsStatusHandler.getDisplay();
		display.syncExec(() -> {
			IShellProvider shellProvider = PlatformUI.getWorkbench().getModalDialogShellProvider();
			PluginStatusDialog dialog = new PluginStatusDialog(shellProvider.getShell());
			dialog.showLink(true);
			dialog.showCancelButton(true);
			dialog.setInput(op.getInput());
			result[0] = dialog.open();
		});
		if (result[0] == IDialogConstants.CANCEL_ID)
			throw new CoreException(Status.CANCEL_STATUS);
	}

}
