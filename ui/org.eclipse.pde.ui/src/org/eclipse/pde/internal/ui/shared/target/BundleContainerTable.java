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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
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
	private IBundleContainerTableReporter fReporter;

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
		fTreeViewer.setContentProvider(new BundleContainerContentProvider());
		fTreeViewer.setLabelProvider(new BundleContainerLabelProvider());
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
		fEditButton.setEnabled(false);
		SWTFactory.setButtonDimensionHint(fEditButton);

		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		fRemoveButton.setLayoutData(new GridData());
		fRemoveButton.setEnabled(false);
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

		fShowLabel.setEnabled(enablement);
		fShowPluginsButton.setEnabled(enablement);
		fShowSourceButton.setEnabled(enablement);

		if (enablement) {
			updateButtons();
		} else {
			fRemoveButton.setEnabled(enablement);
			fRemoveAllButton.setEnabled(enablement);
			fEditButton.setEnabled(enablement);
		}
	}

	private void handleAdd() {
		AddBundleContainerWizard wizard = new AddBundleContainerWizard(fTarget);
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
			IBundleContainer oldContainer = null;
			if (selected instanceof IBundleContainer) {
				oldContainer = (IBundleContainer) selected;
			} else if (selected instanceof IResolvedBundle) {
				TreeItem[] treeSelection = fTreeViewer.getTree().getSelection();
				if (treeSelection.length > 0) {
					Object parent = treeSelection[0].getParentItem().getData();
					if (parent instanceof IBundleContainer) {
						oldContainer = (IBundleContainer) parent;
					}
				}
			}
			if (oldContainer != null) {
				Shell parent = fTreeViewer.getTree().getShell();
				EditBundleContainerWizard wizard = new EditBundleContainerWizard(fTarget, oldContainer);
				WizardDialog dialog = new WizardDialog(parent, wizard);
				if (dialog.open() == Window.OK) {
					// Replace the old container with the new one
					IBundleContainer newContainer = wizard.getBundleContainer();
					if (newContainer != null) {
						IBundleContainer[] containers = fTarget.getBundleContainers();
						java.util.List newContainers = new ArrayList(containers.length);
						for (int i = 0; i < containers.length; i++) {
							if (!containers[i].equals(oldContainer)) {
								newContainers.add(containers[i]);
							}
						}
						newContainers.add(newContainer);
						fTarget.setBundleContainers((IBundleContainer[]) newContainers.toArray(new IBundleContainer[newContainers.size()]));

						// Update the table
						refresh();
						contentsChanged();
						fTreeViewer.setSelection(new StructuredSelection(newContainer), true);
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
	}

	private void handleRemoveAll() {
		fTarget.setBundleContainers(null);
		contentsChanged();
		refresh();
	}

	private void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		fEditButton.setEnabled(!selection.isEmpty() && (selection.getFirstElement() instanceof IBundleContainer || selection.getFirstElement() instanceof IResolvedBundle));
		fRemoveButton.setEnabled(!selection.isEmpty() && selection.getFirstElement() instanceof IBundleContainer);
		fRemoveAllButton.setEnabled(fTarget.getBundleContainers() != null && fTarget.getBundleContainers().length > 0);
	}

	/**
	 * Informs the reporter for this table that something has changed
	 * and is dirty.
	 */
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
	class BundleContainerContentProvider implements ITreeContentProvider {

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

}
