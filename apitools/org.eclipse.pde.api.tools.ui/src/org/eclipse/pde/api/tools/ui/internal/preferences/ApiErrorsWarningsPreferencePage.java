/*******************************************************************************
 * Copyright (c) 2007, 2024 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * Class provides the general API tool preference page
 *
 * @since 1.0.0
 */
public class ApiErrorsWarningsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String DATA_SELECT_OPTION_KEY = "select_option_key"; //$NON-NLS-1$
	public static final String DATA_SELECT_OPTION_QUALIFIER = "select_option_qualifier"; //$NON-NLS-1$

	/**
	 * Key for a Boolean value defining if 'use project specific settings' should be
	 * enabled or not.
	 */
	public static final String USE_PROJECT_SPECIFIC_OPTIONS = "use_project_specific_key"; //$NON-NLS-1$

	public static final String ID = ApiUIPlugin.PLUGIN_ID + ".apitools.errorwarnings.prefpage"; //$NON-NLS-1$
	/**
	 * Id of a setting in the data map applied when the page is opened. Value
	 * must be a Boolean object. If true, the customize project settings link
	 * will be hidden.
	 */
	public static final String NO_LINK = "PropertyAndPreferencePage.nolink"; //$NON-NLS-1$
	/**
	 * Id of a setting in the data map applied when the page is opened. Value
	 * must be an Integer object with a value match the id of a tab on the page
	 * See constants
	 * {@link ApiErrorsWarningsConfigurationBlock#API_USE_SCANS_PAGE_ID},
	 * {@link ApiErrorsWarningsConfigurationBlock#COMPATIBILITY_PAGE_ID},
	 * {@link ApiErrorsWarningsConfigurationBlock#VERSION_MANAGEMENT_PAGE_ID},
	 * {@link ApiErrorsWarningsConfigurationBlock#API_COMPONENT_RESOLUTION_PAGE_ID}
	 * and {@link ApiErrorsWarningsConfigurationBlock#API_USE_SCANS_PAGE_ID} If
	 * an id is provided, the preference page will open with the specified tab
	 * visible
	 */
	public static final String INITIAL_TAB = "PropertyAndPreferencePage.initialTab"; //$NON-NLS-1$

	/**
	 * The main configuration block for the page
	 */
	ApiErrorsWarningsConfigurationBlock block = null;
	private Link link = null;

	private Image expandImage;
	private Image collapseImage;

	/**
	 * Since {@link #applyData(Object)} can be called before createContents,
	 * store the data
	 */
	private Map<String, Object> fPageData = null;

	/**
	 * Constructor
	 */
	public ApiErrorsWarningsPreferencePage() {
		super(PreferenceMessages.ApiErrorsWarningsPreferencePage_0);
	}

	private void expandCollapseItems(Composite parent, boolean expandPage) {
		for (Control control : parent.getChildren()) {
			if (control instanceof ExpandableComposite page) {
				page.setExpanded(expandPage);
				Control child = page.getClient();
				if (child != null && !child.isDisposed()) {
					child.setVisible(expandPage);
				}
			} else if (control instanceof Composite) {
				expandCollapseItems((Composite) control, expandPage);
			}
		}
		parent.layout(true, true);
		ScrolledComposite composite = getScrolledComposite(parent);
		if (composite != null) {
			composite.setMinSize(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
	}

	private ScrolledComposite getScrolledComposite(Composite comp) {
		Composite currentComposite = comp;
		while (currentComposite != null && !(currentComposite instanceof ScrolledComposite)) {
			currentComposite = currentComposite.getParent();
		}
		return (ScrolledComposite) currentComposite;
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		link = new Link(comp, SWT.NONE);
		link.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));
		link.setFont(comp.getFont());
		link.setText(PreferenceMessages.ApiErrorsWarningsPreferencePage_1);
		link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			Set<IJavaProject> set = new HashSet<>();
			try {
				IJavaProject[] projects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
				for (IJavaProject p : projects) {
					IProject project = p.getProject();
					if (Util.isApiProject(project) && block.hasProjectSpecificSettings(project)) {
						set.add(p);
					}
				}
			} catch (JavaModelException jme) {
				// ignore
			}
			ProjectSelectionDialog psd = new ProjectSelectionDialog(getShell(), set);
			if (psd.open() == IDialogConstants.OK_ID) {
				Map<String, Boolean> data = new HashMap<>();
				data.put(NO_LINK, Boolean.TRUE);
				PreferencesUtil.createPropertyDialogOn(getShell(), ((IJavaProject) psd.getFirstResult()).getProject(),
						IApiToolsConstants.ID_ERRORS_WARNINGS_PROP_PAGE,
						new String[] { IApiToolsConstants.ID_ERRORS_WARNINGS_PROP_PAGE }, data).open();
			}
		}));

		ToolBar buttonbar = new ToolBar(comp, SWT.FLAT | SWT.RIGHT);
		buttonbar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true,
		 false));
		ToolItem expandItems = new ToolItem(buttonbar, SWT.PUSH);
		expandImage = AbstractUIPlugin
				.imageDescriptorFromPlugin("org.eclipse.pde.api.tools.ui", "icons/full/elcl16/expandall.svg") //$NON-NLS-1$ //$NON-NLS-2$
			.createImage();
		expandItems.setImage(expandImage);
		expandItems.setToolTipText(PreferenceMessages.PREFERENCE_EXPAND_ALL);

		ToolItem collapseItems = new ToolItem(buttonbar, SWT.PUSH);
		collapseImage = AbstractUIPlugin
				.imageDescriptorFromPlugin("org.eclipse.pde.api.tools.ui", "icons/full/elcl16/collapseall.svg") //$NON-NLS-1$ //$NON-NLS-2$
				.createImage();
		collapseItems.setImage(collapseImage);
		collapseItems.setToolTipText(PreferenceMessages.PREFERENCE_COLLAPSE_ALL);

		block = new ApiErrorsWarningsConfigurationBlock(null, (IWorkbenchPreferenceContainer) getContainer());
		block.createControl(comp);

		expandItems.addListener(SWT.Selection, e -> expandCollapseItems(comp, true));
		collapseItems.addListener(SWT.Selection, e -> expandCollapseItems(comp, false));
		comp.addDisposeListener(e -> {
			if (!expandImage.isDisposed() && expandImage != null) {
				expandImage.dispose();
			}
			if (!collapseImage.isDisposed() && collapseImage != null) {
				collapseImage.dispose();
			}
		});

		// Initialize with data map in case applyData was called before
		// createContents
		applyData(fPageData);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APITOOLS_ERROR_WARNING_PREF_PAGE);
		return comp;
	}

	@Override
	public void dispose() {
		if (block != null) {
			block.dispose();
		}
		super.dispose();
	}

	@Override
	public void init(IWorkbench workbench) {
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
	protected void performApply() {
		block.performApply();
		super.performApply();
	}

	@Override
	protected void performDefaults() {
		block.performDefaults();
		super.performDefaults();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void applyData(Object data) {
		if (data instanceof Map) {
			fPageData = (Map<String, Object>) data;
			if (link != null && fPageData.containsKey(NO_LINK)) {
				link.setVisible(!Boolean.TRUE.equals(fPageData.get(NO_LINK)));
			}
			if (block != null && fPageData.containsKey(INITIAL_TAB)) {
				Integer tabIndex = (Integer) fPageData.get(INITIAL_TAB);
				if (tabIndex != null) {
					try {
						block.selectTab(tabIndex.intValue());
					} catch (NumberFormatException e) {
						// Page was called with bad data, just ignore
					}
				}
			}
			if (block == null) {
				return;
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
