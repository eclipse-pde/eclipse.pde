package org.eclipse.pde.internal.ui.wizards.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class OrganizeManifestsWizard extends Wizard {

	private OrganizeManifestsWizardPage fMainPage;
	private ArrayList fProjects;
	
	public OrganizeManifestsWizard(ArrayList projects) {
		IDialogSettings workbenchSettings = PDEPlugin.getDefault().getDialogSettings();
		fProjects = projects;
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEUIMessages.OrganizeManifestsWizard_title);
		setDialogSettings(workbenchSettings);
	}

	public boolean performFinish() {
		fMainPage.preformOk();
		try {
			OrganizeManifestsOperation op = new OrganizeManifestsOperation(fProjects);
			op.setOperations(fMainPage.getSettings());
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			PDEPlugin.log(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.log(e);
			return false;
		}
		return true;
	}
	
	public void addPages() {
		fMainPage = new OrganizeManifestsWizardPage();
		addPage(fMainPage);
	}
}
