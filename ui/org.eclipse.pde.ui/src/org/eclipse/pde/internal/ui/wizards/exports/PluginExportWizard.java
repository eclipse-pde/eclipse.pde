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

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;

/**
 * Insert the type's description here.
 * 
 * @see Wizard
 */
public class PluginExportWizard extends BaseExportWizard {
	private static final String KEY_WTITLE = "ExportWizard.Plugin.wtitle"; //$NON-NLS-1$
	private static final String STORE_SECTION = "PluginExportWizard"; //$NON-NLS-1$

	/**
	 * The constructor.
	 */
	public PluginExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_EXPORT_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	protected BaseExportWizardPage createPage1() {
		return new PluginExportWizardPage(getSelection());
	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	protected void scheduleExportJob() {
		PluginExportJob job =
			new PluginExportJob(
				page1.getExportType(),
				page1.doExportSource(),
				page1.getDestination(),
				page1.getFileName(),
				page1.getSelectedItems());
		job.setUser(true);
		job.schedule();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizard#generateAntTask(java.io.PrintWriter)
	 */
	protected void generateAntTask(PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<project name=\"build\" default=\"plugin_export\">");
		writer.println("\t<target name=\"plugin_export\">");
		writer.print("\t\t<pde.exportPlugins plugins=\"" + getPluginIDs()
				+ "\" destination=\"" + page1.getDestination() + "\" ");
		String filename = page1.getFileName();
		if (filename != null)
			writer.print("filename =\"" + filename + "\" ");
		writer.print("exportType=\"" + getExportOperation() + "\" ");
		writer.println("exportSource=\"" + (page1.doExportSource() ? "true" : "false") + "\"/>"); 
		writer.println("\t</target>");
		writer.println("</project>");
	}
	
	private String getPluginIDs() {
		StringBuffer buffer = new StringBuffer();
		Object[] objects = page1.getSelectedItems();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (object instanceof IPluginModelBase) {
				buffer.append(((IPluginModelBase)object).getPluginBase().getId());
				if (i < objects.length - 1)
					buffer.append(",");					 //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}

}