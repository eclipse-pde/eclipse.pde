/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.*;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.progress.*;

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
	
	protected String getSettingsSectionName() {
		return STORE_SECTION;
	}
	
	protected void scheduleExportJob() {
		String[] signingInfo = fPage1.useJARFormat() ? fPage2.getSigningInfo() : null;
		PluginExportJob job =
			new PluginExportJob(
				fPage1.doExportToDirectory(),
				fPage1.useJARFormat(),
				fPage1.doExportSource(),
				fPage1.getDestination(),
				fPage1.getFileName(),
				((ExportWizardPageWithTable)fPage1).getSelectedItems(),
				signingInfo);
		job.setUser(true);
		job.schedule();
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.exports.BaseExportWizard#generateAntTask(java.io.PrintWriter)
	 */
	protected void generateAntTask(PrintWriter writer) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		writer.println("<project name=\"build\" default=\"plugin_export\">"); //$NON-NLS-1$
		writer.println("\t<target name=\"plugin_export\">"); //$NON-NLS-1$
		writer.print("\t\t<pde.exportPlugins plugins=\"" + getPluginIDs() //$NON-NLS-1$
				+ "\" destination=\"" + fPage1.getDestination() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		String filename = fPage1.getFileName();
		if (filename != null)
			writer.print("filename=\"" + filename + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print("exportType=\"" + getExportOperation() + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print("useJARFormat=\"" + Boolean.toString(fPage1.useJARFormat()) + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("exportSource=\"" + Boolean.toString(fPage1.doExportSource()) + "\"/>");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		writer.println("\t</target>"); //$NON-NLS-1$
		writer.println("</project>"); //$NON-NLS-1$
	}
	
	private String getPluginIDs() {
		StringBuffer buffer = new StringBuffer();
		Object[] objects = ((ExportWizardPageWithTable)fPage1).getSelectedItems();
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

	protected AdvancedPluginExportPage createPage2() {
		return new AdvancedPluginExportPage("plugin-sign"); //$NON-NLS-1$
	}

}
