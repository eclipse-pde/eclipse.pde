/*
 * Created on Sep 29, 2003
 */
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author melhem
 */
public class ArchiveSection extends PDEFormSection {
	
	private Table fTable;
	private TableViewer fViewer;
	private boolean fUpdateNeeded;
	private Button fAddButton;	
	private Button fEditButton;
	private Button fRemoveButton;
	
	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			ISiteModel model = (ISiteModel)parent;
			return model.getSite().getArchives();
		}
	}

	class ArchiveLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			ISiteArchive archive = (ISiteArchive)obj;
			switch (index) {
				case 0:
					return archive.getPath();
				case 1:
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
	public ArchiveSection(PDEFormPage formPage) {
		super(formPage);
		setHeaderText(PDEPlugin.getResourceString("SiteEditor.ArchiveSection.header"));
		setDescription(PDEPlugin.getResourceString("SiteEditor.ArchiveSection.title"));
		setCollapsable(true);
		setCollapsed(
			((ISiteModel) getFormPage().getModel()).getSite().getArchives().length == 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.internal.FormSection#createClient(org.eclipse.swt.widgets.Composite, org.eclipse.update.ui.forms.internal.FormWidgetFactory)
	 */
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 25;
		container.setLayout(layout);
		
		createTable(container, factory);
		createTableViewer();
		createButtons(container, factory);
		factory.paintBordersFor(container);
		
		return container;
	}
	
	private void createButtons(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		
		fAddButton = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.add"), SWT.PUSH);
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddButton.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc) @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				showDialog(null);
			}
		});
		
		fEditButton = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.edit"), SWT.PUSH);
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEditButton.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc) @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ssel = (IStructuredSelection)fViewer.getSelection();
				if (ssel != null && ssel.size() == 1)
					showDialog((ISiteArchive)ssel.getFirstElement());
			}
		});
		
		
		fRemoveButton = factory.createButton(container, PDEPlugin.getResourceString("SiteEditor.remove"), SWT.PUSH);
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc) @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				handleDelete();
			}
		});
		fRemoveButton.setEnabled(false);
		fEditButton.setEnabled(false);
		factory.paintBordersFor(container);
	}
	
	private void createTable(Composite container, FormWidgetFactory factory) {
		fTable = factory.createTable(container, SWT.FULL_SELECTION|SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		fTable.setLayoutData(gd);
		
		TableColumn col1 = new TableColumn(fTable, SWT.NULL);
		col1.setText(PDEPlugin.getResourceString("SiteEditor.ArchiveSection.col1"));
		
		TableColumn col2 = new TableColumn(fTable, SWT.NULL);
		col2.setText(PDEPlugin.getResourceString("SiteEditor.ArchiveSection.col2"));
		
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
		fViewer.setInput(getFormPage().getModel());
		
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged();				
			}});
	}
	
	private void handleSelectionChanged() {
		ISelection selection = fViewer.getSelection();
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			fRemoveButton.setEnabled(ssel.size() > 0);
		} else {
			fRemoveButton.setEnabled(false);
		}
	}
	
	private void showDialog(final ISiteArchive archive) {
		final ISiteModel model = (ISiteModel) getFormPage().getModel();
		BusyIndicator.showWhile(fTable.getDisplay(), new Runnable() {
			public void run() {
				NewArchiveDialog dialog =
					new NewArchiveDialog(fTable.getShell(), model, archive);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, -1);
				if (dialog.open() == NewArchiveDialog.OK)
					fViewer.refresh();
			}
		});
	}
	
	private void handleDelete() {
		try {
			ISelection selection = fViewer.getSelection();
			if (selection != null && selection instanceof IStructuredSelection) {
				IStructuredSelection ssel = (IStructuredSelection)selection;
				if (ssel.size() > 0) {
					ISiteArchive[] array =
						(ISiteArchive[]) ssel.toList().toArray(new ISiteArchive[ssel.size()]);
					ISite site = ((ISiteModel) getFormPage().getModel()).getSite();
					site.removeArchives(array);
					forceDirty();
					fViewer.refresh();
				}
			}
		} catch (CoreException e) {
		}				
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.update.ui.forms.internal.FormSection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator
				.showWhile(fTable.getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		return false;
	}

	public void update() {
		if (fUpdateNeeded) {
			update(getFormPage().getModel());
		}
	}

	public void update(Object input) {
		fViewer.setInput(input);
		fViewer.refresh();
		fUpdateNeeded = false;
	}
	
	public void initialize(Object input) {
		update(input);
		ISiteModel model = (ISiteModel) input;
		if (!model.isEditable()) {
			fAddButton.setEnabled(false);
			fRemoveButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		fUpdateNeeded = true;
		update();
	}
	
	private void forceDirty() {
		setDirty(true);
		ISiteModel model = (ISiteModel) getFormPage().getModel();

		if (model instanceof IEditable) {
			((IEditable) model).setDirty(true);
		}
		getFormPage().getEditor().fireSaveNeeded();
	}
	
	private void createContextMenu(Control control) {
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				mng.add(new Action(PDEPlugin.getResourceString("SiteEditor.remove")) {
					public void run() {
						doGlobalAction(ActionFactory.DELETE.getId());
					}
				});
				mng.add(new Separator());
				PDEEditorContributor contributor =
					getFormPage().getEditor().getContributor();
				contributor.contextMenuAboutToShow(mng);
			}
		};
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		control.setMenu(popupMenuManager.createContextMenu(control));
	}


}
