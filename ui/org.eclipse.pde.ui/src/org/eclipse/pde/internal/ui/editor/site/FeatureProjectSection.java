package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.WorkspaceSiteBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.feature.OpenReferenceAction;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class FeatureProjectSection
	extends TableSection
	implements IModelProviderListener {
	private static final String SECTION_TITLE =
		"SiteEditor.FeatureProjectSection.title";
	private static final String SECTION_DESC =
		"SiteEditor.FeatureProjectSection.desc";
	private static final String KEY_NEW =
		"SiteEditor.FeatureProjectSection.new";
	private static final String POPUP_NEW = "Menus.new.label";
	private static final String POPUP_DELETE = "Actions.delete.label";
	private boolean updateNeeded;
	private TableViewer projectViewer;
	private Action newAction;
	private Action openAction;
	private Action deleteAction;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof ISiteBuild) {
				return ((ISiteBuild) parent).getFeatures();
			}
			return new Object[0];
		}
	}

	public FeatureProjectSection(BuildPage page) {
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
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (onSave
			&& buildModel instanceof WorkspaceSiteBuildModel
			&& ((WorkspaceSiteBuildModel) buildModel).isDirty()) {
			((WorkspaceSiteBuildModel) buildModel).save();
		}
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 9;

		createViewerPartControl(container, SWT.MULTI, 2, factory);
		TablePart tablePart = getTablePart();
		projectViewer = tablePart.getTableViewer();
		projectViewer.setContentProvider(new PluginContentProvider());
		projectViewer.setLabelProvider(
			PDEPlugin.getDefault().getLabelProvider());
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
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		model.removeModelChangedListener(this);
		if (buildModel!=null)
			buildModel.removeModelChangedListener(this);
		WorkspaceModelManager mng =
			PDECore.getDefault().getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		super.dispose();
	}

	public void expandTo(Object object) {
		if (object instanceof ISiteBuildFeature) {
			projectViewer.setSelection(new StructuredSelection(object), true);
		}
	}
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(openAction);
		manager.add(new Separator());
		manager.add(newAction);
		manager.add(deleteAction);
		manager.add(new Separator());
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
	}

	private void handleNew() {
		final ISiteModel model = (ISiteModel) getFormPage().getModel();
		final ISiteBuildModel buildModel = model.getBuildModel();

		BusyIndicator
			.showWhile(projectViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				BuiltFeaturesWizard wizard =
					new BuiltFeaturesWizard(buildModel);
				WizardDialog dialog =
					new WizardDialog(
						projectViewer.getControl().getShell(),
						wizard);
				dialog.open();
				forceDirty();
			}
		});
	}
	
	private void forceDirty() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (((IEditable) buildModel).isDirty()) {
			setDirty(true);
			((IEditable) model).setDirty(true);
			getFormPage().getEditor().fireSaveNeeded();
		}
	}

	private void handleSelectAll() {
		IStructuredContentProvider provider =
			(IStructuredContentProvider) projectViewer.getContentProvider();
		Object[] elements = provider.getElements(projectViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		projectViewer.setSelection(ssel);
	}

	private void handleDelete() {
		IStructuredSelection ssel =
			(IStructuredSelection) projectViewer.getSelection();

		if (ssel.isEmpty())
			return;
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		ISiteBuild build = buildModel.getSiteBuild();

		try {
			ISiteBuildFeature[] removed = new ISiteBuildFeature[ssel.size()];
			int i = 0;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				ISiteBuildFeature iobj = (ISiteBuildFeature) iter.next();
				removed[i++] = iobj;
			}
			build.removeFeatures(removed);
			forceDirty();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			BusyIndicator
				.showWhile(
					projectViewer.getTable().getDisplay(),
					new Runnable() {
				public void run() {
					handleDelete();
				}
			});
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.SELECT_ALL)) {
			BusyIndicator
				.showWhile(
					projectViewer.getTable().getDisplay(),
					new Runnable() {
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
		ISiteModel model = (ISiteModel) input;
		update(input);

		ISiteBuildModel buildModel = model.getBuildModel();
		getTablePart().setButtonEnabled(0, buildModel.isEditable());
		model.addModelChangedListener(this);
		buildModel.addModelChangedListener(this);
		WorkspaceModelManager mng =
			PDECore.getDefault().getWorkspaceModelManager();
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
			if (obj instanceof ISiteBuildFeature) {
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					projectViewer.update(obj, null);
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					projectViewer.add(e.getChangedObjects());
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					projectViewer.remove(e.getChangedObjects());
				}
			}
		}
	}

	private void makeActions() {
		IModel model = (IModel) getFormPage().getModel();
		newAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newAction.setText(PDEPlugin.getResourceString(POPUP_NEW));
		newAction.setEnabled(model.isEditable());

		deleteAction = new Action() {
			public void run() {
				BusyIndicator
					.showWhile(
						projectViewer.getTable().getDisplay(),
						new Runnable() {
					public void run() {
						handleDelete();
					}
				});
			}
		};
		deleteAction.setEnabled(model.isEditable());
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
		openAction = new OpenReferenceAction(projectViewer);
	}

	public void modelsChanged(IModelProviderEvent event) {
		updateNeeded = true;
		update();
	}

	public void setFocus() {
		if (projectViewer != null)
			projectViewer.getTable().setFocus();
	}

	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}

	public void update(Object input) {
		ISiteModel model = (ISiteModel) input;
		ISiteBuildModel buildModel = model.getBuildModel();
		ISiteBuild build = buildModel.getSiteBuild();
		build.resetReferences();
		projectViewer.setInput(build);
		updateNeeded = false;
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object, Object[])
	 */
	protected boolean canPaste(Object target, Object[] objects) {
		/*
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof FeaturePlugin || !(objects[i] instanceof FeatureData))
				return false;
		}
		*/
		return true;
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	protected void doPaste() {
		/*
		Clipboard clipboard = getFormPage().getEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object [] objects = (Object[])clipboard.getContents(modelTransfer);
		if (objects != null) {
			doPaste(null, objects);
		}
		*/
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object, Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
		/*
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
		*/
	}
}