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

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;


public abstract class BaseExportWizardPage extends ExportWizardPage  {
	private static final String S_JAR_FORMAT = "exportUpdate"; //$NON-NLS-1$
	private static final String S_EXPORT_DIRECTORY = "exportDirectory";	 //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE="exportSource"; //$NON-NLS-1$
	private static final String S_DESTINATION = "destination"; //$NON-NLS-1$
	private static final String S_ZIP_FILENAME = "zipFileName"; //$NON-NLS-1$
	private static final String S_SAVE_AS_ANT = "saveAsAnt"; //$NON-NLS-1$
	private static final String S_ANT_FILENAME = "antFileName"; //$NON-NLS-1$
	private static final String S_JAVAC_TARGET = "javacTarget"; //$NON-NLS-1$
	private static final String S_JAVAC_SOURCE = "javacSource"; //$NON-NLS-1$
	
	private static String[] COMPILER_LEVELS = new String[] {"1.1", "1.2", "1.3", "1.4", "5.0"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
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
	private String fZipExtension = ".zip"; //$NON-NLS-1$
	private Button fJarButton;
	private Combo fJavacSource;
	private Combo fJavacTarget;

	
	public BaseExportWizardPage(String name) {
		super(name);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = getVerticalSpacing();
		container.setLayout(layout);
		
		createTopSection(container);
		createExportDestinationSection(container);
		createCompilerOptionsSection(container);
		createOptionsSection(container);
		
		Dialog.applyDialogFont(container);
		
		// load settings
		IDialogSettings settings = getDialogSettings();
		initializeTopSection();
		initializeCompilerOptionsSection(settings);
		initializeExportOptions(settings);
		initializeDestinationSection(settings);
		pageChanged();
		hookListeners();
		setControl(container);
		hookHelpContext(container);
	}
    
    protected int getVerticalSpacing() {
        return 5;
    }
	
	protected abstract void createTopSection(Composite parent);
	
	protected abstract void initializeTopSection();
	
	private void createCompilerOptionsSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.BaseExportWizardPage_compilerOptions);
		group.setLayout(new GridLayout(5, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.BaseExportWizardPage_javacSource);
		
		fJavacSource = new Combo(group, SWT.READ_ONLY);
		fJavacSource.setItems(COMPILER_LEVELS);
		
		label = new Label(group, SWT.NONE);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.BaseExportWizardPage_javacTarget);
		
