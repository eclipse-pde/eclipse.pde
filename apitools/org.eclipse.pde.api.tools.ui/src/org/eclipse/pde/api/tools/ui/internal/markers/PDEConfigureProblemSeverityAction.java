/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.Map;

import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IJavaProject;
//import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
//import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiErrorsWarningsConfigurationBlock;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiErrorsWarningsConfigurationBlock.Key;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiErrorsWarningsPreferencePage;
import org.eclipse.pde.internal.ui.correction.OptionalMessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.ibm.icu.text.MessageFormat;

public class PDEConfigureProblemSeverityAction extends Action {
	private static final String CONFIGURE_PROBLEM_SEVERITY_DIALOG_ID = "configure_problem_severity_dialog_id"; //$NON-NLS-1$

	private final IJavaProject fProject;

	private final String fOptionId;

	private int tab;


	public PDEConfigureProblemSeverityAction(IJavaProject project, String optionId, int t) {
		super();
		fProject = project;
		fOptionId = optionId;
		tab = t;
	}

	@SuppressWarnings("restriction")
	@Override
	public void run() {
		boolean showPropertyPage;

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		if (!hasProjectSpecificOptions()) {

			String message = MessageFormat.format(
					MarkerMessages.PDEConfigureProblemSeverityAction_0,
					new Object[] { JavaElementLabels.getElementLabel(fProject, JavaElementLabels.ALL_DEFAULT) });

			String[] buttons = new String[] { MarkerMessages.PDEConfigureProblemSeverityAction_1,
					MarkerMessages.PDEConfigureProblemSeverityAction_2,
					IDialogConstants.CANCEL_LABEL };

			// use PDE UI's OptionalMessageDialog
			int result = OptionalMessageDialog.open(CONFIGURE_PROBLEM_SEVERITY_DIALOG_ID, shell,
					MarkerMessages.PDEConfigureProblemSeverityAction_3, null, message,
					MessageDialog.QUESTION, buttons, 0,
					MarkerMessages.PDEConfigureProblemSeverityAction_4);



			if (result == OptionalMessageDialog.NOT_SHOWN) {
				showPropertyPage = false;
			} else if (result == 2 || result == SWT.DEFAULT) {
				return;
			} else if (result == 0) {
				showPropertyPage = true;
			} else {
				showPropertyPage = false;
			}
		} else {
			showPropertyPage = true;
		}

		Map<String, Object> data = new HashMap<>();
		String pageId;

		if (showPropertyPage) {
			pageId = IApiToolsConstants.ID_ERRORS_WARNINGS_PROP_PAGE;
			 data.put(ApiErrorsWarningsPreferencePage.USE_PROJECT_SPECIFIC_OPTIONS,
					Boolean.TRUE);
		} else {
			pageId = ApiErrorsWarningsPreferencePage.ID;
		}
		data.put(ApiErrorsWarningsPreferencePage.DATA_SELECT_OPTION_KEY, fOptionId);
		data.put(ApiErrorsWarningsPreferencePage.DATA_SELECT_OPTION_QUALIFIER, ApiPlugin.PLUGIN_ID);
		data.put(ApiErrorsWarningsPreferencePage.INITIAL_TAB, tab);

		if (showPropertyPage) {
			PreferencesUtil.createPropertyDialogOn(shell, fProject, pageId, null, data).open();
		} else {
			PreferencesUtil.createPreferenceDialogOn(shell,
					pageId, null, data).open();
		}
	}

	private boolean hasProjectSpecificOptions() {
		Key[] keys = ApiErrorsWarningsConfigurationBlock
				.getAllKeys();
		if (fProject.getProject() != null) {
			IScopeContext projectContext = new ProjectScope(fProject.getProject());
			for (int i = 0; i < keys.length; i++) {
				if (keys[i].getStoredValue(projectContext, null) != null) {
					return true;
				}
			}
		}
		return false;
	}

}
