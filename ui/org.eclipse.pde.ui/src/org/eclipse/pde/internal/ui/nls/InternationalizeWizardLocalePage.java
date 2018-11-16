/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/

package org.eclipse.pde.internal.ui.nls;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.progress.WorkbenchJob;

public class InternationalizeWizardLocalePage extends InternationalizationWizardPage implements IModelProviderListener {

	public static final String PAGE_NAME = "InternationalizeWizardLocalePage"; //$NON-NLS-1$

	protected Locale[] fModels = new Locale[0];
	private String fLocation;

	protected TableViewer fSelectedListViewer;
	private boolean fRefreshNeeded = true;

	private Label fCountLabel;
	private TableViewer fAvailableListViewer;

	// this job is used to delay the full filter refresh for 200 milliseconds in case the user is still typing
	private WorkbenchJob fFilterJob;
	private Text fFilterText;
	private AvailableFilter fFilter;

	// fSelected is used to track the selection in a HashMap so we can efficiently
	// filter selected items out of the available item list
	private HashMap<Object, ?> fSelected;
	// used to block the selection listeners from updating button enablement when programatically removing items
	private boolean fBlockSelectionListeners;
	private Button fAddButton;
	private Button fAddAllButton;
	private Button fRemoveButton;
	private Button fRemoveAllButton;

	private InternationalizeModelTable fInternationalizeModelTable;

	private ILabelProvider fLabelProvider = PDEPlugin.getDefault().getLabelProvider();

