/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.ui.progress.*;

/**
 * Insert the type's description here.
 * 
 * @see Wizard
 */
public class FeatureExportWizard extends BaseExportWizard {
	private static final String KEY_WTITLE = "ExportWizard.Feature.wtitle"; //$NON-NLS-1$
	private static final String STORE_SECTION = "FeatureExportWizard"; //$NON-NLS-1$

	/**
	 * The constructor.
	 */
	public FeatureExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_FEATURE_EXPORT_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	protected BaseExportWizardPage createPage1() {
		return new FeatureExportWizardPage(getSelection());
	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	protected void scheduleExportJob() {
		FeatureExportJob job =
			new FeatureExportJob(
				page1.getExportType(),
				page1.doExportSource(),
				page1.getDestination(),
				page1.getFileName(),
				page1.getSelectedItems(),
				page1.getSigningInfo(),
				page1.getJnlpInfo());
		job.setUser(true);
		job.schedule();
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_FEATURE_OBJ);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizard#generateAntTask(java.io.PrintWriter)
	 */
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizard#generateAntTask(java.io.PrintWriter)
	 */
	protected void generateAntTask(PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		writer.println("<project name=\"build\" default=\"feature_export\">"); //$NON-NLS-1$
		writer.println("\t<target name=\"feature_export\">"); //$NON-NLS-1$
		writer.print("\t\t<pde.exportFeatures features=\"" + getFeatureIDs() //$NON-NLS-1$
				+ "\" destination=\"" + page1.getDestination() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		String filename = page1.getFileName();
		if (filename != null)
			writer.print("filename=\"" + filename + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print("exportType=\"" + getExportOperation() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("exportSource=\"" + (page1.doExportSource() ? "true" : "false") + "\"/>");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		writer.println("\t</target>"); //$NON-NLS-1$
		writer.println("</project>"); //$NON-NLS-1$
	}
	
	private String getFeatureIDs() {
		StringBuffer buffer = new StringBuffer();
		Object[] objects = page1.getSelectedItems();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (object instanceof IFeatureModel) {
				buffer.append(((IFeatureModel)object).getFeature().getId());
				if (i < objects.length - 1)
					buffer.append(",");					 //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}


}
