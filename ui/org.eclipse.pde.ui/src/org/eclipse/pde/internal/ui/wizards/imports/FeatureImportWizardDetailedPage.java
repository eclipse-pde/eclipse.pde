/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class FeatureImportWizardDetailedPage extends WizardPage {

	private FeatureImportWizardFirstPage fFirstPage;
	private boolean fIsOtherLocation;
	private IPath fDropLocation;
	private CheckboxTableViewer fFeatureViewer;
	private TablePart fTablePart;
	private IFeatureModel[] fModels;

	public class ContentProvider
	extends DefaultContentProvider
	implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getModels();
		}
	}
	
	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel) {
			super(mainLabel);
		}

		public void updateCounter(int count) {
			super.updateCounter(count);
			dialogChanged();
		}
		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormToolkit toolkit) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, toolkit);
			viewer.setSorter(ListUtil.FEATURE_SORTER);
			return viewer;
		}
	}

	public FeatureImportWizardDetailedPage(FeatureImportWizardFirstPage firstPage) {
		super("FeatureImportWizardDetailedPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.FeatureImportWizard_DetailedPage_title); //$NON-NLS-1$
		setDescription(PDEUIMessages.FeatureImportWizard_DetailedPage_desc); //$NON-NLS-1$

		fFirstPage = firstPage;
		fIsOtherLocation = false;
		fDropLocation = null;
		fTablePart = new TablePart(PDEUIMessages.FeatureImportWizard_DetailedPage_featureList); //$NON-NLS-1$
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	private void initializeFields(boolean isOtherLocation, IPath dropLocation) {
		if (isOtherLocation != fIsOtherLocation
				|| !dropLocation.equals(this.fDropLocation)) {
			this.fIsOtherLocation = fFirstPage.isOtherLocation();
			this.fDropLocation = dropLocation;
			fModels = null;
		}
		if (fModels == null) {
			getModels(); // force loading
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask(
						PDEUIMessages.FeatureImportWizard_messages_updating, //$NON-NLS-1$
						IProgressMonitor.UNKNOWN);
					fFeatureViewer
						.getControl()
						.getDisplay()
						.asyncExec(new Runnable() {
						public void run() {
							fFeatureViewer.setInput(PDEPlugin.getDefault());
							if (getModels() != null)
								fFeatureViewer.setCheckedElements(getModels());
								fTablePart.updateCounter(getModels().length);
						}
					});
					monitor.done();
				}
			};
			try {
				getContainer().run(true, false, op);
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} finally {
				dialogChanged();
			}
			//treePart.updateCounter(0);
		}
	}

	/*
	 * @see DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initializeFields(fFirstPage.isOtherLocation(), fFirstPage.getDropLocation());
		}
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		fTablePart.createControl(container);
		fFeatureViewer = fTablePart.getTableViewer();
		fFeatureViewer.setContentProvider(new ContentProvider());
		fFeatureViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		GridData gd = (GridData) fTablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;
		setControl(container);
		dialogChanged();
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.FEATURE_IMPORT_SECOND_PAGE);
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public IFeatureModel[] getModels() {
		if (fModels != null)
			return fModels;

		final ArrayList result = new ArrayList();
		final IPath home = fDropLocation;
		if (home != null) {
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
					monitor.beginTask(
						PDEUIMessages.FeatureImportWizard_messages_loadingFile, //$NON-NLS-1$
						IProgressMonitor.UNKNOWN);

					try {
						if (!fIsOtherLocation) {
							IFeatureModel[] allModels = PDECore.getDefault()
									.getFeatureModelManager().getModels();
							for (int i = 0; i < allModels.length; i++) {
								if (allModels[i].getUnderlyingResource() == null) {
									result.add(allModels[i]);
								}
							}
						} else {
							MultiStatus errors = doLoadFeatures(result,
									createPath(home), monitor);
							if (errors != null
									&& errors.getChildren().length > 0) {
								PDEPlugin.log(errors);
							}
						}
						fModels =
							(IFeatureModel[]) result.toArray(
								new IFeatureModel[result.size()]);

					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			};
			try {
				getContainer().run(true, false, op);
			} catch (InterruptedException e) {
				return null;
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			}
		}
		return fModels;
	}

	private File createPath(IPath dropLocation) {
		File featuresDir = new File(dropLocation.toFile(), "features"); //$NON-NLS-1$
		if (featuresDir.exists())
			return featuresDir;
		return null;
	}

	private MultiStatus doLoadFeatures(
		ArrayList result,
		File path,
		IProgressMonitor monitor)
		throws CoreException {
		if (path == null)
			return null;
		File[] dirs = path.listFiles();
		if (dirs == null)
			return null;
		monitor.beginTask(PDEUIMessages.FeatureImportWizard_DetailedPage_loading, dirs.length); //$NON-NLS-1$
		ArrayList resultStatus = new ArrayList();
		for (int i = 0; i < dirs.length; i++) {
			File dir = dirs[i];
			if (dir.isDirectory()) {
				File manifest = new File(dir, "feature.xml"); //$NON-NLS-1$
				if (manifest.exists()) {
					IStatus status = doLoadFeature(dir, manifest, result);
					if (status != null)
						resultStatus.add(status);
				}
				monitor.worked(1);
			}
		}
		if (resultStatus != null) {
			IStatus[] children =
				(IStatus[]) resultStatus.toArray(new IStatus[resultStatus.size()]);
			MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.PLUGIN_ID,
					IStatus.OK,
					children,
					PDEUIMessages.FeatureImportWizard_DetailedPage_problemsLoading, //$NON-NLS-1$
					null);
			return multiStatus;
		}
		return null;
	}

	private IStatus doLoadFeature(File dir, File manifest, ArrayList result) {
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(dir.getAbsolutePath());
		IStatus status = null;

		InputStream stream = null;

		try {
			stream = new FileInputStream(manifest);
			model.load(stream, false);
			if(!model.isValid()){
				status =
					new Status(
						IStatus.WARNING,
						PDEPlugin.PLUGIN_ID,
						IStatus.OK,
						"Import location "+dir+" contains invalid feature.", //$NON-NLS-1$ //$NON-NLS-2$
						null);
			}
		} catch (Exception e) {
			// Errors in the file
			status =
				new Status(
					IStatus.ERROR,
					PDEPlugin.PLUGIN_ID,
					IStatus.OK,
					e.getMessage(),
					e);
		}
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
		if (status == null)
			result.add(model);
		return status;
	}

	public IFeatureModel[] getSelectedModels() { 
		Object[] selected = fFeatureViewer.getCheckedElements();
		IFeatureModel[] result = new IFeatureModel[selected.length];
		System.arraycopy(selected, 0, result, 0, selected.length);
		return result;
	}

	private void dialogChanged() {
		String message = null;
		if (fFeatureViewer != null && fFeatureViewer.getTable().getItemCount() == 0) {
			message = PDEUIMessages.FeatureImportWizard_messages_noFeatures; //$NON-NLS-1$
		}
		setMessage(message, WizardPage.INFORMATION);
		setPageComplete(fTablePart.getSelectionCount() > 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		return fTablePart.getSelectionCount() > 0;
	}

}
