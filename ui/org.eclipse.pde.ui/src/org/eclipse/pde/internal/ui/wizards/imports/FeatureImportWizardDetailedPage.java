/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.StatusWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class FeatureImportWizardDetailedPage extends StatusWizardPage {
	private static final String KEY_TITLE =
		"FeatureImportWizard.DetailedPage.title";
	private static final String KEY_DESC =
		"FeatureImportWizard.DetailedPage.desc";
	private FeatureImportWizardFirstPage firstPage;
	private IPath dropLocation;
	private CheckboxTreeViewer featureTreeViewer;
	private TreePart treePart;
	private static final String KEY_SHOW_NAMES =
		"FeatureImportWizard.DetailedPage.showNames";
	private static final String KEY_FEATURE_LIST =
		"FeatureImportWizard.DetailedPage.featureList";

	private static final String KEY_LOADING_RUNTIME =
		"FeatureImportWizard.messages.loadingRuntime";
	private static final String KEY_UPDATING =
		"FeatureImportWizard.messages.updating";
	private static final String KEY_LOADING_FILE =
		"FeatureImportWizard.messages.loadingFile";
	private static final String KEY_NO_FEATURES =
		"FeatureImportWizard.messages.noFeatures";
	private static final String KEY_NO_SELECTED =
		"FeatureImportWizard.errors.noFeatureSelected";
	private IFeatureModel[] models;
	private boolean block;

	public class FeatureContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getElements(Object parent) {
			Object[] result = getRoots();
			return result != null ? result : new Object[0];
		}
		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

	}

	class FeatureTreePart extends TreePart {
		public FeatureTreePart(String[] buttonLabels) {
			super(buttonLabels);
		}
		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormWidgetFactory factory) {
			style |= SWT.H_SCROLL | SWT.V_SCROLL;
			if (factory == null) {
				style |= SWT.BORDER;
			} else {
				style |= FormWidgetFactory.BORDER_STYLE;
			}
			CheckboxTreeViewer treeViewer =
				new CheckboxTreeViewer(parent, style);
			treeViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent e) {
					FeatureTreePart.this.selectionChanged(
						(IStructuredSelection) e.getSelection());
				}
			});
			treeViewer.addDoubleClickListener(new IDoubleClickListener() {
				public void doubleClick(DoubleClickEvent e) {
					FeatureTreePart.this.handleDoubleClick(
						(IStructuredSelection) e.getSelection());
				}
			});
			treeViewer.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent e) {
					featureChecked(
						(IFeatureModel) e.getElement(),
						e.getChecked());
				}
			});
			return treeViewer;
		}
		protected void createMainLabel(Composite parent, int span, FormWidgetFactory factory) {
			Label label = new Label(parent, SWT.NULL);
			GridData gd= new GridData();
			gd.horizontalSpan = span;
			label.setText(PDEPlugin.getResourceString(KEY_FEATURE_LIST));
			label.setLayoutData(gd);
		}
		protected Button createButton(Composite parent, String label, int index, FormWidgetFactory factory) {
			Button button = super.createButton(parent, label, index, factory);
			SWTUtil.setButtonDimensionHint(button);
			return button;
		}
		public void buttonSelected(Button button, int index) {
			FeatureImportWizardDetailedPage.this.buttonSelected(index);
		}
	}

	public FeatureImportWizardDetailedPage(FeatureImportWizardFirstPage firstPage) {
		super("FeatureImportWizardDetailedPage", false);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		this.firstPage = firstPage;
		dropLocation = null;
		updateStatus(createStatus(IStatus.ERROR, ""));

		String[] buttonLabels =
			{
				PDEPlugin.getResourceString(
					WizardCheckboxTablePart.KEY_SELECT_ALL),
				PDEPlugin.getResourceString(
					WizardCheckboxTablePart.KEY_DESELECT_ALL),
				};

		treePart = new FeatureTreePart(buttonLabels);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	private void initializeFields(IPath dropLocation) {
		if (!dropLocation.equals(this.dropLocation)) {
			updateStatus(createStatus(IStatus.OK, ""));
			this.dropLocation = dropLocation;
			models = null;
		}
		if (models == null) {
			getModels(); // force loading
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask(
						PDEPlugin.getResourceString(KEY_UPDATING),
						IProgressMonitor.UNKNOWN);
					featureTreeViewer
						.getControl()
						.getDisplay()
						.asyncExec(new Runnable() {
						public void run() {
							featureTreeViewer.setInput(PDEPlugin.getDefault());
							if (getModels()!=null)
								featureTreeViewer.setCheckedElements(getModels());
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
			}
			finally {
				dialogChanged();
			}
			//treePart.updateCounter(0);
		}
	}

	public void storeSettings(boolean finishPressed) {
	}

	/*
	 * @see DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initializeFields(firstPage.getDropLocation());
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

		treePart.createControl(container, SWT.NULL, 2, null);
		featureTreeViewer = (CheckboxTreeViewer) treePart.getTreeViewer();
		featureTreeViewer.setContentProvider(new FeatureContentProvider());
		featureTreeViewer.setLabelProvider(
			PDEPlugin.getDefault().getLabelProvider());
		GridData gd = (GridData) treePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(
			container,
			IHelpContextIds.PLUGIN_IMPORT_SECOND_PAGE);
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public IFeatureModel[] getModels() {
		if (models != null)
			return models;

		final ArrayList result = new ArrayList();
		final IPath home = dropLocation;
		if (home != null) {
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
					monitor.beginTask(
						PDEPlugin.getResourceString(KEY_LOADING_FILE),
						IProgressMonitor.UNKNOWN);

					try {
						MultiStatus errors =
							doLoadFeatures(result, createPath(home), monitor);
						if (errors != null
							&& errors.getChildren().length > 0) {
							PDEPlugin.log(errors);
						}
						models =
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
		return models;
	}
	
	private Object[] getRoots() {
		return getModels();
	}

	private File createPath(IPath dropLocation) {
		File featuresDir = new File(dropLocation.toFile(), "features");
		if (featuresDir.exists())
			return featuresDir;
		return null;
	}

	private MultiStatus doLoadFeatures(
		ArrayList result,
		File path,
		IProgressMonitor monitor)
		throws CoreException {
		if (path==null) return null;
		File[] dirs = path.listFiles();
		monitor.beginTask("Loading...", dirs.length);
		ArrayList resultStatus = new ArrayList();
		for (int i = 0; i < dirs.length; i++) {
			File dir = dirs[i];
			if (dir.isDirectory()) {
				File manifest = new File(dir, "feature.xml");
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
				(IStatus[]) resultStatus.toArray(
					new IStatus[resultStatus.size()]);
			MultiStatus multiStatus =
				new MultiStatus(
					PDEPlugin.PLUGIN_ID,
					IStatus.OK,
					children,
					"Problems encountered while loading features",
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
		Object[] selected = featureTreeViewer.getCheckedElements();
		IFeatureModel[] result = new IFeatureModel[selected.length];
		System.arraycopy(selected, 0, result, 0, selected.length);
		return result;
	}

	private void dialogChanged() {
		IStatus genStatus = validateFeatures();
		updateStatus(genStatus);
	}

	private IStatus validateFeatures() {
		IFeatureModel[] allModels = getModels();
		if (allModels == null || allModels.length == 0) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_NO_FEATURES));
		}
		if (featureTreeViewer.getCheckedElements().length == 0) {
			return createStatus(
				IStatus.INFO,
				PDEPlugin.getResourceString(KEY_NO_SELECTED));
		}
		return createStatus(IStatus.OK, "");
	}

	private void featureChecked(IFeatureModel model, boolean checked) {
		dialogChanged();
	}

	private void buttonSelected(int index) {
		if (index == 0)
			doSelectAll(true);
		else
			doSelectAll(false);
	}

	private void doSelectAll(boolean select) {
		if (select)
			featureTreeViewer.setCheckedElements(getModels());
		else
			featureTreeViewer.setCheckedElements(new Object[0]);
		dialogChanged();
	}
}