/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.IFeatureModelDelta;
import org.eclipse.pde.internal.core.IFeatureModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class IncludedFeaturesSection extends TableSection implements
		IFeatureModelListener {
	private static final String SECTION_TITLE = "FeatureEditor.IncludedFeatures.title"; //$NON-NLS-1$

	private static final String SECTION_DESC = "FeatureEditor.IncludedFeatures.desc"; //$NON-NLS-1$

	private static final String KEY_NEW = "FeatureEditor.IncludedFeatures.new"; //$NON-NLS-1$

	private static final String POPUP_NEW = "Menus.new.label"; //$NON-NLS-1$

	private static final String POPUP_DELETE = "Actions.delete.label"; //$NON-NLS-1$

	private TableViewer fIncludesViewer;

	private Action fNewAction;

	private Action fOpenAction;

	private Action fDeleteAction;

	class IncludedFeaturesContentProvider extends DefaultContentProvider
			implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				return ((IFeature) parent).getIncludedFeatures();
			}
			return new Object[0];
		}
	}

	public IncludedFeaturesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] { PDEPlugin
				.getResourceString(KEY_NEW) });
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
		getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.verticalSpacing = 5;

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fIncludesViewer = tablePart.getTableViewer();
		fIncludesViewer
				.setContentProvider(new IncludedFeaturesContentProvider());
		fIncludesViewer.setLabelProvider(PDEPlugin.getDefault()
				.getLabelProvider());
		fIncludesViewer.setSorter(ListUtil.NAME_SORTER);
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
		FeatureModelManager mng = PDECore.getDefault()
		.getFeatureModelManager();
		mng.removeFeatureModelListener(this);
		super.dispose();
	}

	public boolean setFormInput(Object object) {
		if (object instanceof IFeatureChild) {
			fIncludesViewer.setSelection(new StructuredSelection(object), true);
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
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
	}

	private void handleNew() {
		BusyIndicator.showWhile(fIncludesViewer.getTable().getDisplay(),
				new Runnable() {
					public void run() {
						IFeatureModel[] allModels = PDECore.getDefault()
								.getFeatureModelManager().getModels();
						ArrayList newModels = new ArrayList();
						for (int i = 0; i < allModels.length; i++) {
							if (canAdd(allModels[i]))
								newModels.add(allModels[i]);
						}
						IFeatureModel[] candidateModels = (IFeatureModel[]) newModels
								.toArray(new IFeatureModel[newModels.size()]);
						FeatureSelectionDialog dialog = new FeatureSelectionDialog(
								fIncludesViewer.getTable().getShell(),
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
		IFeatureChild[] added = new IFeatureChild[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IFeatureModel candidate = (IFeatureModel) candidates[i];
			FeatureChild child = (FeatureChild) model.getFactory()
					.createChild();
			child.loadFrom(candidate.getFeature());
			added[i] = child;
		}
		feature.addIncludedFeatures(added);
	}

	private boolean canAdd(IFeatureModel candidate) {
		IFeature cfeature = candidate.getFeature();

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

		if (cfeature.getId().equals(feature.getId())
				&& cfeature.getVersion().equals(feature.getVersion())) {
			return false;
		}

		boolean isPatchEditor = ((FeatureEditor) getPage().getEditor())
		.isPatchEditor();
		if (isPatchEditor && !isFeaturePatch(candidate.getFeature())) {
			return false;
		}

		IFeatureChild[] features = feature.getIncludedFeatures();

		for (int i = 0; i < features.length; i++) {
			if (features[i].getId().equals(cfeature.getId())
					&& features[i].getVersion().equals(cfeature.getVersion()))
				return false;
		}
		return true;
	}

	private static boolean isFeaturePatch(IFeature feature) {
		IFeatureImport[] imports = feature.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isPatch())
				return true;
		}
		return false;
	}

	private void handleSelectAll() {
		IStructuredContentProvider provider = (IStructuredContentProvider) fIncludesViewer
				.getContentProvider();
		Object[] elements = provider.getElements(fIncludesViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		fIncludesViewer.setSelection(ssel);
	}

	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) fIncludesViewer
				.getSelection();

		if (ssel.isEmpty())
			return;
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();

		try {
			IFeatureChild[] removed = new IFeatureChild[ssel.size()];
			int i = 0;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				IFeatureChild iobj = (IFeatureChild) iter.next();
				removed[i++] = iobj;
			}
			feature.removeIncludedFeatures(removed);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator.showWhile(fIncludesViewer.getTable().getDisplay(),
					new Runnable() {
						public void run() {
							handleDelete();
						}
					});
			return true;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			BusyIndicator.showWhile(fIncludesViewer.getTable().getDisplay(),
					new Runnable() {
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
		FeatureModelManager mng = PDECore.getDefault()
				.getFeatureModelManager();
		mng.addFeatureModelListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof IFeatureChild) {
			if (e.getChangeType() == IModelChangedEvent.CHANGE) {
				fIncludesViewer.update(obj, null);
			} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
				fIncludesViewer.add(e.getChangedObjects());
				if (e.getChangedObjects().length > 0) {
					fIncludesViewer.setSelection(new StructuredSelection(e
							.getChangedObjects()[0]));
				}
			} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
				fIncludesViewer.remove(e.getChangedObjects());
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
		fNewAction.setText(PDEPlugin.getResourceString(POPUP_NEW));
		fNewAction.setEnabled(model.isEditable());

		fDeleteAction = new Action() {
			public void run() {
				BusyIndicator.showWhile(
						fIncludesViewer.getTable().getDisplay(),
						new Runnable() {
							public void run() {
								handleDelete();
							}
						});
			}
		};
		fDeleteAction.setEnabled(model.isEditable());
		fDeleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
		fOpenAction = new OpenReferenceAction(fIncludesViewer);
	}

	public void modelsChanged(final IFeatureModelDelta delta) {
		getSection().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (getSection().isDisposed()) {
					return;
				}
				IFeatureModel[] added = delta.getAdded();
				IFeatureModel[] removed = delta.getRemoved();
				IFeatureModel[] changed = delta.getChanged();
				if (hasModels(added) || hasModels(removed)
						|| hasModels(changed))
					markStale();
			}
		});
	}

	private boolean hasModels(IFeatureModel[] models) {
		if (models == null)
			return false;
		IFeatureModel thisModel = (IFeatureModel) getPage().getModel();
		if (thisModel == null)
			return false;
		for (int i = 0; i < models.length; i++) {
			if (models[i] != thisModel) {
				return true;
			}
		}
		return false;
	}

	public void setFocus() {
		if (fIncludesViewer != null)
			fIncludesViewer.getTable().setFocus();
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fIncludesViewer.setInput(feature);
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
			if (!(objects[i] instanceof FeatureChild))
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

		FeatureChild[] fChildren = new FeatureChild[objects.length];
		try {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof FeatureChild) {
					FeatureChild fChild = (FeatureChild) objects[i];
					fChild.setModel(model);
					fChild.setParent(feature);
					fChildren[i] = fChild;
				}
			}
			feature.addIncludedFeatures(fChildren);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	void fireSelection() {
		ISelection sel = fIncludesViewer.getSelection();
		if (!sel.isEmpty()) {
			fIncludesViewer.setSelection(fIncludesViewer.getSelection());
		} else if (fIncludesViewer.getElementAt(0) != null) {
			fIncludesViewer.setSelection(new StructuredSelection(
					fIncludesViewer.getElementAt(0)));
		}
	}
}
