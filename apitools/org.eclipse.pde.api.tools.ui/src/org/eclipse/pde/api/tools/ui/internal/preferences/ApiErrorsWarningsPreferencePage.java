/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * Class provides the general API tool preference page
 * @since 1.0.0
 */
public class ApiErrorsWarningsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String NO_LINK= "PropertyAndPreferencePage.nolink"; //$NON-NLS-1$
	
	/**
	 * The main configuration block for the page
	 */
	private ApiErrorsWarningsConfigurationBlock block = null;
	private Link link = null;
	
	private HashMap fPageData = null;
	
	/**
	 * Constructor
	 */
	public ApiErrorsWarningsPreferencePage() {
		super(PreferenceMessages.ApiErrorsWarningsPreferencePage_0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite comp  = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		link = new Link(comp, SWT.NONE);
		link.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));
		link.setFont(comp.getFont());
		link.setText(PreferenceMessages.ApiErrorsWarningsPreferencePage_1); 
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				HashSet set = new HashSet();
				try {
					IJavaProject[] projects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
					IProject project = null;
					for(int i = 0; i < projects.length; i++) {
						project = projects[i].getProject();
						try {
							if(project.hasNature(ApiPlugin.NATURE_ID) && block.hasProjectSpecificSettings(project)) {
								set.add(projects[i]);
							}
						}
						catch(CoreException ce) {
							//do nothing ignore the project
						}
					}
				}
				catch (JavaModelException jme) {
					//ignore
				}
				ProjectSelectionDialog psd = new ProjectSelectionDialog(getShell(), set);
				if(psd.open() == IDialogConstants.OK_ID) {
					HashMap data = new HashMap();
					data.put(NO_LINK, Boolean.TRUE);
					PreferencesUtil.createPropertyDialogOn(getShell(), ((IJavaProject) psd.getFirstResult()).getProject(), 
							"org.eclipse.pde.api.tools.ui.apitools.proppage",  //$NON-NLS-1$
							new String[] { "org.eclipse.pde.api.tools.ui.apitools.proppage" }, data).open(); //$NON-NLS-1$
				}
			};
		});
		block = new ApiErrorsWarningsConfigurationBlock(null, (IWorkbenchPreferenceContainer)getContainer());
		block.createControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APITOOLS_ERROR_WARNING_PREF_PAGE);
		return comp;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		if(block != null) {
			block.dispose();
		}
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	public boolean performCancel() {
		block.performCancel();
		return super.performCancel();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		block.performOK();
		return super.performOk();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		block.performApply();
		super.performApply();
	}
	
	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		block.performDefaults();
		super.performDefaults();
	}
	
	/**
	 * @see org.eclipse.jface.preference.PreferencePage#applyData(java.lang.Object)
	 */
	public void applyData(Object data) {
		if(data instanceof HashMap) {
			fPageData = (HashMap) data;
			link.setVisible(!Boolean.TRUE.equals(fPageData.get(NO_LINK)));
		}
	}
}
