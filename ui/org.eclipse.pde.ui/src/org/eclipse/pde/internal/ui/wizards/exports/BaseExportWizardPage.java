/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;


public abstract class BaseExportWizardPage extends WizardPage {
	private static final String S_JAR_FORMAT = "exportUpdate"; //$NON-NLS-1$
	private static final String S_EXPORT_DIRECTORY = "exportDirectory";	 //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE="exportSource"; //$NON-NLS-1$
	private static final String S_DESTINATION = "destination"; //$NON-NLS-1$
	private static final String S_ZIP_FILENAME = "zipFileName"; //$NON-NLS-1$
	private static final String S_SAVE_AS_ANT = "saveAsAnt"; //$NON-NLS-1$
	private static final String S_ANT_FILENAME = "antFileName"; //$NON-NLS-1$
		
	private IStructuredSelection fSelection;

	protected ExportPart fExportPart;
	protected boolean fIsFeatureExport;
	
	private Button fDirectoryButton;
	private Combo fDirectoryCombo;
	private Button fBrowseDirectory;
	
	private Button fArchiveFileButton;
	private Combo fArchiveCombo;
	private Button fBrowseFile;
	
	private Button fIncludeSource;

	private Combo fAntCombo;
	private Button fBrowseAnt;
	private Button fSaveAsAntButton;
	private String fZipExtension = TargetPlatform.getOS().equals("win32") ? ".zip" : ".tar.gz"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private Button fJarButton;

	
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
		container.setLayout(new GridLayout());
		
		createTableViewerSection(container);
		createExportDestinationSection(container);
		createOptionsSection(container);
		
		Dialog.applyDialogFont(container);
		initializeList();
		
