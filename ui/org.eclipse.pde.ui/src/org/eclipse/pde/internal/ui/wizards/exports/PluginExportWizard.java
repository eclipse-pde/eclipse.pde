/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.PluginExportOperation;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.build.RuntimeInstallJob;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PluginExportWizard extends AntGeneratingExportWizard {
	private static final String STORE_SECTION = "PluginExportWizard"; //$NON-NLS-1$

	public PluginExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_EXPORT_WIZ);
	}

	@Override
	protected BaseExportWizardPage createPage1() {
		return new PluginExportWizardPage(getSelection());
	}

	@Override
	protected String getSettingsSectionName() {
		return STORE_SECTION;
	}

	@Override
	protected void scheduleExportJob() {
		// NOTE: Any changes to the content here must also be copied to generateAntTask() and PluginExportTask
		final FeatureExportInfo info = new FeatureExportInfo();
		info.toDirectory = fPage.doExportToDirectory();
		info.useJarFormat = fPage.useJARFormat();
		info.exportSource = fPage.doExportSource();
		info.exportSourceBundle = fPage.doExportSourceBundles();
		info.allowBinaryCycles = fPage.allowBinaryCycles();
		info.useWorkspaceCompiledClasses = fPage.useWorkspaceCompiledClasses();
		info.destinationDirectory = fPage.getDestination();
		info.zipFileName = fPage.getFileName();
		info.items = fPage.getSelectedItems();
		info.signingInfo = fPage.useJARFormat() ? fPage.getSigningInfo() : null;
		info.qualifier = fPage.getQualifier();

		final boolean installAfterExport = fPage.doInstall();
		if (installAfterExport) {
			RuntimeInstallJob.modifyInfoForInstall(info);
		}

		final PluginExportOperation job = new PluginExportOperation(info, PDEUIMessages.PluginExportJob_name);
		job.setUser(true);
		job.setRule(ResourcesPlugin.getWorkspace().getRoot());
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PLUGIN_OBJ);
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (job.hasAntErrors()) {
					// If there were errors when running the ant scripts, inform the user where the logs can be found.
					final File logLocation = new File(info.destinationDirectory, "logs.zip"); //$NON-NLS-1$
					if (logLocation.exists()) {
						PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
							AntErrorDialog dialog = new AntErrorDialog(logLocation);
							dialog.open();
						});
					}
				} else if (event.getResult().isOK() && installAfterExport) {
					// Install the export into the current running platform
					RuntimeInstallJob installJob = new RuntimeInstallJob(PDEUIMessages.PluginExportWizard_InstallJobName, info);
					installJob.setUser(true);
					installJob.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_FEATURE_OBJ);
					installJob.schedule();
				}
			}
		});
		job.schedule();
	}

	@Override
	protected Document generateAntTask() {
		try {
			@SuppressWarnings("restriction")
			Document doc = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createDocumentBuilderWithErrorOnDOCTYPE().newDocument();
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
			export.setAttribute("exportType", getExportOperation()); //$NON-NLS-1$
			export.setAttribute("useJARFormat", Boolean.toString(fPage.useJARFormat())); //$NON-NLS-1$
			export.setAttribute("exportSource", Boolean.toString(fPage.doExportSource())); //$NON-NLS-1$
			if (fPage.doExportSource()) {
				export.setAttribute("exportSourceBundle", Boolean.toString(fPage.doExportSourceBundles())); //$NON-NLS-1$
			}
			String qualifier = fPage.getQualifier();
			if (qualifier != null)
				export.setAttribute("qualifier", qualifier); //$NON-NLS-1$
			target.appendChild(export);
			return doc;
		} catch (DOMException | FactoryConfigurationError | ParserConfigurationException e) {
		}
		return null;
	}

	private String getPluginIDs() {
		StringBuilder buffer = new StringBuilder();
		Object[] objects = fPage.getSelectedItems();
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (object instanceof IPluginModelBase) {
				buffer.append(((IPluginModelBase) object).getPluginBase().getId());
				if (i < objects.length - 1)
					buffer.append(","); //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}

}
