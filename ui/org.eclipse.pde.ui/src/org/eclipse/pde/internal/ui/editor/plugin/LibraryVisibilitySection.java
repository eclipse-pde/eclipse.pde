/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;

public class LibraryVisibilitySection extends TableSection
		implements
			IPartSelectionListener {
	private static final String SECTION_TITLE = "ManifestEditor.ExportSection.title"; //$NON-NLS-1$
	private static final String SECTION_DESC = "ManifestEditor.ExportSection.desc"; //$NON-NLS-1$
	private static final String KEY_FULL_EXPORT = "ManifestEditor.ExportSection.fullExport"; //$NON-NLS-1$
	private static final String KEY_SELECTED_EXPORT = "ManifestEditor.ExportSection.selectedExport"; //$NON-NLS-1$
	private static final String KEY_ADD = "ManifestEditor.ExportSection.add"; //$NON-NLS-1$
	private static final String KEY_REMOVE = "ManifestEditor.ExportSection.remove"; //$NON-NLS-1$

    private Button fFullExportButton;
	private Button fSelectedExportButton;
	private IPluginLibrary fCurrentLibrary;
	private Composite fPackageExportContainer;
	private TableViewer fPackageExportViewer;
	
	class TableContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
            if (parent instanceof IPluginLibrary) {
				String[] filters = ((IPluginLibrary) parent).getContentFilters();
				return filters == null ? new Object[0] : filters;
			}
			return new Object[0];
		}
	}
	class TableLabelProvider extends LabelProvider
			implements ITableLabelProvider {
        
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_PACKAGE);
		}
	}
    
	public LibraryVisibilitySection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[]{
				PDEPlugin.getResourceString(KEY_ADD),
				PDEPlugin.getResourceString(KEY_REMOVE)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		handleDefaultButton = false;
	}
    
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		container.setLayout(new GridLayout());
        
		String label = PDEPlugin.getResourceString(KEY_FULL_EXPORT);
		fFullExportButton = toolkit.createButton(container, label, SWT.RADIO);
		fFullExportButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFullExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (fCurrentLibrary != null)
						fCurrentLibrary.setExported(fFullExportButton.getSelection());
					getTablePart().setButtonEnabled(0, !fFullExportButton.getSelection());
					getTablePart().setButtonEnabled(1, false);
				} catch (CoreException e1) {
				}
			}
		});
        
		label = PDEPlugin.getResourceString(KEY_SELECTED_EXPORT);
		fSelectedExportButton = toolkit.createButton(container, label, SWT.RADIO);
		fSelectedExportButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createPackageViewer(container, toolkit);
        
		update(null);
        
        IPluginModelBase model = (IPluginModelBase) getPage().getModel();
        model.addModelChangedListener(this);
        
        section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setClient(container);
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
		tablePart.setEditable(getPage().getModel().isEditable());
		createViewerPartControl(fPackageExportContainer, SWT.FULL_SELECTION, 2, toolkit);
		fPackageExportViewer = tablePart.getTableViewer();
		fPackageExportViewer.setContentProvider(new TableContentProvider());
		fPackageExportViewer.setLabelProvider(new TableLabelProvider());
		fPackageExportViewer.setSorter(new ViewerSorter());
		toolkit.paintBordersFor(parent);
	}
    
	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		getTablePart().setButtonEnabled(1, item != null);
	}
    
	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleRemove();
	}
    
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		return false;
	}
    
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
    
	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
	}
    
	private void handleAdd() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				String[] names;
                names = fCurrentLibrary.getContentFilters();
				Vector existing = new Vector();
				if (names != null) {
					for (int i = 0; i < names.length; i++) {
						existing.add(names[i]);
					}
				}
				ILabelProvider labelProvider = new JavaElementLabelProvider();
				PackageSelectionDialog dialog = new PackageSelectionDialog(
						fPackageExportViewer.getTable().getShell(),
						labelProvider, JavaCore.create(project), existing);
				if (dialog.open() == PackageSelectionDialog.OK) {
					Object[] elements = dialog.getResult();
					for (int i = 0; i < elements.length; i++) {
						IPackageFragment fragment = (IPackageFragment) elements[i];
						fCurrentLibrary.addContentFilter(fragment.getElementName());
					}
				}
				labelProvider.dispose();
			}
		} catch (CoreException e) {
		}
	}
    
	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fPackageExportViewer.getSelection();
		Object[] items = ssel.toArray();
		try {
			for (int i = 0; i < items.length; i++) {
				fCurrentLibrary.removeContentFilter(items[i].toString());
			}
		} catch (CoreException e) {
		}
	}
    
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			if (fCurrentLibrary!=null)
				update(null);
			markStale();
			return;
		}
		refresh();
	}

	public void refresh() {
		update(fCurrentLibrary);		
		super.refresh();
	}

	public void selectionChanged(IFormPart source, ISelection selection) {
		if (selection == null || selection.isEmpty())
			update(null);
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.getFirstElement() instanceof IPluginLibrary)
			update((IPluginLibrary) ssel.getFirstElement());
	}
    
	private void update(IPluginLibrary library) {
		fCurrentLibrary = library;
		if (library == null) {
			fFullExportButton.setEnabled(false);
			fFullExportButton.setSelection(false);
			fSelectedExportButton.setEnabled(false);
			fSelectedExportButton.setSelection(false);
			fPackageExportViewer.setInput(new Object[0]);
			getTablePart().setButtonEnabled(0, false);
			getTablePart().setButtonEnabled(1, false);
		} else {
    		fFullExportButton.setEnabled(isEditable());
    		fSelectedExportButton.setEnabled(isEditable());
    		fFullExportButton.setSelection(library.isFullyExported());
    		fSelectedExportButton.setSelection(!library.isFullyExported());
    		fPackageExportViewer.setInput(library);
    		getTablePart().setButtonEnabled(1, false);
    		getTablePart().setButtonEnabled(0, fSelectedExportButton.getSelection());
        }
    }
	
}