		// load settings
		IDialogSettings settings = getDialogSettings();	
		initializeExportOptions(settings);
		initializeDestinationSection(settings);
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
		gd.heightHint = 150;
		gd.widthHint = 150;
		gd.horizontalSpan = 2;		
	}
	
	private void createExportDestinationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ExportWizard.destination")); //$NON-NLS-1$
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fArchiveFileButton = new Button(group, SWT.RADIO);
		fArchiveFileButton.setText(PDEPlugin.getResourceString(PDEPlugin.getResourceString("ExportWizard.archive"))); //$NON-NLS-1$
		
		fArchiveCombo = new Combo(group, SWT.BORDER);
		fArchiveCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseFile = new Button(group, SWT.PUSH);
		fBrowseFile.setText(PDEPlugin.getResourceString("ExportWizard.browse")); //$NON-NLS-1$
		fBrowseFile.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseFile);		

		fDirectoryButton = new Button(group, SWT.RADIO);
		fDirectoryButton.setText(PDEPlugin.getResourceString("ExportWizard.directory")); //$NON-NLS-1$

		fDirectoryCombo = new Combo(group, SWT.BORDER);
		fDirectoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseDirectory = new Button(group, SWT.PUSH);
		fBrowseDirectory.setText(PDEPlugin.getResourceString("ExportWizard.browse")); //$NON-NLS-1$
		fBrowseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseDirectory);
	}
	
	protected Composite createOptionsSection(Composite parent) {
		Group comp = new Group(parent, SWT.NONE);
		comp.setText(PDEPlugin.getResourceString("ExportWizard.options")); //$NON-NLS-1$
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
									
		fIncludeSource = new Button(comp, SWT.CHECK);
		fIncludeSource.setText(PDEPlugin.getResourceString("ExportWizard.includeSource")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fIncludeSource.setLayoutData(gd);
		
		fJarButton = new Button(comp, SWT.CHECK);
		fJarButton.setText(PDEPlugin.getResourceString("BaseExportWizardPage.packageJARs")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		fJarButton.setLayoutData(gd);
		fJarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getContainer().updateButtons();
			}
		});

		fSaveAsAntButton = new Button(comp, SWT.CHECK);
		fSaveAsAntButton.setText(PDEPlugin.getResourceString("ExportWizard.antCheck")); //$NON-NLS-1$
		
		fAntCombo = new Combo(comp, SWT.NONE);
		fAntCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseAnt = new Button(comp, SWT.PUSH);
		fBrowseAnt.setText(PDEPlugin.getResourceString("ExportWizard.browse2")); //$NON-NLS-1$
		fBrowseAnt.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseAnt);
		
		return comp;
	}
	
	private void toggleDestinationGroup(boolean useDirectory) {
		fArchiveCombo.setEnabled(!useDirectory);
		fBrowseFile.setEnabled(!useDirectory);
		fDirectoryCombo.setEnabled(useDirectory);
		fBrowseDirectory.setEnabled(useDirectory);
	}
	
	private void hookListeners() {
		fArchiveFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = fArchiveFileButton.getSelection();
				fArchiveCombo.setEnabled(enabled);
				fBrowseFile.setEnabled(enabled);
				fDirectoryCombo.setEnabled(!enabled);
				fBrowseDirectory.setEnabled(!enabled);
				pageChanged();
			}}
		);
				
		fSaveAsAntButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
				fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
				pageChanged();
			}}
		);
		
		fBrowseFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseFile(fArchiveCombo, "*" + fZipExtension); //$NON-NLS-1$
			}
		});
		
		fArchiveCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		fArchiveCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
			}
		});
		
		fDirectoryCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		fDirectoryCombo.addModifyListener(new ModifyListener() {
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
		dialog.setFileName(fArchiveCombo.getText());
		dialog.setFilterExtensions(new String[] {filter});
		String res = dialog.open();
		if (res != null) {
			if (combo.indexOf(res) == -1)
				combo.add(res, 0);
			combo.setText(res);
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
		dialog.setFilterPath(fDirectoryCombo.getText());
		dialog.setText(PDEPlugin.getResourceString("ExportWizard.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("ExportWizard.dialog.message")); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			if (fDirectoryCombo.indexOf(res) == -1)
				fDirectoryCombo.add(res, 0);
			fDirectoryCombo.setText(res);
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

	protected abstract IModel findModelFor(IAdaptable object);

	private void pageChanged() {
		String message = null;
		if (isButtonSelected(fArchiveFileButton) && fArchiveCombo.getText().trim().length() == 0) {
			message = PDEPlugin.getResourceString("ExportWizard.status.nofile"); //$NON-NLS-1$
		}
		if (isButtonSelected(fDirectoryButton) && fDirectoryCombo.getText().trim().length() == 0) {
			message = PDEPlugin.getResourceString("ExportWizard.status.nodirectory"); //$NON-NLS-1$
		}
		if (isButtonSelected(fSaveAsAntButton) && fAntCombo.getText().trim().length() == 0) {
			message = PDEPlugin.getResourceString("ExportWizard.status.noantfile"); //$NON-NLS-1$
		}		
		
		boolean hasSel = fExportPart.getSelectionCount() > 0;
		if (!hasSel) {
			message = PDEPlugin.getResourceString("ExportWizard.status.noselection"); //$NON-NLS-1$
		}
		setMessage(message);
		setPageComplete(hasSel && message == null);
	}
	
	private boolean isButtonSelected(Button button) {
		return button != null && !button.isDisposed() && button.getSelection();
	}

	private void initializeExportOptions(IDialogSettings settings) {		
		fIncludeSource.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
		fJarButton.setSelection(settings.getBoolean(S_JAR_FORMAT));
		fSaveAsAntButton.setSelection(settings.getBoolean(S_SAVE_AS_ANT));
		initializeCombo(settings, S_ANT_FILENAME, fAntCombo);
		fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
		fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
	}
	
	private void initializeDestinationSection(IDialogSettings settings) {
		boolean useDirectory = settings.getBoolean(S_EXPORT_DIRECTORY);
		fDirectoryButton.setSelection(useDirectory);	
		fArchiveFileButton.setSelection(!useDirectory);
		toggleDestinationGroup(useDirectory);
		initializeCombo(settings, S_DESTINATION, fDirectoryCombo);
		initializeCombo(settings, S_ZIP_FILENAME, fArchiveCombo);
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
		settings.put(S_JAR_FORMAT, fJarButton.getSelection());
		settings.put(S_EXPORT_DIRECTORY, fDirectoryButton.getSelection());		
		settings.put(S_EXPORT_SOURCE, fIncludeSource.getSelection());
		settings.put(S_SAVE_AS_ANT, fSaveAsAntButton.getSelection());
		
		saveCombo(settings, S_DESTINATION, fDirectoryCombo);
		saveCombo(settings, S_ZIP_FILENAME, fArchiveCombo);
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

	public boolean doExportSource() {
		return fIncludeSource.getSelection();
	}
	
	public boolean doExportToDirectory() {
		return fDirectoryButton.getSelection();
	}
	
	public boolean useJARFormat() {
		return fJarButton.getSelection();
	}
	
	public String getFileName() {
		if (fArchiveFileButton.getSelection()) {
			String path = fArchiveCombo.getText();
			if (path != null && path.length() > 0) {
				String fileName = new Path(path).lastSegment();
				if (!fileName.endsWith(fZipExtension)) { 
					fileName += fZipExtension;
				}
				return fileName;
			}
		}
		return null;
	}
	
	public String getDestination() {
		if (fArchiveFileButton.getSelection()) {
			String path = fArchiveCombo.getText();
			if (path != null && path.length() > 0) {
				path = new Path(path).removeLastSegments(1).toOSString();
				return new File(path).getAbsolutePath();
			}
			return ""; //$NON-NLS-1$
		}
		
		if (fDirectoryCombo == null || fDirectoryCombo.isDisposed())
			return ""; //$NON-NLS-1$
		
		File dir = new File(fDirectoryCombo.getText().trim());			
		return dir.getAbsolutePath();
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
					IModel model = findModelFor(elements[j]);
					if (isValidModel(model)) {
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
	
	public IWizardPage getNextPage() {
		return (fJarButton.getSelection()) ? super.getNextPage() : null;
	}
	
}
