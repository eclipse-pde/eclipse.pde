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
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
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
	private static final String S_SYNC_PRODUCT = "syncProduct"; //$NON-NLS-1$
	private static final int S_EXP_ROOT = 0;
	private static final int S_EXP_DEST = 1;
	private static final int S_EXP_DIR = 2;

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
	}
	
	protected void initializeTopSection() {	
		initializeProductCombo();
		String[] exSettings = getExportSettings();
		fProductRootText.setText(exSettings != null 
				&& exSettings[S_EXP_ROOT] != null ? 
						exSettings[S_EXP_ROOT] : "eclipse"); //$NON-NLS-1$
		IDialogSettings settings = getDialogSettings();
		String value = settings.get(S_SYNC_PRODUCT);
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
				updateProductFields();
			}
		});
		
		fProductCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pageChanged();
				updateProductFields();
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
	
	private void initializeProductCombo() {
		if (fSelection.size() > 0) {
			Object object = fSelection.getFirstElement();
			if (object instanceof IFile) {
				IFile file = (IFile)object;
				if ("product".equals(file.getFileExtension())) { //$NON-NLS-1$
					String entry = file.getFullPath().toString();
					if (fProductCombo.indexOf(entry) == -1)
						fProductCombo.add(entry, 0);
				}
			}
		}
		
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
					break;
				}
			}
		} catch (CoreException e) {}
		
		String[] items = fProductCombo.getItems();
		if (items != null && items.length > 0)
			fProductCombo.setText(items[items.length - 1]);
	}
	
	private WorkspaceProductModel getProductModel() {
		IFile modelFile = getProductFile();
		if (modelFile == null)
			return null;
		WorkspaceProductModel model = new WorkspaceProductModel(modelFile, true);
		if (!model.isLoaded())
			try {
				model.load();
			} catch (CoreException e) {}
		return model;
	}
	
	protected void updateProductFields() {
		IProduct product = getProduct();
		if (product == null)
			return;
		
		String[] exSettings = getExportSettings();
		fProductRootText.setText(exSettings != null 
				&& exSettings[S_EXP_ROOT] != null ? 
						exSettings[S_EXP_ROOT] : "eclipse"); //$NON-NLS-1$
		setDestinationSection(product);
	}

	
	private String[] getExportSettings() {
		IResource file = getProductFile();
		if (file == null)
			return null;
		String[] settings = null;
		try {
			settings = new String[3];
			settings[S_EXP_ROOT] = file.getPersistentProperty(
					IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_ROOT);
			settings[S_EXP_DEST] = file.getPersistentProperty(
					IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_LOCATION);
			settings[S_EXP_DIR] = file.getPersistentProperty(
					IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_DIR);
		} catch (CoreException e) {
			settings = null;
		}
		return settings;
	}

	private IProduct getProduct() {
		WorkspaceProductModel model = getProductModel();
		if (model == null)
			return null;
		IProduct product = model.getProduct();
		return product;
	}
	
	protected void hookHelpContext(Control control) {
	}
	
	public void saveSettings() {
		super.saveSettings();
		IDialogSettings settings = getDialogSettings();
		saveCombo(settings, S_PRODUCT_CONFIG, fProductCombo);
		settings.put(S_SYNC_PRODUCT, fSyncButton.getSelection());
		
		String[] exSettings = getExportSettings();
		if (exSettings == null)
			exSettings = new String[] {null, null, null};
		
		exSettings[S_EXP_ROOT] = fProductRootText.getText();
		exSettings[S_EXP_DEST] = getDestinationText();
		exSettings[S_EXP_DIR] = Boolean.toString(isDirectoryDest());
		setExportSettings(exSettings);
	}

	private void setExportSettings(String[] exSettings) {
		IResource file = getProductFile();
		if (file == null 
				|| exSettings == null 
				|| exSettings.length != 3)
			return;
		try {
			file.setPersistentProperty(
					IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_ROOT,
					exSettings[S_EXP_ROOT]);
			file.setPersistentProperty(
					IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_LOCATION,
					exSettings[S_EXP_DEST]);
			file.setPersistentProperty(
					IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_DIR,
					exSettings[S_EXP_DIR]);
		} catch (CoreException e) {
		}
	}

	public boolean doSync() {
		return fSyncButton.getSelection();
	}
	
	public String getRootDirectory() {
		return fProductRootText.getText().trim();
	}
	
	public IFile getProductFile() {
		String product = fProductCombo.getText().trim();
		if (product.equals("")) //$NON-NLS-1$
			return null;
		IPath path = new Path(product);
		if (path.segmentCount() < 2)
			return null;
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

    private boolean setDestinationSection(IProduct product) {
    	String[] settings = getExportSettings();
		String dest = settings[S_EXP_DEST];
		if (dest != null) {
			boolean useDir = settings[S_EXP_DIR].equals(Boolean.toString(true));
			fDirectoryButton.setSelection(useDir);	
			fArchiveFileButton.setSelection(!useDir);
			toggleDestinationGroup(useDir);
			if (useDir) {
				if (fDirectoryCombo.indexOf(dest) == -1)
					fDirectoryCombo.add(dest, 0);
			} else {
				if (fArchiveCombo.indexOf(dest) == -1)
		    		fArchiveCombo.add(dest, 0);
			}
			fDirectoryCombo.setText(useDir ? dest : ""); //$NON-NLS-1$
			fArchiveCombo.setText(useDir ? "" : dest); //$NON-NLS-1$
		}
		return (dest != null);
    }
    
	protected void initializeDestinationSection(IDialogSettings settings) {
		IProduct product = getProduct();
		boolean destSet = false;
		if (product != null)
			destSet = setDestinationSection(product);
		if (!destSet)
			super.initializeDestinationSection(settings);
	}
}
