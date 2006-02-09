package org.eclipse.pde.internal.ui.wizards.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class OrganizeManifestsWizard extends Wizard {

	private OrganizeManifestsWizardPage fMainPage;
	private ArrayList fProjects;
	private boolean fWorkToBeDone = false;
	
	public OrganizeManifestsWizard(ArrayList projects) {
		IDialogSettings workbenchSettings = PDEPlugin.getDefault().getDialogSettings();
		fProjects = projects;
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.OrganizeManifestsWizard_title);
		setDialogSettings(workbenchSettings);
		for (int i = 0; i < fProjects.size(); i++) {
			if (WorkspaceModelManager.hasBundleManifest((IProject)fProjects.get(0))) {
				fWorkToBeDone = true;
				break;
			}
		}
	}

	public boolean performFinish() {
		fMainPage.preformOk();
		try {
			getContainer().run(false, true, new OrganizeManifestsOperation(fProjects, fMainPage.getSettings()));
		} catch (InvocationTargetException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	public void addPages() {
		fMainPage = new OrganizeManifestsWizardPage(fWorkToBeDone);
		addPage(fMainPage);
	}
}
