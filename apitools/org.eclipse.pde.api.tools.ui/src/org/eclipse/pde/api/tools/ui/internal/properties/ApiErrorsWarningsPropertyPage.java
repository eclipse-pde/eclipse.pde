/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.properties;

import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiErrorsWarningsConfigurationBlock;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiErrorsWarningsPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * Provides a property page for projects to allow project specific API Tools
 * settings to be applied;
 *
 * @since 1.0.0
 */
public class ApiErrorsWarningsPropertyPage extends PropertyPage {

	/**
	 * The data map passed when showing the page
	 */
	private HashMap<String, Object> fPageData = null;

	ApiErrorsWarningsConfigurationBlock block = null;
	Button pspecific = null;
	Link link = null;

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		Composite tcomp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
		pspecific = SWTFactory.createCheckButton(tcomp, PropertiesMessages.ApiErrorWarningsPropertyPage_0, null, false, 1);
		GridData gd = (GridData) pspecific.getLayoutData();
		gd.horizontalAlignment = GridData.BEGINNING;
		gd.verticalAlignment = GridData.CENTER;
		pspecific.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			boolean psp = pspecific.getSelection();
			block.useProjectSpecificSettings(psp);
			if (link != null) {
				link.setEnabled(!psp);
			}
		}));

		if (offerLink()) {
			link = new Link(tcomp, SWT.NONE);
			link.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));
			link.setFont(comp.getFont());
			link.setText(PropertiesMessages.ApiErrorWarningsPropertyPage_1);
			link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				HashMap<String, Object> data = new HashMap<>();
				data.put(ApiErrorsWarningsPreferencePage.NO_LINK, Boolean.TRUE);
				SWTFactory.showPreferencePage(getShell(), IApiToolsConstants.ID_ERRORS_WARNINGS_PREF_PAGE, data);
			}));
		}
		// collect project
		block = new ApiErrorsWarningsConfigurationBlock(getProject(), (IWorkbenchPreferenceContainer) getContainer());
		block.createControl(comp);

		boolean ps = block.hasProjectSpecificSettings(getProject());
		pspecific.setSelection(ps);
		block.useProjectSpecificSettings(ps);
		if (link != null) {
			link.setEnabled(!ps);
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APITOOLS_ERROR_WARNING_PROP_PAGE);
		return comp;
	}

	/**
	 * @return true if the link should be shown, false otherwise
	 */
	private boolean offerLink() {
		return fPageData == null || !Boolean.TRUE.equals(fPageData.get(ApiErrorsWarningsPreferencePage.NO_LINK));
	}

	/**
	 * @return the backing {@link IProject} for this page or <code>null</code>
	 *         if there isn't one
	 */
	private IProject getProject() {
		IAdaptable element = getElement();
		if (element instanceof IJavaProject) {
			return ((IJavaProject) element).getProject();
		}
		if (element instanceof IProject) {
			return (IProject) element;
		}
		return null;
	}

	@Override
	public boolean performCancel() {
		block.performCancel();
		return super.performCancel();
	}

	@Override
	public boolean performOk() {
		block.performOK();
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		block.performDefaults();
		super.performDefaults();
	}

	@Override
	protected void performApply() {
		block.performApply();
		super.performApply();
	}

	@Override
	public void dispose() {
		block.dispose();
		super.dispose();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void applyData(Object data) {
		if (data instanceof HashMap) {
			fPageData = (HashMap<String, Object>) data;
			if (link != null) {
				link.setVisible(!Boolean.TRUE.equals(fPageData.get(ApiErrorsWarningsPreferencePage.NO_LINK)));
			}
			if (block == null) {
				return;
			}
			if (getProject() != null) {
				Boolean useProjectOptions = (Boolean) fPageData
						.get(ApiErrorsWarningsPreferencePage.USE_PROJECT_SPECIFIC_OPTIONS);

				if (useProjectOptions != null) {
					block.useProjectSpecificSettings(useProjectOptions.booleanValue());
					pspecific.setSelection(useProjectOptions.booleanValue());
				}
			}
			Integer tab = (Integer) fPageData.get(ApiErrorsWarningsPreferencePage.INITIAL_TAB);
			if (tab != null) {
				block.selectTab(tab.intValue());
			}
			Object key = fPageData.get(ApiErrorsWarningsPreferencePage.DATA_SELECT_OPTION_KEY);

			Object qualifier = fPageData.get(ApiErrorsWarningsPreferencePage.DATA_SELECT_OPTION_QUALIFIER);
			if (key instanceof String && qualifier instanceof String) {
				block.selectOption((String) key, (String) qualifier);
			}
		}
	}
}
