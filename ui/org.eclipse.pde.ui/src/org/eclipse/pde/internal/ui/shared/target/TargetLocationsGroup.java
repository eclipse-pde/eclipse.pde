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

import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
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
	private Button fShowReposButton;

	private ITargetDefinition fTarget;
	private ListenerList fChangeListeners = new ListenerList();

	/**
	 * Creates this part using the form toolkit and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to create the widgets with
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
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
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
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
		fShowReposButton = toolkit.createButton(buttonComp, Messages.TargetLocationsGroup_EditRepos, SWT.PUSH);

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
		fShowReposButton = SWTFactory.createPushButton(buttonComp, Messages.TargetLocationsGroup_EditRepos, null);

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
		fTreeViewer.setLabelProvider(new StyledBundleLabelProvider(true, true));
		fTreeViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				boolean isStatus1 = e1 instanceof IStatus;
				boolean isStatus2 = e2 instanceof IStatus;
				if (isStatus1 && !isStatus2) {
					return -1;
				}
				if (!isStatus1 && isStatus2) {
					return 1;
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

		fShowReposButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEditRepos();
			}
		});
		fShowReposButton.setLayoutData(new GridData());
		SWTFactory.setButtonDimensionHint(fShowReposButton);
	}

	/**
	 * Sets the target definition model to use as input for the tree, can be called with different
	 * models to change the tree's input.
	 * @param target target model
	 */
	public void setInput(ITargetDefinition target) {
		fTarget = target;
		fTreeViewer.setInput(fTarget);
		fTreeViewer.expandAll();
		updateButtons();
	}

	private void handleAdd() {
		AddBundleContainerWizard wizard = new AddBundleContainerWizard(fTarget);
		Shell parent = fTreeViewer.getTree().getShell();
		WizardDialog dialog = new WizardDialog(parent, wizard);
		if (dialog.open() != Window.CANCEL) {
			contentsChanged();
			fTreeViewer.refresh();
			fTreeViewer.expandAll();
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
			}
			if (oldContainer != null) {
				Shell parent = fTreeViewer.getTree().getShell();
				EditBundleContainerWizard wizard = new EditBundleContainerWizard(oldContainer);
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
						contentsChanged();
						fTreeViewer.refresh();
						fTreeViewer.expandAll();
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
			Set newContainers = new HashSet();
			newContainers.addAll(Arrays.asList(fTarget.getBundleContainers()));
			Iterator iterator = selection.iterator();
			while (iterator.hasNext()) {
				Object currentSelection = iterator.next();
				if (currentSelection instanceof IBundleContainer) {
					newContainers.remove(currentSelection);
				} else if (currentSelection instanceof InstallableUnitWrapper) {
					IUBundleContainer container = ((InstallableUnitWrapper) currentSelection).getContainer();
					newContainers.remove(container);
					// If there are other root IUs in the container, add a new container without the given IU
					NameVersionDescriptor[] oldIUs = null;
					try {
						oldIUs = container.getRootIUs();
					} catch (CoreException e) {
						// Method should only be returning an array, ignore any errors
					}
					if (oldIUs != null && oldIUs.length > 1) {
						List newIUs = new ArrayList();
						for (int i = 0; i < oldIUs.length; i++) {
							if (!oldIUs[i].equals(currentSelection)) {
								newIUs.add(currentSelection);
							}
						}
						newContainers.add(new IUBundleContainer((NameVersionDescriptor[]) newIUs.toArray(new NameVersionDescriptor[newIUs.size()])));
					}
				}
			}
			if (newContainers.size() > 0) {
				fTarget.setBundleContainers((IBundleContainer[]) newContainers.toArray(new IBundleContainer[newContainers.size()]));
			} else {
				fTarget.setBundleContainers(null);
			}

			contentsChanged();
			fTreeViewer.refresh(false);
			fTreeViewer.expandAll();
			updateButtons();
		}
	}

	private void handleEditRepos() {
		RepositoriesDialog dialog = new RepositoriesDialog(fTreeViewer.getTree().getShell(), fTarget);
		if (dialog.open() == Window.OK) {
			fTarget.setRepositories(dialog.getRepositories());
			contentsChanged();
		}
	}

	private void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		fEditButton.setEnabled(!selection.isEmpty() && selection.getFirstElement() instanceof IBundleContainer);
		// If any container is selected, allow the remove (the remove ignores non-container entries)
		boolean removeAllowed = false;
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			if (next instanceof IBundleContainer || next instanceof InstallableUnitWrapper) {
				removeAllowed = true;
				break;
			}
		}
		fRemoveButton.setEnabled(removeAllowed);
	}

	/**
	 * Informs the reporter for this table that something has changed
	 * and is dirty.
	 */
	private void contentsChanged() {
		Object[] listeners = fChangeListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			((ITargetChangedListener) listeners[i]).contentsChanged(fTarget, this, true);
		}
	}

	/**
	 * Content provider for the tree, primary input is a ITargetDefinition, children are IBundleContainers
	 */
	class BundleContainerContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof MultiStatus) {
				return ((MultiStatus) parentElement).getChildren();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof MultiStatus && ((MultiStatus) element).getChildren().length > 0) {
				return true;
			}
			return false;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ITargetDefinition) {
				List children = new ArrayList();

				IStatus resolveStatus = ((ITargetDefinition) inputElement).getResolveStatus();
				if (resolveStatus != null && resolveStatus.getSeverity() != IStatus.OK) {
					children.add(resolveStatus);
				}

				IBundleContainer[] containers = ((ITargetDefinition) inputElement).getBundleContainers();
				for (int i = 0; i < containers.length; i++) {
					if (containers[i] instanceof IUBundleContainer) {
						try {
							NameVersionDescriptor[] descriptions = ((IUBundleContainer) containers[i]).getRootIUs();
							if (descriptions != null && descriptions.length > 0) {
								for (int j = 0; j < descriptions.length; j++) {
									children.add(new InstallableUnitWrapper(descriptions[j], ((IUBundleContainer) containers[i]), fTarget));
								}
							} else {
								children.add(containers[i]);
							}
						} catch (CoreException e) {
							// Just ignore the container if there is an error getting the IUs
						}
					} else {
						children.add(containers[i]);
					}
				}
				return children.toArray();
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
