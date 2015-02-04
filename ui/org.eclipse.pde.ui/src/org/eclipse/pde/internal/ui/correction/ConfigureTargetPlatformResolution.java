/*******************************************************************************
 *  Copyright (c) 2015 Red Hat Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferencePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.PlatformUI;

public class ConfigureTargetPlatformResolution implements IMarkerResolution2 {


	@Override
	public String getLabel() {
		return PDEUIMessages.ConfigureTargetPlatformResolution_label;
	}

	@Override
	public String getDescription() {
		return PDEUIMessages.ConfigureTargetPlatformResolution_description;
	}

	@Override
	public Image getImage() {
		return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_DEFINITION);
	}

	@Override
	public void run(IMarker marker) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		PreferenceDialog preferenceDialog = new PreferenceDialog(shell, PlatformUI.getWorkbench().getPreferenceManager());
		preferenceDialog.setSelectedNode(TargetPlatformPreferencePage.PAGE_ID);
		preferenceDialog.open();
	}

}
