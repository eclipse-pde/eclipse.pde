/*
 * Created on Mar 14, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.ui.internal.samples;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.internal.model.IIntroAction;
import org.eclipse.ui.intro.internal.model.IntroURL;
import org.eclipse.ui.intro.internal.model.IntroURLParser;
import org.eclipse.update.standalone.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ShowSampleAction extends Action implements IIntroAction {
	private static final String SAMPLE_FEATURE_ID = "org.eclipse.sdk.samples";
	private static final String SAMPLE_FEATURE_VERSION = "3.0.0";
	private static final String UPDATE_SITE = "http://dev.eclipse.org/viewcvs/index.cgi/%7Echeckout%7E/pde-ui-home/samples/";
	private String sampleId;
	/**
	 *  
	 */
	public ShowSampleAction() {
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.intro.internal.model.IIntroAction#initialize(org.eclipse.ui.intro.IIntroSite,
	 *      java.util.Properties)
	 */
	public void initialize(IIntroSite site, Properties params) {
		sampleId = params.getProperty("id");
	}
	public void run() {
		if (sampleId == null)
			return;
		if (!ensureSampleFeaturePresent())
			return;
		SampleWizard wizard = new SampleWizard();
		try {
			wizard.setInitializationData(null, "class", sampleId);
			wizard.setSampleEditorNeeded(false);
			wizard.setSwitchPerspective(false);
			wizard.setSelectRevealEnabled(false);
			wizard.setActivitiesEnabled(false);
			WizardDialog dialog = new WizardDialog(PDEPlugin
					.getActiveWorkbenchShell(), wizard);
			dialog.create();
			dialog.getShell().setText("Eclipse Samples");
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
		url.append("http://org.eclipse.ui.intro/showStandby?");
		url.append("pluginId=org.eclipse.pde.ui");
		url.append("&");
		url.append("partId=org.eclipse.pde.ui.sampleStandbyPart");
		url.append("&");
		url.append("input=");
		url.append(sampleId);
		IntroURLParser parser = new IntroURLParser(url.toString());
		if (parser.hasIntroUrl()) {
			IntroURL introURL = parser.getIntroURL();
			introURL.execute();
			ensureProperContext(wizard);
		}
	}
	private void ensureProperContext(SampleWizard wizard) {
		IConfigurationElement sample = wizard.getSelection();
		String perspId = sample.getAttribute("perspectiveId");
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
						"Samples",
						"The samples are currently not installed. Do you want to dowload samples from Eclipse.org?")) {
			return downloadFeature();
		}
		return false;
	}
	private boolean checkFeature() {
		IPlatformConfiguration config = BootLoader
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
							UPDATE_SITE, null, "false");
					command.run(monitor);
					command.applyChangesNow();
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(PDEPlugin
					.getActiveWorkbenchShell());
			pmd.run(true, true, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		}
		return true;
	}
}