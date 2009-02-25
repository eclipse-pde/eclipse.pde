package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.target.impl.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class EditBundleContainerWizard extends Wizard {

	private ITargetDefinition fTarget;
	private IBundleContainer fContainer;
	private EditDirectoryContainerPage fPage;

	public EditBundleContainerWizard(ITargetDefinition target, IBundleContainer container) {
		fTarget = target;
		fContainer = container;
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(AddBundleContainerSelectionPage.SETTINGS_SECTION);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(AddBundleContainerSelectionPage.SETTINGS_SECTION);
		}
		setDialogSettings(settings);
		setWindowTitle(Messages.EditBundleContainerWizard_0);
	}

	public void addPages() {
		if (fContainer instanceof DirectoryBundleContainer) {
			fPage = new EditDirectoryContainerPage(fTarget, fContainer);
			addPage(fPage);
		} else if (fContainer instanceof ProfileBundleContainer) {
			fPage = new EditProfileContainerPage(fTarget, fContainer);
			addPage(fPage);
		} else if (fContainer instanceof FeatureBundleContainer) {
			fPage = new EditFeatureContainerPage(fTarget, fContainer);
			addPage(fPage);
		}
	}

	public boolean performFinish() {
		if (fPage != null) {
			fContainer = fPage.getBundleContainer();
			return true;
		}
		return false;
	}

	public IBundleContainer getBundleContainer() {
		return fContainer;
	}

}
