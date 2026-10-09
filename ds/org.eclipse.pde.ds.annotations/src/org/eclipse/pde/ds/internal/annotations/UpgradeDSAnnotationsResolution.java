/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

@SuppressWarnings("restriction")
public class UpgradeDSAnnotationsResolution implements IMarkerResolution2 {

	@Override
	public String getLabel() {
		return Messages.UpgradeDSAnnotationsResolution_label;
	}

	@Override
	public String getDescription() {
		return Messages.UpgradeDSAnnotationsResolution_description;
	}

	@Override
	public Image getImage() {
		return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_ADD_ATT);
	}

	@Override
	public void run(IMarker marker) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null
				? PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
				: PDEPlugin.getActiveWorkbenchShell();
		if (shell == null) {
			return;
		}

		IProject project = marker.getResource().getProject();
		String pageId = Activator.PLUGIN_ID;
		if (project != null && project.isAccessible()) {
			PreferencesUtil.createPropertyDialogOn(shell, project, pageId, new String[] { pageId }, null).open();
		} else {
			PreferencesUtil.createPreferenceDialogOn(shell, pageId, new String[] { pageId }, null).open();
		}
	}
}
