package org.eclipse.pde.internal.wizards;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import java.util.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;


public abstract class WizardTreeSelectionPage extends BaseWizardSelectionPage implements ISelectionChangedListener {
	private TreeViewer  categoryTreeViewer;
	private String baseCategory;
	private TableViewer wizardSelectionViewer;

	private final static int        SIZING_LISTS_HEIGHT = 150;
	private final static int        SIZING_DESC_HEIGHT = 100;
	private final static int        SIZING_LISTS_WIDTH = 150;
	private WizardCollectionElement wizardCategories;

public WizardTreeSelectionPage(WizardCollectionElement categories, String baseCategory, String message) {
	super("NewExtension", message);
	this.wizardCategories = categories;
	this.baseCategory = baseCategory;
 } 
public void createControl(Composite parent) {
	// top level group
	Composite outerContainer = new Composite(parent, SWT.NONE);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	outerContainer.setLayout(layout);
	outerContainer.setLayoutData(
		new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

	// tree pane
	Tree tree = new Tree(outerContainer, SWT.BORDER);
	categoryTreeViewer = new TreeViewer(tree);
	categoryTreeViewer.setContentProvider(new TreeContentProvider());
	categoryTreeViewer.setLabelProvider(ElementLabelProvider.INSTANCE);

	categoryTreeViewer.setSorter(new WizardCollectionSorter(baseCategory));
	categoryTreeViewer.addSelectionChangedListener(this);

	GridData gd =
		new GridData(
			GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	gd.heightHint = SIZING_LISTS_HEIGHT;
	gd.widthHint = SIZING_LISTS_WIDTH;
	tree.setLayoutData(gd);

	// wizard actions pane

	Table table = new Table(outerContainer, SWT.BORDER);
	new TableColumn(table, SWT.NONE);
	TableLayout tlayout = new TableLayout();
	tlayout.addColumnData(new ColumnWeightData(100));
	table.setLayout(tlayout);
	
	wizardSelectionViewer = new TableViewer(table);
	wizardSelectionViewer.setContentProvider(new ListContentProvider());
	wizardSelectionViewer.setLabelProvider(ListUtil.TABLE_LABEL_PROVIDER);
	wizardSelectionViewer.setSorter(ListUtil.NAME_SORTER);
	wizardSelectionViewer.addSelectionChangedListener(this);

	gd =    new GridData(
			GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL
				| GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL);
	gd.heightHint = SIZING_LISTS_HEIGHT;
	gd.widthHint = SIZING_LISTS_WIDTH;
	table.setLayoutData(gd);

	// the new composite below is needed in order to make the label span the two
	// defined columns of outerContainer
	Composite descriptionComposite = new Composite(outerContainer, SWT.NONE);
	layout = new GridLayout();
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	descriptionComposite.setLayout(layout);
	GridData data =
		new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
	data.horizontalSpan = 2;
	data.heightHint = SIZING_DESC_HEIGHT;
	descriptionComposite.setLayoutData(data);
	createDescriptionIn(descriptionComposite);

	initializeViewers();
	setControl(outerContainer);
}
protected Object getSingleSelection(IStructuredSelection selection) {
	Object selectedObject = selection.getFirstElement();
	if (selection.size() > 1)
		selectedObject = null; // ie.- a multi-selection
	return selectedObject;
}
private void handleCategorySelection(SelectionChangedEvent selectionEvent) {
	setErrorMessage(null);
	setDescriptionText("");
	setSelectedNode(null);

	WizardCollectionElement selectedCategory =
		(WizardCollectionElement)getSingleSelection((IStructuredSelection)selectionEvent.getSelection());

	if (selectedCategory == null)
		wizardSelectionViewer.setInput(null);
	else
		wizardSelectionViewer.setInput(selectedCategory.getWizards());
}
private void handleWizardSelection(SelectionChangedEvent selectionEvent) {
	setErrorMessage(null);

	WizardElement currentSelection =
		(WizardElement) getSingleSelection((IStructuredSelection) selectionEvent
			.getSelection());

	// If no single selection, clear and return
	if (currentSelection == null) {
		setDescriptionText("");
		setSelectedNode(null);
		return;
	}
	final WizardElement finalSelection = currentSelection;
/*
	BusyIndicator.showWhile(categoryTreeViewer.getControl().getDisplay(), new Runnable() {
		public void run() {
		*/
			setSelectedNode(createWizardNode(finalSelection));
			setDescriptionText((String) finalSelection.getDescription());
/*
		}
	});
*/
}
protected void initializeViewers() {
	categoryTreeViewer.setInput(wizardCategories);
	wizardSelectionViewer.addSelectionChangedListener(this);
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
