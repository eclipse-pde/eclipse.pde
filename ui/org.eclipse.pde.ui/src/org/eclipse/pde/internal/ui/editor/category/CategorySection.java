/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Version;

public class CategorySection extends TreeSection implements IFeatureModelListener {

	private static final int BUTTON_ADD_CATEGORY = 0;
	private static final int BUTTON_ADD_FEATURE = 1;
	private static int fCounter;
	private ISiteModel fModel;
	private TreePart fCategoryTreePart;
	private TreeViewer fCategoryViewer;
	private LabelProvider fSiteLabelProvider;

	class CategoryContentProvider extends DefaultContentProvider implements ITreeContentProvider {
		public Object[] getElements(Object inputElement) {
			// model = (ISite) inputElement;
			ArrayList result = new ArrayList();
			ISiteCategoryDefinition[] catDefs = fModel.getSite().getCategoryDefinitions();
			for (int i = 0; i < catDefs.length; i++) {
				result.add(catDefs[i]);
			}
			ISiteFeature[] features = fModel.getSite().getFeatures();
			for (int i = 0; i < features.length; i++) {
				if (features[i].getCategories().length == 0)
					result.add(new SiteFeatureAdapter(null, features[i]));
			}
			return result.toArray();
		}

		public Object[] getChildren(Object parent) {
			if (parent instanceof ISiteCategoryDefinition) {
				ISiteCategoryDefinition catDef = (ISiteCategoryDefinition) parent;
				ISiteFeature[] features = fModel.getSite().getFeatures();
				HashSet result = new HashSet();
				for (int i = 0; i < features.length; i++) {
					ISiteCategory[] cats = features[i].getCategories();
					for (int j = 0; j < cats.length; j++) {
						if (cats[j].getDefinition() != null && cats[j].getDefinition().equals(catDef)) {
							result.add(new SiteFeatureAdapter(cats[j].getName(), features[i]));
						}
					}
				}
				return result.toArray();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof ISiteCategoryDefinition) {
				ISiteCategoryDefinition catDef = (ISiteCategoryDefinition) element;
				ISiteFeature[] features = fModel.getSite().getFeatures();
				for (int i = 0; i < features.length; i++) {
					ISiteCategory[] cats = features[i].getCategories();
					for (int j = 0; j < cats.length; j++) {
						if (cats[j].getDefinition() != null && cats[j].getDefinition().equals(catDef)) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	public CategorySection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.CategoryDefinitionCategorySection_new, PDEUIMessages.CategorySection_add});
		getSection().setText(PDEUIMessages.CategoryDefinitionCategorySection_title);
		getSection().setDescription(PDEUIMessages.CategoryDefinitionCategorySection_desc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.update.ui.forms.internal.FormSection#createClient(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.update.ui.forms.internal.FormWidgetFactory)
	 */
	public void createClient(Section section, FormToolkit toolkit) {
		fModel = (ISiteModel) getPage().getModel();
		fModel.addModelChangedListener(this);

		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fCategoryTreePart = getTreePart();
		fCategoryViewer = fCategoryTreePart.getTreeViewer();
		fCategoryViewer.setContentProvider(new CategoryContentProvider());
		fSiteLabelProvider = new CategoryLabelProvider();
		fCategoryViewer.setLabelProvider(fSiteLabelProvider);

		fCategoryViewer.setInput(fModel.getSite());
		int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT;
		Transfer[] transfers = new Transfer[] {ModelDataTransfer.getInstance()};
		if (isEditable()) {
			fCategoryViewer.addDropSupport(ops, transfers, new ViewerDropAdapter(fCategoryViewer) {
				public void dragEnter(DropTargetEvent event) {
					Object target = determineTarget(event);
					if (target == null && event.detail == DND.DROP_COPY) {
						event.detail = DND.DROP_MOVE;
					}
					super.dragEnter(event);
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.jface.viewers.ViewerDropAdapter#dragOperationChanged(org.eclipse.swt.dnd.DropTargetEvent)
				 */
				public void dragOperationChanged(DropTargetEvent event) {
					Object target = determineTarget(event);
					if (target == null && event.detail == DND.DROP_COPY) {
						event.detail = DND.DROP_MOVE;
					}
					super.dragOperationChanged(event);
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.jface.viewers.ViewerDropAdapter#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
				 */
				public void dragOver(DropTargetEvent event) {
					Object target = determineTarget(event);
					if (target == null && event.detail == DND.DROP_COPY) {
						event.detail = DND.DROP_MOVE;
					}
					super.dragOver(event);
				}

				/**
				 * Returns the position of the given event's coordinates
				 * relative to its target. The position is determined to
				 * be before, after, or on the item, based on some
				 * threshold value.
				 * 
				 * @param event
				 *            the event
				 * @return one of the <code>LOCATION_* </code>
				 *         constants defined in this class
				 */
				protected int determineLocation(DropTargetEvent event) {
					if (!(event.item instanceof Item)) {
						return LOCATION_NONE;
					}
					Item item = (Item) event.item;
					Point coordinates = new Point(event.x, event.y);
					coordinates = getViewer().getControl().toControl(coordinates);
					if (item != null) {
						Rectangle bounds = getBounds(item);
						if (bounds == null) {
							return LOCATION_NONE;
						}
					}
					return LOCATION_ON;
				}

				public boolean performDrop(Object data) {
					if (!(data instanceof Object[]))
						return false;
					Object target = getCurrentTarget();

					int op = getCurrentOperation();
					Object[] objects = (Object[]) data;
					if (objects.length > 0 && objects[0] instanceof SiteFeatureAdapter) {
						if (op == DND.DROP_COPY && target != null) {
							copyFeature((SiteFeatureAdapter) objects[0], target);
						} else {
							moveFeature((SiteFeatureAdapter) objects[0], target);
						}
						return true;
					}
					return false;
				}

				public boolean validateDrop(Object target, int operation, TransferData transferType) {
					return (target instanceof ISiteCategoryDefinition || target == null);
				}

			});
		}

		fCategoryViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, transfers, new DragSourceListener() {
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection ssel = (IStructuredSelection) fCategoryViewer.getSelection();
				if (ssel == null || ssel.isEmpty() || !(ssel.getFirstElement() instanceof SiteFeatureAdapter)) {
					event.doit = false;
				}
			}

			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection ssel = (IStructuredSelection) fCategoryViewer.getSelection();
				event.data = ssel.toArray();
			}

			public void dragFinished(DragSourceEvent event) {
			}
		});

		fCategoryTreePart.setButtonEnabled(BUTTON_ADD_CATEGORY, isEditable());
		fCategoryTreePart.setButtonEnabled(BUTTON_ADD_FEATURE, isEditable());

		// fCategoryViewer.expandAll();
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private boolean categoryExists(String name) {
		ISiteCategoryDefinition[] defs = fModel.getSite().getCategoryDefinitions();
		for (int i = 0; i < defs.length; i++) {
			ISiteCategoryDefinition def = defs[i];
			String dname = def.getName();
			if (dname != null && dname.equals(name))
				return true;
		}
		return false;
	}

	private void copyFeature(SiteFeatureAdapter adapter, Object target) {
		ISiteFeature feature = findRealFeature(adapter);
		if (feature == null) {
			return;
		}
		/*
		 * if (adapter.category == null) { moveFeature(adapter, target); } else
		 */if (target != null && target instanceof ISiteCategoryDefinition) {
			addCategory(feature, ((ISiteCategoryDefinition) target).getName());
		}
	}

	private void addCategory(ISiteFeature aFeature, String catName) {
		try {
			if (aFeature == null)
				return;
			ISiteCategory[] cats = aFeature.getCategories();
			for (int j = 0; j < cats.length; j++) {
				if (cats[j].getName().equals(catName))
					return;
			}
			ISiteCategory cat = fModel.getFactory().createCategory(aFeature);
			cat.setName(catName);
			expandCategory(catName);
			aFeature.addCategories(new ISiteCategory[] {cat});
		} catch (CoreException e) {
		}
	}

	private void moveFeature(SiteFeatureAdapter adapter, Object target) {
		ISiteFeature feature = findRealFeature(adapter);
		if (feature == null) {
			return;
		}
		if (adapter.category != null) {
			removeCategory(feature, adapter.category);
		}
		if (target != null && target instanceof ISiteCategoryDefinition) {
			addCategory(feature, ((ISiteCategoryDefinition) target).getName());
		}
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case BUTTON_ADD_CATEGORY :
				handleAddCategoryDefinition();
				break;
			case BUTTON_ADD_FEATURE :
				handleNewFeature();
				break;
		}
	}

	protected void handleDoubleClick(IStructuredSelection ssel) {
		super.handleDoubleClick(ssel);
		Object selected = ssel.getFirstElement();
		if (selected instanceof SiteFeatureAdapter) {
			IFeature feature = findFeature(((SiteFeatureAdapter) selected).feature);
			FeatureEditor.openFeatureEditor(feature);
		}
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}

	private void handleAddCategoryDefinition() {
		String name = NLS.bind(PDEUIMessages.CategorySection_newCategoryName, Integer.toString(++fCounter));
		while (categoryExists(name)) {
			name = NLS.bind(PDEUIMessages.CategorySection_newCategoryName, Integer.toString(++fCounter));
		}
		String label = NLS.bind(PDEUIMessages.CategorySection_newCategoryLabel, Integer.toString(fCounter));
		ISiteCategoryDefinition categoryDef = fModel.getFactory().createCategoryDefinition();
		try {
			categoryDef.setName(name);
			categoryDef.setLabel(label);
			fModel.getSite().addCategoryDefinitions(new ISiteCategoryDefinition[] {categoryDef});
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		fCategoryViewer.setSelection(new StructuredSelection(categoryDef), true);
	}

	private boolean handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fCategoryViewer.getSelection();
		Iterator iterator = ssel.iterator();
		boolean success = true;
		Set removedCategories = new HashSet();
		while (iterator.hasNext()) {
			Object object = iterator.next();
			if (object == null)
				continue;
			if (object instanceof ISiteCategoryDefinition) {
				if (!handleRemoveCategoryDefinition((ISiteCategoryDefinition) object)) {
					success = false;
				}
			} else {
				//check if some of features was not removed during category removal
				SiteFeatureAdapter fa = (SiteFeatureAdapter) object;
				if (removedCategories.contains(fa.category))
					continue;

				if (!handleRemoveSiteFeatureAdapter(fa)) {
					success = false;
				}
			}
		}
		return success;
	}

	private boolean handleRemoveCategoryDefinition(ISiteCategoryDefinition catDef) {
		try {
			Object[] children = ((CategoryContentProvider) fCategoryViewer.getContentProvider()).getChildren(catDef);
			for (int i = 0; i < children.length; i++) {
				SiteFeatureAdapter adapter = (SiteFeatureAdapter) children[i];
				ISiteCategory[] cats = adapter.feature.getCategories();
				for (int j = 0; j < cats.length; j++) {
					if (adapter.category.equals(cats[j].getName()))
						adapter.feature.removeCategories(new ISiteCategory[] {cats[j]});
				}
				if (adapter.feature.getCategories().length == 0) {
					fModel.getSite().removeFeatures(new ISiteFeature[] {adapter.feature});
				}
			}
			fModel.getSite().removeCategoryDefinitions(new ISiteCategoryDefinition[] {catDef});
			return true;
		} catch (CoreException e) {
		}
		return false;
	}

	private boolean handleRemoveSiteFeatureAdapter(SiteFeatureAdapter adapter) {
		try {
			ISiteFeature feature = adapter.feature;
			if (adapter.category == null) {
				fModel.getSite().removeFeatures(new ISiteFeature[] {feature});
			} else {
				removeCategory(feature, adapter.category);
				if (feature.getCategories().length == 0)
					fModel.getSite().removeFeatures(new ISiteFeature[] {feature});
			}
			return true;
		} catch (CoreException e) {
		}
		return false;
	}

	private void removeCategory(ISiteFeature aFeature, String catName) {
		try {
			if (aFeature == null)
				return;
			ISiteCategory[] cats = aFeature.getCategories();
			for (int i = 0; i < cats.length; i++) {
				if (catName.equals(cats[i].getName()))
					aFeature.removeCategories(new ISiteCategory[] {cats[i]});
			}
		} catch (CoreException e) {
		}
	}

	private ISiteFeature findRealFeature(SiteFeatureAdapter adapter) {
		ISiteFeature featureCopy = adapter.feature;
		ISiteFeature[] features = fModel.getSite().getFeatures();
		for (int i = 0; i < features.length; i++) {
			if (features[i].getId().equals(featureCopy.getId()) && features[i].getVersion().equals(featureCopy.getVersion())) {
				return features[i];
			}
		}
		return null;
	}

	public void dispose() {
		super.dispose();
		FeatureModelManager mng = PDECore.getDefault().getFeatureModelManager();
		mng.removeFeatureModelListener(this);
		fModel.removeModelChangedListener(this);
		if (fSiteLabelProvider != null)
			fSiteLabelProvider.dispose();
	}

	protected void fillContextMenu(IMenuManager manager) {
		Action removeAction = new Action(PDEUIMessages.CategorySection_remove) {
			public void run() {
				doGlobalAction(ActionFactory.DELETE.getId());
			}
		};
		removeAction.setEnabled(isEditable());
		manager.add(removeAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.CUT.getId())) {
			handleRemove();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			return handleRemove();
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			fCategoryViewer.getTree().selectAll();
			refresh();
		}
		return false;
	}

	public void refresh() {
		fCategoryViewer.refresh();
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	public void initialize() {
		refresh();
		FeatureModelManager mng = PDECore.getDefault().getFeatureModelManager();
		mng.addFeatureModelListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object,
	 *      java.lang.Object[])
	 */
	protected void doPaste(Object target, Object[] objects) {
		try {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof SiteFeatureAdapter) {
					copyFeature((SiteFeatureAdapter) objects[i], target);
				} else if (objects[i] instanceof ISiteCategoryDefinition) {
					fModel.getSite().addCategoryDefinitions(new ISiteCategoryDefinition[] {(ISiteCategoryDefinition) objects[i]});
				}
			}
		} catch (CoreException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object,
	 *      java.lang.Object[])
	 */
	protected boolean canPaste(Object target, Object[] objects) {
		if (target == null || target instanceof ISiteCategoryDefinition) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof SiteFeatureAdapter)
					return true;
				if (objects[i] instanceof ISiteCategoryDefinition) {
					String name = ((ISiteCategoryDefinition) objects[i]).getName();
					ISiteCategoryDefinition[] defs = fModel.getSite().getCategoryDefinitions();
					for (int j = 0; j < defs.length; j++) {
						ISiteCategoryDefinition def = defs[j];
						String dname = def.getName();
						if (dname != null && dname.equals(name))
							return false;
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Finds a feature with the same id and version as a site feature. If
	 * feature is not found, but feature with a M.m.s.qualifier exists it will
	 * be returned.
	 * 
	 * @param siteFeature
	 * @return IFeature or null
	 */
	public static IFeature findFeature(ISiteFeature siteFeature) {
		IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(siteFeature.getId(), siteFeature.getVersion());
		if (model != null)
			return model.getFeature();
		return null;
	}

	private void handleNewFeature() {
		final Control control = fCategoryViewer.getControl();
		BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
			public void run() {
				IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
				ArrayList newModels = new ArrayList();
				for (int i = 0; i < allModels.length; i++) {
					if (canAdd(allModels[i]))
						newModels.add(allModels[i]);
				}
				IFeatureModel[] candidateModels = (IFeatureModel[]) newModels.toArray(new IFeatureModel[newModels.size()]);
				FeatureSelectionDialog dialog = new FeatureSelectionDialog(fCategoryViewer.getTree().getShell(), candidateModels, true);
				if (dialog.open() == Window.OK) {
					Object[] models = dialog.getResult();
					try {
						doAdd(models);
					} catch (CoreException e) {
						PDEPlugin.log(e);
					}
				}
			}
		});
	}

	private boolean canAdd(IFeatureModel candidate) {
		ISiteFeature[] features = fModel.getSite().getFeatures();
		IFeature cfeature = candidate.getFeature();

		for (int i = 0; i < features.length; i++) {
			ISiteFeature bfeature = features[i];
			if (bfeature.getId().equals(cfeature.getId()) && bfeature.getVersion().equals(cfeature.getVersion()))
				return false;
		}
		return true;
	}

	public static ISiteFeature createSiteFeature(ISiteModel model, IFeatureModel featureModel) throws CoreException {
		IFeature feature = featureModel.getFeature();
		ISiteFeature sfeature = model.getFactory().createFeature();
		sfeature.setId(feature.getId());
		sfeature.setVersion(feature.getVersion());
		// sfeature.setURL(model.getBuildModel().getSiteBuild().getFeatureLocation()
		// + "/" + feature.getId() + "_" + feature.getVersion() + ".jar");
		// //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sfeature.setURL("features/" + feature.getId() + "_" + formatVersion(feature.getVersion()) + ".jar"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		sfeature.setOS(feature.getOS());
		sfeature.setWS(feature.getWS());
		sfeature.setArch(feature.getArch());
		sfeature.setNL(feature.getNL());
		sfeature.setIsPatch(isFeaturePatch(feature));
		return sfeature;
	}

	private static String formatVersion(String version) {
		try {
			Version v = new Version(version);
			return v.toString();
		} catch (IllegalArgumentException e) {
		}
		return version;
	}

	private static boolean isFeaturePatch(IFeature feature) {
		IFeatureImport[] imports = feature.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isPatch())
				return true;
		}
		return false;
	}

