/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
 *     Ketan Padegaonkar <KetanPadegaonkar@gmail.com> - bug 233682
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionPointNode;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.actions.OpenSchemaAction;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.refactoring.PDERefactoringAction;
import org.eclipse.pde.internal.ui.refactoring.RefactoringActionFactory;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionPointWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ExtensionPointsSection extends TableSection {
	private TableViewer pointTable;

	class TableContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IPluginBase pluginBase = model.getPluginBase();
			if (pluginBase != null)
				return pluginBase.getExtensionPoints();
			return new Object[0];
		}
	}

	public ExtensionPointsSection(PDEFormPage page, Composite parent) {
		super(page, parent, ExpandableComposite.TITLE_BAR | Section.DESCRIPTION, new String[] {PDEUIMessages.ManifestEditor_DetailExtensionPointSection_new, PDEUIMessages.Actions_delete_label});
		getSection().setText(PDEUIMessages.ManifestEditor_DetailExtensionPointSection_title);
		getSection().setDescription(PDEUIMessages.ExtensionPointsSection_sectionDescAllExtensionPoints);
		fHandleDefaultButton = false;
		getTablePart().setEditable(false);
	}

	@Override
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
			((IModelChangeProvider) model).addModelChangedListener(this);
		tablePart.setButtonEnabled(0, model.isEditable());
	}

	private void selectFirstExtensionPoint() {
		Table table = pointTable.getTable();
		TableItem[] items = table.getItems();
		if (items.length == 0)
			return;
		TableItem firstItem = items[0];
		Object obj = firstItem.getData();
		pointTable.setSelection(new StructuredSelection(obj));
	}

	void fireSelection() {
		pointTable.setSelection(pointTable.getStructuredSelection());
	}

	@Override
	public void dispose() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IModelChangeProvider)
			((IModelChangeProvider) model).removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	public boolean doGlobalAction(String actionId) {

		if (!isEditable()) {
			return false;
		}

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
		return super.doGlobalAction(actionId);
	}

	@Override
	public void refresh() {
		pointTable.refresh();
		getManagedForm().fireSelectionChanged(this, pointTable.getStructuredSelection());
		super.refresh();
	}

	@Override
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginExtensionPoint) {
			pointTable.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		super.selectionChanged(selection);
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginExtensionPoint) {
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				pointTable.add(changeObject);
				pointTable.setSelection(new StructuredSelection(changeObject), true);
				pointTable.getTable().setFocus();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				pointTable.remove(changeObject);
			} else {
				pointTable.update(changeObject, null);
			}
		}
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = pointTable.getStructuredSelection();

		Action newAction = new Action(PDEUIMessages.ManifestEditor_DetailExtensionPointSection_newExtensionPoint) {
			@Override
			public void run() {
				handleNew();
			}
		};
		newAction.setEnabled(isEditable());
		manager.add(newAction);

		if (selection.isEmpty()) {
			getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
			return;
		}
		manager.add(new Separator());
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setBaseModel(model);
		actionGroup.setContext(new ActionContext(selection));
		actionGroup.fillContextMenu(manager);
		manager.add(new Separator());
		if (isEditable() && selection.size() == 1) {
			PDERefactoringAction action = RefactoringActionFactory.createRefactorExtPointAction(PDEUIMessages.ExtensionPointsSection_rename_label);
			action.setSelection(selection.getFirstElement());
			manager.add(action);
			manager.add(new Separator());
		}

		Action deleteAction = new Action(PDEUIMessages.Actions_delete_label) {
			@Override
			public void run() {
				handleDelete();
			}
		};
		deleteAction.setEnabled(isEditable());
		manager.add(deleteAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	@Override
	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
		else if (index == 1)
			handleDelete();
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			PluginExtensionPointNode extensionPoint = (PluginExtensionPointNode) selection.getFirstElement();
			String pointID = extensionPoint.getId();
			// For some stupid reason extensionPoint.getFullId() does not return the full id.
			IBaseModel model = getPage().getPDEEditor().getAggregateModel();
			String basePointID = ((IPluginModelBase) model).getPluginBase().getId();
			pointID = basePointID + '.' + pointID;

			OpenSchemaAction action = new OpenSchemaAction();
			action.setInput(pointID);
			action.run();
		}
	}

	private void handleDelete() {
		Object[] selection = pointTable.getStructuredSelection().toArray();
		for (Object selectedObject : selection) {
			if (selectedObject != null && selectedObject instanceof IPluginExtensionPoint) {
				IStructuredSelection newSelection = null;
				IPluginExtensionPoint ep = (IPluginExtensionPoint) selectedObject;
				IPluginBase plugin = ep.getPluginBase();
				IPluginExtensionPoint[] points = plugin.getExtensionPoints();
				int index = getNewSelectionIndex(getArrayIndex(points, ep), points.length);
				if (index != -1)
					newSelection = new StructuredSelection(points[index]);
				try {
					String schema = ep.getSchema();
					if (schema != null && schema.length() > 0) {
						IProject project = ep.getModel().getUnderlyingResource().getProject();
						IFile schemaFile = project.getFile(schema);
						if (schemaFile != null && schemaFile.exists())
							if (MessageDialog.openQuestion(getSection().getShell(), PDEUIMessages.ExtensionPointsSection_title, NLS.bind(PDEUIMessages.ExtensionPointsSection_message1, schemaFile.getProjectRelativePath().toString())))
								schemaFile.delete(true, true, new NullProgressMonitor());
					}
					plugin.remove(ep);
					if (newSelection != null)
						pointTable.setSelection(newSelection);

				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	private void handleNew() {
		IFile file = ((IFileEditorInput) getPage().getPDEEditor().getEditorInput()).getFile();
		final IProject project = file.getProject();
		BusyIndicator.showWhile(pointTable.getTable().getDisplay(), () -> {
			NewExtensionPointWizard wizard = new NewExtensionPointWizard(project,
					(IPluginModelBase) getPage().getModel(), (ManifestEditor) getPage().getPDEEditor());
			WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
			dialog.create();
			SWTUtil.setDialogSize(dialog, 400, 450);
			dialog.open();
		});
	}

	private IPluginModelBase getPluginModelBase() {
		// Note:  This method will work with fragments as long as a fragment.xml
		// is defined first.  Otherwise, paste will not work out of the box.
		// Get the model
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		// Ensure the model is a bundle plugin model
		if ((model instanceof IBundlePluginModelBase) == false) {
			return null;
		}
		// Get the extension model
		ISharedExtensionsModel extensionModel = ((IBundlePluginModelBase) model).getExtensionsModel();
		// Ensure the extension model is defined
		if ((extensionModel == null) || ((extensionModel instanceof IPluginModelBase) == false)) {
			return null;
		}
		return ((IPluginModelBase) extensionModel);
	}

	@Override
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// By default, fragment.xml does not exist until the first extension
		// or extension point is created.
		// Ensure the file exists before pasting because the model will be
		// null and the paste will fail if it does not exist
		((ManifestEditor) getPage().getEditor()).ensurePluginContextPresence();
		// Get the model
		IPluginModelBase model = getPluginModelBase();
		// Ensure an editable model was actually retrieved
		if (model == null) {
			return;
		}
		IPluginBase pluginBase = model.getPluginBase();
		try {
			// Paste all source objects
			// Since, the extension points are a flat non-hierarchical list,
			// the target object is not needed
			for (Object sourceObject : sourceObjects) {
				if ((sourceObject instanceof IPluginExtensionPoint) && (pluginBase instanceof IDocumentElementNode)) {
					// Extension point object
					IDocumentElementNode extensionPoint = (IDocumentElementNode) sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					extensionPoint.reconnect((IDocumentElementNode) pluginBase, model);
					// Add the extension point to the plug-in
					pluginBase.add((IPluginExtensionPoint) extensionPoint);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		// All source objects must be extension points
		// No restriction on duplicates
		for (Object sourceObject : sourceObjects) {
			if ((sourceObject instanceof IPluginExtensionPoint) == false) {
				return false;
			}
		}
		return true;
	}

	protected void selectExtensionPoint(ISelection selection) {
		pointTable.setSelection(selection, true);
	}
}
