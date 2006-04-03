/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.build.PluginExportJob;
import org.eclipse.ui.progress.IProgressConstants;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PluginExportWizard extends AntGeneratingExportWizard {
	private static final String STORE_SECTION = "PluginExportWizard"; //$NON-NLS-1$

	public PluginExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_EXPORT_WIZ);
	}

	protected BaseExportWizardPage createPage1() {
		return new PluginExportWizardPage(getSelection());
	}
	
	protected String getSettingsSectionName() {
		return STORE_SECTION;
	}
	
	protected void scheduleExportJob() {
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fPage.doExportToDirectory();
		info.useJarFormat = fPage.useJARFormat();
		info.exportSource = fPage.doExportSource();
		info.destinationDirectory = fPage.getDestination();
		info.zipFileName = fPage.getFileName();
		info.items = fPage.getSelectedItems();
		info.signingInfo = fPage.useJARFormat() ? fPage.getSigningInfo() : null;
		
		PluginExportJob job = new PluginExportJob(info);
		job.setUser(true);
		job.schedule();
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ);
	}

	protected Document generateAntTask() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("project"); //$NON-NLS-1$
			root.setAttribute("name", "build"); //$NON-NLS-1$ //$NON-NLS-2$
			root.setAttribute("default", "plugin_export"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);
			
			Element target = doc.createElement("target"); //$NON-NLS-1$
			target.setAttribute("name", "plugin_export"); //$NON-NLS-1$ //$NON-NLS-2$
			root.appendChild(target);
			
			Element export = doc.createElement("pde.exportPlugins"); //$NON-NLS-1$
			export.setAttribute("plugins", getPluginIDs()); //$NON-NLS-1$
			export.setAttribute("destination", fPage.getDestination()); //$NON-NLS-1$
			String filename = fPage.getFileName();
			if (filename != null)
				export.setAttribute("filename", filename); //$NON-NLS-1$
			export.setAttribute("exportType", getExportOperation());  //$NON-NLS-1$
			export.setAttribute("useJARFormat", Boolean.toString(fPage.useJARFormat()));  //$NON-NLS-1$
			export.setAttribute("exportSource", Boolean.toString(fPage.doExportSource()));  //$NON-NLS-1$
			target.appendChild(export);
			return doc;
		} catch (DOMException e) {
		} catch (FactoryConfigurationError e) {
		} catch (ParserConfigurationException e) {
		}
		return null;
	}
	
	private String getPluginIDs() {
		StringBuffer buffer = new StringBuffer();
		Object[] objects = fPage.getSelectedItems();
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
