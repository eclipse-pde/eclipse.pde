/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ProductConfigurationSection {

	private static final String S_PRODUCT_CONFIG = "productConfig"; //$NON-NLS-1$
	private static final String S_PRODUCT_ROOT = "productRoot"; //$NON-NLS-1$

	private Text fProductRootText;
	private Combo fProductCombo;

	private ProductExportWizardPage fPage;

	public ProductConfigurationSection(ProductExportWizardPage page) {
		fPage = page;
	}

	public Control createControl(Composite parent) {
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

		fProductRootText = new Text(group, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fProductRootText.setLayoutData(gd);
		return group;
	}

	private void handleBrowse() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(fPage.getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ProductExportWizardPage_fileSelection);
		dialog.setMessage(PDEUIMessages.ProductExportWizardPage_productSelection);
		dialog.addFilter(new FileExtensionFilter("product")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		IFile product = getProductFile();
		if (product != null)
			dialog.setInitialSelection(product);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.PRODUCT_CONFIGURATION_SELECTION);
		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			String value = file.getFullPath().toString();
			if (fProductCombo.indexOf(value) == -1)
				fProductCombo.add(value, 0);
			fProductCombo.setText(value);
		}
	}

	protected void hookListeners() {
		fProductCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateProductFields();
				fPage.pageChanged();
			}
		});
	}

	protected void initialize(IStructuredSelection selection, IDialogSettings settings) {
		for (int i = 0; i < 6; i++) {
			String curr = settings.get(S_PRODUCT_CONFIG + String.valueOf(i));
			if (curr != null && fProductCombo.indexOf(curr) == -1) {
				IFile file = getProductFile(curr);
				if (file.exists())
					fProductCombo.add(curr);
			}
		}

		if (selection.size() > 0) {
			Object object = selection.getFirstElement();
			if (object instanceof IFile) {
				IFile file = (IFile) object;
				if ("product".equals(file.getFileExtension())) { //$NON-NLS-1$
					String entry = file.getFullPath().toString();
					if (fProductCombo.indexOf(entry) == -1)
						fProductCombo.add(entry, 0);
					fProductCombo.setText(entry);
				}
			} else if (object instanceof IContainer) {
				IContainer container = (IContainer) object;
				try {
					if (container.isAccessible()) {
						IResource[] resources = container.members();
						for (int i = 0; i < resources.length; i++) {
							IResource resource = resources[i];
							if (resource instanceof IFile && resource.getName().endsWith(".product")) { //$NON-NLS-1$
								String path = resource.getFullPath().toString();
								if (fProductCombo.indexOf(path) == -1)
									fProductCombo.add(path, 0);
							}
						}
					}
					if (fProductCombo.getItemCount() > 0)
						fProductCombo.setText(fProductCombo.getItem(0));
				} catch (CoreException e) {
				}
			}
		} else if (fProductCombo.getItemCount() > 0) {
			fProductCombo.setText(fProductCombo.getItem(0));
		}

		setRoot(settings);
		hookListeners();
	}

	protected IFile getProductFile() {
		return getProductFile(fProductCombo.getText().trim());
	}

	protected IFile getProductFile(String path) {
		if (path == null || path.length() == 0)
			return null;

		IPath thePath = new Path(path);
		return thePath.segmentCount() < 2 ? null : PDEPlugin.getWorkspace().getRoot().getFile(new Path(path));
	}

	protected void updateProductFields() {
		IFile file = getProductFile();
		String root = null;
		try {
			if (file != null && file.exists())
				root = file.getPersistentProperty(IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_ROOT);
		} catch (CoreException e) {
		}
		if (root != null)
			fProductRootText.setText(root);

		fPage.updateProductFields();
	}

	protected void setRoot(IDialogSettings settings) {
		IFile file = getProductFile();
		String root = null;
		try {
			if (file != null && file.exists())
				root = file.getPersistentProperty(IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_ROOT);
		} catch (CoreException e) {
		}
		if (root == null)
			root = settings.get(S_PRODUCT_CONFIG);
		if (root == null)
			root = "eclipse"; //$NON-NLS-1$
		fProductRootText.setText(root);
	}

	protected String getRootDirectory() {
		return fProductRootText.getText().trim();
	}

	protected void saveSettings(IDialogSettings settings) {
		saveCombo(settings);
		settings.put(S_PRODUCT_ROOT, fProductRootText.getText().trim());
		IFile file = getProductFile();
		try {
			if (file != null && file.exists()) {
				file.setPersistentProperty(IPDEUIConstants.DEFAULT_PRODUCT_EXPORT_ROOT, getRootDirectory());
			}
		} catch (CoreException e) {
		}
	}

	protected void saveCombo(IDialogSettings settings) {
		if (fProductCombo.getText().trim().length() > 0) {
			settings.put(S_PRODUCT_CONFIG + String.valueOf(0), fProductCombo.getText().trim());
			String[] items = fProductCombo.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(S_PRODUCT_CONFIG + String.valueOf(i + 1), items[i].trim());
			}
		}
	}

	protected String validate() {
		String configLocation = fProductCombo.getText().trim();
		if (configLocation.length() == 0)
			return PDEUIMessages.ProductExportWizardPage_noProduct;

		IPath path = new Path(configLocation);
		IResource resource = PDEPlugin.getWorkspace().getRoot().findMember(path);
		if (resource == null || !(resource instanceof IFile))
			return PDEUIMessages.ProductExportWizardPage_productNotExists;

		if (!resource.getName().endsWith(".product")) //$NON-NLS-1$
			return PDEUIMessages.ProductExportWizardPage_wrongExtension;

		return null;
	}

}
