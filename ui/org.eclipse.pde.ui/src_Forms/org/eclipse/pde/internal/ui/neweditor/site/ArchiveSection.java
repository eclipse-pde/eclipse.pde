/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Sep 29, 2003
 */
package org.eclipse.pde.internal.ui.neweditor.site;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.*;
/**
 * 
 */
public class ArchiveSection extends PDESection {
	private Table fTable;
	private TableViewer fViewer;
	private ISiteModel fModel;
	private ISiteBuildModel fBuildModel;
	private Button fAddButton;
	private Button fEditButton;
	private Button fRemoveButton;

	class FolderProvider extends WorkbenchContentProvider {
		public boolean hasChildren(Object element) {
			Object[] children = getChildren(element);
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IFolder) {
					return true;
				}
			}
			return false;
		}
	}
	class ContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			ISiteModel model = (ISiteModel) parent;
			return model.getSite().getArchives();
		}
	}
	class ArchiveLabelProvider extends LabelProvider
			implements
				ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			ISiteArchive archive = (ISiteArchive) obj;
			switch (index) {
				case 0 :
					return archive.getPath();
				case 1 :
					return archive.getURL();
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}
	/**
	 * @param formPage
	 */
	public ArchiveSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION);
		getSection()
				.setText(
						PDEPlugin
								.getResourceString("SiteEditor.ArchiveSection.header"));
		getSection().setDescription(
				PDEPlugin.getResourceString("SiteEditor.ArchiveSection.instruction"));
		createClient(getSection(), formPage.getManagedForm().getToolkit());
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.update.ui.forms.internal.FormSection#createClient(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.update.ui.forms.internal.FormWidgetFactory)
	 */
	public void createClient(Section section, FormToolkit toolkit) {
		fModel = (ISiteModel) getPage().getModel();
		fModel.addModelChangedListener(this);
		fBuildModel = fModel.getBuildModel();
		if (fBuildModel != null)
			fBuildModel.addModelChangedListener(this);
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 9;
		layout.numColumns = 2;
		container.setLayout(layout);

		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		createTable(container, toolkit);
		createTableViewer();
		createButtons(container, toolkit);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private void createButtons(Composite parent, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		container.setLayout(layout);
		container
				.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		fAddButton = toolkit.createButton(container, PDEPlugin
				.getResourceString("SiteEditor.add"), SWT.PUSH);
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showDialog(null);
			}
		});
		fEditButton = toolkit.createButton(container, PDEPlugin
				.getResourceString("SiteEditor.edit"), SWT.PUSH);
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ssel = (IStructuredSelection) fViewer
						.getSelection();
				if (ssel != null && ssel.size() == 1)
					showDialog((ISiteArchive) ssel.getFirstElement());
			}
		});
		fRemoveButton = toolkit.createButton(container, PDEPlugin
				.getResourceString("SiteEditor.remove"), SWT.PUSH);
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDelete();
			}
		});
		fRemoveButton.setEnabled(false);
		fEditButton.setEnabled(false);
		toolkit.paintBordersFor(container);
	}
	private void createTable(Composite container, FormToolkit toolkit) {
		fTable = toolkit.createTable(container, SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		fTable.setLayoutData(gd);
		TableColumn col1 = new TableColumn(fTable, SWT.NULL);
		col1.setText(PDEPlugin
				.getResourceString("SiteEditor.ArchiveSection.col1"));
		TableColumn col2 = new TableColumn(fTable, SWT.NULL);
		col2.setText(PDEPlugin
				.getResourceString("SiteEditor.ArchiveSection.col2"));
		TableLayout tlayout = new TableLayout();
		tlayout.addColumnData(new ColumnWeightData(50, 200));
		tlayout.addColumnData(new ColumnWeightData(50, 200));
		fTable.setLayout(tlayout);
		fTable.setHeaderVisible(true);
		fTable.setLinesVisible(true);
		createContextMenu(fTable);
	}
	private void createTableViewer() {
		fViewer = new TableViewer(fTable);
		fViewer.setContentProvider(new ContentProvider());
		fViewer.setLabelProvider(new ArchiveLabelProvider());
		fViewer.setInput(getPage().getModel());
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged();
			}
		});
	}
	private void handleSelectionChanged() {
		ISelection selection = fViewer.getSelection();
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			fRemoveButton.setEnabled(ssel.size() > 0);
			fEditButton.setEnabled(ssel.size() == 1);
		} else {
			fRemoveButton.setEnabled(false);
			fEditButton.setEnabled(false);
		}
	}
	private void showDialog(final ISiteArchive archive) {
		final ISiteModel model = (ISiteModel) getPage().getModel();
		BusyIndicator.showWhile(fTable.getDisplay(), new Runnable() {
			public void run() {
				NewArchiveDialog dialog = new NewArchiveDialog(fTable
						.getShell(), model, archive);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, -1);
				dialog.open();
			}
		});
	}
	private void handleDelete() {
		try {
			ISelection selection = fViewer.getSelection();
			if (selection != null && selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection) selection;
				if (ssel.size() > 0) {
					ISiteArchive[] array = (ISiteArchive[]) ssel.toList()
							.toArray(new ISiteArchive[ssel.size()]);
					ISite site = ((ISiteModel) getPage().getModel()).getSite();
					site.removeArchives(array);
				}
			}
		} catch (CoreException e) {
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.update.ui.forms.internal.FormSection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator.showWhile(fTable.getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		return false;
	}
	public void refresh() {
		fViewer.refresh();
		super.refresh();
	}
	public void initialize() {
		refresh();
	}
	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}
	private void createContextMenu(Control control) {
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				mng.add(new Action(PDEPlugin
						.getResourceString("SiteEditor.remove")) {
					public void run() {
						doGlobalAction(ActionFactory.DELETE.getId());
					}
				});
				mng.add(new Separator());
				PDEFormEditorContributor contributor = getPage().getPDEEditor()
						.getContributor();
				contributor.contextMenuAboutToShow(mng);
			}
		};
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		control.setMenu(popupMenuManager.createContextMenu(control));
	}
	public void commit(boolean onSave) {
		if (onSave && fBuildModel instanceof WorkspaceSiteBuildModel
				&& ((WorkspaceSiteBuildModel) fBuildModel).isDirty()) {
			((WorkspaceSiteBuildModel) fBuildModel).save();
		}
	}
}