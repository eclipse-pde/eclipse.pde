package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


public class FeatureExportWizardPage extends BaseExportWizardPage {
	public FeatureExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"featureExport",
			PDEPlugin.getResourceString("ExportWizard.Feature.pageBlock"),
			true);
		setTitle(PDEPlugin.getResourceString("ExportWizard.Feature.pageTitle"));
	}

	public Object[] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getWorkspaceFeatureModels();
	}
	protected void createZipSection(Composite container) {
		zipRadio =
			createRadioButton(
				container,
				PDEPlugin.getResourceString("ExportWizard.Feature.zip"));

		/*includeSource = new Button(container, SWT.CHECK);
		includeSource.setText(PDEPlugin.getResourceString("ExportWizard.includeSource"));
		includeSource.setSelection(true);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		gd.horizontalIndent = 25;
		includeSource.setLayoutData(gd);*/		

	}
	
	protected void createUpdateJarsSection(Composite container) {
		updateRadio =
			createRadioButton(
				container,
				PDEPlugin.getResourceString("ExportWizard.Plugin.updateJars"));

		directoryLabel = new Label(container, SWT.NULL);
		directoryLabel.setText(PDEPlugin.getResourceString("ExportWizard.destination"));
		GridData gd = new GridData();
		directoryLabel.setLayoutData(gd);

		destination = new Combo(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		destination.setLayoutData(gd);
		browseDirectory = new Button(container, SWT.PUSH);
		browseDirectory.setText(PDEPlugin.getResourceString("ExportWizard.browse"));
		browseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(browseDirectory);
	}
	
	protected void pageChanged() {
		String dest = getDestination();
		boolean hasDest = dest.length() > 0;
		boolean hasSel = exportPart.getSelectionCount() > 0;

		String message = null;
		if (!hasSel) {
			message = PDEPlugin.getResourceString("ExportWizard.status.noselection");
		} else if (!hasDest) {
			message = PDEPlugin.getResourceString("ExportWizard.status.nodirectory");
		}
		setMessage(message);
		setPageComplete(hasSel && hasDest);
	}

}
