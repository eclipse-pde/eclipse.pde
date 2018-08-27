/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.progress.UIJob;

/**
 * This resolution helps users to pick a default API profile when the tooling
 * has been set up but there is no default profile
 *
 * @since 1.0.0
 */
public class DefaultApiProfileResolution implements IMarkerResolution2 {

	@Override
	public String getDescription() {
		return MarkerMessages.DefaultApiProfileResolution_0;
	}

	@Override
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_ECLIPSE_PROFILE);
	}

	@Override
	public String getLabel() {
		return MarkerMessages.DefaultApiProfileResolution_1;
	}

	@Override
	public void run(IMarker marker) {
		UIJob job = new UIJob(MarkerMessages.DefaultApiProfileResolution_2) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				SWTFactory.showPreferencePage(ApiUIPlugin.getShell(), IApiToolsConstants.ID_BASELINES_PREF_PAGE, null);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}
}
