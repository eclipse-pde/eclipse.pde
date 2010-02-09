/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 274853
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.net.URI;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

public class FeatureOptionsTab extends ExportOptionsTab {

	private static final String S_MULTI_PLATFORM = "multiplatform"; //$NON-NLS-1$
	private static final String S_EXPORT_METADATA = "p2metadata"; //$NON-NLS-1$
	private static final String S_CATEGORY_FILE = "category_file"; //$NON-NLS-1$
	private static final String S_CREATE_CATEGORIES = "create_categories"; //$NON-NLS-1$

	private Button fMultiPlatform;
	private Button fExportMetadata;

	private Button fCategoryButton;
	private Combo fCategoryCombo;
	private Button fCategoryBrowse;

	private class CategoryResourceListSelectionDialog extends FilteredResourcesSelectionDialog {

		public CategoryResourceListSelectionDialog(Shell shell, boolean multi, IContainer container, int typesMask) {
			super(shell, multi, container, typesMask);
			addListFilter(new ViewerFilter() {

				public boolean select(Viewer viewer, Object parentElement, Object element) {
					IResource resource = (IResource) element;
					if (resource != null && resource instanceof IFile) {
						IFile file = (IFile) resource;
						String extension = file.getFileExtension();
						return extension != null && extension.toLowerCase().equals("xml"); //$NON-NLS-1$
					}
					return false;
				}
			});
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog#validateItem(java.lang.Object)
		 */
		protected IStatus validateItem(Object item) {
			if (item instanceof IResource) {
				IResource resource = (IResource) item;
				if (resource != null && resource instanceof IFile) {
					try {
						IFile file = (IFile) resource;
						IContentDescription description = file.getContentDescription();
						if (description != null) {
							IContentType type = description.getContentType();
							if (type != null && type.getId().equalsIgnoreCase("org.eclipse.pde.categoryManifest")) { //$NON-NLS-1$
								return Status.OK_STATUS;
							}
						}
					} catch (CoreException e) {
						// do not log anything as we don't care, bug 274678
					}
				}
			}
			return new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, PDEUIMessages.FeatureOptionsTab_0);
		}
	}

	public FeatureOptionsTab(FeatureExportWizardPage page) {
		super(page);
	}

