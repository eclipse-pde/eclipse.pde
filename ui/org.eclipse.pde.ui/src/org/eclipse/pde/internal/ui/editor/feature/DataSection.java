package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.IModelProviderListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.feature.FeatureData;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.editor.PropertiesAction;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class DataSection
	extends TableSection
	implements IModelProviderListener {
	private static final String SECTION_TITLE = "FeatureEditor.DataSection.title";
	private static final String SECTION_DESC = "FeatureEditor.DataSection.desc";
	private static final String KEY_NEW = "FeatureEditor.DataSection.new";
	private static final String POPUP_NEW = "Menus.new.label";
	private static final String POPUP_DELETE = "Actions.delete.label";
	private boolean updateNeeded;
	private PropertiesAction propertiesAction;
	private TableViewer dataViewer;
	private Action newAction;
	private Action openAction;
	private Action deleteAction;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				return ((IFeature) parent).getData();
			}
			return new Object[0];
		}
	}

	public DataSection(FeatureAdvancedPage page) {
		super(page, new String[] { PDEPlugin.getResourceString(KEY_NEW)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
		//setCollapsable(true);
		//IFeatureModel model = (IFeatureModel)page.getModel();
		//IFeature feature = model.getFeature();
		//setCollapsed(feature.getData().length==0);
	}

	public void commitChanges(boolean onSave) {
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 9;

		createViewerPartControl(container, SWT.MULTI, 2, factory);
		TablePart tablePart = getTablePart();
		dataViewer = tablePart.getTableViewer();
		dataViewer.setContentProvider(new PluginContentProvider());
		dataViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		factory.paintBordersFor(container);
		makeActions();
		return container;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		openAction.run();
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		super.dispose();
	}
	public void expandTo(Object object) {
		if (object instanceof IFeatureData) {
			dataViewer.setSelection(new StructuredSelection(object), true);
		}
	}
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(openAction);
		manager.add(new Separator());
		manager.add(newAction);
		manager.add(deleteAction);
		manager.add(new Separator());
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());
		manager.add(propertiesAction);
	}

	private void handleNew() {
		final IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IResource resource = model.getUnderlyingResource();
		final IContainer folder = resource.getParent();

		BusyIndicator.showWhile(dataViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				ResourceSelectionDialog dialog =
					new ResourceSelectionDialog(dataViewer.getTable().getShell(), folder, null);
				dialog.open();
				Object[] result = dialog.getResult();
				processNewResult(model, folder, result);
			}
		});
	}
	private void processNewResult(
		IFeatureModel model,
		IContainer folder,
		Object[] result) {
		if (result==null || result.length==0) return;
		IPath folderPath = folder.getProjectRelativePath();
		ArrayList entries = new ArrayList();
		for (int i = 0; i < result.length; i++) {
			Object item = result[i];
			if (item instanceof IFile) {
				IFile file = (IFile) item;
				IPath filePath = file.getProjectRelativePath();
				int matching = filePath.matchingFirstSegments(folderPath);
				IPath relativePath = filePath.removeFirstSegments(matching);
				entries.add(relativePath);
			}
		}
		if (entries.size() > 0) {
			try {
				IFeatureData[] array = new IFeatureData[entries.size()];
				for (int i = 0; i < array.length; i++) {
					IFeatureData data = model.getFactory().createData();
					IPath path = (IPath) entries.get(i);
					data.setId(path.toString());
					array[i] = data;
				}
				model.getFeature().addData(array);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void handleSelectAll() {
		IStructuredContentProvider provider =
			(IStructuredContentProvider) dataViewer.getContentProvider();
		Object[] elements = provider.getElements(dataViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		dataViewer.setSelection(ssel);
	}
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) dataViewer.getSelection();

		if (ssel.isEmpty())
			return;
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
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
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			BusyIndicator.showWhile(dataViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.SELECT_ALL)) {
			BusyIndicator.showWhile(dataViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleSelectAll();
				}
			});
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
	protected void selectionChanged(IStructuredSelection selection) {
		getFormPage().setSelection(selection);
	}
	public void initialize(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		update(input);
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		mng.addModelProviderListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
			if (getFormPage().isVisible()) {
				update();
			}
		} else {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeatureData && !(obj instanceof IFeaturePlugin)) {
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					dataViewer.update(obj, null);
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					dataViewer.add(e.getChangedObjects());
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					dataViewer.remove(e.getChangedObjects());
				}
			}
		}
	}
	private void makeActions() {
		IModel model = (IModel)getFormPage().getModel();
		newAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newAction.setText(PDEPlugin.getResourceString(POPUP_NEW));
		newAction.setEnabled(model.isEditable());

		deleteAction = new Action() {
			public void run() {
				BusyIndicator.showWhile(dataViewer.getTable().getDisplay(), new Runnable() {
					public void run() {
						handleDelete();
					}
				});
			}
		};
		deleteAction.setEnabled(model.isEditable());
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
		openAction = new OpenReferenceAction(dataViewer);
		propertiesAction = new PropertiesAction(getFormPage().getEditor());
	}

	public void modelsChanged(IModelProviderEvent event) {
		updateNeeded = true;
		update();
	}

	public void setFocus() {
		if (dataViewer != null)
			dataViewer.getTable().setFocus();
	}

	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}

	public void update(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		IFeature feature = model.getFeature();
		dataViewer.setInput(feature);
		updateNeeded = false;
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object, Object[])
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
		Clipboard clipboard = getFormPage().getEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object [] objects = (Object[])clipboard.getContents(modelTransfer);
		if (objects != null) {
			doPaste(null, objects);
		}
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object, Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();
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
}