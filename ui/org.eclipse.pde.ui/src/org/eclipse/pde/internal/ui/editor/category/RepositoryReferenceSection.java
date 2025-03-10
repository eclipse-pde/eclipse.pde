/*******************************************************************************
 * Copyright (c) 2014, 2021 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.IRepositoryReference;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.isite.ISiteModelFactory;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.RepositoryDialog;
import org.eclipse.pde.internal.ui.dialogs.RepositoryDialog.RepositoryResult;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class RepositoryReferenceSection extends TableSection {

	private static class ContentProvider implements IStructuredContentProvider {

		ContentProvider() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ISite) {
				return ((ISite) inputElement).getRepositoryReferences();
			}
			return new Object[0];
		}


	}

	private class LabelProvider extends PDELabelProvider {
		@Override
		public Image getColumnImage(Object obj, int index) {
			if (index == 1) {
				return get(PDEPluginImages.DESC_REPOSITORY_OBJ);
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int index) {
			IRepositoryReference repo = (IRepositoryReference) obj;
			return switch (index) {
				case 0 -> repo.getName();
				case 1 -> repo.getURL();
				case 2 -> Boolean.toString(repo.getEnabled());
				default -> null;
			};
		}

	}

	private TableViewer fRepositoryTable;
	private TableEditor fEnabledColumnEditor;
	private ISiteModel fModel;

	public RepositoryReferenceSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, getButtonLabels());
	}

	private static String[] getButtonLabels() {
		String[] labels = new String[4];
		labels[0] = PDEUIMessages.RepositorySection_add;
		labels[1] = PDEUIMessages.RepositorySection_edit;
		labels[2] = PDEUIMessages.RepositorySection_remove;
		labels[3] = PDEUIMessages.RepositorySection_removeAll;
		return labels;
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		fModel = (ISiteModel) getPage().getModel();
		fModel.addModelChangedListener(this);

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		sectionData.verticalSpan = 2;
		section.setLayoutData(sectionData);

		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI | SWT.FULL_SELECTION, 2, toolkit);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		TablePart tablePart = getTablePart();
		fRepositoryTable = tablePart.getTableViewer();

		final Table table = fRepositoryTable.getTable();

		final TableColumn nameColumn = new TableColumn(table, SWT.LEFT);
		nameColumn.setText(PDEUIMessages.UpdatesSection_NameColumn);
		nameColumn.setWidth(120);

		final TableColumn locationColumn = new TableColumn(table, SWT.LEFT);
		locationColumn.setText(PDEUIMessages.UpdatesSection_LocationColumn);
		locationColumn.setWidth(200);

		final TableColumn enabledColumn = new TableColumn(table, SWT.LEFT);
		enabledColumn.setText(PDEUIMessages.UpdatesSection_EnabledColumn);
		enabledColumn.setWidth(80);

		GridData data = (GridData) tablePart.getControl().getLayoutData();
		data.minimumWidth = 200;

		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable());

		table.setHeaderVisible(true);
		toolkit.paintBordersFor(container);

		table.addControlListener(new ControlListener() {

			@Override
			public void controlMoved(ControlEvent e) {
			}

			@Override
			public void controlResized(ControlEvent e) {
				int size = table.getSize().x;
				nameColumn.setWidth(size / 7 * 2);
				locationColumn.setWidth(size / 7 * 4);
				enabledColumn.setWidth(size / 7 * 1);
			}

		});

		fRepositoryTable.setLabelProvider(new LabelProvider());
		fRepositoryTable.setContentProvider(new ContentProvider());
		fRepositoryTable.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				IRepositoryReference r1 = (IRepositoryReference) e1;
				IRepositoryReference r2 = (IRepositoryReference) e2;
				return super.compare(viewer, r1.getURL(), r2.getURL());
			}
		});
		fRepositoryTable.setInput(getSite());
		createEditors();

		section.setClient(container);

		section.setText(PDEUIMessages.RepositorySection_title);
		section.setDescription(PDEUIMessages.RepositorySection_description);
	}


	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case 0 -> handleAdd();
			case 1 -> handleEdit(fRepositoryTable.getStructuredSelection());
			case 2 -> handleDelete();
			case 3 -> handleRemoveAll();
		}
	}


	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleEdit(selection);
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			handleDelete();
			return false;
		}
		return super.doGlobalAction(actionId);
	}


	private void handleEdit(IStructuredSelection selection) {
		clearEditors();
		if (!selection.isEmpty()) {
			IRepositoryReference repo = (IRepositoryReference) selection.toArray()[0];
			RepositoryDialog dialog = getRepositoryDialog(repo.getName(), repo.getURL());
			if (dialog.open() == Window.OK) {
				updateModel(repo, dialog.getResult());
			}
		}
	}

	private RepositoryDialog getRepositoryDialog(String name, String repoURL) {
		RepositoryDialog dialog = new RepositoryDialog(PDEPlugin.getActiveWorkbenchShell(), repoURL, name);
		dialog.setTitle(PDEUIMessages.RepositorySection_title);
		return dialog;
	}

	private void updateModel(IRepositoryReference repo, RepositoryResult result) {
		try {
			if (repo != null) {
				getSite().removeRepositoryReferences(new IRepositoryReference[] { repo });
			}
			ISiteModelFactory factory = getModel().getFactory();
			IRepositoryReference newRepo = factory.createRepositoryReference();
			if (result.name() != null) {
				newRepo.setName(result.name());
			}
			newRepo.setURL(result.url());
			newRepo.setEnabled(true);
			getSite().addRepositoryReferences(new IRepositoryReference[] { newRepo });
			fRepositoryTable.refresh();
			fRepositoryTable.setSelection(new StructuredSelection(newRepo));
			updateButtons();
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	private void handleDelete() {
		clearEditors();
		IStructuredSelection ssel = fRepositoryTable.getStructuredSelection();
		if (!ssel.isEmpty()) {
			Object[] objects = ssel.toArray();
			IRepositoryReference[] repos = new IRepositoryReference[objects.length];
			System.arraycopy(objects, 0, repos, 0, objects.length);
			try {
				getSite().removeRepositoryReferences(repos);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
			fRepositoryTable.refresh(false);
			updateButtons();
		}
	}

	private void handleRemoveAll() {
		clearEditors();
		try {
			getSite().removeRepositoryReferences(getSite().getRepositoryReferences());
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
		fRepositoryTable.refresh(false);
		updateButtons();
	}

	private void handleAdd() {
		clearEditors();
		RepositoryDialog dialog = getRepositoryDialog(null, null);
		if (dialog.open() == Window.OK) {
			updateModel(null, dialog.getResult());
		}
	}

	@Override
	public void refresh() {
		fRepositoryTable.refresh();
		updateButtons();
		super.refresh();
	}


	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

	@Override
	public boolean setFormInput(Object input) {
		if (input instanceof ISite) {
			fRepositoryTable.setSelection(new StructuredSelection(input), true);
			return true;
		}
		return super.setFormInput(input);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		fRepositoryTable.setInput(getSite());
		fRepositoryTable.refresh();
		updateButtons();
		clearEditors();
	}

	private void updateButtons() {
		TablePart tablePart = getTablePart();
		ISelection selection = getViewerSelection();
		boolean enabled = isEditable() && !selection.isEmpty() && selection instanceof IStructuredSelection && ((IStructuredSelection) selection).getFirstElement() instanceof IRepositoryReference;
		tablePart.setButtonEnabled(1, enabled);
		tablePart.setButtonEnabled(2, enabled);
		tablePart.setButtonEnabled(3, isEditable() && getSite().getRepositoryReferences().length > 0);
	}

	private void clearEditors() {
		Control oldEditor = fEnabledColumnEditor.getEditor();
		if (oldEditor != null && !oldEditor.isDisposed()) {
			oldEditor.dispose();
		}

	}

	private void createEditors() {
		final Table table = fRepositoryTable.getTable();

		fEnabledColumnEditor = new TableEditor(table);
		fEnabledColumnEditor.horizontalAlignment = SWT.CENTER;
		fEnabledColumnEditor.grabHorizontal = true;
		fEnabledColumnEditor.minimumWidth = 50;

		table.addSelectionListener(widgetSelectedAdapter(e -> showControls()));
	}

	private void showControls() {
		// Clean up any previous editor control
		clearEditors();

		// Identify the selected row
		Table table = fRepositoryTable.getTable();
		IStructuredSelection selection = fRepositoryTable.getStructuredSelection();
		if (selection.isEmpty()) {
			return;
		}
		final TableItem item = table.getSelection()[0];
		if (item != null && !isEditable()) {
			return;
		}

		if (item != null) {
			final IRepositoryReference repo = (IRepositoryReference) selection.getFirstElement();
			final CCombo combo = new CCombo(table, SWT.BORDER | SWT.READ_ONLY);
			combo.setItems(new String[] {Boolean.toString(true), Boolean.toString(false)});
			combo.setText(item.getText(2));
			combo.pack();
			combo.addSelectionListener(widgetSelectedAdapter(e -> {
				item.setText(2, combo.getText());
				try {
					repo.setEnabled(Boolean.parseBoolean(combo.getText()));
				} catch (CoreException ex) {
					PDEPlugin.log(ex);
				}
			}));
			fEnabledColumnEditor.setEditor(combo, item, 2);
		}
	}

	public ISiteModel getModel() {
		return fModel;
	}

	public ISite getSite() {
		return fModel.getSite();
	}
}
