/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * UI part that can be added to a dialog or to a form editor.  Contains a table displaying
 * the bundle containers of a target definition.  Also has buttons to add, edit and remove
 * bundle containers of varying types.
 * 
 * @see TargetEditor
 * @see TargetDefinitionContentPage
 * @see ITargetDefinition
 * @see IBundleContainer
 */
public class TargetLocationsGroup {

	private TreeViewer fTreeViewer;
	private Button fAddButton;
	private Button fEditButton;
	private Button fRemoveButton;
	private Button fUpdateButton;
	private Button fShowContentButton;

	private ITargetDefinition fTarget;
	private ListenerList fChangeListeners = new ListenerList();

	/**
	 * Creates this part using the form toolkit and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to create the widgets with
	 * @return generated instance of the table part
	 */
	public static TargetLocationsGroup createInForm(Composite parent, FormToolkit toolkit) {
		TargetLocationsGroup contentTable = new TargetLocationsGroup();
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	/**
	 * Creates this part using standard dialog widgets and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @return generated instance of the table part
	 */
	public static TargetLocationsGroup createInDialog(Composite parent) {
		TargetLocationsGroup contentTable = new TargetLocationsGroup();
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	/**
	 * Private constructor, use one of {@link #createTableInDialog(Composite, ITargetChangedListener)}
	 * or {@link #createTableInForm(Composite, FormToolkit, ITargetChangedListener)}.
	 * 
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 */
	private TargetLocationsGroup() {

	}

	/**
	 * Adds a listener to the set of listeners that will be notified when the bundle containers
	 * are modified.  This method has no effect if the listener has already been added. 
	 * 
	 * @param listener target changed listener to add
	 */
	public void addTargetChangedListener(ITargetChangedListener listener) {
		fChangeListeners.add(listener);
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

		Tree atree = toolkit.createTree(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		atree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		atree.setLayoutData(gd);
		atree.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL && fRemoveButton.getEnabled()) {
					handleRemove();
				}

			}
		});

