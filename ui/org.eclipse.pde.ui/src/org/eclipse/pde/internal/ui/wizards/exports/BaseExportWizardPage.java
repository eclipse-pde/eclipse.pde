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

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.preferences.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;


public abstract class BaseExportWizardPage extends WizardPage {
	private String S_EXPORT_UPDATE = "exportUpdate"; //$NON-NLS-1$
	private String S_EXPORT_DIRECTORY = "exportDirectory";	 //$NON-NLS-1$
	private String S_EXPORT_SOURCE="exportSource"; //$NON-NLS-1$
	private String S_DESTINATION = "destination"; //$NON-NLS-1$
	private String S_ZIP_FILENAME = "zipFileName"; //$NON-NLS-1$
	private String S_SAVE_AS_ANT = "saveAsAnt"; //$NON-NLS-1$
	private String S_ANT_FILENAME = "antFileName"; //$NON-NLS-1$
		
	private IStructuredSelection fSelection;

	protected ExportPart fExportPart;
	protected boolean fIsFeatureExport;
	
	private Label fDirectoryLabel;
	private Combo fDestination;
	private Button fBrowseDirectory;
	
	private Label fFileLabel;
	private Combo fZipFile;
	private Button fBrowseFile;
	
	private Button fIncludeSource;
	private Combo fExportFormats;
	private Label fAntLabel;
	private Combo fAntCombo;
	private Button fBrowseAnt;
	private Button fSaveAsAntButton;

	
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
		this.fSelection = selection;
		this.fIsFeatureExport = featureExport;
		fExportPart =
			new ExportPart(
				choiceLabel,
				new String[] {
					PDEPlugin.getResourceString(ExportPart.KEY_SELECT_ALL),
					PDEPlugin.getResourceString(ExportPart.KEY_DESELECT_ALL),
					null,
					PDEPlugin.getResourceString("ExportWizard.workingSet") }); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("ExportWizard.Plugin.description")); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		
		createTableViewerSection(container);
		createOptionsSection(container);
		createExportDestinationSection(container);
		createAntBuildSection(container);
		
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
		
		fExportPart.createControl(composite);
		GridData gd = (GridData) fExportPart.getControl().getLayoutData();
		gd.heightHint = 125;
		gd.widthHint = 150;
		gd.horizontalSpan = 2;		
	}
	
	private void createExportDestinationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ExportWizard.destination.group")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fFileLabel = new Label(group, SWT.NONE);
		fFileLabel.setText(PDEPlugin.getResourceString("ExportWizard.zipFile")); //$NON-NLS-1$
		
		fZipFile = new Combo(group, SWT.BORDER);
		fZipFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseFile = new Button(group, SWT.PUSH);
		fBrowseFile.setText(PDEPlugin.getResourceString("ExportWizard.browse")); //$NON-NLS-1$
		fBrowseFile.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseFile);		

		fDirectoryLabel = new Label(group, SWT.NULL);
		fDirectoryLabel.setText(PDEPlugin.getResourceString("ExportWizard.destination")); //$NON-NLS-1$

		fDestination = new Combo(group, SWT.BORDER);
		fDestination.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseDirectory = new Button(group, SWT.PUSH);
		fBrowseDirectory.setText(PDEPlugin.getResourceString("ExportWizard.browse")); //$NON-NLS-1$
		fBrowseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseDirectory);
	}
	
	private void createOptionsSection(Composite parent) {
		Group comp = new Group(parent, SWT.NONE);
		comp.setText(PDEPlugin.getResourceString("ExportWizard.options")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = new Label(comp, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ExportWizard.format")); //$NON-NLS-1$
		
		fExportFormats = new Combo(comp, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fExportFormats.setLayoutData(gd);
		fExportFormats.setItems(new String[]{
				PDEPlugin.getResourceString("ExportWizard.zip"), //$NON-NLS-1$
				PDEPlugin.getResourceString("ExportWizard.directory"), //$NON-NLS-1$
				PDEPlugin.getResourceString("ExportWizard.updateJars")}); //$NON-NLS-1$
		
		fIncludeSource = new Button(comp, SWT.CHECK);
		fIncludeSource.setText(PDEPlugin.getResourceString("ExportWizard.includeSource")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fIncludeSource.setLayoutData(gd);
				
		Button button = new Button(comp, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ExportWizard.buildOptions.button")); //$NON-NLS-1$
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showPreferencePage(new BuildOptionsPreferenceNode());
			}
		});
		SWTUtil.setButtonDimensionHint(button);		
	}
	
	private void createAntBuildSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ExportWizard.antTitle")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fSaveAsAntButton = new Button(group, SWT.CHECK);
		fSaveAsAntButton.setText(PDEPlugin.getResourceString("ExportWizard.antCheck")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fSaveAsAntButton.setLayoutData(gd);
		
		fAntLabel = new Label(group, SWT.NONE);
		fAntLabel.setText(PDEPlugin.getResourceString("ExportWizard.antLabel")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 20;
		fAntLabel.setLayoutData(gd);
		
		fAntCombo = new Combo(group, SWT.NONE);
		fAntCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseAnt = new Button(group, SWT.PUSH);
		fBrowseAnt.setText(PDEPlugin.getResourceString("ExportWizard.browse2")); //$NON-NLS-1$
		fBrowseAnt.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseAnt);
		
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
		fSaveAsAntButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAntLabel.setEnabled(fSaveAsAntButton.getSelection());
				fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
				fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
				pageChanged();
			}}
		);
		
		fExportFormats.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				enableZipOption(doExportAsZip());
				enableDirectoryOption(!doExportAsZip());
				fIncludeSource.setEnabled(!doExportAsUpdateJars());
				pageChanged();
			}}
		);
		
		fBrowseFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseFile(fZipFile, "*.zip"); //$NON-NLS-1$
			}
		});
		
		fZipFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		fZipFile.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
			}
		});
		
		fDestination.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		fDestination.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
			}
		});
		
		fBrowseDirectory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseDestination();
			}
		});
		
		fBrowseAnt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseFile(fAntCombo, "*.xml"); //$NON-NLS-1$
			}
		});
		
		fAntCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		fAntCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
			}
		});		
	}

	private void chooseFile(Combo combo, String filter) {
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setFileName(fZipFile.getText());
		dialog.setFilterExtensions(new String[] {filter});
		String res = dialog.open();
		if (res != null) {
			if (combo.indexOf(res) == -1)
				combo.add(res, 0);
			combo.setText(res);
		}
	}
	
	private void enableZipOption(boolean enabled) {
		if (fFileLabel.getEnabled() != enabled) {
			fFileLabel.setEnabled(enabled);
			fZipFile.setEnabled(enabled);
			fBrowseFile.setEnabled(enabled);
		}
	}
	
	private void enableDirectoryOption(boolean enabled) {
		if (fDirectoryLabel.getEnabled() != enabled) {
			fDirectoryLabel.setEnabled(enabled);
			fDestination.setEnabled(enabled);
			fBrowseDirectory.setEnabled(enabled);
		}		
	}

	protected abstract Object[] getListElements();

	private void initializeList() {
		TableViewer viewer = fExportPart.getTableViewer();
		viewer.setContentProvider(new ExportListProvider());
		viewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		viewer.setSorter(ListUtil.PLUGIN_SORTER);
		fExportPart.getTableViewer().setInput(
			PDECore.getDefault().getWorkspaceModelManager());
		checkSelected();
	}

	private void chooseDestination() {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
		dialog.setFilterPath(fDestination.getText());
		dialog.setText(PDEPlugin.getResourceString("ExportWizard.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("ExportWizard.dialog.message")); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			if (fDestination.indexOf(res) == -1)
				fDestination.add(res, 0);
			fDestination.setText(res);
		}
	}

	protected void checkSelected() {
		Object[] elems = fSelection.toArray();
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
		fExportPart.setSelection(checked.toArray());
		if (checked.size() > 0)
			fExportPart.getTableViewer().reveal(checked.get(0));
	}

	protected abstract IModel findModelFor(IProject project);

	private void pageChanged() {
		String message = null;
		if (fSaveAsAntButton != null && !fSaveAsAntButton.isDisposed() && fSaveAsAntButton.getSelection()) {
			if (fAntCombo.getText().trim().length() == 0)
				message = PDEPlugin.getResourceString("ExportWizard.status.noantfile"); //$NON-NLS-1$
		}		
		if (fExportFormats != null && !fExportFormats.isDisposed() && doExportAsZip()) {
			if (fZipFile.getText().trim().length() == 0)
				message = PDEPlugin.getResourceString("ExportWizard.status.nofile"); //$NON-NLS-1$
		} else if (fDestination != null && !fDestination.isDisposed()){
			if (fDestination.getText().trim().length() == 0)
				message = PDEPlugin.getResourceString("ExportWizard.status.nodirectory"); //$NON-NLS-1$
		}
		
		boolean hasSel = fExportPart.getSelectionCount() > 0;
		if (!hasSel) {
			message = PDEPlugin.getResourceString("ExportWizard.status.noselection"); //$NON-NLS-1$
		}
		setMessage(message);
		setPageComplete(hasSel && message == null);
	}

	private void loadSettings() {
		IDialogSettings settings = getDialogSettings();	
		initializeExportOptions(settings);
		initializeDestinationSection(settings);
		initializeAntBuildSection(settings);	
	}
	
	private void initializeExportOptions(IDialogSettings settings) {
		boolean exportUpdate = settings.getBoolean(S_EXPORT_UPDATE);
		boolean exportDirectory = settings.getBoolean(S_EXPORT_DIRECTORY);			
		int index = FeatureExportJob.EXPORT_AS_ZIP;
		if (exportUpdate) {
			index = FeatureExportJob.EXPORT_AS_UPDATE_JARS;
		} else if (exportDirectory){
			index = FeatureExportJob.EXPORT_AS_DIRECTORY;
		} 
		fExportFormats.setText(fExportFormats.getItem(index));
		enableZipOption(!exportUpdate && !exportDirectory);
		enableDirectoryOption(exportUpdate || exportDirectory);		

		// initialize the options section
		fIncludeSource.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
		fIncludeSource.setEnabled(!doExportAsUpdateJars());	
	}
	
	private void initializeDestinationSection(IDialogSettings settings) {
		initializeCombo(settings, S_DESTINATION, fDestination);
		initializeCombo(settings, S_ZIP_FILENAME, fZipFile);
	}
	
	private void initializeAntBuildSection(IDialogSettings settings) {
		fSaveAsAntButton.setSelection(settings.getBoolean(S_SAVE_AS_ANT));
		initializeCombo(settings, S_ANT_FILENAME, fAntCombo);
		fAntLabel.setEnabled(fSaveAsAntButton.getSelection());
		fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
		fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
	}
	
	private void initializeCombo(IDialogSettings settings, String key, Combo combo) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(key + String.valueOf(i));
			if (curr != null && !list.contains(curr)) {
				list.add(curr);
			}
		}
		String[] items = (String[])list.toArray(new String[list.size()]);
		combo.setItems(items);
		if (items.length > 0)
			combo.setText(items[0]);				
	}

	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		
		settings.put(S_EXPORT_UPDATE, doExportAsUpdateJars());
		settings.put(S_EXPORT_DIRECTORY, doExportAsDirectory());		
		settings.put(S_EXPORT_SOURCE, fIncludeSource.getSelection());
		settings.put(S_SAVE_AS_ANT, fSaveAsAntButton.getSelection());
		
		saveCombo(settings, S_DESTINATION, fDestination);
		saveCombo(settings, S_ZIP_FILENAME, fZipFile);
		saveCombo(settings, S_ANT_FILENAME, fAntCombo);
	}
	
	private void saveCombo(IDialogSettings settings, String key, Combo combo) {
		if (combo.getText().trim().length() > 0) {
			settings.put(key + String.valueOf(0), combo.getText().trim());
			String[] items = combo.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(key + String.valueOf(i + 1), items[i].trim());
			}
		}	
	}

	public Object[] getSelectedItems() {
		return fExportPart.getSelection();
	}

	public boolean doExportAsZip() {
		return fExportFormats.getSelectionIndex() == FeatureExportJob.EXPORT_AS_ZIP;
	}
	
	public boolean doExportAsUpdateJars() {
		return fExportFormats.getSelectionIndex() == FeatureExportJob.EXPORT_AS_UPDATE_JARS;
	}
	
	public boolean doExportAsDirectory() {
		return fExportFormats.getSelectionIndex() == FeatureExportJob.EXPORT_AS_DIRECTORY;
	}
	
	public boolean doExportSource() {
		return (fIncludeSource != null && fIncludeSource.getSelection());
	}

	
	public String getFileName() {
		if (doExportAsZip()) {
			String path = fZipFile.getText();
			if (path != null && path.length() > 0) {
				String fileName = new Path(path).lastSegment();
				if (!fileName.endsWith(".zip")) { //$NON-NLS-1$
					fileName += ".zip"; //$NON-NLS-1$
				}
				return fileName;
			}
		}
		return null;
	}
	
	public String getDestination() {
		if (fExportFormats != null && doExportAsZip()) {
			String path = fZipFile.getText();
			if (path != null && path.length() > 0) {
				path = new Path(path).removeLastSegments(1).toOSString();
				return new File(path).getAbsolutePath();
			}
			return ""; //$NON-NLS-1$
		}
		
		if (fDestination == null || fDestination.isDisposed())
			return ""; //$NON-NLS-1$
		
		File dir = new File(fDestination.getText().trim());			
		return dir.getAbsolutePath();
	}
	
	public int getExportType() {
		return fExportFormats.getSelectionIndex();
	}
	
	protected abstract void hookHelpContext(Control control);
	
	private void handleWorkingSets() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(getShell(), true);
		if (dialog.open() == Window.OK) {
			ArrayList models = new ArrayList();
			IWorkingSet[] workingSets = dialog.getSelection();
			for (int i = 0; i < workingSets.length; i++) {
				IAdaptable[] elements = workingSets[i].getElements();
				for (int j = 0; j < elements.length; j++) {
					IAdaptable element = elements[j];
					if (element instanceof IJavaProject)
						element = ((IJavaProject)element).getProject();
					if (element instanceof IProject) {
						IModel model = findModelFor((IProject)element);
						if (isValidModel(model))
							models.add(model);						
					}
				}
			}
			fExportPart.setSelection(models.toArray());
		}
	}
	
	protected abstract boolean isValidModel(IModel model);
	
	public boolean doGenerateAntFile() {
		return fSaveAsAntButton.getSelection();
	}
	
	public String getAntBuildFileName() {
		return fAntCombo.getText().trim();
	}
	
}
