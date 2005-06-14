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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.build.FeatureExportInfo;
import org.eclipse.pde.internal.ui.build.FeatureExportJob;
import org.eclipse.ui.progress.IProgressConstants;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FeatureExportWizard extends BaseExportWizard {
	private static final String STORE_SECTION = "FeatureExportWizard"; //$NON-NLS-1$
	private AdvancedFeatureExportPage fPage3;
	private CrossPlatformExportPage fPage2;

	/**
	 * The constructor.
	 */
	public FeatureExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_FEATURE_EXPORT_WIZ);
	}
	
	public void addPages() {
		super.addPages();
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.findFeatureModel("org.eclipse.platform.launchers"); //$NON-NLS-1$
		if (model != null) {
			fPage2 = new CrossPlatformExportPage("environment", model); //$NON-NLS-1$
			addPage(fPage2);
		}	
		fPage3 = new AdvancedFeatureExportPage();
		addPage(fPage3);
		((FeatureExportWizardPage)fPage1).setFeaturePage(fPage3);
	}

	protected BaseExportWizardPage createPage1() {
		return new FeatureExportWizardPage(getSelection());
	}
	
	protected String getSettingsSectionName() {
		return STORE_SECTION;
	}
	
	protected void scheduleExportJob() {
		FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fPage1.doExportToDirectory();
		info.useJarFormat = fPage1.useJARFormat();
		info.exportSource = fPage1.doExportSource();
		info.destinationDirectory = fPage1.getDestination();
		info.zipFileName = fPage1.getFileName();
		info.targets = fPage2 == null ? null : 
			fPage1.doMultiPlatform() ? fPage2.getTargets() : null;
		info.javacSource = fPage1.getJavacSource();
		info.javacTarget = fPage1.getJavacTarget();
		info.items = ((ExportWizardPageWithTable)fPage1).getSelectedItems();
		info.signingInfo = fPage1.useJARFormat() ? fPage3.getSigningInfo() : null;
		info.jnlpInfo = fPage1.useJARFormat() ? fPage3.getJNLPInfo() : null;
		
		FeatureExportJob job = new FeatureExportJob(info);
		job.setUser(true);
		job.schedule();
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_FEATURE_OBJ);
	}
	
	protected Document generateAntTask() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("project"); //$NON-NLS-1$
			root.setAttribute("name", "build"); //$NON-NLS-1$ //$NON-NLS-2$
			root.setAttribute("default", "feature_export"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(root);
			
			Element target = doc.createElement("target"); //$NON-NLS-1$
			target.setAttribute("name", "feature_export"); //$NON-NLS-1$ //$NON-NLS-2$
			root.appendChild(target);
			
			Element export = doc.createElement("pde.exportFeatures"); //$NON-NLS-1$
			export.setAttribute("features", getFeatureIDs()); //$NON-NLS-1$
			export.setAttribute("destination", fPage1.getDestination()); //$NON-NLS-1$
			String filename = fPage1.getFileName();
			if (filename != null)
				export.setAttribute("filename", filename); //$NON-NLS-1$
			export.setAttribute("exportType", getExportOperation());  //$NON-NLS-1$
			export.setAttribute("useJARFormat", Boolean.toString(fPage1.useJARFormat())); //$NON-NLS-1$
			export.setAttribute("exportSource", Boolean.toString(fPage1.doExportSource())); //$NON-NLS-1$
			export.setAttribute("source", fPage1.getJavacSource()); //$NON-NLS-1$
			export.setAttribute("target", fPage1.getJavacTarget()); //$NON-NLS-1$
			return doc;
		} catch (DOMException e) {
		} catch (FactoryConfigurationError e) {
		} catch (ParserConfigurationException e) {
		}
		return null;
	}
	
	private String getFeatureIDs() {
		StringBuffer buffer = new StringBuffer();
		Object[] objects = ((ExportWizardPageWithTable)fPage1).getSelectedItems();
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
