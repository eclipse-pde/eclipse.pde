package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.FeatureImport;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.plugin.Fragment;
import org.eclipse.pde.internal.core.plugin.Plugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class RequiresSection
	extends TableSection
	implements IModelProviderListener {
		public static final int MULTI_SELECTION = 33;
	private static final String KEY_TITLE =
		"FeatureEditor.RequiresSection.title";
	private static final String KEY_DESC = "FeatureEditor.RequiresSection.desc";
	private static final String KEY_NEW_BUTTON =
		"FeatureEditor.RequiresSection.newButton";
	private static final String KEY_SYNC_BUTTON =
		"FeatureEditor.RequiresSection.syncButton";
	private static final String KEY_COMPUTE =
		"FeatureEditor.RequiresSection.compute";
	private static final String KEY_DELETE = 
		"Actions.delete.label";
	private boolean updateNeeded;
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

	public RequiresSection(FeatureReferencePage page) {
		super(
			page,
			new String[] {
				PDEPlugin.getResourceString(KEY_NEW_BUTTON),
				PDEPlugin.getResourceString(KEY_COMPUTE)});
		setHeaderText(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		getTablePart().setEditable(false);
	}

	public void commitChanges(boolean onSave) {
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);

		syncButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(KEY_SYNC_BUTTON),
				SWT.CHECK);
		//syncButton.setSelection(true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		syncButton.setLayoutData(gd);

		createViewerPartControl(container, SWT.MULTI, 2, factory);

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
		factory.paintBordersFor(container);
		return container;
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
		else if (index == 1)
			recomputeImports();
	}

	private void handleNew() {
		final IFeatureModel model = (IFeatureModel) getFormPage().getModel();
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

	private void handleDelete() {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();
		IStructuredSelection selection =
			(IStructuredSelection) pluginViewer.getSelection();
		if (selection.isEmpty())
			return;

		try {
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				IFeatureImport iimport = (IFeatureImport) iter.next();
				feature.removeImport(iimport);
			}
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
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		WorkspaceModelManager mng =
			PDECore.getDefault().getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			BusyIndicator.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
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
		if (actionId.equals(IWorkbenchActionConstants.SELECT_ALL)) {
			BusyIndicator.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
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
		IStructuredSelection selection = (StructuredSelection)pluginViewer.getSelection();
		if (!selection.isEmpty()) {
			manager.add(deleteAction);
			manager.add(new Separator());
		}
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		//IFeatureImport iimport = (IFeatureImport) selection.getFirstElement();
		getFormPage().setSelection(selection);
		/*
		if (iimport != null)
			fireSelectionNotification(iimport);
		*/
		fireChangeNotification(MULTI_SELECTION, selection);
	}
	public void initialize(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		update(input);
		if (model.isEditable() == false) {
			getTablePart().setButtonEnabled(0, false);
			syncButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
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
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();
		try {
			feature.computeImports();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void modelsChanged(IModelProviderEvent event) {
		updateNeeded = true;
		update();
	}

	public void setFocus() {
		if (pluginViewer != null)
			pluginViewer.getTable().setFocus();
	}

	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}

	public void update(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		IFeature feature = model.getFeature();
		pluginViewer.setInput(feature);
		updateNeeded = false;
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		Object [] objects = (Object[])clipboard.getContents(ModelDataTransfer.getInstance());
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
		Clipboard clipboard = getFormPage().getEditor().getClipboard();
		Object [] objects = (Object[])clipboard.getContents(ModelDataTransfer.getInstance());
		if (objects != null && canPaste(null,objects))
			doPaste(null, objects);
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object, Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();
		try {
			for (int i = 0; i < objects.length; i++) {
				FeatureImport fImport = (FeatureImport) objects[i];
				fImport.setModel(model);
				fImport.setParent(feature);
				setPluginModel(fImport);
				feature.addImport(fImport);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}
	
	private void setPluginModel(FeatureImport fImport) {
		Plugin plugin = (Plugin)fImport.getPlugin();
		if (plugin.getPluginBase() instanceof Fragment) {
			IFragmentModel[] fragments =
				PDECore.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
			for (int i = 0; i < fragments.length; i++) {
				IFragment fragment = fragments[i].getFragment();
				if (fragment.getId().equals(plugin.getId())) {
					if (plugin.getVersion() == null || fragment.getVersion().equals(plugin.getVersion())) {
						plugin.setModel(fragment.getModel());
						return;
					}
				}
			}
		} else {
			plugin.setModel(PDECore.getDefault().findPlugin(plugin.getId(), plugin.getVersion(), 0).getModel());
		}
	}
	
}