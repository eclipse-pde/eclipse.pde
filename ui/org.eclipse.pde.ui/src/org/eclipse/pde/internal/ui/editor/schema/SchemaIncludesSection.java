/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Code 9 Corporation - ongoing enhancements
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaInclude;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class SchemaIncludesSection extends TableSection {

	private TableViewer fViewer;

	class PDEProjectFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IProject) {
				try {
					return ((IProject) element).hasNature(PDE.PLUGIN_NATURE);
				} catch (CoreException e) {
				}
			} else if (element instanceof IFile) {
				return isUnlistedInclude((IFile) element);
			}
			return true;
		}
	}

	public SchemaIncludesSection(SchemaOverviewPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.SchemaIncludesSection_addButton, PDEUIMessages.SchemaIncludesSection_removeButton});
		getSection().setText(PDEUIMessages.SchemaIncludesSection_title);
		getSection().setDescription(PDEUIMessages.SchemaIncludesSection_description);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fViewer = tablePart.getTableViewer();
		fViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fViewer.setContentProvider(ArrayContentProvider.getInstance());

		getSchema().addModelChangedListener(this);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));

		initialize();
	}

	@Override
	protected void buttonSelected(int index) {
		if (index == 0)
			handleNewInclude();
		else
			handleRemoveInclude();
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getManagedForm().fireSelectionChanged(this, selection);
		getPage().getPDEEditor().setSelection(selection);
		if (!getSchema().isEditable())
			return;
		Object object = fViewer.getStructuredSelection().getFirstElement();
		getTablePart().setButtonEnabled(1, object instanceof ISchemaInclude);
	}

	@Override
	public void dispose() {
		ISchema schema = getSchema();
		if (schema != null)
			schema.removeModelChangedListener(this);
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		int changeType = e.getChangeType();
		if (changeType == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object[] objects = e.getChangedObjects();
		for (Object changedObject : objects) {
			if (changedObject instanceof ISchemaInclude) {
				if (changeType == IModelChangedEvent.INSERT) {
					fViewer.add(changedObject);
				} else if (changeType == IModelChangedEvent.REMOVE) {
					fViewer.remove(changedObject);
				}
			}
		}
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemoveInclude();
			return true;
		}
		return super.doGlobalAction(actionId);
	}

	private ISchema getSchema() {
		return (ISchema) getPage().getModel();
	}

	protected void handleRemoveInclude() {
		Object[] selected = new Object[0];
		IStructuredSelection selection = fViewer.getStructuredSelection();
		if (selection.isEmpty())
			return;
		selected = selection.toArray();
		Schema schema = (Schema) getSchema();
		for (Object selectedObject : selected) {
			schema.removeInclude((ISchemaInclude) selectedObject);
		}
	}

	protected void handleNewInclude() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getPage().getSite().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ProductExportWizardPage_fileSelection);
		dialog.setMessage(PDEUIMessages.SchemaIncludesSection_dialogMessage);
		dialog.addFilter(new FileExtensionFilter("exsd")); //$NON-NLS-1$
		dialog.addFilter(new PDEProjectFilter());
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			Object result = dialog.getFirstResult();
			if (!(result instanceof IFile))
				return;
			IFile newInclude = (IFile) result;

			String location = getIncludeLocation(newInclude);
			ISchemaInclude include = new SchemaInclude(getSchema(), location, false);
			ISchema schema = getSchema();
			if (schema instanceof Schema)
				((Schema) schema).addInclude(include);
		}
	}

	private void initialize() {
		refresh();
	}

	private String getIncludeLocation(IFile file) {
		IEditorInput input = getPage().getEditorInput();
		if (!(input instanceof IFileEditorInput))
			return null;
		IPath schemaPath = ((IFileEditorInput) input).getFile().getFullPath();
		IPath currPath = file.getFullPath();
		int matchinSegments = schemaPath.matchingFirstSegments(currPath);
		if (matchinSegments > 0) {
			schemaPath = schemaPath.removeFirstSegments(matchinSegments);
			currPath = currPath.removeFirstSegments(matchinSegments);
			if (schemaPath.segmentCount() == 1)
				return currPath.toString();
			StringBuilder sb = new StringBuilder();
			while (schemaPath.segmentCount() > 1) {
				sb.append("../"); //$NON-NLS-1$
				schemaPath = schemaPath.removeFirstSegments(1);
			}
			String location = sb.toString() + currPath.toString();
			return location.trim().length() > 0 ? location : null;
		}
		IPluginModelBase model = PluginRegistry.findModel(file.getProject());
		String id = model.getPluginBase().getId();
		if (id != null)
			return "schema://" + id + "/" + file.getProjectRelativePath().toString(); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	private boolean isUnlistedInclude(IFile file) {
		String location = getIncludeLocation(file);
		if (location == null)
			return false;
		boolean unlisted = true;
		ISchemaInclude[] includes = getSchema().getIncludes();
		for (ISchemaInclude include : includes) {
			if (include.getLocation().equals(location)) {
				unlisted = false;
				break;
			}
		}
		return unlisted;
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		Object object = selection.getFirstElement();
		if (object instanceof ISchemaInclude) {
			IEditorInput edinput = getPage().getEditorInput();
			if (!(edinput instanceof IFileEditorInput))
				return;
			String path = ((ISchemaInclude) object).getLocation();
			IPath includePath = new Path(((ISchemaInclude) object).getLocation());
			boolean result = false;
			if (path.startsWith("schema:")) { //$NON-NLS-1$
				result = SchemaEditor.openSchema(includePath);
			} else {
				IFile currSchemaFile = ((IFileEditorInput) edinput).getFile();
				IProject project = currSchemaFile.getProject();
				IPath currSchemaPath = currSchemaFile.getProjectRelativePath();
				IFile file = project.getFile(currSchemaPath.removeLastSegments(1).append(includePath));
				result = SchemaEditor.openSchema(file);
			}
			if (!result)
				MessageDialog.openWarning(getPage().getSite().getShell(), PDEUIMessages.SchemaIncludesSection_missingWarningTitle, NLS.bind(PDEUIMessages.SchemaIncludesSection_missingWarningMessage, includePath.toString()));
		}
	}

	@Override
	public void refresh() {
		getTablePart().setButtonEnabled(0, getSchema().isEditable());
		getTablePart().setButtonEnabled(1, false);
		fViewer.setInput(getSchema().getIncludes());
		super.refresh();
	}
}
