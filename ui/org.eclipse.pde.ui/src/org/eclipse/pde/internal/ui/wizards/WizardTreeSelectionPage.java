/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public abstract class WizardTreeSelectionPage extends BaseWizardSelectionPage implements ISelectionChangedListener {
	private TreeViewer categoryTreeViewer;
	private String baseCategory;
	protected TableViewer wizardSelectionViewer;

	private WizardCollectionElement wizardCategories;

	public WizardTreeSelectionPage(WizardCollectionElement categories, String baseCategory, String message) {
		super("NewExtension", message); //$NON-NLS-1$
		this.wizardCategories = categories;
		this.baseCategory = baseCategory;
	}

	public void advanceToNextPage() {
		getContainer().showPage(getNextPage());
	}

	public void createControl(Composite parent) {
		// top level group
		Composite container = new Composite(parent, SWT.NULL);
		FillLayout flayout = new FillLayout();
		flayout.marginWidth = 5;
		flayout.marginHeight = 5;
		container.setLayout(flayout);
		SashForm rootSash = new SashForm(container, SWT.VERTICAL);
		SashForm outerSash = new SashForm(rootSash, SWT.HORIZONTAL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;

		// tree pane
		Tree tree = new Tree(outerSash, SWT.BORDER);
		categoryTreeViewer = new TreeViewer(tree);
		categoryTreeViewer.setContentProvider(new TreeContentProvider());
		categoryTreeViewer.setLabelProvider(ElementLabelProvider.INSTANCE);

		categoryTreeViewer.setComparator(new WizardCollectionComparator(baseCategory));
		categoryTreeViewer.addSelectionChangedListener(this);

		// wizard actions pane
		Table table = new Table(outerSash, SWT.BORDER);
		new TableColumn(table, SWT.NONE);
		TableLayout tlayout = new TableLayout();
		tlayout.addColumnData(new ColumnWeightData(100));
		table.setLayout(tlayout);

		wizardSelectionViewer = new TableViewer(table);
		wizardSelectionViewer.setContentProvider(new ListContentProvider());
		wizardSelectionViewer.setLabelProvider(ListUtil.TABLE_LABEL_PROVIDER);
		wizardSelectionViewer.setComparator(ListUtil.NAME_COMPARATOR);
		wizardSelectionViewer.addSelectionChangedListener(this);
		wizardSelectionViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				BusyIndicator.showWhile(wizardSelectionViewer.getControl().getDisplay(), new Runnable() {
					public void run() {
						selectionChanged(new SelectionChangedEvent(wizardSelectionViewer, wizardSelectionViewer.getSelection()));
						advanceToNextPage();
					}
				});
			}
		});

		// the new composite below is needed in order to make the label span the two
		// defined columns of outerContainer
		Composite descriptionComposite = new Composite(rootSash, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		descriptionComposite.setLayout(layout);
		createDescriptionIn(descriptionComposite);

		initializeViewers();
		rootSash.setWeights(new int[] {70, 30});
		setControl(container);
	}

	protected Object getSingleSelection(IStructuredSelection selection) {
		Object selectedObject = selection.getFirstElement();
		if (selection.size() > 1)
			selectedObject = null; // ie.- a multi-selection
		return selectedObject;
	}

	private void handleCategorySelection(SelectionChangedEvent selectionEvent) {
		setErrorMessage(null);
		setDescriptionText(""); //$NON-NLS-1$
		setSelectedNode(null);

		WizardCollectionElement selectedCategory = (WizardCollectionElement) getSingleSelection((IStructuredSelection) selectionEvent.getSelection());

		if (selectedCategory == null)
			wizardSelectionViewer.setInput(null);
		else
			wizardSelectionViewer.setInput(selectedCategory.getWizards());
	}

	private void handleWizardSelection(SelectionChangedEvent selectionEvent) {
		setErrorMessage(null);

		WizardElement currentSelection = (WizardElement) getSingleSelection((IStructuredSelection) selectionEvent.getSelection());

		// If no single selection, clear and return
		if (currentSelection == null) {
			setDescriptionText(""); //$NON-NLS-1$
			setSelectedNode(null);
			return;
		}
		final WizardElement finalSelection = currentSelection;
		/*
			BusyIndicator.showWhile(categoryTreeViewer.getControl().getDisplay(), new Runnable() {
				public void run() {
				*/
		setSelectedNode(createWizardNode(finalSelection));
		setDescriptionText(finalSelection.getDescription());
		/*
				}
			});
		*/
	}

	protected void initializeViewers() {
		categoryTreeViewer.setInput(wizardCategories);
		wizardSelectionViewer.addSelectionChangedListener(this);
		Object[] categories = wizardCategories.getChildren();
		if (categories.length > 0)
			categoryTreeViewer.setSelection(new StructuredSelection(categories[0]));
		categoryTreeViewer.getTree().setFocus();
	}

	public void selectionChanged(SelectionChangedEvent selectionEvent) {
		if (selectionEvent.getSelectionProvider().equals(categoryTreeViewer))
			handleCategorySelection(selectionEvent);
		else
			handleWizardSelection(selectionEvent);
	}

	public void setSelectedNode(IWizardNode node) {
		super.setSelectedNode(node);
	}
}
