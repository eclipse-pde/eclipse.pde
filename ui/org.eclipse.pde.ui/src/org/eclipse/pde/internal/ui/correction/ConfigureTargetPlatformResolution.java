/*******************************************************************************
 *  Copyright (c) 2015, 2017 Red Hat Inc.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *     Christian Dietrich (itemis AG) - show target platform preference page correctly
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferencePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

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
		String pageId = TargetPlatformPreferencePage.PAGE_ID;
		PreferencesUtil.createPreferenceDialogOn(shell, pageId, null, null).open();
	}

}
