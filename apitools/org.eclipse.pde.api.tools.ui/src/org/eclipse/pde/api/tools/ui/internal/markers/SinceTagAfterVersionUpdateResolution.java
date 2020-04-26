/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

public class SinceTagAfterVersionUpdateResolution extends SinceTagResolution {

	IMarker markerVersion = null;
	private int dialogResult = -1;
	public SinceTagAfterVersionUpdateResolution(IMarker markerVer ,IMarker marker) {
		super(marker);
		markerVersion = markerVer;

	}

	@Override
	public String getLabel() {
		return NLS.bind(MarkerMessages.SinceTagResolution_add_since_tag_after_version_update, markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null));
	}
	@Override
	public void run(final IMarker marker) {
		if (markerVersion != null) {
			// confirm major version update
			if (IApiProblem.MAJOR_VERSION_CHANGE == ApiProblemFactory
					.getProblemKind(markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0))) {
				final int[] result = new int[1];
				getDisplay().syncExec(() -> {
					String title = MarkerMessages.SinceTagAfterVersionUpdateResolution_confirm;
					MessageDialog dialog = new MessageDialog(getActiveShell(), title, null,
							MarkerMessages.SinceTagAfterVersionUpdateResolution_question, MessageDialog.QUESTION, 1,
							MarkerMessages.SinceTagAfterVersionUpdateResolution_update,
							MarkerMessages.SinceTagAfterVersionUpdateResolution_dont_update,
							IDialogConstants.CANCEL_LABEL);
					result[0] = dialog.open();
					dialogResult = Integer.valueOf(result[0]);

				});
			}
			if (dialogResult == 2) {
				return;// cancel
			}
			if (dialogResult == 1) {
				super.run(marker);
				return;// dont update major version but add since tag
			}

			new VersionNumberingResolution(markerVersion).run(markerVersion);
			String componentVersionString = markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null);
			StringBuilder buffer = new StringBuilder();
			Version componentVersion = new Version(componentVersionString);
			buffer.append(componentVersion.getMajor()).append('.').append(componentVersion.getMinor());
			try {
				markerVersion.setAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, buffer.toString());
			} catch (CoreException e) {
				ApiUIPlugin.log(e);
			}
			this.newVersionValue = markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null);
		}

		try {
			marker.setAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, this.newVersionValue);
		} catch (CoreException e) {
			ApiUIPlugin.log(e);
		}
		super.run(marker);
	}

	@Override
	public String getDescription() {
		return NLS.bind(MarkerMessages.SinceTagResolution_add_since_tag_after_version_update,
				markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null));
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {

		return new IMarker[0];
	}

	public Shell getActiveShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}

	private static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

}
