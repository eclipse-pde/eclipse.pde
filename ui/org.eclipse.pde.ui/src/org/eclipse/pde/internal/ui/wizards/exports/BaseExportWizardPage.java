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

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


public abstract class BaseExportWizardPage extends WizardPage {
	private static final String S_JAR_FORMAT = "exportUpdate"; //$NON-NLS-1$
	private static final String S_EXPORT_DIRECTORY = "exportDirectory";	 //$NON-NLS-1$
	private static final String S_EXPORT_SOURCE="exportSource"; //$NON-NLS-1$
	private static final String S_DESTINATION = "destination"; //$NON-NLS-1$
	private static final String S_ZIP_FILENAME = "zipFileName"; //$NON-NLS-1$
	private static final String S_SAVE_AS_ANT = "saveAsAnt"; //$NON-NLS-1$
	private static final String S_ANT_FILENAME = "antFileName"; //$NON-NLS-1$
		
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
		createOptionsSection(container);
		
		Dialog.applyDialogFont(container);
		
		// load settings
		IDialogSettings settings = getDialogSettings();
		initializeTopSection();
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
		fSaveAsAntButton.setText(PDEPlugin.getResourceString("ExportWizard.antCheck")); //$NON-NLS-1$
		
		fAntCombo = new Combo(comp, SWT.NONE);
		fAntCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fBrowseAnt = new Button(comp, SWT.PUSH);
		fBrowseAnt.setText(PDEPlugin.getResourceString("ExportWizard.browse2")); //$NON-NLS-1$
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
		dialog.setFileName(fArchiveCombo.getText());
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

	protected abstract void pageChanged();
	
	protected String validateBottomSections() {
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
	}

	public void saveSettings() {
		IDialogSettings settings = getDialogSettings();	
        if (fJarButton != null)
            settings.put(S_JAR_FORMAT, fJarButton.getSelection());
        
		settings.put(S_EXPORT_DIRECTORY, fDirectoryButton.getSelection());		
		settings.put(S_EXPORT_SOURCE, fIncludeSource.getSelection());
        
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
	
	public String getAntBuildFileName() {
		return fAntCombo != null ? fAntCombo.getText().trim() : "";
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
