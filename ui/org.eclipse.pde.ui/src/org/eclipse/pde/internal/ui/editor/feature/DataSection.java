/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.feature.FeatureData;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DataSection extends TableSection {
	private TableViewer fDataViewer;

	private Action fNewAction;

	private Action fOpenAction;

	private Action fDeleteAction;

	class PluginContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				return ((IFeature) parent).getData();
			}
			return new Object[0];
		}
	}

	public DataSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.FeatureEditor_DataSection_new});
		getSection().setText(PDEUIMessages.FeatureEditor_DataSection_title);
		getSection().setDescription(PDEUIMessages.FeatureEditor_DataSection_desc);
		getTablePart().setEditable(false);
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fDataViewer = tablePart.getTableViewer();
		fDataViewer.setContentProvider(new PluginContentProvider());
		fDataViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		initialize();
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		fOpenAction.run();
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	public boolean setFormInput(Object object) {
		if (object instanceof IFeatureData) {
			fDataViewer.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fOpenAction);
		manager.add(new Separator());
		manager.add(fNewAction);
		manager.add(fDeleteAction);
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void handleNew() {
		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		IResource resource = model.getUnderlyingResource();
		final IContainer folder = resource.getParent();

		BusyIndicator.showWhile(fDataViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				ResourceSelectionDialog dialog = new ResourceSelectionDialog(fDataViewer.getTable().getShell(), folder, null);
				dialog.open();
				Object[] result = dialog.getResult();
				processNewResult(model, folder, result);
			}
		});
	}

	private void processNewResult(IFeatureModel model, IContainer folder, Object[] result) {
		if (result == null || result.length == 0)
			return;
		IPath folderPath = folder.getProjectRelativePath();
		ArrayList entries = new ArrayList();
		for (int i = 0; i < result.length; i++) {
			Object item = result[i];
			if (item instanceof IFile) {
				IFile file = (IFile) item;
				IPath filePath = file.getProjectRelativePath();
				int matching = filePath.matchingFirstSegments(folderPath);
				IPath relativePath = filePath.removeFirstSegments(matching);
				String path = relativePath.toString();
				if (canAdd(model, path))
					entries.add(path);
			}
		}
		if (entries.size() > 0) {
			try {
				IFeatureData[] array = new IFeatureData[entries.size()];
				for (int i = 0; i < array.length; i++) {
					IFeatureData data = model.getFactory().createData();
					String path = (String) entries.get(i);
					data.setId(path);
					array[i] = data;
				}
				model.getFeature().addData(array);
				fDataViewer.setSelection(new StructuredSelection(array[0]));
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	private boolean canAdd(IFeatureModel model, String path) {
		if (ICoreConstants.FEATURE_FILENAME_DESCRIPTOR.equals(path))
			return false;
		IFeatureData[] data = model.getFeature().getData();
		for (int i = 0; i < data.length; i++) {
			if (path.equals(data[i].getId()))
				return false;
		}
		return true;

	}

	private void handleSelectAll() {
		IStructuredContentProvider provider = (IStructuredContentProvider) fDataViewer.getContentProvider();
		Object[] elements = provider.getElements(fDataViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		fDataViewer.setSelection(ssel);
	}

	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) fDataViewer.getSelection();

		if (ssel.isEmpty())
			return;
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();

		try {
			IFeatureData[] removed = new IFeatureData[ssel.size()];
			int i = 0;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				IFeatureData iobj = (IFeatureData) iter.next();
				removed[i++] = iobj;
			}
			feature.removeData(removed);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator.showWhile(fDataViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			BusyIndicator.showWhile(fDataViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleSelectAll();
				}
			});
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

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof IFeatureData && !(obj instanceof IFeaturePlugin)) {
			if (e.getChangeType() == IModelChangedEvent.CHANGE) {
				fDataViewer.update(obj, null);
			} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
				fDataViewer.add(e.getChangedObjects());
			} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
				fDataViewer.remove(e.getChangedObjects());
			}
		}
	}

	private void makeActions() {
		IModel model = (IModel) getPage().getModel();
		fNewAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		fNewAction.setText(PDEUIMessages.Menus_new_label);
		fNewAction.setEnabled(model.isEditable());

		fDeleteAction = new Action() {
			public void run() {
				BusyIndicator.showWhile(fDataViewer.getTable().getDisplay(), new Runnable() {
					public void run() {
						handleDelete();
					}
				});
			}
		};
		fDeleteAction.setEnabled(model.isEditable());
		fDeleteAction.setText(PDEUIMessages.Actions_delete_label);
		fOpenAction = new OpenReferenceAction(fDataViewer);
	}

	public void setFocus() {
		if (fDataViewer != null)
			fDataViewer.getTable().setFocus();
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fDataViewer.setInput(feature);
		super.refresh();
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object,
	 *      Object[])
	 */
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof FeaturePlugin || !(objects[i] instanceof FeatureData))
				return false;
		}
		return true;
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	protected void doPaste() {
		Clipboard clipboard = getPage().getPDEEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object[] objects = (Object[]) clipboard.getContents(modelTransfer);
		if (objects != null) {
			doPaste(null, objects);
		}
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object,
	 *      Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		if (!model.isEditable()) {
			return;
		}
		FeatureData[] fData = new FeatureData[objects.length];
		try {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof FeatureData && !(objects[i] instanceof FeaturePlugin)) {
					FeatureData fd = (FeatureData) objects[i];
					fd.setModel(model);
					fd.setParent(feature);
					fData[i] = fd;
				}
			}
			feature.addData(fData);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	void fireSelection() {
		ISelection sel = fDataViewer.getSelection();
		if (!sel.isEmpty()) {
			fDataViewer.setSelection(fDataViewer.getSelection());
		} else if (fDataViewer.getElementAt(0) != null) {
			fDataViewer.setSelection(new StructuredSelection(fDataViewer.getElementAt(0)));
		}
	}
}
