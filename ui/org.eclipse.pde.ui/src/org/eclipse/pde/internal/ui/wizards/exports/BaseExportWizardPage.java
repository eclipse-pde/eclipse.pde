/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.preferences.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;


public abstract class BaseExportWizardPage extends WizardPage {
	private String S_EXPORT_UPDATE = "exportUpdate";
	private String S_EXPORT_DIRECTORY = "exportDirectory";	
	private String S_DESTINATION = "destination";
	private String S_EXPORT_SOURCE="exportSource";
	private String S_ZIP_FILENAME = "zipFileName";
		
	private IStructuredSelection selection;

	protected ExportPart exportPart;
	protected boolean featureExport;
	
	private Button directoryRadio;
	private Button zipRadio;
	private Button updateRadio;

	private Label directoryLabel;
	private Combo destination;
	private Button browseDirectory;
	
	private Label fileLabel;
	private Combo zipFile;
	private Button browseFile;
	
	private Button includeSource;
	
	class ExportListProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getListElements();
		}
	}

	class ExportPart extends WizardCheckboxTablePart {
		public ExportPart(String label, String[] buttonLabels) {
			super(label, buttonLabels);
		}

		public void updateCounter(int count) {
			super.updateCounter(count);
			pageChanged();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart#buttonSelected(org.eclipse.swt.widgets.Button, int)
		 */
		protected void buttonSelected(Button button, int index) {
			switch (index) {
				case 0:
					handleSelectAll(true);
					break;
				case 1:
					handleSelectAll(false);
					break;
				case 3:
					handleWorkingSets();
			}
		}
	}

	public BaseExportWizardPage(
		IStructuredSelection selection,
		String name,
		String choiceLabel,
		boolean featureExport) {
		super(name);
		this.selection = selection;
		this.featureExport = featureExport;
		exportPart =
			new ExportPart(
				choiceLabel,
				new String[] {
					PDEPlugin.getResourceString(ExportPart.KEY_SELECT_ALL),
					PDEPlugin.getResourceString(ExportPart.KEY_DESELECT_ALL),
					null,
					PDEPlugin.getResourceString("ExportWizard.workingSet") });
		setDescription(PDEPlugin.getResourceString("ExportWizard.Plugin.description"));
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		
		createTableViewerSection(container);
		createExportTypeSection(container);
		createOptionsSection(container);
		createExportDestinationSection(container);
		
		Dialog.applyDialogFont(container);
		initializeList();
		loadSettings();
		pageChanged();
		hookListeners();
		setControl(container);
		hookHelpContext(container);
	}
	
	private void createTableViewerSection(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		exportPart.createControl(composite);
		GridData gd = (GridData) exportPart.getControl().getLayoutData();
		gd.heightHint = 125;
		gd.widthHint = 150;
		gd.horizontalSpan = 2;		
	}
	
	private void createExportTypeSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(
			PDEPlugin.getResourceString(
				featureExport
					? "ExportWizard.Feature.label"
					: "ExportWizard.Plugin.label"));
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		zipRadio = new Button(group, SWT.RADIO);
		zipRadio.setText(PDEPlugin.getResourceString("ExportWizard.zip"));

		directoryRadio = new Button(group, SWT.RADIO);
		directoryRadio.setText(PDEPlugin.getResourceString("ExportWizard.directory"));

		updateRadio = new Button(group, SWT.RADIO);
		updateRadio.setText(PDEPlugin.getResourceString("ExportWizard.updateJars"));
	}
	
	private void createExportDestinationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ExportWizard.destination.group"));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fileLabel = new Label(group, SWT.NONE);
		fileLabel.setText(PDEPlugin.getResourceString("ExportWizard.zipFile"));
		
		zipFile = new Combo(group, SWT.BORDER);
		zipFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		browseFile = new Button(group, SWT.PUSH);
		browseFile.setText(PDEPlugin.getResourceString("ExportWizard.browse"));
		browseFile.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(browseFile);		

		directoryLabel = new Label(group, SWT.NULL);
		directoryLabel.setText(PDEPlugin.getResourceString("ExportWizard.destination"));

		destination = new Combo(group, SWT.BORDER);
		destination.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		browseDirectory = new Button(group, SWT.PUSH);
		browseDirectory.setText(PDEPlugin.getResourceString("ExportWizard.browse"));
		browseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(browseDirectory);
	}
	
	private void createOptionsSection(Composite parent) {
		Group comp = new Group(parent, SWT.NONE);
		comp.setText(PDEPlugin.getResourceString("ExportWizard.options"));
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		includeSource = new Button(comp, SWT.CHECK);
		includeSource.setText(PDEPlugin.getResourceString("ExportWizard.includeSource"));
		includeSource.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		Button button = new Button(comp, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ExportWizard.buildOptions.button"));
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showPreferencePage(new BuildOptionsPreferenceNode());
			}
		});
		SWTUtil.setButtonDimensionHint(button);
	}
	
	private void showPreferencePage(final IPreferenceNode targetNode) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog =
			new PreferenceDialog(getControl().getShell(), manager);
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				dialog.open();
			}
		});
	}
	
	private void hookListeners() {
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
		
		destination.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		destination.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
			}
		});
		
		browseDirectory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doBrowseDirectory();
			}
		});
		
		updateRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = updateRadio.getSelection();
				enableZipOption(!enabled);
				enableDirectoryOption(enabled);
				if (includeSource.getEnabled() == enabled)
					includeSource.setEnabled(!enabled);
				pageChanged();
			}
		});
		
		directoryRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = directoryRadio.getSelection();
				enableZipOption(!enabled);
				enableDirectoryOption(enabled);
				if (includeSource.getEnabled() != enabled)
					includeSource.setEnabled(enabled);
				pageChanged();
			}
		});
		
		zipRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = zipRadio.getSelection();
				enableZipOption(enabled);
				enableDirectoryOption(!enabled);
				if (includeSource.getEnabled() != enabled)
					includeSource.setEnabled(enabled);
				pageChanged();
			}
		});

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
	
	private void enableZipOption(boolean enabled) {
		if (fileLabel.getEnabled() != enabled) {
			fileLabel.setEnabled(enabled);
			zipFile.setEnabled(enabled);
			browseFile.setEnabled(enabled);
		}
	}
	
	private void enableDirectoryOption(boolean enabled) {
		if (directoryLabel.getEnabled() != enabled) {
			directoryLabel.setEnabled(enabled);
			destination.setEnabled(enabled);
			browseDirectory.setEnabled(enabled);
		}		
	}

	protected abstract Object[] getListElements();

	private void initializeList() {
		TableViewer viewer = exportPart.getTableViewer();
		viewer.setContentProvider(new ExportListProvider());
		viewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		viewer.setSorter(ListUtil.PLUGIN_SORTER);
		exportPart.getTableViewer().setInput(
			PDECore.getDefault().getWorkspaceModelManager());
		checkSelected();
	}

	private void doBrowseDirectory() {
		IPath result = chooseDestination();
		if (result != null) {
			destination.setText(result.toOSString());
		}
	}
	
	private IPath chooseDestination() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(destination.getText());
		dialog.setText(PDEPlugin.getResourceString("ExportWizard.dialog.title"));
		dialog.setMessage(PDEPlugin.getResourceString("ExportWizard.dialog.message"));
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	protected void checkSelected() {
		Object[] elems = selection.toArray();
		ArrayList checked = new ArrayList(elems.length);

		for (int i = 0; i < elems.length; i++) {
			Object elem = elems[i];
			IProject project = null;

			if (elem instanceof IFile) {
				IFile file = (IFile) elem;
				project = file.getProject();
			} else if (elem instanceof IProject) {
				project = (IProject) elem;
			} else if (elem instanceof IJavaProject) {
				project = ((IJavaProject) elem).getProject();
			}
			if (project != null) {
				IModel model = findModelFor(project);
				if (model != null && !checked.contains(model)) {
					checked.add(model);
				}
			}
		}
		exportPart.setSelection(checked.toArray());
		if (checked.size() > 0)
			exportPart.getTableViewer().reveal(checked.get(0));
	}

	private IModel findModelFor(IProject project) {
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		return manager.getWorkspaceModel(project);
	}

	private void pageChanged() {
		boolean hasDestination = false;
		String message = null;
		if (zipRadio != null && !zipRadio.isDisposed() && zipRadio.getSelection()) {
			hasDestination = zipFile.getText().length() > 0;
			if (!hasDestination)
				message = PDEPlugin.getResourceString("ExportWizard.status.nofile");
		} else if (destination != null && !destination.isDisposed()){
			hasDestination = destination.getText().length() > 0;
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

	private void loadSettings() {
		IDialogSettings settings = getDialogSettings();
		
		// initialize the radio buttons
		boolean exportUpdate = settings.getBoolean(S_EXPORT_UPDATE);
		boolean exportDirectory = settings.getBoolean(S_EXPORT_DIRECTORY);		
		zipRadio.setSelection(!exportUpdate && !exportDirectory);
		updateRadio.setSelection(exportUpdate);
		directoryRadio.setSelection(exportDirectory);
		
		// enable/disable portions of the destination section
		enableZipOption(!exportUpdate && !exportDirectory);
		enableDirectoryOption(!zipRadio.getSelection());

		// initialize the directory combo box
		ArrayList items = new ArrayList();
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(S_DESTINATION + String.valueOf(i));
			if (curr != null && !items.contains(curr)) {
				items.add(curr);
			}
		}
		destination.setItems((String[]) items.toArray(new String[items.size()]));
		if (items.size() > 0)
			destination.setText(items.get(0).toString());
			
		// initialize the file combo box
		items.clear();
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(S_ZIP_FILENAME + String.valueOf(i));
			if (curr != null && !items.contains(curr)) {
				items.add(curr);
			}
		}
		zipFile.setItems((String[]) items.toArray(new String[items.size()]));
		if (items.size() > 0)
			zipFile.setText(items.get(0).toString());
			
		// initialize the options section
		includeSource.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
		includeSource.setEnabled(!updateRadio.getSelection());
	}

	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(S_EXPORT_UPDATE, updateRadio.getSelection());
		settings.put(S_EXPORT_DIRECTORY, directoryRadio.getSelection());
		
		settings.put(S_EXPORT_SOURCE, includeSource.getSelection());
			
		if (destination.getText().length() > 0) {
			settings.put(S_DESTINATION + String.valueOf(0), destination.getText());
			String[] items = destination.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(S_DESTINATION + String.valueOf(i + 1), items[i]);
			}
		}
		if (zipFile.getText().length() > 0) {
			settings.put(S_ZIP_FILENAME + String.valueOf(0), zipFile.getText());
			String[] items = zipFile.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(S_ZIP_FILENAME + String.valueOf(i+1), items[i]);
			}
		}
	}

	public Object[] getSelectedItems() {
		return exportPart.getSelection();
	}

	public boolean getExportZip() {
		return zipRadio.getSelection();
	}
	
	public boolean getExportSource() {
		if (includeSource == null)
			return false;
		return includeSource.getSelection();
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
		
		if (destination == null || destination.isDisposed())
			return "";
			
		return destination.getText();
	}
	
	public int getExportType() {
		if (directoryRadio.getSelection())
			return BaseExportJob.EXPORT_AS_DIRECTORY;
		if (zipRadio.getSelection())
			return BaseExportJob.EXPORT_AS_ZIP;
		return BaseExportJob.EXPORT_AS_UPDATE_JARS;
	}
	
	protected abstract void hookHelpContext(Control control);
	
	private void handleWorkingSets() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(getShell(), true);
		if (dialog.open() == Window.OK) {
			ArrayList models = new ArrayList();
			WorkspaceModelManager wManager = PDECore.getDefault().getWorkspaceModelManager();
			IWorkingSet[] workingSets = dialog.getSelection();
			for (int i = 0; i < workingSets.length; i++) {
				IAdaptable[] elements = workingSets[i].getElements();
				for (int j = 0; j < elements.length; j++) {
					IAdaptable element = elements[j];
					if (element instanceof IJavaProject)
						element = ((IJavaProject)element).getProject();
					if (element instanceof IProject) {
						IModel model = wManager.getWorkspaceModel((IProject)element);
						if (isValidModel(model))
							models.add(model);						
					}
				}
			}
			exportPart.setSelection(models.toArray());
		}
	}
	
	protected abstract boolean isValidModel(IModel model);
}
