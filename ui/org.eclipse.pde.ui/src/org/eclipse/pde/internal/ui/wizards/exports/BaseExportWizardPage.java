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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


public abstract class BaseExportWizardPage extends WizardPage {
	private String S_EXPORT_UPDATE = "exportUpdate";
	private String S_DESTINATION = "destination";
	private String S_EXPORT_SOURCE="exportSource";
	private String S_ZIP_FILENAME = "zipFileName";
		
	private IStructuredSelection selection;
	private Combo destination;
	private Combo zipFile;

	protected ExportPart exportPart;
	protected boolean featureExport;
	private Button zipRadio;
	private Button updateRadio;
	private Button browseDirectory;
	private Button includeSource;

	private Label directoryLabel;
	private Button browseFile;
	private Label label;
	
	class ExportListProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getListElements();
		}
	}

	class ExportPart extends WizardCheckboxTablePart {
		public ExportPart(String label) {
			super(label);
		}

		public void updateCounter(int count) {
			super.updateCounter(count);
			pageChanged();
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
		exportPart = new ExportPart(choiceLabel);
		setDescription(PDEPlugin.getResourceString("ExportWizard.Plugin.description"));
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		
		createTableViewerSection(container);
		createExportSection(container);
		createOptionsSection(container);
		
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
		gd.heightHint = 100;
		gd.widthHint = 150;
		gd.horizontalSpan = 2;		
	}
	
	private void createExportSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(
			PDEPlugin.getResourceString(
				featureExport
					? "ExportWizard.Feature.label"
					: "ExportWizard.Plugin.label"));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createZipSection(group);
		createUpdateJarsSection(group);		
	}
	
	private void createOptionsSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEPlugin.getResourceString("ExportWizard.buildOptions.title"));
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = new Label(group, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ExportWizard.buildOptions.label"));
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button button = new Button(group, SWT.PUSH);
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
	
	private void createZipSection(Composite container) {
		zipRadio =
			createButton(
				container,
				PDEPlugin.getResourceString("ExportWizard.Plugin.zip"),
				SWT.RADIO,
				GridData.BEGINNING);
												
		label = new Label(container, SWT.NONE);
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

	private void createUpdateJarsSection(Composite container) {
		updateRadio =
			createButton(
				container,
				PDEPlugin.getResourceString("ExportWizard.Plugin.updateJars"),
				SWT.RADIO,
				GridData.BEGINNING);

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
				enableZipSection(!enabled);
				enableUpdateJarsSection(enabled);
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
	
	private void enableZipSection(boolean enabled) {
		label.setEnabled(enabled);
		zipFile.setEnabled(enabled);
		browseFile.setEnabled(enabled);
		includeSource.setEnabled(enabled);		
	}
	
	private void enableUpdateJarsSection(boolean enabled) {
		directoryLabel.setEnabled(enabled);
		destination.setEnabled(enabled);
		browseDirectory.setEnabled(enabled);		
	}

	private Button createButton(Composite container, String text, int style, int align) {
		Button button = new Button(container, style);
		button.setText(text);
		GridData gd = new GridData(align);
		gd.horizontalSpan = 3;
		gd.horizontalIndent = 0;
		button.setLayoutData(gd);
		return button;
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

	private void loadSettings() {
		IDialogSettings settings = getDialogSettings();
		boolean exportUpdate = settings.getBoolean(S_EXPORT_UPDATE);
		zipRadio.setSelection(!exportUpdate);
		updateRadio.setSelection(exportUpdate);
		enableZipSection(!updateRadio.getSelection());

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

		includeSource.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
		enableUpdateJarsSection(!zipRadio.getSelection());
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
	}

	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(S_EXPORT_UPDATE, updateRadio.getSelection());
		
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
	
	protected abstract void hookHelpContext(Control control);
}
