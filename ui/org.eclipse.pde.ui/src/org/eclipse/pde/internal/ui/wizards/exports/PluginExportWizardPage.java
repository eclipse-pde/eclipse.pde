package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.help.WorkbenchHelp;


public class PluginExportWizardPage extends BaseExportWizardPage {
	private Label label;

	private Button browseFile;

	public PluginExportWizardPage(IStructuredSelection selection) {
		super(
			selection,
			"pluginExport",
			PDEPlugin.getResourceString("ExportWizard.Plugin.pageBlock"),
			false);
		setTitle(PDEPlugin.getResourceString("ExportWizard.Plugin.pageTitle"));
	}

	public Object[] getListElements() {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getAllModels();
	}
	
	protected void hookHelpContext(Control control) {
		WorkbenchHelp.setHelp(control, IHelpContextIds.PLUGIN_EXPORT_WIZARD);
	}
	
	protected void createZipSection(Composite container) {
		zipRadio =
			createRadioButton(
				container,
				PDEPlugin.getResourceString("ExportWizard.Plugin.zip"));
						
						
		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("ExportWizard.zipFile"));
		GridData gd = new GridData();
		gd.horizontalIndent = 25;
		label.setLayoutData(gd);
		
		zipFile = new Combo(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		zipFile.setLayoutData(gd);
		
		browseFile = new Button(container, SWT.PUSH);
		browseFile.setText(PDEPlugin.getResourceString("ExportWizard.browse"));
		browseFile.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(browseFile);
		
		includeSource = new Button(container, SWT.CHECK);
		includeSource.setText(PDEPlugin.getResourceString("ExportWizard.includeSource"));
		includeSource.setSelection(true);
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.horizontalIndent = 25;
		includeSource.setLayoutData(gd);		
	}
	
	protected void createUpdateJarsSection(Composite container) {
		updateRadio =
			createRadioButton(
				container,
				PDEPlugin.getResourceString("ExportWizard.Plugin.updateJars"));

		directoryLabel = new Label(container, SWT.NULL);
		directoryLabel.setText(PDEPlugin.getResourceString("ExportWizard.destination"));
		GridData gd = new GridData();
		gd.horizontalIndent = 25;
		directoryLabel.setLayoutData(gd);

		destination = new Combo(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		destination.setLayoutData(gd);
		browseDirectory = new Button(container, SWT.PUSH);
		browseDirectory.setText(PDEPlugin.getResourceString("ExportWizard.browse"));
		browseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(browseDirectory);
	}

	
	protected void enableZipSection(boolean enabled) {
		label.setEnabled(enabled);
		zipFile.setEnabled(enabled);
		browseFile.setEnabled(enabled);
		includeSource.setEnabled(enabled);		
	}
	
	protected void enableUpdateJarsSection(boolean enabled) {
		directoryLabel.setEnabled(enabled);
		destination.setEnabled(enabled);
		browseDirectory.setEnabled(enabled);		
	}
	
	protected void hookListeners() {
		browseFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doBrowseFile();
			}
		});
		
		zipFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		zipFile.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
			}
		});

		super.hookListeners();
	}

	private void doBrowseFile() {
		IPath path = chooseFile();
		if (path != null) {
			zipFile.setText(path.toOSString());
		}
	}

	private IPath chooseFile() {
		FileDialog dialog = new FileDialog(getShell());
		dialog.setFileName(zipFile.getText());
		dialog.setFilterExtensions(new String[] {"*.zip"});
		dialog.setText(PDEPlugin.getResourceString("ExportWizard.filedialog.title"));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}
	
	protected void pageChanged() {
		boolean hasDestination = false;
		String message = null;
		if (zipRadio != null && !zipRadio.isDisposed() && zipRadio.getSelection()) {
			hasDestination = zipFile.getText().length() > 0;
			if (!hasDestination)
				message = PDEPlugin.getResourceString("ExportWizard.status.nofile");
		} else {
			hasDestination = getDestination().length() > 0;
			if (!hasDestination)
				message = PDEPlugin.getResourceString("ExportWizard.status.nodirectory");
		}
		
		boolean hasSel = exportPart.getSelectionCount() > 0;
		if (!hasSel) {
			message = PDEPlugin.getResourceString("ExportWizard.status.noselection");
		}
		setMessage(message);
		setPageComplete(hasSel && hasDestination);
	}
	
	public String getFileName() {
		if (zipRadio.getSelection()) {
			String path = zipFile.getText();
			if (path != null && path.length() > 0) {
				String fileName = new Path(path).lastSegment();
				if (!fileName.endsWith(".zip")) {
					fileName += ".zip";
				}
				return fileName;
			}
		}
		return null;
	}
	
	public String getDestination() {
		if (zipRadio != null && zipRadio.getSelection()) {
			String path = zipFile.getText();
			if (path != null && path.length() > 0) {
				return new Path(path).removeLastSegments(1).toOSString();
			}
			return "";
		}
		return super.getDestination();
	}
		
}
