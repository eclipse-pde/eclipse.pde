/*
 * Created on Jan 29, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.plugin;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.extension.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.part.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ExtensionPointsSection extends TableSection {
	public static final String SECTION_TITLE = "ManifestEditor.DetailExtensionPointSection.title"; //$NON-NLS-1$
	public static final String SECTION_DESC = "ManifestEditor.DetailExtensionPointSection.desc"; //$NON-NLS-1$
	public static final String SECTION_NEW = "ManifestEditor.DetailExtensionPointSection.new"; //$NON-NLS-1$
	public static final String POPUP_NEW_EXTENSION_POINT =
		"ManifestEditor.DetailExtensionPointSection.newExtensionPoint"; //$NON-NLS-1$
	public static final String POPUP_OPEN_SCHEMA =
		"ManifestEditor.DetailExtensionPointSection.openSchema"; //$NON-NLS-1$
	public static final String POPUP_DELETE = "Actions.delete.label";	 //$NON-NLS-1$
	private TableViewer pointTable;
	
	class TableContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			IPluginModelBase model = (IPluginModelBase)getPage().getModel();
			IPluginBase pluginBase = model.getPluginBase();
			if (pluginBase!=null)
				return pluginBase.getExtensionPoints();
			return new Object[0];
		}
	}
	
	public ExtensionPointsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[]{PDEPlugin
				.getResourceString(SECTION_NEW)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		handleDefaultButton = false;
		getTablePart().setEditable(false);
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		TablePart tablePart = getTablePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		pointTable = tablePart.getTableViewer();
		pointTable.setContentProvider(new TableContentProvider());
		pointTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
		section.setClient(container);
		pointTable.setInput(getPage());
		selectFirstExtensionPoint();
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider)model).addModelChangedListener(this);
		tablePart.setButtonEnabled(0, model.isEditable());
	}
	private void selectFirstExtensionPoint() {
		Table table = pointTable.getTable();
		TableItem [] items = table.getItems();
		if (items.length==0) return;
		TableItem firstItem = items[0];
		Object obj = firstItem.getData();
		pointTable.setSelection(new StructuredSelection(obj));
	}
	void fireSelection() {
		pointTable.setSelection(pointTable.getSelection());
	}
	public void dispose() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider)model).removeModelChangedListener(this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}	
	public void refresh() {
		pointTable.refresh();
		getManagedForm().fireSelectionChanged(this, pointTable.getSelection());
		super.refresh();
	}
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginExtensionPoint) {
			pointTable.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		super.selectionChanged(selection);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginExtensionPoint) {
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				pointTable.add(changeObject);
				pointTable.setSelection(
					new StructuredSelection(changeObject),
					true);
				pointTable.getTable().setFocus();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				pointTable.remove(changeObject);
			} else {
				pointTable.update(changeObject, null);
			}
		}
	}
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = pointTable.getSelection();

		Action newAction =
			new Action(
				PDEPlugin.getResourceString(POPUP_NEW_EXTENSION_POINT)) {
			public void run() {
				handleNew();
			}
		};
		newAction.setEnabled(isEditable());
		manager.add(newAction);

		if (!selection.isEmpty()) {
			Object object =
				((IStructuredSelection) selection).getFirstElement();
			final IPluginExtensionPoint point = (IPluginExtensionPoint) object;
			
			if (point.getSchema() != null) {
				final IEditorInput input =
					getPage().getEditor().getEditorInput();
				if (input instanceof IFileEditorInput
					|| input instanceof SystemFileEditorInput) {

					Action openSchemaAction =
						new Action(
							PDEPlugin.getResourceString(POPUP_OPEN_SCHEMA)) {
						public void run() {
							handleOpenSchema(point);
						}
					};
					manager.add(openSchemaAction);
				}
			}

			manager.add(new Separator());

			Action deleteAction =
				new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					IPluginBase plugin = point.getPluginBase();
					try {
						plugin.remove(point);
					} catch (CoreException e) {
					}
				}
			};
			deleteAction.setEnabled(isEditable());
			manager.add(deleteAction);
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		if (!selection.isEmpty()) {
			PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
			actionGroup.setContext(new ActionContext(selection));
			actionGroup.fillContextMenu(manager);
		}
	}
	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
	}
	private void handleDelete() {
		Object object =
			((IStructuredSelection) pointTable.getSelection())
				.getFirstElement();
		if (object != null && object instanceof IPluginExtensionPoint) {
			IPluginExtensionPoint ep = (IPluginExtensionPoint) object;
			IPluginBase plugin = ep.getPluginBase();
			try {
				plugin.remove(ep);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void handleNew() {
		IFile file =
			((IFileEditorInput) getPage().getPDEEditor().getEditorInput())
				.getFile();
		final IProject project = file.getProject();
		BusyIndicator
			.showWhile(pointTable.getTable().getDisplay(), new Runnable() {
			public void run(){
				NewExtensionPointWizard wizard =
					new NewExtensionPointWizard(
						project,
						(IPluginModelBase) getPage().getModel(), 
						(ManifestEditor)getPage().getPDEEditor());
				WizardDialog dialog =
					new WizardDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 450);
				dialog.open();
			}
		});
	}

	private void handleOpenSchema(IPluginExtensionPoint point) {
		String schema = point.getSchema();
		IModel model = point.getModel();
		IResource resource = model.getUnderlyingResource();
		final IWorkbenchPage page = PDEPlugin.getActivePage();

		final IEditorInput input;

		if (resource != null) {
			IProject project = resource.getProject();
			IFile file = project.getFile(schema);
			input = new FileEditorInput(file);
		} else {
			IPluginModelBase pmodel = (IPluginModelBase) model;
			String location = pmodel.getInstallLocation();
			if (location.startsWith("file:")) //$NON-NLS-1$
				location = location.substring(5);
			File file = new File(location + File.separator + schema);
			if (file.exists()==false) {
				// try source location
				SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
				file = manager.findSourceFile(point.getPluginBase(), new Path(schema));
			}
			input = new SystemFileEditorInput(file);
		}
		BusyIndicator
			.showWhile(pointTable.getTable().getDisplay(), new Runnable() {
			public void run() {
				try {
					page.openEditor(input, IPDEUIConstants.SCHEMA_EDITOR_ID);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
	protected void doPaste(Object target, Object[] objects) {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof IPluginExtensionPoint) {
					PluginExtensionPoint point = (PluginExtensionPoint) obj;
					point.setModel(model);
					point.setParent(plugin);
					plugin.add(point);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	protected boolean canPaste(Object target, Object[] objects) {
		if (objects[0] instanceof IPluginExtensionPoint)
			return true;
		return false;
	}	
}