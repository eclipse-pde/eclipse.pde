package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.SourceLocationManager;
import org.eclipse.pde.internal.core.plugin.PluginExtensionPoint;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionPointWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class DetailExtensionPointSection
	extends TableSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE =
		"ManifestEditor.DetailExtensionPointSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.DetailExtensionPointSection.desc";
	public static final String SECTION_NEW =
		"ManifestEditor.DetailExtensionPointSection.new";
	public static final String POPUP_NEW_EXTENSION_POINT =
		"ManifestEditor.DetailExtensionPointSection.newExtensionPoint";
	public static final String POPUP_OPEN_SCHEMA =
		"ManifestEditor.DetailExtensionPointSection.openSchema";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private FormWidgetFactory factory;
	private TableViewer pointTable;

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
		super(page, new String[] { PDEPlugin.getResourceString(SECTION_NEW)});
		this.setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		this.setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
	}
	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		this.factory = factory;
		Composite container = createClientContainer(parent, 2, factory);

		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);
		TablePart part = getTablePart();
		pointTable = part.getTableViewer();
		pointTable.setContentProvider(new TableContentProvider());
		pointTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		factory.paintBordersFor(container);

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

		Action newAction =
			new Action(
				PDEPlugin.getResourceString(POPUP_NEW_EXTENSION_POINT)) {
			public void run() {
				handleNew();
			}
		};
		newAction.setEnabled(!isReadOnly());
		manager.add(newAction);

		if (!selection.isEmpty()) {
			Object object =
				((IStructuredSelection) selection).getFirstElement();
			final IPluginExtensionPoint point = (IPluginExtensionPoint) object;
			
			if (point.getSchema() != null) {
				final IEditorInput input =
					getFormPage().getEditor().getEditorInput();
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
			deleteAction.setEnabled(!isReadOnly());
			manager.add(deleteAction);
		}
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		if (!selection.isEmpty()) {
			PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
			actionGroup.setContext(new ActionContext(selection));
			actionGroup.fillContextMenu(manager);
			manager.add(new Separator());

			manager.add(new PropertiesAction(getFormPage().getEditor()));
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
			((IFileEditorInput) getFormPage().getEditor().getEditorInput())
				.getFile();
		final IProject project = file.getProject();
		BusyIndicator
			.showWhile(pointTable.getTable().getDisplay(), new Runnable() {
			public void run() {
				NewExtensionPointWizard wizard =
					new NewExtensionPointWizard(
						project,
						(IPluginModelBase) getFormPage().getModel());
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
			if (location.startsWith("file:"))
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
				if (pointTable.getTable().isFocusControl()) {
					ISelection sel = getFormPage().getSelection();
					if (sel != null && sel instanceof IStructuredSelection) {
						IStructuredSelection ssel = (IStructuredSelection) sel;
						if (!ssel.isEmpty()
							&& ssel.getFirstElement().equals(changeObject)) {
							// update property sheet
							asyncResendSelection(sel);
						}
					}
				}
			}
		}
	}

	private void asyncResendSelection(final ISelection sel) {
		pointTable.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				getFormPage().setSelection(sel);
			}
		});
	}
	public void setFocus() {
		pointTable.getTable().setFocus();
	}
	protected void doPaste(Object target, Object[] objects) {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
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