	private class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return ((InternationalizeModelTable) inputElement).getModels();
		}
	}

	public InternationalizeWizardLocalePage(InternationalizeModelTable modelTable, String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.InternationalizeWizard_LocalePage_pageTitle);
		setDescription(PDEUIMessages.InternationalizeWizard_LocalePage_pageDescription);

		PDEPlugin.getDefault().getLabelProvider().connect(this);
		PDECore.getDefault().getModelManager().getExternalModelManager().addModelProviderListener(this);

		fInternationalizeModelTable = modelTable;
		fSelected = new HashMap<>();

		IWizardContainer container = getContainer();
		if (container != null)
			container.updateButtons();
	}

	private void addFilter() {
		fFilter = new AvailableFilter(fSelected, fLabelProvider);
		fAvailableListViewer.addFilter(fFilter);
		fFilterJob = new WorkbenchJob("FilterJob") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				handleFilter();
				return Status.OK_STATUS;
			}
		};
		fFilterJob.setSystem(true);
	}

	private void handleFilter() {
		boolean changed = false;
		String newFilter;
		if (fFilterText == null || (newFilter = fFilterText.getText().trim()).length() == 0)
			newFilter = AvailableFilter.WILDCARD;
		changed = fFilter.setPattern(newFilter);
		if (changed) {
			fAvailableListViewer.getTable().setRedraw(false);
			fAvailableListViewer.refresh();
			fAvailableListViewer.getTable().setRedraw(true);
			updateButtonEnablement(false, false);
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 20;
		container.setLayout(layout);

		createScrollArea(container);
		createAvailableList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		createButtonArea(container);
		createLocaleList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		updateCount();

		addViewerListeners();
		addFilter();

		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
	}

	protected Composite createLocaleList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.InternationalizeWizard_LocalePage_internationalizeList);

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 225;
		table.setLayoutData(gd);

		fSelectedListViewer = new TableViewer(table);
		fSelectedListViewer.setLabelProvider(fLabelProvider);
		fSelectedListViewer.setContentProvider(new ContentProvider());
		fSelectedListViewer.setComparator(ListUtil.NAME_COMPARATOR);
		return container;
	}

	protected boolean isRefreshNeeded() {
		if (fRefreshNeeded) {
			fRefreshNeeded = false;
			return true;
		}
		if (fLocation == null) {
			return true;
		}
		return false;
	}

	protected void addLocale(Locale model, ArrayList<Locale> selected) {
		if (!selected.contains(model)) {
			selected.add(model);
		}
	}

	public List<Object> getLocalesForInternationalization() {
		TableItem[] items = fSelectedListViewer.getTable().getItems();
		List<Object> result = new ArrayList<>();
		for (TableItem item : items) {
			result.add(item.getData());
		}
		return result;
	}

	public void storeSettings() {
		// TODO
	}

	@Override
	public void modelsChanged(IModelProviderEvent event) {
		fRefreshNeeded = true;
	}

	private void initialize() {
		updateButtonEnablement(true, true);
		setPageComplete(false);
	}

	private void addViewerListeners() {
		fAvailableListViewer.addDoubleClickListener(event -> handleAdd());

		fSelectedListViewer.addDoubleClickListener(event -> handleRemove());

		fAvailableListViewer.addSelectionChangedListener(event -> {
			if (!fBlockSelectionListeners)
				updateSelectionBasedEnablement(event.getSelection(), true);
		});

		fSelectedListViewer.addSelectionChangedListener(event -> {
			if (!fBlockSelectionListeners)
				updateSelectionBasedEnablement(event.getSelection(), false);
		});

		fFilterText.addModifyListener(e -> {
			fFilterJob.cancel();
			fFilterJob.schedule(200);
		});

	}

	private Composite createAvailableList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData());

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.InternationalizeWizard_LocalePage_availableList);

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 225;
		gd.heightHint = 400;
		table.setLayoutData(gd);

		fAvailableListViewer = new TableViewer(table);
		fAvailableListViewer.setLabelProvider(fLabelProvider);
		fAvailableListViewer.setContentProvider(new ContentProvider());
		fAvailableListViewer.setInput(fInternationalizeModelTable);
		fAvailableListViewer.setComparator(ListUtil.NAME_COMPARATOR);

		return container;
	}

	private Composite createButtonArea(Composite parent) {
		ScrolledComposite comp = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		Composite container = new Composite(comp, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginTop = 50;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.verticalIndent = 15;
		container.setLayoutData(gd);

		fAddButton = new Button(container, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.ImportWizard_DetailedPage_add);
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddButton.addSelectionListener(widgetSelectedAdapter(e -> handleAdd()));
		SWTUtil.setButtonDimensionHint(fAddButton);

		fAddAllButton = new Button(container, SWT.PUSH);
		fAddAllButton.setText(PDEUIMessages.ImportWizard_DetailedPage_addAll);
		fAddAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddAllButton.addSelectionListener(widgetSelectedAdapter(e -> handleAddAll()));
		SWTUtil.setButtonDimensionHint(fAddAllButton);

		fRemoveButton = new Button(container, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.ImportWizard_DetailedPage_remove);
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveButton.addSelectionListener(widgetSelectedAdapter(e -> handleRemove()));
		SWTUtil.setButtonDimensionHint(fRemoveButton);

		fRemoveAllButton = new Button(container, SWT.PUSH);
		fRemoveAllButton.setText(PDEUIMessages.ImportWizard_DetailedPage_removeAll);
		fRemoveAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveAllButton.addSelectionListener(widgetSelectedAdapter(e -> handleRemoveAll()));
		SWTUtil.setButtonDimensionHint(fRemoveAllButton);

		fCountLabel = new Label(container, SWT.NONE);
		fCountLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		comp.setContent(container);
		comp.setMinHeight(250);
		comp.setExpandHorizontal(true);
		comp.setExpandVertical(true);
		return container;
	}

	private Composite createScrollArea(Composite parent) {
		Group container = createFilterContainer(parent, PDEUIMessages.InternationalizeWizard_LocalePage_filter, PDEUIMessages.ImportWizard_DetailedPage_search);
		fFilterText = createFilterText(container, ""); //$NON-NLS-1$
		return container;
	}

	protected void refreshPage() {
		fSelectedListViewer.getTable().removeAll();
		fSelected = new HashMap<>();
		fAvailableListViewer.refresh();
		pageChanged();
	}

	protected void pageChanged() {
		pageChanged(false, false);
	}

	protected void pageChanged(boolean doAddEnablement, boolean doRemoveEnablement) {
		if (fSelectedListViewer.getTable().getItemCount() == 0) {
			setErrorMessage(PDEUIMessages.InternationalizeWizard_LocalePage_selectionError);
		} else {
			setErrorMessage(null);
		}

		updateCount();
		updateButtonEnablement(doAddEnablement, doRemoveEnablement);
		setPageComplete(fSelectedListViewer.getTable().getItemCount() > 0);
	}

	private void updateCount() {
		fCountLabel.setText(NLS.bind(PDEUIMessages.ImportWizard_DetailedPage_count,
				Integer.toString(fSelectedListViewer.getTable().getItemCount()),
				Integer.toString(fAvailableListViewer.getTable().getItemCount()
						+ fSelectedListViewer.getTable().getItemCount())
		));
		fCountLabel.getParent().layout();
	}

	private void updateButtonEnablement(boolean doAddEnablement, boolean doRemoveEnablement) {
		int availableCount = fAvailableListViewer.getTable().getItemCount();
		int importCount = fSelectedListViewer.getTable().getItemCount();

		if (doAddEnablement)
			updateSelectionBasedEnablement(fAvailableListViewer.getStructuredSelection(), true);
		if (doRemoveEnablement)
			updateSelectionBasedEnablement(fSelectedListViewer.getStructuredSelection(), false);

		fAddAllButton.setEnabled(availableCount > 0);
		fRemoveAllButton.setEnabled(importCount > 0);
	}

	private void updateSelectionBasedEnablement(ISelection theSelection, boolean available) {
		if (available)
			fAddButton.setEnabled(!theSelection.isEmpty());
		else
			fRemoveButton.setEnabled(!theSelection.isEmpty());
	}

	private void handleAdd() {
		IStructuredSelection ssel = fAvailableListViewer.getStructuredSelection();
		if (!ssel.isEmpty()) {
			Table table = fAvailableListViewer.getTable();
			int index = table.getSelectionIndices()[0];
			Object[] selection = ssel.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (Object selectedObject : selection) {
				doAdd(selectedObject);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
			pageChanged(true, false);
		}
	}

	private void handleAddAll() {
		TableItem[] items = fAvailableListViewer.getTable().getItems();

		ArrayList<Object> data = new ArrayList<>();
		for (TableItem item : items) {
			data.add(item.getData());
		}
		if (!data.isEmpty()) {
			Object[] datas = data.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (Object dataObject : datas) {
				doAdd(dataObject);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			pageChanged(true, false);
		}
	}

	private void handleRemove() {
		IStructuredSelection ssel = fSelectedListViewer.getStructuredSelection();
		if (!ssel.isEmpty()) {
			Table table = fSelectedListViewer.getTable();
			int index = table.getSelectionIndices()[0];
			Object[] selection = ssel.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (Object selectedObject : selection) {
				doRemove(selectedObject);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
			pageChanged(false, true);
		}
	}

	private void doAdd(Object o) {
		fInternationalizeModelTable.removeModel(o);
		fSelectedListViewer.add(o);
		fAvailableListViewer.remove(o);
		fSelected.put(o, null);
	}

	private void doRemove(Object o) {
		fInternationalizeModelTable.addModel(o);
		fSelected.remove(o);
		fSelectedListViewer.remove(o);
		fAvailableListViewer.add(o);
	}

	// used to prevent flicker during operations that move items between lists
	private void setRedraw(boolean redraw) {
		fAvailableListViewer.getTable().setRedraw(redraw);
		fSelectedListViewer.getTable().setRedraw(redraw);
	}

	private void handleRemoveAll() {
		TableItem[] items = fSelectedListViewer.getTable().getItems();

		ArrayList<Object> data = new ArrayList<>();
		for (TableItem item : items) {
			data.add(item.getData());
		}
		if (!data.isEmpty()) {
			Object[] datas = data.toArray();
			setBlockSelectionListeners(true);
			setRedraw(false);
			for (Object dataObject : datas) {
				doRemove(dataObject);
			}
			setRedraw(true);
			setBlockSelectionListeners(false);
			pageChanged(false, true);
		}
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		PDECore.getDefault().getModelManager().getExternalModelManager().removeModelProviderListener(this);
	}

	private void setBlockSelectionListeners(boolean blockSelectionListeners) {
		fBlockSelectionListeners = blockSelectionListeners;
	}

	@Override
	public boolean isCurrentPage() {
		return super.isCurrentPage();
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}
}
