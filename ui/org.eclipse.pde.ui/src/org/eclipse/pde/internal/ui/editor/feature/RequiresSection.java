/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.feature.FeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.plugin.Fragment;
import org.eclipse.pde.internal.core.plugin.Plugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class RequiresSection extends TableSection implements
		IModelProviderListener {
	public static final int MULTI_SELECTION = 33;

	private static final String KEY_TITLE = "FeatureEditor.RequiresSection.title"; //$NON-NLS-1$

	private static final String KEY_DESC = "FeatureEditor.RequiresSection.desc"; //$NON-NLS-1$

	private static final String KEY_NEW_PLUGIN_BUTTON = "FeatureEditor.RequiresSection.plugin"; //$NON-NLS-1$

	private static final String KEY_NEW_FEATURE_BUTTON = "FeatureEditor.RequiresSection.feature"; //$NON-NLS-1$

	private static final String KEY_SYNC_BUTTON = "FeatureEditor.RequiresSection.sync"; //$NON-NLS-1$

	private static final String KEY_COMPUTE = "FeatureEditor.RequiresSection.compute"; //$NON-NLS-1$

	private static final String KEY_DELETE = "Actions.delete.label"; //$NON-NLS-1$

	private Button fSyncButton;

	private TableViewer fPluginViewer;

	private Action fDeleteAction;

	class ImportContentProvider extends DefaultContentProvider implements
			IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature)
				return ((IFeature) parent).getImports();
			return new Object[0];
		}
	}

	public RequiresSection(FeatureDependenciesPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {
				PDEPlugin.getResourceString(KEY_NEW_PLUGIN_BUTTON),
				PDEPlugin.getResourceString(KEY_NEW_FEATURE_BUTTON), null,
				PDEPlugin.getResourceString(KEY_COMPUTE) });
		getSection().setText(PDEPlugin.getResourceString(KEY_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(KEY_DESC));
		getTablePart().setEditable(false);
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);

		fSyncButton = toolkit.createButton(container, PDEPlugin
				.getResourceString(KEY_SYNC_BUTTON), SWT.CHECK);
		// syncButton.setSelection(true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		fSyncButton.setLayoutData(gd);

		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);

		TablePart tablePart = getTablePart();
		fPluginViewer = tablePart.getTableViewer();
		fPluginViewer.setContentProvider(new ImportContentProvider());
		fPluginViewer.setSorter(ListUtil.NAME_SORTER);
		fPluginViewer.setLabelProvider(PDEPlugin.getDefault()
				.getLabelProvider());

		fDeleteAction = new Action() {
			public void run() {
				handleDelete();
			}
		};
		fDeleteAction.setText(PDEPlugin.getResourceString(KEY_DELETE));
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	protected void buttonSelected(int index) {
		switch (index) {
		case 0:
			handleNewPlugin();
			break;
		case 1:
			handleNewFeature();
			break;
		case 2:
			break;
		case 3:
			recomputeImports();
			break;
		}
	}

	private void handleNewPlugin() {
		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(),
				new Runnable() {
					public void run() {
						NewFeatureRequireWizardPage page = new NewFeatureRequireWizardPage(
								model);
						ReferenceWizard wizard = new ReferenceWizard(model,
								page);
						WizardDialog dialog = new WizardDialog(PDEPlugin
								.getActiveWorkbenchShell(), wizard);
						dialog.create();
						dialog.open();
					}
				});
	}

	private void handleNewFeature() {
		BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(),
				new Runnable() {
					public void run() {
						IFeatureModel[] allModels = PDECore.getDefault()
								.getFeatureModelManager().getAllFeatures();
						ArrayList newModels = new ArrayList();
						for (int i = 0; i < allModels.length; i++) {
							if (canAdd(allModels[i]))
								newModels.add(allModels[i]);
						}
						IFeatureModel[] candidateModels = (IFeatureModel[]) newModels
								.toArray(new IFeatureModel[newModels.size()]);
						FeatureSelectionDialog dialog = new FeatureSelectionDialog(
								fPluginViewer.getTable().getShell(),
								candidateModels, true);
						if (dialog.open() == Window.OK) {
							Object[] models = dialog.getResult();
							try {
								doAdd(models);
							} catch (CoreException e) {
								PDECore.log(e);
							}
						}
					}
				});
	}

	private void doAdd(Object[] candidates) throws CoreException {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IFeatureImport[] added = new IFeatureImport[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IFeatureModel candidate = (IFeatureModel) candidates[i];
			FeatureImport iimport = (FeatureImport) model.getFactory().createImport();
			iimport.loadFrom(candidate.getFeature());
			added[i] = iimport;
		}
		feature.addImports(added);
	}

	private boolean canAdd(IFeatureModel candidate) {
		IFeature cfeature = candidate.getFeature();

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

		if (cfeature.getId().equals(feature.getId())
				&& cfeature.getVersion().equals(feature.getVersion())) {
			return false;
		}

		IFeatureImport[] features = feature.getImports();

		for (int i = 0; i < features.length; i++) {
			if (features[i].getId().equals(cfeature.getId())
					&& features[i].getVersion().equals(cfeature.getVersion()))
				return false;
		}
		return true;
	}

	private void handleDelete() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();
		IStructuredSelection selection = (IStructuredSelection) fPluginViewer
				.getSelection();
		if (selection.isEmpty())
			return;

		try {
			IFeatureImport[] deleted = new IFeatureImport[selection.size()];
			int i = 0;
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				IFeatureImport iimport = (IFeatureImport) iter.next();
				deleted[i++] = iimport;
			}
			feature.removeImports(deleted);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleSelectAll() {
		IStructuredContentProvider provider = (IStructuredContentProvider) fPluginViewer
				.getContentProvider();
		Object[] elements = provider.getElements(fPluginViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		fPluginViewer.setSelection(ssel);
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		WorkspaceModelManager mng = PDECore.getDefault()
				.getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(),
					new Runnable() {
						public void run() {
							handleDelete();
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
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(),
					new Runnable() {
						public void run() {
							handleSelectAll();
						}
					});
			return true;
		}
		return false;
	}

	public void expandTo(Object object) {
		if (object instanceof IFeatureImport) {
			StructuredSelection ssel = new StructuredSelection(object);
			fPluginViewer.setSelection(ssel);
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (StructuredSelection) fPluginViewer
				.getSelection();
		if (!selection.isEmpty()) {
			manager.add(fDeleteAction);
			manager.add(new Separator());
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		getPage().getManagedForm().fireSelectionChanged(this, selection);
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		if (model.isEditable() == false) {
			getTablePart().setButtonEnabled(0, false);
			getTablePart().setButtonEnabled(1, false);
			getTablePart().setButtonEnabled(3, false);
			fSyncButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
		WorkspaceModelManager mng = PDECore.getDefault()
				.getWorkspaceModelManager();
		mng.addModelProviderListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeatureImport) {
				fPluginViewer.refresh(obj);
			}
		} else {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeatureImport) {
				if (e.getChangeType() == IModelChangedEvent.INSERT) {
					fPluginViewer.add(e.getChangedObjects());
					if (e.getChangedObjects().length > 0) {
						fPluginViewer.setSelection(new StructuredSelection(e
								.getChangedObjects()[0]));
					}
				} else
					fPluginViewer.remove(e.getChangedObjects());
			} else if (obj instanceof IFeaturePlugin) {
				if (fSyncButton.getSelection()) {
					recomputeImports();
				}
			}
		}
	}

	private void recomputeImports() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		try {
			feature.computeImports();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void modelsChanged(IModelProviderEvent event) {
		IModel[] added = event.getAddedModels();
		IModel[] removed = event.getRemovedModels();
		IModel[] changed = event.getChangedModels();
		if (hasPluginModels(added) || hasPluginModels(removed)
				|| hasPluginModels(changed))
			markStale();
	}

	private boolean hasPluginModels(IModel[] models) {
		if (models == null)
			return false;
		if (models.length == 0)
			return false;
		for (int i = 0; i < models.length; i++) {
			if (models[i] instanceof IPluginModelBase)
				return true;
		}
		return false;
	}

	public void setFocus() {
		if (fPluginViewer != null)
			fPluginViewer.getTable().setFocus();
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fPluginViewer.setInput(feature);
		super.refresh();
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		Object[] objects = (Object[]) clipboard.getContents(ModelDataTransfer
				.getInstance());
		if (objects != null && objects.length > 0) {
			return canPaste(null, objects);
		}
		return false;
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object,
	 *      Object[])
	 */
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof FeatureImport))
				return false;
		}
		return true;
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	protected void doPaste() {
		Clipboard clipboard = getPage().getPDEEditor().getClipboard();
		Object[] objects = (Object[]) clipboard.getContents(ModelDataTransfer
				.getInstance());
		if (objects != null && canPaste(null, objects))
			doPaste(null, objects);
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

		IFeatureImport[] imports = new IFeatureImport[objects.length];
		try {
			for (int i = 0; i < objects.length; i++) {
				FeatureImport fImport = (FeatureImport) objects[i];
				fImport.setModel(model);
				fImport.setParent(feature);
				reconnectReference(fImport);
				imports[i] = fImport;
			}
			feature.addImports(imports);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}

	private void reconnectReference(FeatureImport fImport) {
		if (fImport.getType() == IFeatureImport.FEATURE) {
			fImport.setFeature(PDECore.getDefault().findFeature(
					fImport.getId(), fImport.getVersion(), fImport.getMatch()));
		} else {
			Plugin plugin = (Plugin) fImport.getPlugin();
			if (plugin.getPluginBase() instanceof Fragment) {
				IFragmentModel[] fragments = PDECore.getDefault()
						.getWorkspaceModelManager().getFragmentModels();
				for (int i = 0; i < fragments.length; i++) {
					IFragment fragment = fragments[i].getFragment();
					if (fragment.getId().equals(plugin.getId())) {
						if (plugin.getVersion() == null
								|| fragment.getVersion().equals(
										plugin.getVersion())) {
							plugin.setModel(fragment.getModel());
							return;
						}
					}
				}
			} else {
				plugin.setModel(PDECore.getDefault().findPlugin(plugin.getId(),
						plugin.getVersion(), 0).getModel());
			}
		}
	}

	void fireSelection() {
		ISelection sel = fPluginViewer.getSelection();
		if (!sel.isEmpty()) {
			fPluginViewer.setSelection(fPluginViewer.getSelection());
		} else if (fPluginViewer.getElementAt(0) != null) {
			fPluginViewer.setSelection(new StructuredSelection(fPluginViewer
					.getElementAt(0)));
		}
	}
}
