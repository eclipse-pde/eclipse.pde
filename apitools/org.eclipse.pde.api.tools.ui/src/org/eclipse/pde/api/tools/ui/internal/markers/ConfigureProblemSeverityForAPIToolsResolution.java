/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiBaselinePreferencePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolutionRelevance;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import com.ibm.icu.text.MessageFormat;

public class ConfigureProblemSeverityForAPIToolsResolution extends WorkbenchMarkerResolution
		implements IJavaCompletionProposal, IMarkerResolutionRelevance {
	protected IMarker fBackingMarker = null;
	protected String fCategory = null;

	/**
	 * Constructor
	 *
	 * @param marker the backing marker for the resolution
	 */
	public ConfigureProblemSeverityForAPIToolsResolution(IMarker marker) {
		fBackingMarker = marker;
	}

	protected String resolveCategoryName() {
		if (fCategory == null) {
			int problemid = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, -1);
			int category = ApiProblemFactory.getProblemCategory(problemid);
			switch (category) {
			case IApiProblem.CATEGORY_COMPATIBILITY: {
				fCategory = MarkerMessages.FilterProblemResolution_compatible;
				break;
			}
			case IApiProblem.CATEGORY_API_BASELINE: {
				fCategory = MarkerMessages.FilterProblemResolution_default_profile;
				break;
			}
			case IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION: {
				fCategory = MarkerMessages.FilterProblemResolution_api_component;
				break;
			}
			case IApiProblem.CATEGORY_SINCETAGS: {
				fCategory = MarkerMessages.FilterProblemResolution_since_tag;
				break;
			}
			case IApiProblem.CATEGORY_USAGE: {
				fCategory = MarkerMessages.FilterProblemResolution_usage;
				break;
			}
			case IApiProblem.CATEGORY_VERSION: {
				fCategory = MarkerMessages.FilterProblemResolution_version_number;
				break;
			}
			default:
				break;
			}
		}
		return fCategory;
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
			return MessageFormat.format(MarkerMessages.ConfigureProblemSeverity_desc,
					ApiProblemFactory.getLocalizedMessage(ApiProblemFactory.getProblemMessageId(id), args),
					resolveCategoryName());

		} catch (CoreException e) {
		}
		return null;
	}

	@Override
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_CONFIG_SEV);
	}


	@Override
	public String getLabel() {
		return MessageFormat.format(MarkerMessages.ConfigureProblemSeverity_label,
				resolveCategoryName());
	}

	@Override
	public int getRelevance() {
		return IApiToolProposalRelevance.CONFIGURE_PROBLEM_SEVERITY;
	}

	@Override
	public void run(IMarker[] markers, IProgressMonitor monitor) {

		UIJob job = new UIJob("") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				// Configure problem severity for missing baseline
				// This doesn't have project specific option
				if (fBackingMarker.getAttribute(IApiMarkerConstants.API_MARKER_ATTR_ID,
						-1) == IApiMarkerConstants.DEFAULT_API_BASELINE_MARKER_ID) {
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					Map<String, Object> data = new HashMap<>();
					data.put(ApiBaselinePreferencePage.DATA_SELECT_OPTION_KEY,
							ApiBaselinePreferencePage.MISSING_BASELINE_OPTION);
					PreferencesUtil
							.createPreferenceDialogOn(shell, IApiToolsConstants.ID_BASELINES_PREF_PAGE, null, data)
							.open();

					return Status.OK_STATUS;
				}
				// Configure problem severity for API Error/Warning( Usage Error, API compatibility error, API
				// version error, since tag error, analysis option etc )
				IJavaProject project = JavaCore.create(fBackingMarker.getResource().getProject());
				int id = ApiProblemFactory.getProblemId(fBackingMarker);
				int tab = -1;
				String key = null;
				key = Util.getAPIToolPreferenceKey(id);
				tab = Util.getAPIToolPreferenceTab(id);
				PDEConfigureProblemSeverityAction problemSeverityAction = new PDEConfigureProblemSeverityAction(
						project, key ,
						tab);
				problemSeverityAction.run();
				return Status.OK_STATUS;
			}

		};
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
		int id = ApiProblemFactory.getProblemId(fBackingMarker);
		for (int i = 0; i < markers.length; i++) {
			try {
				if (Util.isApiProblemMarker(markers[i]) && !fBackingMarker.equals(markers[i])
						&& !markers[i].getType().equals(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER)) {
					if (ApiProblemFactory.getProblemId(markers[i]) == id) {
						mset.add(markers[i]);
					}
				}
			} catch (CoreException ce) {
				// do nothing just don't add the filter
			}
		}
		int size = mset.size();
		return mset.toArray(new IMarker[size]);
	}

	@Override
	public void apply(IDocument document) {
	}


	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	@Override
	public String getDisplayString() {
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public int getRelevanceForResolution() {
		return IApiToolProposalRelevance.CONFIGURE_PROBLEM_SEVERITY;
	}
}
