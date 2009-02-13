/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import com.ibm.icu.text.MessageFormat;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.target.impl.*;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.progress.UIJob;

/**
 * UI part that can be added to a dialog or to a form editor.  Contains a table displaying
 * the bundle containers of a target definition.  Also has buttons to add, edit and remove
 * bundle containers of varying types.
 * 
 * @see TargetEditor
 * @see ITargetDefinition
 * @see IBundleContainer
 */
public class BundleContainerTable {

	private TreeViewer fTreeViewer;
	private Button fAddButton;
	private Button fEditButton;
	private Button fRemoveButton;
	private Button fRemoveAllButton;
	private Label fShowLabel;
	private Button fShowPluginsButton;
	private Button fShowSourceButton;
	private ViewerFilter fPluginFilter;
	private ViewerFilter fSourceFilter;

	private ITargetDefinition fTarget;
	private IBundleContainerTableReporter fReporter; // TODO Remove when proper model/editor listening is done

	/**
	 * Creates this part using the form toolkit and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to create the widgets with
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 * @return generated instance of the table part
	 */
	public static BundleContainerTable createTableInForm(Composite parent, FormToolkit toolkit, IBundleContainerTableReporter reporter) {
		BundleContainerTable contentTable = new BundleContainerTable(reporter);
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	/**
	 * Creates this part using standard dialog widgets and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 * @return generated instance of the table part
	 */
	public static BundleContainerTable createTableInDialog(Composite parent, IBundleContainerTableReporter reporter) {
		BundleContainerTable contentTable = new BundleContainerTable(reporter);
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	/**
	 * Private constructor, use one of {@link #createTableInDialog(Composite, IBundleContainerTableReporter)}
	 * or {@link #createTableInForm(Composite, FormToolkit, IBundleContainerTableReporter)}.
	 * 
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 */
	private BundleContainerTable(IBundleContainerTableReporter reporter) {
		fReporter = reporter;
	}

	/**
	 * Creates the part contents from a toolkit
	 * @param parent parent composite
	 * @param toolkit form toolkit to create widgets
	 */
	private void createFormContents(Composite parent, FormToolkit toolkit) {
		Composite comp = toolkit.createComposite(parent);
		comp.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		Tree atree = toolkit.createTree(comp, SWT.V_SCROLL | SWT.H_SCROLL);
		atree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		atree.setLayoutData(gd);

		Composite buttonComp = toolkit.createComposite(comp);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_0, SWT.PUSH);
		fEditButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_1, SWT.PUSH);
		fRemoveButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_2, SWT.PUSH);
		fRemoveAllButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_3, SWT.PUSH);

		Composite filterComp = toolkit.createComposite(buttonComp);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		filterComp.setLayout(layout);
		filterComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

		fShowLabel = toolkit.createLabel(filterComp, Messages.BundleContainerTable_9);
		fShowPluginsButton = toolkit.createButton(filterComp, Messages.BundleContainerTable_14, SWT.CHECK);
		fShowSourceButton = toolkit.createButton(filterComp, Messages.BundleContainerTable_15, SWT.CHECK);

		initializeTreeViewer(atree);
		initializeButtons();
		initializeFilters();

		toolkit.paintBordersFor(comp);
	}

	/**
	 * Creates the part contents using SWTFactory
	 * @param parent parent composite
	 */
	private void createDialogContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);

		Tree atree = new Tree(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		atree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		atree.setLayoutData(gd);

		Composite buttonComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_0, null);
		fEditButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_1, null);
		fRemoveButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_2, null);
		fRemoveAllButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_3, null);

		Composite filterComp = SWTFactory.createComposite(buttonComp, 1, 1, GridData.BEGINNING, 0, 0);
		filterComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

		fShowLabel = SWTFactory.createLabel(filterComp, Messages.BundleContainerTable_9, 1);
		fShowPluginsButton = SWTFactory.createCheckButton(filterComp, Messages.BundleContainerTable_14, null, true, 1);
		fShowSourceButton = SWTFactory.createCheckButton(filterComp, Messages.BundleContainerTable_15, null, true, 1);

		initializeTreeViewer(atree);
		initializeButtons();
		initializeFilters();
	}

	/**
	 * Sets up the tree viewer using the given tree
	 * @param tree
	 */
	private void initializeTreeViewer(Tree tree) {
		fTreeViewer = new TreeViewer(tree);
		fTreeViewer.setContentProvider(new TargetContentProvider());
		fTreeViewer.setLabelProvider(new TargetLabelProvider());
		fTreeViewer.setComparator(new ViewerComparator());
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (!event.getSelection().isEmpty()) {
					Object selectedElement = ((IStructuredSelection) event.getSelection()).getFirstElement();
					fTreeViewer.setExpandedState(selectedElement, !fTreeViewer.getExpandedState(selectedElement));
				}
			}
		});
		fTreeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
	}

	/**
	 * Sets up the buttons, the button fields must already be created before calling this method
	 */
	private void initializeButtons() {
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		fAddButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fAddButton);

		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
			}
		});
		fEditButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fEditButton);

		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		fRemoveButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fRemoveButton);

		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		fRemoveAllButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fRemoveAllButton);

		fShowPluginsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fShowPluginsButton.getSelection()) {
					fTreeViewer.addFilter(fPluginFilter);
				} else {
					fTreeViewer.removeFilter(fPluginFilter);
				}
			}
		});
		fShowPluginsButton.setSelection(true);
		GridData gd = new GridData();
		gd.horizontalIndent = 10;
		fShowPluginsButton.setLayoutData(gd);

		fShowSourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fShowSourceButton.getSelection()) {
					fTreeViewer.addFilter(fSourceFilter);
				} else {
					fTreeViewer.removeFilter(fSourceFilter);
				}
			}
		});
		fShowSourceButton.setSelection(true);
		gd = new GridData();
		gd.horizontalIndent = 10;
		fShowSourceButton.setLayoutData(gd);
	}

	private void initializeFilters() {
		fSourceFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IResolvedBundle) {
					if (((IResolvedBundle) element).isSourceBundle()) {
						return false;
					}
				}
				return true;
			}
		};
		fPluginFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IResolvedBundle) {
					if (!((IResolvedBundle) element).isSourceBundle()) {
						return false;
					}
				}
				return true;
			}
		};
	}

	/**
	 * Sets the target definition model to use as input for the tree, can be called with different
	 * models to change the tree's input.
	 * @param target target model
	 */
	public void setInput(ITargetDefinition target) {
		fTarget = target;
		refresh();
	}

	/**
	 * Refreshes the contents of the table
	 */
	public void refresh() {
		if (!fTarget.isResolved()) {
			fReporter.runResolveOperation(new ResolveContainersOperation());
		} else {
			fTreeViewer.setInput(fTarget);
			fTreeViewer.refresh();
			updateButtons();
		}
	}

	private void setEnabled(boolean enablement) {
		fTreeViewer.getControl().setEnabled(enablement);
		fAddButton.setEnabled(enablement);
		fRemoveButton.setEnabled(enablement);
		fRemoveAllButton.setEnabled(enablement);
		fEditButton.setEnabled(enablement);
		fShowLabel.setEnabled(enablement);
		fShowPluginsButton.setEnabled(enablement);
		fShowSourceButton.setEnabled(enablement);
	}

	private void handleAdd() {
		AddBundleContainerWizard wizard = new AddBundleContainerWizard((ITargetDefinition) fTreeViewer.getInput());
		Shell parent = fTreeViewer.getTree().getShell();
		WizardDialog dialog = new WizardDialog(parent, wizard);
		if (dialog.open() != Window.CANCEL) {
			refresh();
			contentsChanged();
		}
	}

	private void handleEdit() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (!selection.isEmpty()) {
			Object selected = selection.getFirstElement();
			IBundleContainer container = null;
			if (selected instanceof IBundleContainer) {
				container = (IBundleContainer) selected;
			} else if (selected instanceof BundleInfo) {
				// TODO Selecting a child should allow editing its parent.
			}
			if (container != null) {
				// We need to get a list of all possible bundles, remove restrictions while resolving
				BundleInfo[] oldRestrictions = container.getIncludedBundles();
				IResolvedBundle[] resolvedBundles = null;
				try {
					container.setIncludedBundles(null);
					resolvedBundles = container.getBundles();
				} finally {
					container.setIncludedBundles(oldRestrictions);
				}

				RestrictionsListSelectionDialog dialog = new RestrictionsListSelectionDialog(fTreeViewer.getTree().getShell(), resolvedBundles, oldRestrictions);
				if (dialog.open() == Window.OK) {
					Object[] result = dialog.getResult();
					if (result != null) {
						if (result.length == resolvedBundles.length) {
							container.setIncludedBundles(null);
							if (oldRestrictions != null) {
								contentsChanged();
								refresh();
							}
						} else {
							BundleInfo[] selectedRestrictions = new BundleInfo[result.length];
							for (int i = 0; i < result.length; i++) {
								IResolvedBundle rb = (IResolvedBundle) result[i];
								selectedRestrictions[i] = rb.getBundleInfo();
							}
							BundleInfo[] newRestrictions = new BundleInfo[selectedRestrictions.length];
							for (int i = 0; i < selectedRestrictions.length; i++) {
								newRestrictions[i] = new BundleInfo(selectedRestrictions[i].getSymbolicName(), dialog.isUseVersion() ? selectedRestrictions[i].getVersion() : null, null, BundleInfo.NO_LEVEL, false);
							}
							container.setIncludedBundles(newRestrictions);
							contentsChanged();
							refresh();
						}
					}
				}
			}
		}
	}

	private void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (!selection.isEmpty()) {
			Object selected = selection.getFirstElement();
			IBundleContainer container = null;
			if (selected instanceof IBundleContainer) {
				container = (IBundleContainer) selected;
			} else if (selected instanceof BundleInfo) {
				// TODO Selecting a child should allow removing its parent?
			}
			IBundleContainer[] currentContainers = fTarget.getBundleContainers();
			ArrayList newBundleContainers = new ArrayList(currentContainers.length);
			for (int i = 0; i < currentContainers.length; i++) {
				if (!currentContainers[i].equals(container)) {
					newBundleContainers.add(currentContainers[i]);
				}
			}
			fTarget.setBundleContainers((IBundleContainer[]) newBundleContainers.toArray(new IBundleContainer[newBundleContainers.size()]));
			contentsChanged();
			refresh();
		}
	}

	private void handleRemoveAll() {
		fTarget.setBundleContainers(null);
		contentsChanged();
		refresh();
	}

	private void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		// TODO Support editing and removing of bundles directly
		fEditButton.setEnabled(!selection.isEmpty() && selection.getFirstElement() instanceof IBundleContainer);
		fRemoveButton.setEnabled(!selection.isEmpty() && selection.getFirstElement() instanceof IBundleContainer);
		fRemoveAllButton.setEnabled(fTarget.getBundleContainers() != null && fTarget.getBundleContainers().length > 0);
	}

	private void contentsChanged() {
		fReporter.contentsChanged();
	}

	/**
	 * Runnable that resolves the target.  Disables the table while running
	 */
	class ResolveContainersOperation implements IRunnableWithProgress {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			Job job = new UIJob(Messages.BundleContainerTable_16) {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					if (!fTreeViewer.getControl().isDisposed()) {
						setEnabled(false);
						fTreeViewer.setInput(Messages.BundleContainerTable_17);
						fTreeViewer.refresh();
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
			fTarget.resolve(monitor);
			if (!monitor.isCanceled()) {
				job = new UIJob(Messages.BundleContainerTable_18) {
					public IStatus runInUIThread(IProgressMonitor monitor) {
						if (!fTreeViewer.getControl().isDisposed()) {
							setEnabled(true);
							fTreeViewer.setInput(fTarget);
							fTreeViewer.refresh();
							updateButtons();
						}
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
			}
		}
	}

	/**
	 * Content provider for the tree, primary input is a ITargetDefinition, children are IBundleContainers
	 */
	class TargetContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ITargetDefinition) {
				IBundleContainer[] containers = ((ITargetDefinition) parentElement).getBundleContainers();
				return containers != null ? containers : new Object[0];
			} else if (parentElement instanceof IBundleContainer) {
				IBundleContainer container = (IBundleContainer) parentElement;
				if (container.isResolved()) {
					IStatus status = container.getBundleStatus();
					if (!status.isOK() && !status.isMultiStatus()) {
						return new Object[] {status};
					}
					return container.getBundles();
				}
				// We should only be populating the table if the containers are resolved, but just in case
				return new Object[] {new Status(IStatus.ERROR, PDEPlugin.getPluginId(), Messages.BundleContainerTable_19)};
			} else if (parentElement instanceof IResolvedBundle) {
				IStatus status = ((IResolvedBundle) parentElement).getStatus();
				if (!status.isOK()) {
					return new Object[] {status};
				}
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			// Since we are already resolved we can't be more efficient
			return getChildren(element).length > 0;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ITargetDefinition) {
				IBundleContainer[] containers = ((ITargetDefinition) inputElement).getBundleContainers();
				if (containers != null) {
					return containers;
				}
			} else if (inputElement instanceof String) {
				return new Object[] {inputElement};
			}
			return new Object[0];
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	/**
	 * Label provider for the tree
	 */
	class TargetLabelProvider extends BundleInfoLabelProvider {
		public String getText(Object element) {
			try {
				if (element instanceof FeatureBundleContainer) {
					FeatureBundleContainer container = (FeatureBundleContainer) element;
					String version = container.getFeatureVersion();
					if (version != null) {
						return MessageFormat.format(Messages.BundleContainerTable_5, new String[] {container.getFeatureId(), version, container.getLocation(false), getIncludedBundlesLabel(container)});
					}
					return MessageFormat.format(Messages.BundleContainerTable_6, new String[] {container.getFeatureId(), container.getLocation(false), getIncludedBundlesLabel(container)});
				} else if (element instanceof DirectoryBundleContainer) {
					DirectoryBundleContainer container = (DirectoryBundleContainer) element;
					return MessageFormat.format(Messages.BundleContainerTable_7, new String[] {container.getLocation(false), getIncludedBundlesLabel(container)});
				} else if (element instanceof ProfileBundleContainer) {
					ProfileBundleContainer container = (ProfileBundleContainer) element;
					String config = container.getConfigurationLocation();
					if (config != null) {
						return MessageFormat.format(Messages.BundleContainerTable_8, new String[] {container.getLocation(false), config, getIncludedBundlesLabel(container)});
					}
					return MessageFormat.format(Messages.BundleContainerTable_7, new String[] {container.getLocation(false), getIncludedBundlesLabel(container)});
				}
			} catch (CoreException e) {
				return MessageFormat.format(Messages.BundleContainerTable_4, new String[] {e.getMessage()});
			}
			if (element instanceof IStatus) {
				return ((IStatus) element).getMessage();
			}
			return super.getText(element);
		}

		public Image getImage(Object element) {
			if (element instanceof IBundleContainer) {
				int flag = 0;
				IBundleContainer container = (IBundleContainer) element;
				if (container.isResolved()) {
					IStatus status = container.getBundleStatus();
					if (status.getSeverity() == IStatus.WARNING) {
						flag = SharedLabelProvider.F_WARNING;
					} else if (status.getSeverity() == IStatus.ERROR) {
						flag = SharedLabelProvider.F_ERROR;
					}
				}
				if (element instanceof FeatureBundleContainer) {
					return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ, flag);
				} else if (element instanceof DirectoryBundleContainer) {
					ImageDescriptor image = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
					return PDEPlugin.getDefault().getLabelProvider().get(image, flag);
				} else if (element instanceof ProfileBundleContainer) {
					return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION, flag);
				}
			} else if (element instanceof IStatus) {
				int severity = ((IStatus) element).getSeverity();
				if (severity == IStatus.WARNING) {
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
				} else if (severity == IStatus.ERROR) {
					return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
				}
			}

			return super.getImage(element);
		}

		/**
		 * Returns a label describing the number of bundles included (ex. 5 of 10 plug-ins)
		 * or an empty string if there is a problem determining the number of bundles
		 * @param container bundle container to check for inclusions
		 * @return string label
		 */
		private String getIncludedBundlesLabel(IBundleContainer container) {
			// TODO Provide convenience methods in IBundleContainer to access all bundles?
			if (!container.isResolved() || (!container.getBundleStatus().isOK() && !container.getBundleStatus().isMultiStatus()) || container.getBundles() == null) {
				return ""; //$NON-NLS-1$
			}

			BundleInfo[] restrictions = container.getIncludedBundles();
			if (restrictions != null) {
				container.setIncludedBundles(null);
			}
			int bundleCount = container.getBundles().length;
			String bundleCountString = Integer.toString(bundleCount);
			if (restrictions != null) {
				container.setIncludedBundles(restrictions);
			}

			if (restrictions != null && restrictions.length > bundleCount) {
				// If some bundles are missing, the bundleCount is likely wrong, just do the best we can
				return ""; //$NON-NLS-1$
			}

			return MessageFormat.format(Messages.BundleContainerTable_10, new String[] {restrictions != null ? Integer.toString(restrictions.length) : bundleCountString, bundleCountString});

		}
	}

}
