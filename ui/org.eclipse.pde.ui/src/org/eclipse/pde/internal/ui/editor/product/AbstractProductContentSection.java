/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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
 *     Hannes Wellmann - Unify and clean-up Product Editor's PluginSection and FeatureSection
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public abstract class AbstractProductContentSection<S extends AbstractProductContentSection<S>> extends TableSection {

	private final Predicate<Object> elementFilter;
	private final List<Consumer<S>> buttonHandlers;

	protected AbstractProductContentSection(PDEFormPage formPage, Composite parent, List<String> buttonLabels,
			List<Consumer<S>> buttonHandlers, Predicate<Object> elementFilter) {
		super(formPage, parent, Section.DESCRIPTION, buttonLabels.toArray(String[]::new));
		this.elementFilter = elementFilter;
		this.buttonHandlers = buttonHandlers;
	}

	TableViewer getTableViewer() {
		return getTablePart().getTableViewer();
	}

	Table getTable() {
		return getTableViewer().getTable();
	}

	IStructuredSelection getTableSelection() {
		return getTableViewer().getStructuredSelection();
	}

	IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	IProduct getProduct() {
		return getModel().getProduct();
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		sectionData.verticalSpan = 2;
		section.setLayoutData(sectionData);

		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		getTableViewer().setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		GridData data = (GridData) getTablePart().getControl().getLayoutData();
		data.minimumWidth = 200;

		populateSection(section, container, toolkit);

		toolkit.paintBordersFor(container);
		section.setClient(container);

		getModel().addModelChangedListener(this);
		createSectionToolbar(section);
	}

	abstract void populateSection(Section section, Composite container, FormToolkit toolkit);

	void createAutoIncludeRequirementsButton(Composite container, String buttonLabel) {
		Button autoInclude = new Button(container, SWT.CHECK);
		autoInclude.setText(buttonLabel);
		autoInclude.setSelection(getProduct().includeRequirementsAutomatically());
		if (isEditable()) {
			autoInclude.addSelectionListener(widgetSelectedAdapter(
					e -> getProduct().setIncludeRequirementsAutomatically(autoInclude.getSelection())));
		} else {
			autoInclude.setEnabled(false); // default is true
		}
	}

	<T> void configureTable(Function<IProduct, T[]> provider, ViewerComparator comparator) {
		TableViewer fTable = getTableViewer();
		fTable.setContentProvider((IStructuredContentProvider) p -> provider.apply((IProduct) p));
		fTable.setComparator(comparator);
		fTable.setInput(getProduct());
	}

	void enableTableButtons(int... buttonIndices) {
		TablePart tablePart = getTablePart();
		for (int button : buttonIndices) {
			tablePart.setButtonEnabled(button, isEditable());
		}
	}

	private void createSectionToolbar(Section section) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		toolbar.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
		getToolbarActions().forEach(toolBarManager::add);
		toolBarManager.update(true);
		section.setTextClient(toolbar);
	}

	abstract List<Action> getToolbarActions();

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection ssel = getTableSelection();
		if (ssel == null) {
			return;
		}
		Action openAction = createAction(PDEUIMessages.PluginSection_open,
				() -> handleDoubleClick(getTableSelection()));
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);

		manager.add(new Separator());

		Action removeAction = createAction(PDEUIMessages.PluginSection_remove, this::handleRemove);
		removeAction.setEnabled(isEditable() && !ssel.isEmpty());
		manager.add(removeAction);

		Action removeAll = createAction(PDEUIMessages.PluginSection_removeAll, this::handleRemoveAll);
		removeAll.setEnabled(isEditable());
		manager.add(removeAll);

		manager.add(new Separator());

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	@Override
	protected boolean createCount() {
		return true;
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	@Override
	public boolean setFormInput(Object input) {
		if (elementFilter.test(input)) {
			getTableViewer().setSelection(new StructuredSelection(input), true);
			return true;
		}
		return super.setFormInput(input);
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			handleRemove();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return super.doGlobalAction(actionId);
	}

	@Override
	protected boolean canPaste(Object target, Object[] objects) {
		return Stream.of(objects).anyMatch(elementFilter);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void buttonSelected(int index) {
		if (index < buttonHandlers.size()) {
			buttonHandlers.get(index).accept((S) this);
		}
	}

	void handleRemove() {
		IStructuredSelection ssel = getTableSelection();
		if (!ssel.isEmpty()) {
			removeElements(getProduct(), ssel.toList());
			updateButtons(true, true);
		}
	}

	abstract void removeElements(IProduct product, List<Object> elements);

	abstract void handleRemoveAll();

	@Override
	public void refresh() {
		getTableViewer().refresh();
		updateButtons(true, true);
		super.refresh();
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons(true, false);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged();
			return;
		}
		TableViewer tableViewer = getTableViewer();
		if (e.getChangeType() == IModelChangedEvent.INSERT) {
			Stream.of(e.getChangedObjects()).filter(elementFilter).forEach(tableViewer::add);

		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Stream.of(e.getChangedObjects()).filter(elementFilter).forEach(tableViewer::remove);

			// Update Selection
			Table table = getTable();
			int count = table.getItemCount();
			if (count != 0) {
				int index = table.getSelectionIndex();
				table.setSelection(Math.min(index, count - 1));
			} // else, nothing to select

		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			tableViewer.refresh();
		}
		updateButtons(false, true);
	}

	private void handleModelEventWorldChanged() {
		// This section can get disposed if the configuration is changed from
		// plugins to features or vice versa. Subsequently, the configuration
		// page is removed and readded. In this circumstance, abort the refresh
		if (getTable().isDisposed()) {
			return;
		}
		// Reload the input
		getTableViewer().setInput(getProduct());
		// Perform the refresh
		refresh();
	}

	abstract void updateButtons(boolean updateRemove, boolean updateRemoveAll);

	void updateRemoveButtons(int btnRemove, int btnRemoveAll) {
		TablePart tablePart = getTablePart();
		if (btnRemove > -1) {
			ISelection selection = getViewerSelection();
			tablePart.setButtonEnabled(btnRemove,
					isEditable() && !selection.isEmpty()
							&& selection instanceof IStructuredSelection structuredSelection
							&& elementFilter.test(structuredSelection.getFirstElement()));
		}
		if (btnRemoveAll > -1) {
			tablePart.setButtonEnabled(btnRemoveAll,
					isEditable() && tablePart.getTableViewer().getTable().getItemCount() > 0);
		}
	}

	// --- utility methods ---

	static Action createPushAction(String text, ImageDescriptor image, Runnable runAction) {
		Action action = createAction(text, IAction.AS_PUSH_BUTTON, runAction);
		action.setImageDescriptor(image);
		return action;
	}

	static Action createAction(String text, Runnable action) {
		return createAction(text, 0, action);
	}

	static <S> int addButton(String label, Consumer<S> handler, List<String> labels, List<Consumer<S>> handlers) {
		labels.add(label);
		handlers.add(handler);
		return labels.size() - 1;
	}

	private static Action createAction(String text, int style, Runnable action) {
		return new Action(text, style) {
			@Override
			public void run() {
				action.run();
			}
		};
	}

	@SuppressWarnings("unchecked")
	static <T> T[] filterToArray(Stream<Object> stream, Class<T> type) {
		return stream.filter(type::isInstance).map(type::cast).toArray(l -> (T[]) Array.newInstance(type, l));
	}
}