		Composite buttonComp = toolkit.createComposite(comp);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_0, SWT.PUSH);
		fEditButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_1, SWT.PUSH);
		fRemoveButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_2, SWT.PUSH);
		fUpdateButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_3, SWT.PUSH);

		fShowContentButton = toolkit.createButton(comp, Messages.TargetLocationsGroup_1, SWT.CHECK);

		initializeTreeViewer(atree);
		initializeButtons();

		toolkit.paintBordersFor(comp);
	}

	/**
	 * Creates the part contents using SWTFactory
	 * @param parent parent composite
	 */
	private void createDialogContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);

		Tree atree = new Tree(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI);
		atree.setFont(comp.getFont());
		atree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		atree.setLayoutData(gd);
		atree.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL && fRemoveButton.getEnabled()) {
					handleRemove();
				}
			}
		});

		Composite buttonComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_0, null);
		fEditButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_1, null);
		fRemoveButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_2, null);
		fUpdateButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_3, null);

		fShowContentButton = SWTFactory.createCheckButton(comp, Messages.TargetLocationsGroup_1, null, false, 2);

		initializeTreeViewer(atree);
		initializeButtons();
	}

	/**
	 * Sets up the tree viewer using the given tree
	 * @param tree
	 */
	private void initializeTreeViewer(Tree tree) {
		fTreeViewer = new TreeViewer(tree);
		fTreeViewer.setContentProvider(new BundleContainerContentProvider());
		fTreeViewer.setLabelProvider(new StyledBundleLabelProvider(true, false));
		fTreeViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				// Status at the end of the list
				if (e1 instanceof IStatus && !(e2 instanceof IStatus)) {
					return 1;
				}
				if (e2 instanceof IStatus && !(e1 instanceof IStatus)) {
					return -1;
				}
				return super.compare(viewer, e1, e2);
			}
		});
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (!event.getSelection().isEmpty()) {
					handleEdit();
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

		fUpdateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleUpdate();
			}
		});
		fUpdateButton.setLayoutData(new GridData());
		fUpdateButton.setEnabled(false);
		SWTFactory.setButtonDimensionHint(fUpdateButton);

		fShowContentButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTreeViewer.refresh();
				fTreeViewer.expandAll();
			}
		});
		fShowContentButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fShowContentButton);
	}

	/**
	 * Sets the target definition model to use as input for the tree, can be called with different
	 * models to change the tree's input.
	 * @param target target model
	 */
	public void setInput(ITargetDefinition target) {
		fTarget = target;
		fTreeViewer.setInput(fTarget);
		updateButtons();
	}

	private void handleAdd() {
		AddBundleContainerWizard wizard = new AddBundleContainerWizard(fTarget);
		Shell parent = fTreeViewer.getTree().getShell();
		WizardDialog dialog = new WizardDialog(parent, wizard);
		if (dialog.open() != Window.CANCEL) {
			contentsChanged(false);
			fTreeViewer.refresh();
			updateButtons();
		}
	}

	private void handleEdit() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (!selection.isEmpty()) {
			Object selected = selection.getFirstElement();
			IBundleContainer oldContainer = null;
			if (selected instanceof IBundleContainer) {
				oldContainer = (IBundleContainer) selected;
			} else if (selected instanceof IUWrapper) {
				oldContainer = ((IUWrapper) selected).getParent();
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
						contentsChanged(false);
						fTreeViewer.refresh();
						updateButtons();
						fTreeViewer.setSelection(new StructuredSelection(newContainer), true);
					}
				}
			}
		}
	}

	private void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		IBundleContainer[] containers = fTarget.getBundleContainers();
		if (!selection.isEmpty() && containers != null && containers.length > 0) {
			List toRemove = new ArrayList();
			boolean removedSite = false;
			boolean removedContainer = false;
			for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
				Object currentSelection = iterator.next();
				if (currentSelection instanceof IBundleContainer) {
					if (currentSelection instanceof IUBundleContainer) {
						removedSite = true;
					}
					removedContainer = true;
					toRemove.add(currentSelection);
				}
				if (currentSelection instanceof IUWrapper) {
					toRemove.add(currentSelection);
				}
			}

			if (removedContainer) {
				Set newContainers = new HashSet();
				newContainers.addAll(Arrays.asList(fTarget.getBundleContainers()));
				newContainers.removeAll(toRemove);
				if (newContainers.size() > 0) {
					fTarget.setBundleContainers((IBundleContainer[]) newContainers.toArray(new IBundleContainer[newContainers.size()]));
				} else {
					fTarget.setBundleContainers(null);
				}

				// If we remove a site container, the content change update must force a re-resolve bug 275458 / bug 275401
				contentsChanged(removedSite);
				fTreeViewer.refresh(false);
				updateButtons();
			} else {
				for (Iterator iterator = toRemove.iterator(); iterator.hasNext();) {
					Object current = iterator.next();
					if (current instanceof IUWrapper) {
						((IUWrapper) current).getParent().removeInstallableUnit(((IUWrapper) current).getIU());
					}
				}
				contentsChanged(removedSite);
				fTreeViewer.refresh(true);
				updateButtons();
			}
		}
	}

	private void handleUpdate() {
		// go over the selection and make a map from container to set of ius to update.
		// if the set is empty then the whole container is to be updated.
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		Map toUpdate = new HashMap();
		for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
			Object currentSelection = iterator.next();
			if (currentSelection instanceof IBundleContainer)
				toUpdate.put(currentSelection, new HashSet(0));
			else if (currentSelection instanceof IUWrapper) {
				IUWrapper wrapper = (IUWrapper) currentSelection;
				Set iuSet = (Set) toUpdate.get(wrapper.getParent());
				if (iuSet == null) {
					iuSet = new HashSet();
					iuSet.add(wrapper.getIU().getId());
					toUpdate.put(wrapper.getParent(), iuSet);
				} else if (!iuSet.isEmpty())
					iuSet.add(wrapper.getIU().getId());
			}
		}
		if (toUpdate.isEmpty())
			return;

		JobChangeAdapter listener = new JobChangeAdapter() {
			public void done(final IJobChangeEvent event) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						// XXX what if everything is disposed by the time we get back?
						UpdateTargetJob job = (UpdateTargetJob) event.getJob();
						contentsChanged(job.isUpdated());
						fTreeViewer.refresh(true);
						try {
							ITargetHandle currentTarget = TargetPlatformService.getDefault().getWorkspaceTargetHandle();
							if (job.isUpdated() && fTarget.getHandle().equals(currentTarget))
								LoadTargetDefinitionJob.load(fTarget);
						} catch (CoreException e) {
							// do nothing if we could not see the current target.
						}
						updateButtons();
					}
				});
			}
		};
		UpdateTargetJob.update(toUpdate, listener);
	}

	private void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		fEditButton.setEnabled(!selection.isEmpty() && (selection.getFirstElement() instanceof IBundleContainer || selection.getFirstElement() instanceof IUWrapper));
		// If any container is selected, allow the remove (the remove ignores non-container entries)
		boolean removeAllowed = false;
		boolean updateAllowed = false;
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			if (removeAllowed && updateAllowed) {
				break;
			}
			Object current = iter.next();
			if (current instanceof IUBundleContainer) {
				updateAllowed = true;
			}
			if (current instanceof IBundleContainer) {
				removeAllowed = true;
			}
			if (current instanceof IUWrapper) {
				removeAllowed = true;
				updateAllowed = true;
			}
		}
		fRemoveButton.setEnabled(removeAllowed);
		fUpdateButton.setEnabled(updateAllowed);
	}

	/**
	 * Informs the reporter for this table that something has changed
	 * and is dirty.
	 */
	private void contentsChanged(boolean force) {
		Object[] listeners = fChangeListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			((ITargetChangedListener) listeners[i]).contentsChanged(fTarget, this, true, force);
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
					IStatus status = container.getStatus();
					if (!status.isOK() && !status.isMultiStatus()) {
						return new Object[] {status};
					}
					if (fShowContentButton.getSelection()) {
						return container.getBundles();
					} else if (!status.isOK()) {
						// Show multi-status children so user can easily see problems
						if (status.isMultiStatus()) {
							return status.getChildren();
						}
					} else if (parentElement instanceof IUBundleContainer) {
						// Show the IUs as children
						try {
							// if this is a bundle container then we must be sure that all bundle containers are
							// happy since they all share the same profile.
							if (!P2TargetUtils.isResolved(fTarget)) {
								return new Object[0];
							}
							IInstallableUnit[] units = ((IUBundleContainer) parentElement).getInstallableUnits();
							// Wrap the units so that they remember their parent container
							List wrappedUnits = new ArrayList(units.length);
							for (int i = 0; i < units.length; i++) {
								wrappedUnits.add(new IUWrapper(units[i], (IUBundleContainer) parentElement));
							}
							return wrappedUnits.toArray();
						} catch (CoreException e) {
							return new Object[] {e.getStatus()};
						}
					}
				}
			} else if (parentElement instanceof MultiStatus) {
				return ((MultiStatus) parentElement).getChildren();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof IUWrapper) {
				return ((IUWrapper) element).getParent();
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			// Since we are already resolved we can't be more efficient
			return getChildren(element).length > 0;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ITargetDefinition) {
				boolean hasContainerStatus = false;
				Collection result = new ArrayList();
				IBundleContainer[] containers = ((ITargetDefinition) inputElement).getBundleContainers();
				if (containers != null) {
					for (int i = 0; i < containers.length; i++) {
						result.add(containers[i]);
						if (containers[i].getStatus() != null && !containers[i].getStatus().isOK()) {
							hasContainerStatus = true;
						}
					}
				}
				// If a container has a problem, it is displayed as a child, if there is a status outside of the container status (missing bundle, etc.) put it as a separate item
				if (!hasContainerStatus) {
					IStatus status = ((ITargetDefinition) inputElement).getBundleStatus();
					if (status != null && !status.isOK()) {
						result.add(status);
					}
				}
				return result.toArray();
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
	 * Wraps an installable unit so that it knows what bundle container parent it belongs to
	 * in the tree.
	 */
	class IUWrapper {
		private IInstallableUnit fIU;
		private IUBundleContainer fParent;

		public IUWrapper(IInstallableUnit unit, IUBundleContainer parent) {
			fIU = unit;
			fParent = parent;
		}

		public IInstallableUnit getIU() {
			return fIU;
		}

		public IUBundleContainer getParent() {
			return fParent;
		}
	}

}
