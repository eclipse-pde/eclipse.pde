/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ProductExportWizardPage extends BaseExportWizardPage {
	
	private static final String S_PRODUCT_CONFIG = "productConfig"; //$NON-NLS-1$
	private static final String S_ROOT_DIR = "productRoot"; //$NON-NLS-1$
	private static final String S_SYNC_PRODUCT = "syncProduct"; //$NON-NLS-1$

	private Button fSyncButton;
	private Text fProductRootText;
	private Combo fProductCombo;
	private IStructuredSelection fSelection;

	public ProductExportWizardPage(IStructuredSelection selection) {
		super("productExport"); //$NON-NLS-1$
		fSelection = selection;
		setTitle(PDEUIMessages.ProductExportWizardPage_title); 
		setDescription(PDEUIMessages.ProductExportWizardPage_desc); 
	}

	protected void createTopSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createConfigurationSection(container);
		createSynchronizationSection(container);
		checkForProductFile(container);
	}
	
	protected void initializeTopSection() {
		if (fSelection.size() > 0) {
			Object object = fSelection.getFirstElement();
			if (object instanceof IFile) {
				IFile file = (IFile)object;
				if ("product".equals(file.getFileExtension())) { //$NON-NLS-1$
					String entry = file.getFullPath().toString();
					if (fProductCombo.indexOf(entry) == -1) {
						fProductCombo.add(entry, 0);
					}
					fProductCombo.setText(entry);	
				}
			}
		}
		
		IDialogSettings settings = getDialogSettings();		
		String value = settings.get(S_ROOT_DIR);
		fProductRootText.setText(value == null ? "eclipse" : value); //$NON-NLS-1$
		
		value = settings.get(S_SYNC_PRODUCT);
		fSyncButton.setSelection(value == null ? true : settings.getBoolean(S_SYNC_PRODUCT));	
	}
	
	private void createConfigurationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout(3, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.ProductExportWizardPage_productGroup); 
		
		Label label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.ProductExportWizardPage_config); 
		
		fProductCombo = new Combo(group, SWT.BORDER);
		fProductCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button browse = new Button(group, SWT.PUSH);
		browse.setText(PDEUIMessages.ProductExportWizardPage_browse); 
		browse.setLayoutData(new GridData());
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		SWTUtil.setButtonDimensionHint(browse);
		
		label = new Label(group, SWT.NONE);
		label.setText(PDEUIMessages.ProductExportWizardPage_root); 
		
		fProductRootText = new Text(group, SWT.SINGLE|SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fProductRootText.setLayoutData(gd);
	}
	
	private void createSynchronizationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 7;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.ProductExportWizardPage_sync); 
		
		Label label = new Label(group, SWT.WRAP);
		label.setText(PDEUIMessages.ProductExportWizardPage_syncText); 
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 400;
		label.setLayoutData(gd);
		
		fSyncButton = new Button(group, SWT.CHECK);
		fSyncButton.setText(PDEUIMessages.ProductExportWizardPage_syncButton); 
		gd = new GridData();
		gd.horizontalIndent = 20;
		fSyncButton.setLayoutData(gd);
	}
	
	protected String getJarButtonText() {
		return PDEUIMessages.BaseExportWizardPage_packageJARs; 
	}
	
	protected void hookListeners() {
		super.hookListeners();
		
		fProductCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		fProductCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
			}
		});
	}

	protected void pageChanged() {
		super.pageChanged();
		String errorMessage = null;
		String message = null;
		String configLocation = fProductCombo.getText().trim();
		if (configLocation.length() == 0) {
			message = PDEUIMessages.ProductExportWizardPage_noProduct; 
		} else {
			IPath path = new Path(configLocation);
			IResource resource = PDEPlugin.getWorkspace().getRoot().findMember(path);
			if (resource == null || !(resource instanceof IFile)) {
				errorMessage = PDEUIMessages.ProductExportWizardPage_productNotExists; 
			} else if (!path.lastSegment().endsWith(".product")) { //$NON-NLS-1$
				errorMessage = PDEUIMessages.ProductExportWizardPage_wrongExtension; 
			}
		}
		if (message == null && errorMessage == null)
			message = validateBottomSections();
		setErrorMessage(errorMessage);
		setMessage(message);
		setPageComplete(getMessage() == null && getErrorMessage() == null);
	}
	
	public IWizardPage getNextPage() {
		if (!doMultiPlatform()) {
			return null;
		}
		return getWizard().getNextPage(this);
	}
	
	private void handleBrowse() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getContainer().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ProductExportWizardPage_fileSelection); 
		dialog.setMessage(PDEUIMessages.ProductExportWizardPage_productSelection); 
		dialog.addFilter(new FileExtensionFilter("product"));  //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		IFile product = getProductFile();
		if (product != null) dialog.setInitialSelection(product);

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			String value = file.getFullPath().toString();
			if (fProductCombo.indexOf(value) == -1)
				fProductCombo.add(value, 0);
			fProductCombo.setText(value);
		}
	}
	
	private void checkForProductFile(Composite container) {
		IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
		if (projects.length == 0) return;
		
		try {
			IResource[] members = projects[0].members();
			for (int i = 0; i < members.length; i++) {
				if (members[i] instanceof IContainer)
					continue;
				String name = members[i].getName();
				if (name.endsWith(".product")) { //$NON-NLS-1$
					String path = members[i].getFullPath().toString();
					if (fProductCombo.indexOf(path) == -1)
						fProductCombo.add(path, 0);
					fProductCombo.setText(path);
					return;
				}
			}
		} catch (CoreException e) {}
	}
	
	
	protected void hookHelpContext(Control control) {
	}
	
	public void saveSettings() {
		super.saveSettings();
		IDialogSettings settings = getDialogSettings();
		saveCombo(settings, S_PRODUCT_CONFIG, fProductCombo);
		settings.put(S_ROOT_DIR, fProductRootText.getText().trim());
		settings.put(S_SYNC_PRODUCT, fSyncButton.getSelection());
	}
	
	public boolean doSync() {
		return fSyncButton.getSelection();
	}
	
	public String getRootDirectory() {
		return fProductRootText.getText().trim();
	}
	
	public IFile getProductFile() {
		String product = fProductCombo.getText().trim();
		if (product.equals("")) return null; //$NON-NLS-1$
		IPath path = new Path(product);
		return PDEPlugin.getWorkspace().getRoot().getFile(path);
	}
    
    protected boolean addAntSection() {
        return false;
    }
    
    protected boolean addJARFormatSection() {
        return false;
    }
    
    protected int getVerticalSpacing() {
        return super.getVerticalSpacing() * 2;
    }

}
