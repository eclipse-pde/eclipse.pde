/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Collections;
import java.util.Vector;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class LibraryVisibilitySection extends TableSection implements IPartSelectionListener {

	private static int ADD_INDEX = 0;
	private static int REMOVE_INDEX = 1;

	private Button fFullExportButton;
	private Button fSelectedExportButton;
	private IPluginLibrary fCurrentLibrary;
	private Composite fPackageExportContainer;
	private TableViewer fPackageExportViewer;
	private Action fAddAction;
	private Action fRemoveAction;

	static class TableContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginLibrary) {
				String[] filters = ((IPluginLibrary) parent).getContentFilters();
				return filters == null ? new Object[0] : filters;
			}
			return new Object[0];
		}
	}

	static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		@Override
		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
		}
	}

	public LibraryVisibilitySection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.ManifestEditor_ExportSection_add, PDEUIMessages.ManifestEditor_ExportSection_remove});
		getSection().setText(PDEUIMessages.ManifestEditor_ExportSection_title);
		getSection().setDescription(PDEUIMessages.ManifestEditor_ExportSection_desc);
		fHandleDefaultButton = false;
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 1));

		String label = PDEUIMessages.ManifestEditor_ExportSection_fullExport;
		fFullExportButton = toolkit.createButton(container, label, SWT.RADIO);
		fFullExportButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFullExportButton.addSelectionListener(widgetSelectedAdapter(e -> {
			try {
				if (fCurrentLibrary != null)
					fCurrentLibrary.setExported(fFullExportButton.getSelection());
				getTablePart().setButtonEnabled(0, !fFullExportButton.getSelection());
				getTablePart().setButtonEnabled(1, false);
			} catch (CoreException e1) {
			}
		}));

		label = PDEUIMessages.ManifestEditor_ExportSection_selectedExport;
		fSelectedExportButton = toolkit.createButton(container, label, SWT.RADIO);
		fSelectedExportButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createPackageViewer(container, toolkit);
		update(null);
		makeActions();

		if (getPage().getModel() instanceof IPluginModelBase model) {
			model.addModelChangedListener(this);
		}

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setClient(container);
	}

	private void makeActions() {
		fAddAction = new Action(PDEUIMessages.ManifestEditor_ExportSection_add) {
			@Override
			public void run() {
				handleAdd();
			}
		};
		fAddAction.setEnabled(isEditable());

		fRemoveAction = new Action(PDEUIMessages.ManifestEditor_ExportSection_remove) {
			@Override
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());
	}

	private void createPackageViewer(Composite parent, FormToolkit toolkit) {
		fPackageExportContainer = toolkit.createComposite(parent);
		fPackageExportContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 2;
		fPackageExportContainer.setLayout(layout);
		fPackageExportContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		EditableTablePart tablePart = getTablePart();
		IBaseModel model = getPage().getModel();
		tablePart.setEditable(model != null && model.isEditable());
		createViewerPartControl(fPackageExportContainer, SWT.FULL_SELECTION, 2, toolkit);
		fPackageExportViewer = tablePart.getTableViewer();
		fPackageExportViewer.setContentProvider(new TableContentProvider());
		fPackageExportViewer.setLabelProvider(new TableLabelProvider());
		fPackageExportViewer.setComparator(new ViewerComparator());
		toolkit.paintBordersFor(parent);
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		getTablePart().setButtonEnabled(1, item != null);
	}

	@Override
	protected void buttonSelected(int index) {
		if (index == ADD_INDEX)
			handleAdd();
		else if (index == REMOVE_INDEX)
			handleRemove();
	}

	@Override
	public boolean doGlobalAction(String actionId) {

		if (!isEditable()) {
			return false;
		}

		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		return false;
	}

	@Override
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		if (fSelectedExportButton.isEnabled() && fSelectedExportButton.getSelection()) {
			manager.add(fAddAction);
			manager.add(new Separator());
		}
		if (!fPackageExportViewer.getStructuredSelection().isEmpty()) {
			manager.add(fRemoveAction);
		}
		// Copy, cut, and paste operations not supported for plug-ins that do
		// not have a MANIFEST.MF
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager, false);
	}

	private void handleAdd() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				String[] names;
				names = fCurrentLibrary.getContentFilters();
				Vector<String> existing = new Vector<>();
				if (names != null) {
					Collections.addAll(existing, names);
				}
				ILabelProvider labelProvider = new JavaElementLabelProvider();
				PackageSelectionDialog dialog = new PackageSelectionDialog(fPackageExportViewer.getTable().getShell(), labelProvider, JavaCore.create(project), existing, true);
				if (dialog.open() == Window.OK) {
					Object[] elements = dialog.getResult();
					for (Object element : elements) {
						IPackageFragment fragment = (IPackageFragment) element;
						fCurrentLibrary.addContentFilter(fragment.getElementName());
					}
				}
				labelProvider.dispose();
			}
		} catch (CoreException e) {
		}
	}

	private void handleRemove() {
		IStructuredSelection ssel = fPackageExportViewer.getStructuredSelection();
		Object[] items = ssel.toArray();
		try {
			for (Object item : items) {
				fCurrentLibrary.removeContentFilter(item.toString());
			}
		} catch (CoreException e) {
		}
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			if (fCurrentLibrary != null)
				update(null);
			markStale();
			return;
		}
		refresh();
	}

	@Override
	public void refresh() {
		update(fCurrentLibrary);
		super.refresh();
	}

	@Override
	public void selectionChanged(IFormPart source, ISelection selection) {
		if (selection == null || selection.isEmpty())
			update(null);
		IStructuredSelection ssel = (IStructuredSelection) selection;
		boolean enable = ssel.size() == 1;
		setEnabled(enable);
		if (enable && ssel.getFirstElement() instanceof IPluginLibrary)
			update((IPluginLibrary) ssel.getFirstElement());
	}

	private void setEnabled(boolean b) {
		fFullExportButton.setEnabled(b);
		fSelectedExportButton.setEnabled(b);
		fPackageExportViewer.getTable().setEnabled(b);
		if (!b) {
			getTablePart().setButtonEnabled(ADD_INDEX, false);
			getTablePart().setButtonEnabled(REMOVE_INDEX, false);
		}
	}

	private void update(IPluginLibrary library) {
		fCurrentLibrary = library;
		if (library == null) {
			fFullExportButton.setEnabled(false);
			fFullExportButton.setSelection(false);
			fSelectedExportButton.setEnabled(false);
			fSelectedExportButton.setSelection(false);
			fPackageExportViewer.setInput(new Object[0]);
			getTablePart().setButtonEnabled(ADD_INDEX, false);
			getTablePart().setButtonEnabled(REMOVE_INDEX, false);
		} else {
			fFullExportButton.setEnabled(isEditable());
			fSelectedExportButton.setEnabled(isEditable());
			fFullExportButton.setSelection(library.isFullyExported());
			fSelectedExportButton.setSelection(!library.isFullyExported());
			fPackageExportViewer.setInput(library);
			getTablePart().setButtonEnabled(REMOVE_INDEX, false);
			getTablePart().setButtonEnabled(ADD_INDEX, isEditable() && fSelectedExportButton.getSelection());
		}
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		// Paste not supported for plug-ins that do not have a MANIFEST.MF
		return false;
	}

}
