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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * 
 * @see TargetEditor
 * @see TargetDefinitionContentPage
 * @see ITargetDefinition
 * @see IBundleContainer
 */
public class TargetReposGroup {

	private TableViewer fTableViewer;
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
	public static TargetReposGroup createInForm(Composite parent, FormToolkit toolkit) {
		TargetReposGroup contentTable = new TargetReposGroup();
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
	public static TargetReposGroup createInDialog(Composite parent) {
		TargetReposGroup contentTable = new TargetReposGroup();
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	/**
	 * Private constructor, use one of {@link #createTableInDialog(Composite, ITargetChangedListener)}
	 * or {@link #createTableInForm(Composite, FormToolkit, ITargetChangedListener)}.
	 * 
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 */
	private TargetReposGroup() {

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

		Table aTable = toolkit.createTable(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		aTable.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		aTable.setLayoutData(gd);
		// TODO
//		aTable.addKeyListener(new KeyAdapter() {
//			public void keyPressed(KeyEvent e) {
//				if (e.keyCode == SWT.DEL && fRemoveButton.getEnabled()) {
//					handleRemove();
//				}
//
//			}
//		});

//		Composite buttonComp = toolkit.createComposite(comp);
//		GridLayout layout = new GridLayout();
//		layout.marginWidth = layout.marginHeight = 0;
//		buttonComp.setLayout(layout);
//		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
//
//		fAddButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_0, SWT.PUSH);
//		fEditButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_1, SWT.PUSH);
//		fRemoveButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_2, SWT.PUSH);
//
//		fShowContentButton = toolkit.createButton(comp, Messages.TargetLocationsGroup_1, SWT.CHECK);

		initializeTableViewer(aTable);
//		initializeButtons();

		toolkit.paintBordersFor(comp);
	}

	/**
	 * Creates the part contents using SWTFactory
	 * @param parent parent composite
	 */
	private void createDialogContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);

		Table aTable = new Table(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI);
		aTable.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		aTable.setLayoutData(gd);
		// TODO
//		aTable.addKeyListener(new KeyAdapter() {
//			public void keyPressed(KeyEvent e) {
//				if (e.keyCode == SWT.DEL && fRemoveButton.getEnabled()) {
//					handleRemove();
//				}
//			}
//		});

//		Composite buttonComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH);
//		GridLayout layout = new GridLayout();
//		layout.marginHeight = 0;
//		layout.marginWidth = 0;
//		buttonComp.setLayout(layout);
//		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
//
//		fAddButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_0, null);
//		fEditButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_1, null);
//		fRemoveButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_2, null);
//
//		fShowContentButton = SWTFactory.createCheckButton(comp, Messages.TargetLocationsGroup_1, null, false, 2);

		initializeTableViewer(aTable);
//		initializeButtons();
	}

	/**
	 * Sets up the table viewer using the given table
	 * @param table
	 */
	private void initializeTableViewer(Table table) {
		fTableViewer = new TableViewer(table);
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setLabelProvider(new StyledBundleLabelProvider(true, false));
		fTableViewer.setComparator(new ViewerComparator());
//		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
//			public void selectionChanged(SelectionChangedEvent event) {
//				updateButtons();
//			}
//		});
//		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
//			public void doubleClick(DoubleClickEvent event) {
//				if (!event.getSelection().isEmpty()) {
//					handleEdit();
//				}
//			}
//		});
	}

//	/**
//	 * Sets up the buttons, the button fields must already be created before calling this method
//	 */
//	private void initializeButtons() {
//		fAddButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				handleAdd();
//			}
//		});
//		fAddButton.setLayoutData(new GridData());
//		SWTFactory.setButtonDimensionHint(fAddButton);
//
//		fEditButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				handleEdit();
//			}
//		});
//		fEditButton.setLayoutData(new GridData());
//		fEditButton.setEnabled(false);
//		SWTFactory.setButtonDimensionHint(fEditButton);
//
//		fRemoveButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				handleRemove();
//			}
//		});
//		fRemoveButton.setLayoutData(new GridData());
//		fRemoveButton.setEnabled(false);
//		SWTFactory.setButtonDimensionHint(fRemoveButton);
//
//		fShowContentButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				fTreeViewer.refresh();
//				fTreeViewer.expandAll();
//			}
//		});
//		fShowContentButton.setLayoutData(new GridData());
//		SWTFactory.setButtonDimensionHint(fShowContentButton);
//	}

	/**
	 * Sets the target definition model to use as input for the tree, can be called with different
	 * models to change the tree's input.
	 * @param target target model
	 */
	public void setInput(ITargetDefinition target) {
		fTarget = target;
		fTableViewer.setInput(fTarget.getRepositories());
//		updateButtons();
	}

//	private void handleAdd() {
//		AddBundleContainerWizard wizard = new AddBundleContainerWizard(fTarget);
//		Shell parent = fTreeViewer.getTree().getShell();
//		WizardDialog dialog = new WizardDialog(parent, wizard);
//		if (dialog.open() != Window.CANCEL) {
//			contentsChanged(false);
//			fTreeViewer.refresh();
//			updateButtons();
//		}
//	}
//
//	private void handleEdit() {
//		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
//		if (!selection.isEmpty()) {
//			Object selected = selection.getFirstElement();
//			IBundleContainer oldContainer = null;
//			if (selected instanceof IBundleContainer) {
//				oldContainer = (IBundleContainer) selected;
//			} else if (selected instanceof IResolvedBundle) {
//				TreeItem[] treeSelection = fTreeViewer.getTree().getSelection();
//				if (treeSelection.length > 0) {
//					Object parent = treeSelection[0].getParentItem().getData();
//					if (parent instanceof IBundleContainer) {
//						oldContainer = (IBundleContainer) parent;
//					}
//				}
//			}
//			if (oldContainer != null) {
//				Shell parent = fTreeViewer.getTree().getShell();
//				EditBundleContainerWizard wizard = new EditBundleContainerWizard(fTarget, oldContainer);
//				WizardDialog dialog = new WizardDialog(parent, wizard);
//				if (dialog.open() == Window.OK) {
//					// Replace the old container with the new one
//					IBundleContainer newContainer = wizard.getBundleContainer();
//					if (newContainer != null) {
//						IBundleContainer[] containers = fTarget.getBundleContainers();
//						java.util.List newContainers = new ArrayList(containers.length);
//						for (int i = 0; i < containers.length; i++) {
//							if (!containers[i].equals(oldContainer)) {
//								newContainers.add(containers[i]);
//							}
//						}
//						newContainers.add(newContainer);
//						fTarget.setBundleContainers((IBundleContainer[]) newContainers.toArray(new IBundleContainer[newContainers.size()]));
//
//						// Update the table
//						contentsChanged(false);
//						fTreeViewer.refresh();
//						updateButtons();
//						fTreeViewer.setSelection(new StructuredSelection(newContainer), true);
//					}
//				}
//			}
//		}
//	}
//
//	private void handleRemove() {
//		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
//		IBundleContainer[] containers = fTarget.getBundleContainers();
//		if (!selection.isEmpty() && containers != null && containers.length > 0) {
//			Set newContainers = new HashSet();
//			newContainers.addAll(Arrays.asList(fTarget.getBundleContainers()));
//			Iterator iterator = selection.iterator();
//			boolean removedSite = false;
//			while (iterator.hasNext()) {
//				Object currentSelection = iterator.next();
//				if (currentSelection instanceof IBundleContainer) {
//					if (currentSelection instanceof IUBundleContainer) {
//						removedSite = true;
//					}
//					newContainers.remove(currentSelection);
//				}
//			}
//			if (newContainers.size() > 0) {
//				fTarget.setBundleContainers((IBundleContainer[]) newContainers.toArray(new IBundleContainer[newContainers.size()]));
//			} else {
//				fTarget.setBundleContainers(null);
//			}
//
//			// If we remove a site container, the content change update must force a re-resolve bug 275458 / bug 275401
//			contentsChanged(removedSite);
//			fTreeViewer.refresh(false);
//			updateButtons();
//		}
//	}
//
//	private void updateButtons() {
//		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
//		fEditButton.setEnabled(!selection.isEmpty() && selection.getFirstElement() instanceof IBundleContainer);
//		// If any container is selected, allow the remove (the remove ignores non-container entries)
//		boolean removeAllowed = false;
//		Iterator iter = selection.iterator();
//		while (iter.hasNext()) {
//			if (iter.next() instanceof IBundleContainer) {
//				removeAllowed = true;
//				break;
//			}
//		}
//		fRemoveButton.setEnabled(removeAllowed);
//	}
//
//	/**
//	 * Informs the reporter for this table that something has changed
//	 * and is dirty.
//	 */
//	private void contentsChanged(boolean force) {
//		Object[] listeners = fChangeListeners.getListeners();
//		for (int i = 0; i < listeners.length; i++) {
//			((ITargetChangedListener) listeners[i]).contentsChanged(fTarget, this, true, force);
//		}
//	}

}