	protected void addAdditionalOptions(Composite comp) {
		fJarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fExportMetadata.setEnabled(fJarButton.getSelection());
				fCategoryButton.setEnabled(fExportMetadata.getSelection() && fJarButton.getSelection());
				updateCategoryGeneration();
			}
		});
		fExportMetadata = SWTFactory.createCheckButton(comp, PDEUIMessages.ExportWizard_includesMetadata, null, false, 1);
		GridData data = (GridData) fExportMetadata.getLayoutData();
		data.horizontalIndent = 20;

		Composite categoryComposite = new Composite(comp, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.horizontalIndent = 20;
		categoryComposite.setLayoutData(data);
		GridLayout layout = new GridLayout(3, false);
		layout.marginRight = 0;
		layout.marginLeft = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginBottom = 0;
		layout.marginTop = 0;
		categoryComposite.setLayout(layout);

		fCategoryButton = new Button(categoryComposite, SWT.CHECK);
		fCategoryButton.setText(PDEUIMessages.ExportWizard_generateCategories + ":"); //$NON-NLS-1$
		fCategoryButton.setSelection(true);

		fCategoryCombo = new Combo(categoryComposite, SWT.NONE);
		fCategoryCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fCategoryBrowse = new Button(categoryComposite, SWT.PUSH);
		fCategoryBrowse.setText(PDEUIMessages.ExportWizard_browse);
		fCategoryBrowse.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fCategoryBrowse);

		// Only visible for delta pack/multiple platform support enabled
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.getDeltaPackFeature();
		if (model != null) {
			fMultiPlatform = new Button(comp, SWT.CHECK);
			fMultiPlatform.setText(PDEUIMessages.ExportWizard_multi_platform);
		}

	}

	protected String getJarButtonText() {
		return PDEUIMessages.BaseExportWizardPage_fPackageJARs;
	}

	/**
	 * @return whether to generate p2 metadata on export
	 */
	protected boolean doExportMetadata() {
		return fExportMetadata.isEnabled() && fExportMetadata.getSelection();
	}

	protected URI getCategoryDefinition() {
		if (doExportCategories()) {
			File f = new File(fCategoryCombo.getText().trim());
			if (f.exists())
				return f.toURI();
		}
		return null;
	}

	/**
	 * @return whether to publish categories when exporting
	 */
	private boolean doExportCategories() {
		return doExportMetadata() && fCategoryButton.getSelection() && fCategoryCombo.getText().trim().length() > 0;
	}

	protected void initialize(IDialogSettings settings) {
		super.initialize(settings);
		if (fMultiPlatform != null) {
			fMultiPlatform.setSelection(settings.getBoolean(S_MULTI_PLATFORM));
		}

		String selected = settings.get(S_EXPORT_METADATA);
		fExportMetadata.setSelection(selected == null ? true : Boolean.TRUE.toString().equals(selected));

		// This enablement depends on both the jar button being selected and the destination setting being something other than install (bug 276989)
		fExportMetadata.setEnabled(fJarButton.getSelection());
		String exportType = settings.get(ExportDestinationTab.S_EXPORT_TYPE);
		if (exportType != null && exportType.length() > 0) {
			try {
				int exportTypeCode = Integer.parseInt(exportType);
				if (exportTypeCode == ExportDestinationTab.TYPE_INSTALL) {
					fExportMetadata.setEnabled(false);
				}
			} catch (NumberFormatException e) {
			}
		}

		selected = settings.get(S_CREATE_CATEGORIES);

		fCategoryButton.setEnabled(fExportMetadata.getSelection() && fJarButton.getSelection());
		fCategoryButton.setSelection(selected == null ? true : Boolean.TRUE.toString().equals(selected));
		if (settings.get(S_CATEGORY_FILE) != null)
			fCategoryCombo.setText(settings.get(S_CATEGORY_FILE));
		updateCategoryGeneration();
	}

	protected void saveSettings(IDialogSettings settings) {
		super.saveSettings(settings);
		if (fMultiPlatform != null) {
			settings.put(S_MULTI_PLATFORM, fMultiPlatform.getSelection());
		}
		settings.put(S_EXPORT_METADATA, doExportMetadata());
		settings.put(S_CREATE_CATEGORIES, fCategoryButton.getSelection());
		settings.put(S_CATEGORY_FILE, fCategoryCombo.getText());
	}

	protected void hookListeners() {
		super.hookListeners();
		if (fMultiPlatform != null) {
			fMultiPlatform.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fPage.pageChanged();
				}
			});
		}

		fExportMetadata.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fCategoryButton.setEnabled(fExportMetadata.getSelection() && fJarButton.getSelection());
				updateCategoryGeneration();
			}
		});
		fCategoryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateCategoryGeneration();
			}
		});

		fCategoryBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openFile(fCategoryCombo);
			}
		});
	}

	protected void updateCategoryGeneration() {
		fCategoryBrowse.setEnabled(fExportMetadata.getSelection() && fCategoryButton.getSelection() && fJarButton.getSelection());
		fCategoryCombo.setEnabled(fExportMetadata.getSelection() && fCategoryButton.getSelection() && fJarButton.getSelection());
	}

	protected boolean doMultiplePlatform() {
		return fMultiPlatform != null && fMultiPlatform.getSelection();
	}

	protected void setEnabledForInstall(boolean enabled) {
		super.setEnabledForInstall(enabled);
		fExportMetadata.setEnabled(enabled);
		fCategoryButton.setEnabled(enabled);
		fCategoryCombo.setEnabled(enabled && fCategoryButton.getSelection());
		fCategoryBrowse.setEnabled(enabled && fCategoryButton.getSelection());
		if (fMultiPlatform != null) {
			fMultiPlatform.setEnabled(enabled);
		}
	}

	protected void openFile(Combo combo) {
		CategoryResourceListSelectionDialog dialog = new CategoryResourceListSelectionDialog(fPage.getShell(), false, PDEPlugin.getWorkspace().getRoot(), IResource.FILE);
		dialog.setInitialPattern("**"); //$NON-NLS-1$
		dialog.create();
		String path = combo.getText();
		if (path.trim().length() == 0)
			path = PDEPlugin.getWorkspace().getRoot().getLocation().toString();
		if (dialog.open() == Window.OK) {
			Object[] objects = dialog.getResult();
			if (objects.length == 1) {
				String result = ((IResource) objects[0]).getRawLocation().toOSString();
				if (result != null) {
					if (combo.indexOf(result) == -1)
						combo.add(result, 0);
					combo.setText(result);
				}
			}
		}
	}
}
