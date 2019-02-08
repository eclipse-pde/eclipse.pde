/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.correction;

import com.ibm.icu.text.MessageFormat;
import java.util.*;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.preferences.CompilersPreferencePage;
import org.eclipse.pde.internal.ui.preferences.PDECompilersConfigurationBlock;
import org.eclipse.pde.internal.ui.preferences.PDECompilersConfigurationBlock.Key;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolutionRelevance;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class ConfigureProblemSeverityForPDECompilerResolution extends AbstractManifestMarkerResolution
		implements IJavaCompletionProposal, IMarkerResolutionRelevance {
	private static final String CONFIGURE_PROBLEM_SEVERITY_DIALOG_ID = "configure_problem_severity_dialog_id_compiler"; //$NON-NLS-1$
	String id = ""; //$NON-NLS-1$

	public ConfigureProblemSeverityForPDECompilerResolution(IMarker mker, int type, String key) {
		super(type, mker);
		id = key;
	}


	@Override
	public String getDescription() {
		return NLS.bind(PDEUIMessages.ConfigureProblemSeverityForPDECompiler_6,
				marker.getAttribute(IMarker.MESSAGE, (String) null));
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.ConfigureProblemSeverityForPDECompiler_0;
	}

	@Override
	public void run(IMarker marker) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		boolean isManifestCompilerOption = isManifestCompilerOption();

		Map<String, Object> data = new HashMap<>();
		data.put(CompilersPreferencePage.DATA_SELECT_OPTION_KEY, id);
		data.put(CompilersPreferencePage.DATA_SELECT_OPTION_QUALIFIER, PDE.PLUGIN_ID);

		// If other than manifest compiler option, then show workspace preference
		// since there is no project specific settings.
		if (!isManifestCompilerOption) {
			PreferencesUtil
					.createPreferenceDialogOn(shell, CompilersPreferencePage.PDE_COMPILER_PREFERENCE_ID, null, data)
					.open();
			return;
		}

		// only manifest compiler option here
		IJavaProject project = JavaCore.create(marker.getResource().getProject());
		boolean hasProjectOptions = hasProjectSpecificOptions();
		boolean showPropertyPage = false;
		if (!hasProjectOptions) {
			String message = MessageFormat.format(
					PDEUIMessages.ConfigureProblemSeverityForPDECompiler_1,
					new Object[] { JavaElementLabels.getElementLabel(project, JavaElementLabels.ALL_DEFAULT) });

			String[] buttons = new String[] { PDEUIMessages.ConfigureProblemSeverityForPDECompiler_2, PDEUIMessages.ConfigureProblemSeverityForPDECompiler_3,
					IDialogConstants.CANCEL_LABEL };
			int result = OptionalMessageDialog.open(CONFIGURE_PROBLEM_SEVERITY_DIALOG_ID, shell,
					PDEUIMessages.ConfigureProblemSeverityForPDECompiler_4, null, message, MessageDialog.QUESTION, buttons, 0,
					PDEUIMessages.ConfigureProblemSeverityForPDECompiler_5);

			if (result == OptionalMessageDialog.NOT_SHOWN) {
				showPropertyPage = false;
			} else if (result == 2 || result == SWT.DEFAULT) {
				return;
			} else if (result == 0) {
				showPropertyPage = true;
			} else {
				showPropertyPage = false;
			}
		}
		else {
			showPropertyPage = true;
		}

		String pageId;
		if (showPropertyPage) {
			pageId = CompilersPreferencePage.PDE_COMPILER_PROPERTY_ID;
			data.put(CompilersPreferencePage.USE_PROJECT_SPECIFIC_OPTIONS, Boolean.TRUE);
		} else {
			pageId = CompilersPreferencePage.PDE_COMPILER_PREFERENCE_ID;
		}

		if (showPropertyPage) {
			PreferencesUtil.createPropertyDialogOn(shell, project, pageId, null, data).open();
		} else {
			PreferencesUtil.createPreferenceDialogOn(shell, pageId, null, data).open();
		}
	}

	private boolean isManifestCompilerOption() {
		String str = marker.getAttribute(PDEMarkerFactory.compilerKey, ""); //$NON-NLS-1$
		if (str.length() > 0) {
			if (str.equals(CompilerFlags.S_OPEN_TAGS) || str.equals(CompilerFlags.F_UNRESOLVED_FEATURES)
					|| str.equals(CompilerFlags.F_UNRESOLVED_PLUGINS))
				return false;
		}
		return true;
	}

	private boolean hasProjectSpecificOptions() {
		IJavaProject project = JavaCore.create(marker.getResource().getProject());
		Key[] keys = PDECompilersConfigurationBlock.getAllKeys();
		if (project != null) {
			IScopeContext projectContext = new ProjectScope(project.getProject());
			for (int i = 0; i < keys.length; i++) {
				if (keys[i].getStoredValue(projectContext, null) != null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		HashSet<IMarker> mset = new HashSet<>(markers.length);
		for (IMarker iMarker : markers) {
			if (iMarker.equals(marker))
				continue;
			String str = iMarker.getAttribute(PDEMarkerFactory.compilerKey, ""); //$NON-NLS-1$
			if (str.equals(id))
				mset.add(iMarker);
		}
		int size = mset.size();
		return mset.toArray(new IMarker[size]);
	}

	@Override
	protected void createChange(BundleModel model) {
		// TODO Auto-generated method stub
	}

	@Override
	public void apply(IDocument document) {
		// TODO Auto-generated method stub

	}

	@Override
	public Point getSelection(IDocument document) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDisplayString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRelevance() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int getRelevanceForResolution() {
		return -1;
	}

}
