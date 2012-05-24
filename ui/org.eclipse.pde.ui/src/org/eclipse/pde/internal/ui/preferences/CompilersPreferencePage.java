/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * Allows PDE compiler preferences to be set
 */
public class CompilersPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String NO_LINK = "PropertyAndPreferencePage.nolink"; //$NON-NLS-1$

	private PDECompilersConfigurationBlock fBlock = null;
	private Link link = null;

	/**
	 * Since {@link #applyData(Object)} can be called before createContents, store the data
	 */
	private Map fPageData = null;

	/**
	 *  
	 */
	public CompilersPreferencePage() {
		super();
		// only used when page is shown programmatically
		setTitle(PDEUIMessages.CompilersPreferencePage_title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite composite) {
		super.createControl(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.COMPILERS_PREFERENCE_PAGE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		link = new Link(comp, SWT.NONE);
		link.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false));
		link.setFont(comp.getFont());
		link.setText(PDEUIMessages.CompilersPreferencePage_configure_project_specific_settings);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				HashSet set = new HashSet();
				try {
					IJavaProject[] projects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
					IProject project = null;
					for (int i = 0; i < projects.length; i++) {
						project = projects[i].getProject();
						try {
							if (project.hasNature(PDE.PLUGIN_NATURE) && fBlock.hasProjectSpecificSettings(project)) {
								set.add(projects[i]);
							}
						} catch (CoreException ce) {
							//do nothing ignore the project
						}
					}
				} catch (JavaModelException jme) {
					//ignore
				}
				ProjectSelectionDialog psd = new ProjectSelectionDialog(getShell(), set);
				if (psd.open() == IDialogConstants.OK_ID) {
					HashMap data = new HashMap();
					data.put(NO_LINK, Boolean.TRUE);
					PreferencesUtil.createPropertyDialogOn(getShell(), ((IJavaProject) psd.getFirstResult()).getProject(), "org.eclipse.pde.internal.ui.properties.compilersPropertyPage", //$NON-NLS-1$
							new String[] {"org.eclipse.pde.internal.ui.properties.compilersPropertyPage"}, data).open(); //$NON-NLS-1$
				}
			}
		});
		fBlock = new PDECompilersConfigurationBlock(null, (IWorkbenchPreferenceContainer) getContainer());
		fBlock.createControl(comp);

		// Initialize with data map in case applyData was called before createContents
		applyData(fPageData);

		return comp;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		if (fBlock != null) {
			fBlock.dispose();
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	public boolean performCancel() {
		fBlock.performCancel();
		return super.performCancel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		fBlock.performOK();
		return super.performOk();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		fBlock.performApply();
		super.performApply();
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fBlock.performDefaults();
		super.performDefaults();
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#applyData(java.lang.Object)
	 */
	public void applyData(Object data) {
		if (data instanceof Map) {
			fPageData = (Map) data;
			if (link != null && fPageData.containsKey(NO_LINK)) {
				link.setVisible(!Boolean.TRUE.equals(((Map) data).get(NO_LINK)));
			}
		}
	}
}
