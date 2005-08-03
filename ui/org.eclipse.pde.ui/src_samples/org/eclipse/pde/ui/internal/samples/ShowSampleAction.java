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
package org.eclipse.pde.ui.internal.samples;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.standalone.InstallCommand;

public class ShowSampleAction extends Action implements IIntroAction {
	private static final String SAMPLE_FEATURE_ID = "org.eclipse.sdk.samples"; //$NON-NLS-1$
	private static final String SAMPLE_FEATURE_VERSION = "3.1.0"; //$NON-NLS-1$
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
        
         Runnable r= new Runnable() {
                public void run() {
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
                    dialog.setPageSize(450, 500);
                    if (dialog.open() == WizardDialog.OK) {
                        switchToSampleStandby(wizard);
                    }
                } catch (CoreException e) {
                    PDEPlugin.logException(e);
                }
            }
        };
        
        Shell currentShell = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getShell();
        currentShell.getDisplay().asyncExec(r);
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
						PDEUIMessages.ShowSampleAction_msgTitle, 
						PDEUIMessages.ShowSampleAction_msgDesc)) { 
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
