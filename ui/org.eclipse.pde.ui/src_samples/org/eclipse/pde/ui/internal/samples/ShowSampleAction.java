/*
 * Created on Mar 14, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.ui.internal.samples;

import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.internal.model.IIntroAction;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ShowSampleAction extends Action implements IIntroAction {
	private String sampleId;
	/**
	 * 
	 */
	public ShowSampleAction() {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.intro.internal.model.IIntroAction#initialize(org.eclipse.ui.intro.IIntroSite, java.util.Properties)
	 */
	public void initialize(IIntroSite site, Properties params) {
		sampleId = params.getProperty("id");
	}
	public void run() {
		if (sampleId==null) return;
		SampleWizard wizard = new SampleWizard();
		try {
			wizard.setInitializationData(null, "class", sampleId);
			WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
			dialog.create();
			dialog.getShell().setText("Eclipse Samples");
			dialog.getShell().setSize(400, 500);
			if (dialog.open()==WizardDialog.OK) {
				// switch to the workbench
				IWorkbench workbench = PlatformUI.getWorkbench();
				workbench.setIntroStandby(workbench.findIntro(), true);
			}
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}