/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.feature;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.Plugin;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.neweditor.TableSection;
import org.eclipse.pde.internal.ui.newparts.TablePart;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class RequiresSection
	extends TableSection
	implements IModelProviderListener {
	public static final int MULTI_SELECTION = 33;
	private static final String KEY_TITLE =
		"FeatureEditor.RequiresSection.title";
	private static final String KEY_DESC = "FeatureEditor.RequiresSection.desc";
	private static final String KEY_NEW_PLUGIN_BUTTON =
		"FeatureEditor.RequiresSection.newPluginButton";
	private static final String KEY_NEW_FEATURE_BUTTON =
		"FeatureEditor.RequiresSection.newFeatureButton";
	private static final String KEY_SYNC_BUTTON =
		"FeatureEditor.RequiresSection.syncButton";
	private static final String KEY_COMPUTE =
		"FeatureEditor.RequiresSection.compute";
	private static final String KEY_DELETE = "Actions.delete.label";
	private Button syncButton;
	private TableViewer pluginViewer;
	private Action deleteAction;

	class ImportContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature)
				return ((IFeature) parent).getImports();
			return new Object[0];
		}
	}

	public RequiresSection(FeatureReferencePage page, Composite parent) {
		super(
			page,
			parent,
			Section.DESCRIPTION,
			new String[] {
				PDEPlugin.getResourceString(KEY_NEW_PLUGIN_BUTTON),
				PDEPlugin.getResourceString(KEY_NEW_FEATURE_BUTTON),
				null,
				PDEPlugin.getResourceString(KEY_COMPUTE)});
		getSection().setText(PDEPlugin.getResourceString(KEY_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(KEY_DESC));
		getTablePart().setEditable(false);
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);

		syncButton =
			toolkit.createButton(
				container,
				PDEPlugin.getResourceString(KEY_SYNC_BUTTON),
				SWT.CHECK);
		//syncButton.setSelection(true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		syncButton.setLayoutData(gd);

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);

		TablePart tablePart = getTablePart();
		pluginViewer = tablePart.getTableViewer();
		pluginViewer.setContentProvider(new ImportContentProvider());
		pluginViewer.setSorter(ListUtil.NAME_SORTER);
		pluginViewer.setLabelProvider(
			PDEPlugin.getDefault().getLabelProvider());

		deleteAction = new Action() {
			public void run() {
				handleDelete();
			}
		};
		deleteAction.setText(PDEPlugin.getResourceString(KEY_DELETE));
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNewPlugin();
				break;
			case 1 :
				handleNewFeature();
				break;
			case 2 :
				break;
			case 3 :
				recomputeImports();
				break;
		}
	}

	private void handleNewPlugin() {
		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		BusyIndicator
			.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				NewFeatureRequireWizardPage page =
					new NewFeatureRequireWizardPage(model);
				ReferenceWizard wizard = new ReferenceWizard(model, page);
				WizardDialog dialog =
					new WizardDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				dialog.open();
			}
		});
	}

	private void handleNewFeature() {
		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		BusyIndicator
			.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				RequiredFeaturesWizard wizard =
					new RequiredFeaturesWizard(model);
				WizardDialog dialog =
					new WizardDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				dialog.open();
			}
		});
	}

	private void handleDelete() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IStructuredSelection selection =
			(IStructuredSelection) pluginViewer.getSelection();
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
		IStructuredContentProvider provider =
			(IStructuredContentProvider) pluginViewer.getContentProvider();
		Object[] elements = provider.getElements(pluginViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		pluginViewer.setSelection(ssel);
	}
	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		model.removeModelChangedListener(this);
		WorkspaceModelManager mng =
			PDECore.getDefault().getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator
				.showWhile(
					pluginViewer.getTable().getDisplay(),
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
			BusyIndicator
				.showWhile(
					pluginViewer.getTable().getDisplay(),
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
			pluginViewer.setSelection(ssel);
		}
	}
	protected void fillContextMenu(IMenuManager manager) {
		/*
		manager.add(openAction);
		manager.add(propertiesAction);
		manager.add(new Separator());
		*/
		IStructuredSelection selection =
			(StructuredSelection) pluginViewer.getSelection();
		if (!selection.isEmpty()) {
			manager.add(deleteAction);
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
			syncButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
		WorkspaceModelManager mng =
			PDECore.getDefault().getWorkspaceModelManager();
		mng.addModelProviderListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeatureImport) {
				pluginViewer.refresh(obj);
			}
		} else {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeatureImport) {
				if (e.getChangeType() == IModelChangedEvent.INSERT)
					pluginViewer.add(e.getChangedObjects());
				else
					pluginViewer.remove(e.getChangedObjects());
			} else if (obj instanceof IFeaturePlugin) {
				if (syncButton.getSelection()) {
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
		IModel [] added = event.getAddedModels();
		IModel [] removed = event.getRemovedModels();
		IModel [] changed = event.getChangedModels();
		if (hasPluginModels(added)||hasPluginModels(removed)||hasPluginModels(changed))		
			markStale();
	}
	private boolean hasPluginModels(IModel [] models) {
		if (models==null) return false;
		if (models.length==0) return false;
		for (int i=0; i<models.length; i++) {
			if (models[i] instanceof IPluginModelBase)
				return true;
		}
		return false;
	}

	public void setFocus() {
		if (pluginViewer != null)
			pluginViewer.getTable().setFocus();
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel)getPage().getModel();
		IFeature feature = model.getFeature();
		pluginViewer.setInput(feature);
		super.refresh();
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		Object[] objects =
			(Object[]) clipboard.getContents(ModelDataTransfer.getInstance());
		if (objects != null && objects.length > 0) {
			return canPaste(null, objects);
		}
		return false;
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object, Object[])
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
		Object[] objects =
			(Object[]) clipboard.getContents(ModelDataTransfer.getInstance());
		if (objects != null && canPaste(null, objects))
			doPaste(null, objects);
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object, Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

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
			fImport.setFeature(
				PDECore
					.getDefault()
					.findFeature(fImport.getId(), fImport.getVersion(), fImport.getMatch()));
		} else {
			Plugin plugin = (Plugin) fImport.getPlugin();
			if (plugin.getPluginBase() instanceof Fragment) {
				IFragmentModel[] fragments =
					PDECore
						.getDefault()
						.getWorkspaceModelManager()
						.getFragmentModels();
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
				plugin.setModel(
					PDECore
						.getDefault()
						.findPlugin(plugin.getId(), plugin.getVersion(), 0)
						.getModel());
			}
		}
	}
}
