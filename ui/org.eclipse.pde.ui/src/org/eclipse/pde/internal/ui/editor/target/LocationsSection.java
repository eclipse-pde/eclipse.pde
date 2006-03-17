package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class LocationsSection extends TableSection {
	
	private TableViewer fContentViewer;
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			ITarget target = getTarget();
			return target.getAdditionalDirectories();
		}
	}

	public LocationsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.TWISTIE, new String[] {PDEUIMessages.LocationsSection_add,PDEUIMessages.LocationsSection_edit, PDEUIMessages.LocationsSection_remove});
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginWidth = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 2;
		layout.verticalSpacing = 15;
		client.setLayout(layout);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createViewerPartControl(client, SWT.MULTI, 2, toolkit);
		
		TablePart tablePart = getTablePart();
		GridData data = (GridData) tablePart.getControl().getLayoutData();
		data.grabExcessVerticalSpace = true;
		data.grabExcessHorizontalSpace = true;
		fContentViewer = tablePart.getTableViewer();
		fContentViewer.setContentProvider(new ContentProvider());
		fContentViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fContentViewer.setInput(PDECore.getDefault().getModelManager());
		fContentViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fContentViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				IAdditionalLocation loc1 = (IAdditionalLocation)e1;
				return loc1.getPath().compareToIgnoreCase(((IAdditionalLocation)e2).getPath());
			}
		});
		fContentViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				// if user double clicked on one selected item
				if (((StructuredSelection)fContentViewer.getSelection()).toArray().length == 1)
					handleEdit();
			}
		});
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
		section.setText(PDEUIMessages.LocationsSection_title);
		section.setDescription(PDEUIMessages.LocationsSection_description);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		updateButtons();
		if (getModel().getTarget().getAdditionalDirectories().length > 0)
			section.setExpanded(true);
		
		getModel().addModelChangedListener(this);
	}
	
	private ITarget getTarget() {
		return getModel().getTarget();
	}
	
	private ITargetModel getModel() {
		return (ITargetModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	protected void updateButtons() {
		int selectionNum = ((StructuredSelection)fContentViewer.getSelection()).toArray().length;
		TablePart table = getTablePart();
		table.setButtonEnabled(1, selectionNum == 1);
		table.setButtonEnabled(2, selectionNum > 0);
	}
	
	protected void buttonSelected(int index) {
		switch (index) {
		case 0:
			handleAdd();
			break;
		case 1:
			handleEdit();
			break;
		case 2:
			handleDelete();
		}
	}
	
	protected void handleAdd() {
		showDialog(null);
	}
	
	protected void handleEdit() {
		showDialog((IAdditionalLocation)((StructuredSelection)fContentViewer.getSelection()).iterator().next());
	}
	
	protected void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection)fContentViewer.getSelection();
		if (ssel.size() > 0) {
			Object[] objects = ssel.toArray();
			ITarget target = getTarget();
			IAdditionalLocation[] dirs = new IAdditionalLocation[objects.length];
			System.arraycopy(objects, 0, dirs, 0, objects.length);
			target.removeAdditionalDirectories(dirs);
		}
	}
	
	private void showDialog(final IAdditionalLocation location) {
		final ITarget model = getTarget();
		BusyIndicator.showWhile(fContentViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				LocationDialog dialog = new LocationDialog(fContentViewer.getTable()
						.getShell(), model, location);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 500, -1);
				dialog.open();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object[] objects = e.getChangedObjects();
		if (e.getChangeType() == IModelChangedEvent.INSERT) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IAdditionalLocation) {
					fContentViewer.add(objects[i]);
				}
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IAdditionalLocation) {
					fContentViewer.remove(objects[i]);
				}
			}
		}
		if (e.getChangedProperty() == IAdditionalLocation.P_PATH) {
			fContentViewer.refresh();
		}
	}
	
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		} 	
		if (actionId.equals(ActionFactory.CUT.getId())) {
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}
	
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof IAdditionalLocation)
				return true;
		}
		return false;
	}
	
	protected void doPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof IAdditionalLocation &&
					!hasPath(((IAdditionalLocation)objects[i]).getPath())) {
				IAdditionalLocation loc = (IAdditionalLocation)objects[i];
				loc.setModel(getModel());
				getTarget().addAdditionalDirectories(new IAdditionalLocation[] {loc});	
			}
		}
	}
	
	protected boolean hasPath(String path) {
		Path checkPath = new Path(path);
		IAdditionalLocation[] locs = getModel().getTarget().getAdditionalDirectories();
		for (int i = 0; i < locs.length; i++) {
			if (ExternalModelManager.arePathsEqual(new Path(locs[i].getPath()), checkPath))
				return true;
		}
		return isTargetLocation(checkPath);
	}
	
	private boolean isTargetLocation(Path path) {
		ILocationInfo info = getModel().getTarget().getLocationInfo();
		if (info.useDefault()) {
			Path home = new Path(ExternalModelManager.computeDefaultPlatformPath());
			return ExternalModelManager.arePathsEqual(home, path);
		} 
		return ExternalModelManager.arePathsEqual(new Path(info.getPath()) , path);
	}

	public void dispose() {
		ITargetModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fContentViewer.refresh();
		updateButtons();
		super.refresh();
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection ssel = (IStructuredSelection)fContentViewer.getSelection();
		if (ssel == null)
			return;
		
		Action removeAction = new Action(PDEUIMessages.ContentSection_remove) { 
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && ssel.size() > 0);
		manager.add(removeAction);
		
		manager.add(new Separator());
		
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}
}
