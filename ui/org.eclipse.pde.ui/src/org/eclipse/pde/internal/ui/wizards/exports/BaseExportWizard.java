/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Insert the type's description here.
 * 
 * @see Wizard
 */
public abstract class BaseExportWizard
	extends Wizard
	implements IExportWizard, IPreferenceConstants {

	private IStructuredSelection selection;
	protected BaseExportWizardPage page1;

	/**
	 * The constructor.
	 */
	public BaseExportWizard() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		IDialogSettings masterSettings =
			PDEPlugin.getDefault().getDialogSettings();
		setNeedsProgressMonitor(true);
		setDialogSettings(getSettingsSection(masterSettings));
	}

	public void addPages() {
		page1 = createPage1();
		addPage(page1);
	}

	protected abstract BaseExportWizardPage createPage1();

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public IStructuredSelection getSelection() {
		return selection;
	}

	protected abstract IDialogSettings getSettingsSection(IDialogSettings masterSettings);

	/**
	 * @see Wizard#init
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	/**
	 * @see Wizard#performFinish
	 */
	public boolean performFinish() {
		page1.saveSettings();
		if (page1.doGenerateAntFile()) {
			generateAntBuildFile(page1.getAntBuildFileName());
		}
		if (page1.doExportAsZip()) {
			File zipFile = new File(page1.getDestination(), page1.getFileName());
			if (zipFile.exists()) {
				if (!MessageDialog.openQuestion(getContainer().getShell(),
						"Confirm Replace", "The file \"" + page1.getDestination() + File.separator + page1.getFileName() +  "\" already exists.  Do you want to overwrite it?"))
					return false;
				zipFile.delete();
			}
		}
		scheduleExportJob();
		return true;
	}
	
	private void generateAntBuildFile(String filename) {
		String parent = new Path(filename).removeLastSegments(1).toOSString();
		String buildFilename = new Path(filename).lastSegment();
		if (!buildFilename.endsWith(".xml"))
			buildFilename += ".xml";
		File dir = new File(new File(parent).getAbsolutePath());
		if (!dir.exists())
			dir.mkdirs();
		
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(new File(dir, buildFilename)));
			generateAntTask(writer);
			writer.close();
		} catch (IOException e) {
		}
	}
		
	protected abstract void generateAntTask(PrintWriter writer);
	
	protected abstract void scheduleExportJob();
	
	protected String getExportOperation() {
		int exportType = page1.getExportType();
		switch (exportType) {
			case FeatureExportJob.EXPORT_AS_ZIP:
				return "zip";
			case FeatureExportJob.EXPORT_AS_DIRECTORY:
				return "directory";
			case FeatureExportJob.EXPORT_AS_UPDATE_JARS:
				return "update";
		}
		return "zip";
	}

}