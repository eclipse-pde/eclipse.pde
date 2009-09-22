/*******************************************************************************
 * Copyright (c) 2009 eXXcellent solutions gmbh, EclipseSource Corporation
 * and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Achim Demelt, eXXcellent solutions gmbh - initial API and implementation
 *     EclipseSource - initial API and implementation, ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.launching.PDEMessages;
import org.eclipse.pde.internal.launching.launcher.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class PluginValidationStatusHandler implements IStatusHandler {
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
		display.syncExec(new Runnable() {
			public void run() {
				PluginStatusDialog dialog = new PluginStatusDialog(display.getActiveShell());
				dialog.showCancelButton(true);
				dialog.setInput(op.getInput());
				result[0] = dialog.open();
			}
		});
		if (result[0] == IDialogConstants.CANCEL_ID)
			throw new CoreException(Status.CANCEL_STATUS);
	}

}
