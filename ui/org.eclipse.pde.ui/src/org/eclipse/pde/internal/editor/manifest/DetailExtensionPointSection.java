package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.pde.model.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import java.util.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.window.*;
import org.eclipse.swt.custom.*;
import org.eclipse.pde.internal.parts.TablePart;
import org.eclipse.pde.internal.model.plugin.PluginExtensionPoint;

public class DetailExtensionPointSection
	extends TableSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE =
		"ManifestEditor.DetailExtensionPointSection.title";
	public static final String SECTION_NEW =
		"ManifestEditor.DetailExtensionPointSection.new";
	public static final String POPUP_NEW_EXTENSION_POINT =
		"ManifestEditor.DetailExtensionPointSection.newExtensionPoint";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private FormWidgetFactory factory;
	private TableViewer pointTable;
	private SchemaRegistry schemaRegistry;
	private ExternalModelManager pluginInfoRegistry;

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginBase) {
				return ((IPluginBase) parent).getExtensionPoints();
			}
			return new Object[0];
		}
	}

	public DetailExtensionPointSection(ManifestExtensionPointPage page) {
		super(page, new String [] { PDEPlugin.getResourceString(SECTION_NEW) });
		this.setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		schemaRegistry = PDEPlugin.getDefault().getSchemaRegistry();
		pluginInfoRegistry = PDEPlugin.getDefault().getExternalModelManager();
	}
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		Composite container = createClientContainer(parent, 2, factory);
		
		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);
		TablePart part = getTablePart();
		pointTable = part.getTableViewer();
		pointTable.setContentProvider(new TableContentProvider());
		pointTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		factory.paintBordersFor(container);
		
		GridData gd = (GridData)part.getControl().getLayoutData();
		gd.widthHint = 200;
		return container;
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		fireSelectionNotification(item);
		getFormPage().setSelection(selection);
	}
	
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.CUT)) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(IWorkbenchActionConstants.PASTE)) {
			doPaste();
			return true;
		}
		return false;
	}
	public void expandTo(Object object) {
		pointTable.setSelection(new StructuredSelection(object), true);
	}
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = pointTable.getSelection();

		manager
			.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_EXTENSION_POINT)) {
			public void run() {
				handleNew();
			}
		});

		if (!selection.isEmpty()) {
			Object object = ((IStructuredSelection) selection).getFirstElement();
			final IPluginExtensionPoint point = (IPluginExtensionPoint) object;

			manager.add(new Separator());
			manager.add(new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					IPluginBase plugin = point.getPluginBase();
					try {
						plugin.remove(point);
					} catch (CoreException e) {
					}
				}
			});
		}
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());
		manager.add(new PropertiesAction(getFormPage().getEditor()));
	}
	
	protected void buttonSelected(int index) {
		if (index==0) handleNew();
	}
	private void handleDelete() {
		Object object =
			((IStructuredSelection) pointTable.getSelection()).getFirstElement();
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
			((IFileEditorInput) getFormPage().getEditor().getEditorInput()).getFile();
		final IProject project = file.getProject();
		BusyIndicator.showWhile(pointTable.getTable().getDisplay(), new Runnable() {
			public void run() {
				NewExtensionPointWizard wizard =
					new NewExtensionPointWizard(
						project,
						(IPluginModelBase) getFormPage().getModel());
				WizardDialog dialog =
					new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				dialog.getShell().setSize(400, 450);
				dialog.open();
			}
		});
	}
	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		pointTable.setInput(model.getPluginBase());
		setReadOnly(!model.isEditable());
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			pointTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginExtensionPoint) {
			if (event.getChangeType() == event.INSERT) {
				pointTable.add(changeObject);
				pointTable.setSelection(new StructuredSelection(changeObject), true);
				pointTable.getTable().setFocus();
			} else if (event.getChangeType() == event.REMOVE) {
				pointTable.remove(changeObject);
			} else {
				pointTable.update(changeObject, null);
				if (pointTable.getTable().isFocusControl()) {
					ISelection sel = getFormPage().getSelection();
					if (sel != null && sel instanceof IStructuredSelection) {
						IStructuredSelection ssel = (IStructuredSelection) sel;
						if (!ssel.isEmpty() && ssel.getFirstElement().equals(changeObject)) {
							// update property sheet
							getFormPage().setSelection(sel);
						}
					}
				}
			}
		}
	}
	public void setFocus() {
		pointTable.getTable().setFocus();
	}
	protected void doPaste(Object target, Object[] objects) {
		IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
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
		if (objects[0] instanceof IPluginExtensionPoint) return true;
		return false;
	}
}