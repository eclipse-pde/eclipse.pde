package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;

/**
 * Insert the type's description here.
 * @see Wizard
 */
public class FeatureExportWizard extends BaseExportWizard {
	private static final String KEY_WTITLE = "BaseExportWizard.wtitle";
	private static final String STORE_SECTION = "FeatureExportWizard";

	/**
	 * The constructor.
	 */
	public FeatureExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXPRJ_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	protected BaseExportWizardPage createPage1() {
		return new FeatureExportWizardPage(getSelection());
	}

	/**
	 * 
	 */
	protected void doExport(
		boolean exportZip,
		boolean addSourceZips,
		String destination,
		IModel model,
		IProgressMonitor monitor)
		throws CoreException {
		IPluginModelBase modelBase = (IPluginModelBase) model;

		String label =
			PDEPlugin.getDefault().getLabelProvider().getObjectText(
				modelBase.getPluginBase());
		monitor.beginTask("", 2);
		monitor.subTask(label);
		try {
			Thread.sleep(1000);
			monitor.worked(1);
			Thread.sleep(1000);
			monitor.worked(1);
		} catch (InterruptedException e) {
		}
	}
}
