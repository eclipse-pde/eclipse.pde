package org.eclipse.pde.internal.ui.editor.site;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.IModelProviderListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.isite.ISiteBuild;
import org.eclipse.pde.internal.core.isite.ISiteBuildFeature;
import org.eclipse.pde.internal.core.isite.ISiteBuildModel;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.site.WorkspaceSiteBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.CheckboxTableSection;
import org.eclipse.pde.internal.ui.editor.feature.OpenReferenceAction;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.CheckboxTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class FeatureProjectSection
	extends CheckboxTableSection
	implements IModelProviderListener {
	private static final String SECTION_TITLE =
		"SiteEditor.FeatureProjectSection.title";
	private static final String SECTION_DESC =
		"SiteEditor.FeatureProjectSection.desc";
	private static final String KEY_NEW =
		"SiteEditor.FeatureProjectSection.new";
	private static final String KEY_SELECT_ALL =
		"SiteEditor.FeatureProjectSection.selectAll";
	private static final String KEY_DESELECT_ALL =
		"SiteEditor.FeatureProjectSection.deselectAll";
	private static final String POPUP_NEW = "Menus.new.label";
	private static final String POPUP_DELETE = "Actions.delete.label";
	private boolean updateNeeded;
	private CheckboxTableViewer projectViewer;
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
		super(
			page,
			new String[] {
				PDEPlugin.getResourceString(KEY_NEW),
				null,
				PDEPlugin.getResourceString(KEY_SELECT_ALL),
				PDEPlugin.getResourceString(KEY_DESELECT_ALL)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
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
		CheckboxTablePart tablePart = getTablePart();
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
		switch (index) {
			case 0 :
				handleNew();
				break;
			case 2 :
				handleSelectAll(true);
				break;
			case 3 :
				handleSelectAll(false);
				break;
		}
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		model.removeModelChangedListener(this);
		if (buildModel != null)
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

		if (model.isEditable() == false) {
			projectViewer.setAllGrayed(true);
			getTablePart().setButtonEnabled(2, false);
			getTablePart().setButtonEnabled(3, false);
		}

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
		updateChecks(model, buildModel);
		updateNeeded = false;
	}

	protected void elementChecked(Object element, boolean checked) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		if (model.isEditable() == false) {
			projectViewer.setChecked(element, !checked);
			return;
		}
		ISiteBuildFeature selection = (ISiteBuildFeature) element;
		updateSiteFeature(selection, checked);
	}

	protected void handleSelectAll(boolean selected) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		projectViewer.setAllChecked(selected);
		ISiteBuildFeature[] sbfeatures =
			buildModel.getSiteBuild().getFeatures();
		ArrayList result = new ArrayList();

		try {
			for (int i = 0; i < sbfeatures.length; i++) {
				ISiteBuildFeature sbfeature = sbfeatures[i];
				ISiteFeature sfeature = findMatchingSiteFeature(sbfeature);
				if (selected && sfeature == null) {
					// add
					sfeature = createSiteFeature(model, sbfeature);
					result.add(sfeature);
				} else if (!selected && sfeature != null) {
					result.add(sfeature);
				}
			}
			if (result.size() > 0) {
				ISiteFeature[] array =
					(ISiteFeature[]) result.toArray(
						new ISiteFeature[result.size()]);
				if (selected)
					model.getSite().addFeatures(array);
				else
					model.getSite().removeFeatures(array);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private ISiteFeature createSiteFeature(
		ISiteModel model,
		ISiteBuildFeature sbfeature)
		throws CoreException {
		ISiteFeature sfeature = model.getFactory().createFeature();
		sfeature.setId(sbfeature.getId());
		sfeature.setVersion(sbfeature.getVersion());
		sfeature.setURL(sbfeature.getTargetURL());
		return sfeature;
	}

	private void updateSiteFeature(ISiteBuildFeature sbfeature, boolean add) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISite site = model.getSite();
		ISiteFeature sfeature = findMatchingSiteFeature(sbfeature);
		if (add) {
			// make sure it exists - add if needed
			if (sfeature == null) {
				try {
					sfeature = createSiteFeature(model, sbfeature);
					site.addFeatures(new ISiteFeature[] { sfeature });
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		} else {
			// make sure it does not exist - remove if needed
			if (sfeature != null) {
				try {
					site.removeFeatures(new ISiteFeature[] { sfeature });
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	private ISiteFeature findMatchingSiteFeature(ISiteBuildFeature sbfeature) {
		ISiteModel model = (ISiteModel) getFormPage().getModel();
		ISiteFeature[] sfeatures = model.getSite().getFeatures();
		return findMatchingSiteFeature(sbfeature, sfeatures);
	}

	private ISiteFeature findMatchingSiteFeature(
		ISiteBuildFeature sbfeature,
		ISiteFeature[] sfeatures) {
		for (int j = 0; j < sfeatures.length; j++) {
			ISiteFeature sfeature = sfeatures[j];
			if (matches(sfeature, sbfeature))
				return sfeature;
		}
		return null;
	}

	private void updateChecks(ISiteModel model, ISiteBuildModel buildModel) {
		ISiteBuildFeature[] sbfeatures =
			buildModel.getSiteBuild().getFeatures();
		ISiteFeature[] sfeatures = model.getSite().getFeatures();
		ArrayList checked = new ArrayList();
		for (int i = 0; i < sbfeatures.length; i++) {
			ISiteBuildFeature sbfeature = sbfeatures[i];
			ISiteFeature sfeature =
				findMatchingSiteFeature(sbfeature, sfeatures);
			if (sfeature != null)
				checked.add(sbfeature);
		}
		projectViewer.setCheckedElements(checked.toArray());
	}

	private boolean matches(
		ISiteFeature sfeature,
		ISiteBuildFeature sbfeature) {
		String targetURL = sbfeature.getTargetURL();
		String url = sfeature.getURL();
		return (
			url != null
				&& targetURL != null
				&& url.equalsIgnoreCase(targetURL));
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