/*******************************************************************************
 * Copyright (c)  2017, 2018 IBM Corporation and others.
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

import java.text.MessageFormat;
import java.util.HashSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

/**
 * Marker resolution for explaining a specific API tool error as quickfix
 * resolution
 */
public class ProblemExplainIncompatibilityResolution extends WorkbenchMarkerResolution {

	protected IMarker fBackingMarker = null;


	/**
	 * Constructor
	 *
	 * @param marker the backing marker for the resolution
	 */
	public ProblemExplainIncompatibilityResolution(IMarker marker) {
		fBackingMarker = marker;
	}

	@Override
	public String getDescription() {
		try {
			String value = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
			String[] args = new String[0];
			if (value != null) {
				args = value.split("#"); //$NON-NLS-1$
			}
			int id = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0);
			return MessageFormat.format(MarkerMessages.ExplainProblemResolution_explain_incompatibility_desc,
					ApiProblemFactory.getLocalizedMessage(ApiProblemFactory.getProblemMessageId(id), args));
		} catch (CoreException e) {
		}
		return null;
	}

	@Override
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_HELP_PAGE);
	}

	@Override
	public String getLabel() {
		return MarkerMessages.ExplainProblemResolution_explain_incompatibility;
	}

	@Override
	public void run(IMarker[] markers, IProgressMonitor m) {
		// Since only 1 page is made as of now , so for all explain
		// incompatibilities we can show the same page. However in future if the
		// pages are split, from marker we can get the type of incompatibility
		// and show different page URL.
		UIJob job = UIJob.create("", monitor -> { //$NON-NLS-1$
			IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
			helpSystem.displayHelpResource("/org.eclipse.pde.doc.user/reference/api-tooling/api_evolution.htm"); //$NON-NLS-1$
		});
		job.setSystem(true);
		job.schedule();
	}

	@Override
	public void run(IMarker marker) {
		run(new IMarker[] { marker }, null);
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		HashSet<IMarker> mset = new HashSet<>(markers.length);
		for (IMarker marker : markers) {
			try {
				if (Util.isApiProblemMarker(marker) && !fBackingMarker.equals(marker)
						&& !marker.getType().equals(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER)) {
					if (ApiMarkerResolutionGenerator.hasExplainProblemResolution(marker)) {
						mset.add(marker);
					}
				}
			} catch (CoreException ce) {
				// do nothing just don't add the filter
			}
		}
		int size = mset.size();
		return mset.toArray(new IMarker[size]);
	}
}