	public ISiteModel getModel() {
		return fModel;
	}

	/**
	 * 
	 * @param candidates
	 *            Array of IFeatureModel
	 * @param monitor
	 * @throws CoreException
	 */
	public void doAdd(Object[] candidates) throws CoreException {
		// Category to add features to
		String categoryName = null;
		ISelection selection = fCategoryViewer.getSelection();
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof ISiteCategoryDefinition) {
				categoryName = ((ISiteCategoryDefinition) element).getName();
			} else if (element instanceof SiteFeatureAdapter) {
				categoryName = ((SiteFeatureAdapter) element).category;
			}
		}
		//
		ISiteFeature[] added = new ISiteFeature[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IFeatureModel candidate = (IFeatureModel) candidates[i];
			ISiteFeature child = createSiteFeature(fModel, candidate);
			if (categoryName != null) {
				addCategory(child, categoryName);
			}
			added[i] = child;
		}

		// Update model
		fModel.getSite().addFeatures(added);
		// Select last added feature
		if (added.length > 0) {
			if (categoryName != null) {
				expandCategory(categoryName);
			}
			fCategoryViewer.setSelection(new StructuredSelection(new SiteFeatureAdapter(categoryName, added[added.length - 1])), true);
		}
	}

	void fireSelection() {
		fCategoryViewer.setSelection(fCategoryViewer.getSelection());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFormInput(java.lang.Object)
	 */
	public boolean setFormInput(Object input) {
		if (input instanceof ISiteCategoryDefinition) {
			fCategoryViewer.setSelection(new StructuredSelection(input), true);
			return true;
		}
		if (input instanceof SiteFeatureAdapter) {
			// first, expand the category, otherwise tree will not find the feature
			String category = ((SiteFeatureAdapter) input).category;
			if (category != null) {
				expandCategory(category);
			}
			fCategoryViewer.setSelection(new StructuredSelection(input), true);
			return true;
		}
		return super.setFormInput(input);
	}

	private void expandCategory(String category) {
		if (category != null) {
			ISiteCategoryDefinition[] catDefs = fModel.getSite().getCategoryDefinitions();
			for (int i = 0; i < catDefs.length; i++) {
				if (category.equals(catDefs[i].getName())) {
					fCategoryViewer.expandToLevel(catDefs[i], 1);
					break;
				}
			}
		}

	}

	public void modelsChanged(IFeatureModelDelta delta) {
		markStale();
	}
}
