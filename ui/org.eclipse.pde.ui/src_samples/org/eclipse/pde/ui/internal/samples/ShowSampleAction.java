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
package org.eclipse.pde.ui.internal.samples;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.standalone.InstallCommand;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ShowSampleAction extends Action implements IIntroAction {
	private static final String SAMPLE_FEATURE_ID = "org.eclipse.sdk.samples"; //$NON-NLS-1$
	private static final String SAMPLE_FEATURE_VERSION = "3.0.0"; //$NON-NLS-1$
	private static final String UPDATE_SITE = "http://dev.eclipse.org/viewcvs/index.cgi/%7Echeckout%7E/pde-ui-home/samples/"; //$NON-NLS-1$
	private String sampleId;
	/**
	 *  
	 */
	public ShowSampleAction() {
	}

	public void run(IIntroSite site, Properties params) {
		sampleId = params.getProperty("id"); //$NON-NLS-1$
		if (sampleId == null)
			return;
		if (!ensureSampleFeaturePresent())
			return;
		SampleWizard wizard = new SampleWizard();
		try {
			wizard.setInitializationData(null, "class", sampleId); //$NON-NLS-1$
			wizard.setSampleEditorNeeded(false);
			wizard.setSwitchPerspective(false);
			wizard.setSelectRevealEnabled(false);
			wizard.setActivitiesEnabled(false);
			WizardDialog dialog = new WizardDialog(PDEPlugin
					.getActiveWorkbenchShell(), wizard);
			dialog.create();
			dialog.getShell().setText(PDEPlugin.getResourceString("ShowSampleAction.title")); //$NON-NLS-1$
			dialog.getShell().setSize(400, 500);
			if (dialog.open() == WizardDialog.OK) {
				switchToSampleStandby(wizard);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void switchToSampleStandby(SampleWizard wizard) {
		StringBuffer url = new StringBuffer();
		url.append("http://org.eclipse.ui.intro/showStandby?"); //$NON-NLS-1$
		url.append("pluginId=org.eclipse.pde.ui"); //$NON-NLS-1$
		url.append("&"); //$NON-NLS-1$
		url.append("partId=org.eclipse.pde.ui.sampleStandbyPart"); //$NON-NLS-1$
		url.append("&"); //$NON-NLS-1$
		url.append("input="); //$NON-NLS-1$
		url.append(sampleId);
		IIntroURL introURL = IntroURLFactory.createIntroURL(url.toString());
		if (introURL != null) {
			introURL.execute();
			ensureProperContext(wizard);
		}
	}
	private void ensureProperContext(SampleWizard wizard) {
		IConfigurationElement sample = wizard.getSelection();
		String perspId = sample.getAttribute("perspectiveId"); //$NON-NLS-1$
		if (perspId!=null) {
			try {
				wizard.enableActivities();
				PlatformUI.getWorkbench().showPerspective(perspId, PDEPlugin.getActiveWorkbenchWindow());
				wizard.selectReveal(PDEPlugin.getActiveWorkbenchShell());
			}
			catch (WorkbenchException e) {
				PDEPlugin.logException(e);
			}
		}
		enableActivities(sample);
	}
	private void enableActivities(IConfigurationElement sample) {
	}
	private boolean ensureSampleFeaturePresent() {
		if (checkFeature())
			return true;
		// the feature is not present - ask to download
		if (MessageDialog
				.openQuestion(
						PDEPlugin.getActiveWorkbenchShell(),
						PDEPlugin.getResourceString("ShowSampleAction.msgTitle"), //$NON-NLS-1$
						PDEPlugin.getResourceString("ShowSampleAction.msgDesc"))) { //$NON-NLS-1$
			return downloadFeature();
		}
		return false;
	}
	private boolean checkFeature() {
		IPlatformConfiguration config = ConfiguratorUtils
				.getCurrentPlatformConfiguration();
		IPlatformConfiguration.IFeatureEntry [] features = config
				.getConfiguredFeatureEntries();
		PluginVersionIdentifier sampleVersion = new PluginVersionIdentifier(
				SAMPLE_FEATURE_VERSION);
		for (int i = 0; i < features.length; i++) {
			String id = features[i].getFeatureIdentifier();
			if (SAMPLE_FEATURE_ID.equals(id)) {
				String version = features[i].getFeatureVersion();
				PluginVersionIdentifier fversion = new PluginVersionIdentifier(
						version);
				if (fversion.isCompatibleWith(sampleVersion))
					return true;
			}
		}
		return false;
	}
	private boolean downloadFeature() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					InstallCommand command = new InstallCommand(
							SAMPLE_FEATURE_ID, SAMPLE_FEATURE_VERSION,
							UPDATE_SITE, null, "false"); //$NON-NLS-1$
					command.run(monitor);
					command.applyChangesNow();
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}
}
