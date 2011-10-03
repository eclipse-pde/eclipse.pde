/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.Vector;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.NewWizard;

/**
 * Wizard to convert one or more java projects into plug-in projects by creating
 * the basic bundle files and setting the plug-in nature.
 *
 */
public class ConvertedProjectWizard extends NewWizard {

	private static final String STORE_SECTION = "ConvertedProjectWizard"; //$NON-NLS-1$

	private ConvertedProjectsPage mainPage;
	private Vector selected;
	private IProject[] fUnconverted;

	public ConvertedProjectWizard(IProject[] projects, Vector initialSelection) {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
		setWindowTitle(PDEUIMessages.ConvertedProjectWizard_title);

		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(STORE_SECTION);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(STORE_SECTION);
		}
		setDialogSettings(settings);

		setNeedsProgressMonitor(true);
		this.selected = initialSelection;
		this.fUnconverted = projects;
	}

	public void addPages() {
		mainPage = new ConvertedProjectsPage(fUnconverted, selected);
		addPage(mainPage);
	}

	public boolean performFinish() {
		return mainPage.finish();
	}
}