		fJavacTarget = new Combo(group, SWT.READ_ONLY);
		fJavacTarget.setItems(COMPILER_LEVELS);
		
	}
	
	private void initializeCompilerOptionsSection(IDialogSettings settings) {
		String target = settings.get(S_JAVAC_TARGET);
		if (target == null)
			target = JavaCore.getPlugin().getPluginPreferences().getString(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
		fJavacTarget.setText(target);
		
		String source = settings.get(S_JAVAC_SOURCE);
		if (source == null)
			source = JavaCore.getPlugin().getPluginPreferences().getString(JavaCore.COMPILER_SOURCE);
		fJavacSource.setText(source);		
	}
	
	private void createExportDestinationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PDEUIMessages.ExportWizard_destination); //$NON-NLS-1$
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fArchiveFileButton = new Button(group, SWT.RADIO);
		fArchiveFileButton.setText(PDEUIMessages.ExportWizard_archive); //$NON-NLS-1$
		
		fArchiveCombo = new Combo(group, SWT.BORDER);
		fArchiveCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseFile = new Button(group, SWT.PUSH);
		fBrowseFile.setText(PDEUIMessages.ExportWizard_browse); //$NON-NLS-1$
		fBrowseFile.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseFile);		

		fDirectoryButton = new Button(group, SWT.RADIO);
		fDirectoryButton.setText(PDEUIMessages.ExportWizard_directory); //$NON-NLS-1$

		fDirectoryCombo = new Combo(group, SWT.BORDER);
		fDirectoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseDirectory = new Button(group, SWT.PUSH);
		fBrowseDirectory.setText(PDEUIMessages.ExportWizard_browse); //$NON-NLS-1$
		fBrowseDirectory.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseDirectory);
	}
	
	protected Composite createOptionsSection(Composite parent) {
		Group comp = new Group(parent, SWT.NONE);
		comp.setText(PDEUIMessages.ExportWizard_options); //$NON-NLS-1$
		comp.setLayout(new GridLayout(3, false));
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
									
		fIncludeSource = new Button(comp, SWT.CHECK);
		fIncludeSource.setText(PDEUIMessages.ExportWizard_includeSource); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fIncludeSource.setLayoutData(gd);
		
        if (addJARFormatSection()) {
    		fJarButton = new Button(comp, SWT.CHECK);
    		fJarButton.setText(getJarButtonText());
    		gd = new GridData();
    		gd.horizontalSpan = 3;
    		fJarButton.setLayoutData(gd);
    		fJarButton.addSelectionListener(new SelectionAdapter() {
    			public void widgetSelected(SelectionEvent e) {
    				getContainer().updateButtons();
    			}
    		});
        }
        
		if (addAntSection())
            createAntSection(comp);
		return comp;
	}
	
	protected void createAntSection(Composite comp) {
		fSaveAsAntButton = new Button(comp, SWT.CHECK);
		fSaveAsAntButton.setText(PDEUIMessages.ExportWizard_antCheck); //$NON-NLS-1$
		
		fAntCombo = new Combo(comp, SWT.NONE);
		fAntCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseAnt = new Button(comp, SWT.PUSH);
		fBrowseAnt.setText(PDEUIMessages.ExportWizard_browse2); //$NON-NLS-1$
		fBrowseAnt.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseAnt);		
	}
	
	protected abstract String getJarButtonText();
	
	private void toggleDestinationGroup(boolean useDirectory) {
		fArchiveCombo.setEnabled(!useDirectory);
		fBrowseFile.setEnabled(!useDirectory);
		fDirectoryCombo.setEnabled(useDirectory);
		fBrowseDirectory.setEnabled(useDirectory);
	}
	
	protected void hookListeners() {
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
		
        if (addAntSection()) {
            fSaveAsAntButton.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
                    fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
                    pageChanged();
                }}
            );
            
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
	}

	private void chooseFile(Combo combo, String filter) {
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		String path = fArchiveCombo.getText();
		if (path.trim().length() == 0)
			path = PDEPlugin.getWorkspace().getRoot().getLocation().toString();
		dialog.setFileName(path);
		dialog.setFilterExtensions(new String[] {filter});
		String res = dialog.open();
		if (res != null) {
			if (combo.indexOf(res) == -1)
				combo.add(res, 0);
			combo.setText(res);
		}
	}
	
	private void chooseDestination() {
		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
		String path = fDirectoryCombo.getText();
		if (path.trim().length() == 0)
			path = PDEPlugin.getWorkspace().getRoot().getLocation().toString();
		dialog.setFilterPath(path);
		dialog.setText(PDEUIMessages.ExportWizard_dialog_title); //$NON-NLS-1$
		dialog.setMessage(PDEUIMessages.ExportWizard_dialog_message); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			if (fDirectoryCombo.indexOf(res) == -1)
				fDirectoryCombo.add(res, 0);
			fDirectoryCombo.setText(res);
		}
	}

	protected abstract void pageChanged();
	
	protected String validateBottomSections() {
		String message = null;
		if (isButtonSelected(fArchiveFileButton) && fArchiveCombo.getText().trim().length() == 0) {
			message = PDEUIMessages.ExportWizard_status_nofile; //$NON-NLS-1$
		}
		if (isButtonSelected(fDirectoryButton) && fDirectoryCombo.getText().trim().length() == 0) {
			message = PDEUIMessages.ExportWizard_status_nodirectory; //$NON-NLS-1$
		}
		if (isButtonSelected(fSaveAsAntButton) && fAntCombo.getText().trim().length() == 0) {
			message = PDEUIMessages.ExportWizard_status_noantfile; //$NON-NLS-1$
		}
		return message;
	}
	
	
	private boolean isButtonSelected(Button button) {
		return button != null && !button.isDisposed() && button.getSelection();
	}

	protected void initializeExportOptions(IDialogSettings settings) {		
		fIncludeSource.setSelection(settings.getBoolean(S_EXPORT_SOURCE));
        if (fJarButton != null)
            fJarButton.setSelection(settings.getBoolean(S_JAR_FORMAT));
        if (addAntSection()) {
    		fSaveAsAntButton.setSelection(settings.getBoolean(S_SAVE_AS_ANT));
    		initializeCombo(settings, S_ANT_FILENAME, fAntCombo);
    		fAntCombo.setEnabled(fSaveAsAntButton.getSelection());
    		fBrowseAnt.setEnabled(fSaveAsAntButton.getSelection());
        }
	}
	
	private void initializeDestinationSection(IDialogSettings settings) {
		boolean useDirectory = settings.getBoolean(S_EXPORT_DIRECTORY);
		fDirectoryButton.setSelection(useDirectory);	
		fArchiveFileButton.setSelection(!useDirectory);
		toggleDestinationGroup(useDirectory);
		initializeCombo(settings, S_DESTINATION, fDirectoryCombo);
		initializeCombo(settings, S_ZIP_FILENAME, fArchiveCombo);
	}
	
	protected void initializeCombo(IDialogSettings settings, String key, Combo combo) {
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
		else
			combo.setText(""); //$NON-NLS-1$
	}

	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();	
        if (fJarButton != null)
            settings.put(S_JAR_FORMAT, fJarButton.getSelection());
        
		settings.put(S_EXPORT_DIRECTORY, fDirectoryButton.getSelection());		
		settings.put(S_EXPORT_SOURCE, fIncludeSource.getSelection());
		
		settings.put(S_JAVAC_SOURCE, fJavacSource.getText());
		settings.put(S_JAVAC_TARGET, fJavacTarget.getText());
        
        if (fSaveAsAntButton != null)
            settings.put(S_SAVE_AS_ANT, fSaveAsAntButton.getSelection());
		
		saveCombo(settings, S_DESTINATION, fDirectoryCombo);
		saveCombo(settings, S_ZIP_FILENAME, fArchiveCombo);
        if (fAntCombo != null)
            saveCombo(settings, S_ANT_FILENAME, fAntCombo);
	}
	
	protected void saveCombo(IDialogSettings settings, String key, Combo combo) {
		if (combo.getText().trim().length() > 0) {
			settings.put(key + String.valueOf(0), combo.getText().trim());
			String[] items = combo.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(key + String.valueOf(i + 1), items[i].trim());
			}
		}	
	}

	public boolean doExportSource() {
		return fIncludeSource.getSelection();
	}
	
	public boolean doExportToDirectory() {
		return fDirectoryButton.getSelection();
	}
	
	public boolean useJARFormat() {
		return fJarButton != null && fJarButton.getSelection();
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
		
	public boolean doGenerateAntFile() {
		return fSaveAsAntButton != null && fSaveAsAntButton.getSelection();
	}
	
	public String getJavacTarget() {
		return fJavacTarget.getText();
	}
	
	public String getJavacSource() {
		return fJavacSource.getText();
	}
	
	public String getAntBuildFileName() {
		return fAntCombo != null ? fAntCombo.getText().trim() : ""; //$NON-NLS-1$
	}
	
	public IWizardPage getNextPage() {
		return (fJarButton != null && fJarButton.getSelection()) ? super.getNextPage() : null;
	}
    
    protected boolean addAntSection() {
        return true;
    }
    
    protected boolean addJARFormatSection() {
        return true;
    }
	
}
