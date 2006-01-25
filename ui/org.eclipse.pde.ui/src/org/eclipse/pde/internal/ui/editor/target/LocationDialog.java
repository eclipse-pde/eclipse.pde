package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class LocationDialog extends StatusDialog {
	
	private Text fPath;
	private ITarget fTarget;
	private IAdditionalLocation fLocation;
	private IStatus fOkStatus;
	private IStatus fErrorStatus;

	public LocationDialog(Shell parent, ITarget target, IAdditionalLocation location) {
		super(parent);
		fTarget = target;
		fLocation = location;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 5;
		layout.marginHeight = layout.marginWidth = 10;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);
		
		createEntries(container);

		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		};
		fPath.addModifyListener(listener);
		setTitle(PDEUIMessages.SiteEditor_NewArchiveDialog_title); 
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.NEW_ARCHIVE_DIALOG);
		
		dialogChanged();
		
		return container;
	}
	
	protected void createEntries(Composite container) {
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEUIMessages.SiteEditor_NewArchiveDialog_path); 
		label.setLayoutData(new GridData());
		
		fPath = new Text(container, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fPath.setLayoutData(gd);
		
		if (fLocation != null) {
			fPath.setText(fLocation.getPath());
		}
		
		Button fs = new Button(container, SWT.PUSH);
		fs.setText(PDEUIMessages.LocationDialog_fileSystem);
		fs.setLayoutData(new GridData());
		fs.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowseFileSystem();
			}
		});
		
		Button var = new Button(container, SWT.PUSH);
		var.setText(PDEUIMessages.LocationDialog_variables);
		var.setLayoutData(new GridData());
		var.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleInsertVariable();
			}
		});
	}
	
	private IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK,
				message, null);
	}
	
	private void dialogChanged() {
		IStatus status = null;
		if (fPath.getText().length() == 0)
			status = getEmptyErrorStatus();
		else {
			if (hasPath(fPath.getText()))
				status = createErrorStatus(PDEUIMessages.LocationDialog_locationExists); 
		}
		if (status == null)
			status = getOKStatus();
		updateStatus(status);
	}
	
	private IStatus getOKStatus() {
		if (fOkStatus == null)
			fOkStatus = new Status(IStatus.OK, PDEPlugin.getPluginId(),
					IStatus.OK, "", //$NON-NLS-1$
					null);
		return fOkStatus;
	}
	
	private IStatus getEmptyErrorStatus() {
		if (fErrorStatus == null)
			fErrorStatus = createErrorStatus(PDEUIMessages.LocationDialog_emptyPath); 
		return fErrorStatus;
	}
	
	protected boolean hasPath(String path) {
		Path checkPath = new Path(path);
		Path currentPath = (fLocation != null) ? new Path(fLocation.getPath()) : null;
		if (currentPath != null && ExternalModelManager.arePathsEqual(checkPath, currentPath))
			return false;
		IAdditionalLocation[] locs = fTarget.getAdditionalDirectories();
		for (int i = 0; i < locs.length; i++) {
			if (ExternalModelManager.arePathsEqual(new Path(locs[i].getPath()), checkPath))
				return true;
		}
		return isTargetLocation(checkPath);
	}
	
	private boolean isTargetLocation(Path path) {
		ILocationInfo info = fTarget.getLocationInfo();
		if (info.useDefault()) {
			Path home = new Path(ExternalModelManager.computeDefaultPlatformPath());
			return ExternalModelManager.arePathsEqual(home, path);
		} 
		return ExternalModelManager.arePathsEqual(new Path(info.getPath()) , path);
	}
	
	protected void handleBrowseFileSystem() {
		DirectoryDialog dialog = new DirectoryDialog(PDEPlugin.getActiveWorkbenchShell());
		dialog.setFilterPath(fPath.getText());
		dialog.setText(PDEUIMessages.BaseBlock_dirSelection); 
		dialog.setMessage(PDEUIMessages.BaseBlock_dirChoose); 
		String result = dialog.open();
		if (result != null) {
			fPath.setText(result);
		}
	}
	
	private void handleInsertVariable() {
		StringVariableSelectionDialog dialog = 
					new StringVariableSelectionDialog(PDEPlugin.getActiveWorkbenchShell());
		if (dialog.open() == StringVariableSelectionDialog.OK) {
			fPath.insert(dialog.getVariableExpression());
		}
	}
	
	protected void okPressed() {
		boolean add = fLocation == null;
		if (add) {
			fLocation = fTarget.getModel().getFactory().createAdditionalLocation();
		}
		fLocation.setPath(fPath.getText());
		if (add) 
			fTarget.addAdditionalDirectories(new IAdditionalLocation[]{fLocation});
		super.okPressed();
	}
}
