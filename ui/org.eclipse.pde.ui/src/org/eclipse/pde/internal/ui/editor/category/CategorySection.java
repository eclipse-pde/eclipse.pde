/*******************************************************************************
 * Copyright (c) 2009, 2019 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource - initial API and implementation
 *     IBM Corporation - ongoing enhancements
 *     Red Hat Inc - Support for bundles and nested categories
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 296392, 351356
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - ongoing enhancements
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
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
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

public class CategorySection extends TreeSection implements IFeatureModelListener {

	private static final int BUTTON_ADD_CATEGORY = 0;
	private static final int BUTTON_ADD_FEATURE = 1;
	private static final int BUTTON_ADD_BUNDLE = 2;
	private static final int BUTTON_REMOVE = 3;
	private static int fCounter;
	private ISiteModel fModel;
	private TreePart fCategoryTreePart;
	private TreeViewer fCategoryViewer;
	private LabelProvider fSiteLabelProvider;

	class CategoryContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			// model = (ISite) inputElement;
			ArrayList<IWritable> result = new ArrayList<>();
			ISiteCategoryDefinition[] catDefs = fModel.getSite().getCategoryDefinitions();
			for (ISiteCategoryDefinition catDef : catDefs) {
				if (catDef.getCategories().length == 0) {
					result.add(new SiteCategoryDefinitionAdapter(null, catDef));
				}
			}
			ISiteFeature[] features = fModel.getSite().getFeatures();
			for (ISiteFeature feature : features) {
				if (feature.getCategories().length == 0)
					result.add(new SiteFeatureAdapter(null, feature));
			}
			ISiteBundle[] bundles = fModel.getSite().getBundles();
			for (ISiteBundle bundle : bundles) {
				if (bundle.getCategories().length == 0)
					result.add(new SiteBundleAdapter(null, bundle));
			}
			return result.toArray();
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (parent instanceof SiteCategoryDefinitionAdapter) {
				ISiteCategoryDefinition catDef = ((SiteCategoryDefinitionAdapter) parent).category;
				HashSet<IWritable> result = new HashSet<>();
				ISiteCategoryDefinition[] categories = fModel.getSite().getCategoryDefinitions();
				for (ISiteCategoryDefinition categorie : categories) {
					ISiteCategory[] cats = categorie.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							result.add(new SiteCategoryDefinitionAdapter(cat.getName(), categorie));
						}
					}
				}
				ISiteFeature[] features = fModel.getSite().getFeatures();
				for (ISiteFeature feature : features) {
					ISiteCategory[] cats = feature.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							result.add(new SiteFeatureAdapter(cat.getName(), feature));
						}
					}
				}
				ISiteBundle[] bundles = fModel.getSite().getBundles();
				for (ISiteBundle bundle : bundles) {
					ISiteCategory[] cats = bundle.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							result.add(new SiteBundleAdapter(cat.getName(), bundle));
						}
					}
				}
				return result.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof SiteCategoryDefinitionAdapter) {
				ISiteCategoryDefinition catDef = ((SiteCategoryDefinitionAdapter) element).category;
				ISiteFeature[] features = fModel.getSite().getFeatures();
				for (ISiteFeature feature : features) {
					ISiteCategory[] cats = feature.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							return true;
						}
					}
				}
				ISiteBundle[] bundles = fModel.getSite().getBundles();
				for (ISiteBundle bundle : bundles) {
					ISiteCategory[] cats = bundle.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							return true;
						}
					}
				}
				ISiteCategoryDefinition[] categories = fModel.getSite().getCategoryDefinitions();
				for (ISiteCategoryDefinition categorie : categories) {
					ISiteCategory[] cats = categorie.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	public CategorySection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION,
				new String[] { PDEUIMessages.CategoryDefinitionCategorySection_new, PDEUIMessages.CategorySection_add,
						PDEUIMessages.CategorySection_addBundle, PDEUIMessages.CategorySection_remove });
		getSection().setText(PDEUIMessages.CategoryDefinitionCategorySection_title);
		getSection().setDescription(PDEUIMessages.CategoryDefinitionCategorySection_desc);
	}

	@Override
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
				@Override
				public void dragEnter(DropTargetEvent event) {
					Object target = determineTarget(event);
					if (target == null && event.detail == DND.DROP_COPY) {
						event.detail = DND.DROP_MOVE;
					}
					super.dragEnter(event);
				}

				@Override
				public void dragOperationChanged(DropTargetEvent event) {
					Object target = determineTarget(event);
					if (target == null && event.detail == DND.DROP_COPY) {
						event.detail = DND.DROP_MOVE;
					}
					super.dragOperationChanged(event);
				}

				@Override
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
				@Override
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

				@Override
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
					if (objects.length > 0 && objects[0] instanceof SiteBundleAdapter) {
						if (op == DND.DROP_COPY && target != null) {
							copyBundle((SiteBundleAdapter) objects[0], target);
						} else {
							moveBundle((SiteBundleAdapter) objects[0], target);
						}
						return true;
					}
					if (objects.length > 0 && objects[0] instanceof SiteCategoryDefinitionAdapter) {
						SiteCategoryDefinitionAdapter adapter = (SiteCategoryDefinitionAdapter) objects[0];
						if (op == DND.DROP_COPY && target != null) {
							copyCategory(adapter, target);
						} else {
							moveCategory(adapter, target);
						}
						// ensure that after moving we will have at least one
						// level expanded
						expandCategory(adapter.category.getName());
						return true;
					}
					return false;
				}

				@Override
				public boolean validateDrop(Object target, int operation, TransferData transferType) {
					return (target instanceof SiteCategoryDefinitionAdapter || target == null);
				}

			});
		}

		fCategoryViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, transfers, new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection ssel = fCategoryViewer.getStructuredSelection();
				if (ssel == null || ssel.isEmpty()
						|| !(ssel.getFirstElement() instanceof SiteFeatureAdapter
								|| ssel.getFirstElement() instanceof SiteBundleAdapter
								|| ssel.getFirstElement() instanceof SiteCategoryDefinitionAdapter)) {
					event.doit = false;
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection ssel = fCategoryViewer.getStructuredSelection();
				event.data = ssel.toArray();
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
			}
		});

		fCategoryTreePart.setButtonEnabled(BUTTON_ADD_CATEGORY, isEditable());
		fCategoryTreePart.setButtonEnabled(BUTTON_ADD_FEATURE, isEditable());
		fCategoryTreePart.setButtonEnabled(BUTTON_ADD_BUNDLE, isEditable());
		fCategoryTreePart.setButtonEnabled(BUTTON_REMOVE, isEditable());

		// fCategoryViewer.expandAll();
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private boolean categoryExists(String name) {
		ISiteCategoryDefinition[] defs = fModel.getSite().getCategoryDefinitions();
		for (ISiteCategoryDefinition def : defs) {
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
		 */if (target != null && target instanceof SiteCategoryDefinitionAdapter) {
			addCategory(feature, ((SiteCategoryDefinitionAdapter) target).category.getName());
		}
	}

	private void copyBundle(SiteBundleAdapter adapter, Object target) {
		ISiteBundle bundle = findRealBundle(adapter);
		if (bundle == null) {
			return;
		}
		if (target != null && target instanceof SiteCategoryDefinitionAdapter) {
			addCategory(bundle, ((SiteCategoryDefinitionAdapter) target).category.getName());
		}
	}

	private void copyCategory(SiteCategoryDefinitionAdapter categoryAdapter, Object target) {
		ISiteCategoryDefinition categoryDef = categoryAdapter.category;
		if (categoryDef != null) {
			return;
		}
		if (target != null && target instanceof SiteCategoryDefinitionAdapter) {
			addCategory(categoryDef, ((SiteCategoryDefinitionAdapter) target).category.getName());
		}
	}

	private void addCategory(ISiteBundle aBundle, String catName) {
		try {
			if (aBundle == null)
				return;
			ISiteCategory[] cats = aBundle.getCategories();
			for (ISiteCategory cat : cats) {
				if (cat.getName().equals(catName))
					return;
			}
			ISiteCategory cat = fModel.getFactory().createCategory(aBundle);
			cat.setName(catName);
			expandCategory(catName);
			aBundle.addCategories(new ISiteCategory[] { cat });
		} catch (CoreException e) {
		}
	}

	private void addCategory(ISiteFeature aFeature, String catName) {
		try {
			if (aFeature == null)
				return;
			ISiteCategory[] cats = aFeature.getCategories();
			for (ISiteCategory cat : cats) {
				if (cat.getName().equals(catName))
					return;
			}
			ISiteCategory cat = fModel.getFactory().createCategory(aFeature);
			cat.setName(catName);
			expandCategory(catName);
			aFeature.addCategories(new ISiteCategory[] {cat});
		} catch (CoreException e) {
		}
	}

	private void addCategory(final ISiteCategoryDefinition definition, final String parentCategoryName) {
		try {
			if (definition == null) {
				return;
			}
			ISiteCategory[] cats = definition.getCategories();
			for (ISiteCategory cat : cats) {
				if (cat.getName().equals(parentCategoryName)) {
					return;
				}
			}
			ISiteCategory parentCategoryRef = fModel.getFactory().createCategory(definition);
			parentCategoryRef.setName(parentCategoryName);
			definition.addCategories(new ISiteCategory[] { parentCategoryRef });
			if (!definition.isInTheModel()) {
				fModel.getSite().addCategoryDefinitions(new ISiteCategoryDefinition[] { definition });
			}
			expandCategory(parentCategoryName);
			fCategoryViewer.setSelection(
					new StructuredSelection(new SiteCategoryDefinitionAdapter(parentCategoryName, definition)), true);
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
		if (target != null && target instanceof SiteCategoryDefinitionAdapter) {
			addCategory(feature, ((SiteCategoryDefinitionAdapter) target).category.getName());
		}
	}

	private void moveBundle(SiteBundleAdapter adapter, Object target) {
		ISiteBundle bundle = findRealBundle(adapter);
		if (bundle == null) {
			return;
		}
		if (adapter.category != null) {
			removeCategory(bundle, adapter.category);
		}
		if (target != null && target instanceof SiteCategoryDefinitionAdapter) {
			addCategory(bundle, ((SiteCategoryDefinitionAdapter) target).category.getName());
		}
	}

	private void moveCategory(SiteCategoryDefinitionAdapter categoryAdapter, Object target) {
		ISiteCategoryDefinition categoryDef = findRealCategoryDefinition(categoryAdapter);
		if (categoryDef == null) {
			return;
		}
		if (categoryAdapter.parentCategory != null) {
			removeCategory(categoryDef, categoryAdapter.parentCategory);
		}
		if (target != null && target instanceof SiteCategoryDefinitionAdapter) {
			addCategory(categoryDef, ((SiteCategoryDefinitionAdapter) target).category.getName());
		}
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case BUTTON_ADD_CATEGORY :
				handleAddCategoryDefinition();
				break;
			case BUTTON_ADD_FEATURE :
				handleNewFeature();
				break;
			case BUTTON_ADD_BUNDLE :
				handleNewBundle();
				break;
		case BUTTON_REMOVE:
			handleRemove();
			break;
		default:
			break;
		}
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection ssel) {
		super.handleDoubleClick(ssel);
		Object selected = ssel.getFirstElement();
		if (selected instanceof SiteFeatureAdapter) {
			IFeature feature = findFeature(((SiteFeatureAdapter) selected).feature);
			FeatureEditor.openFeatureEditor(feature);
		} else if (selected instanceof SiteBundleAdapter) {
			ManifestEditor.openPluginEditor(((SiteBundleAdapter) selected).bundle.getId());
		}
	}

	@Override
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
			String catDefReference = null;
			categoryDef.setName(name);
			categoryDef.setLabel(label);
			Object firstElement = fCategoryViewer.getStructuredSelection().getFirstElement();
			if (firstElement instanceof SiteCategoryDefinitionAdapter) {
				// creating new nested category inside of something existing
				SiteCategoryDefinitionAdapter adapter = (SiteCategoryDefinitionAdapter) firstElement;
				catDefReference = adapter.category.getName();
				addCategory(categoryDef, catDefReference);
			}
			else {
				// if not selected adapter (of some category) then lets create
				// top-level category
				fModel.getSite().addCategoryDefinitions(new ISiteCategoryDefinition[] { categoryDef });
				fCategoryViewer.setSelection(
						new StructuredSelection(new SiteCategoryDefinitionAdapter(null, categoryDef)), true);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private boolean handleRemove() {
		IStructuredSelection ssel = fCategoryViewer.getStructuredSelection();
		Iterator<?> iterator = ssel.iterator();
		boolean success = true;
		Set<?> removedCategories = new HashSet<>();
		while (iterator.hasNext()) {
			Object object = iterator.next();
			if (object == null)
				continue;
			if (object instanceof SiteCategoryDefinitionAdapter) {
				if (!handleRemoveCategoryDefinition((SiteCategoryDefinitionAdapter) object)) {
					success = false;
				}
			} else if (object instanceof SiteFeatureAdapter) {
				// No need to remove the feature if its category is already removed
				SiteFeatureAdapter fa = (SiteFeatureAdapter) object;
				if (removedCategories.contains(fa.category))
					continue;

				if (!handleRemoveSiteFeatureAdapter(fa)) {
					success = false;
				}
			} else if (object instanceof SiteBundleAdapter) {
				// No need to remove the bundle if its category is already removed
				SiteBundleAdapter ba = (SiteBundleAdapter) object;
				if (removedCategories.contains(ba.category))
					continue;

				if (!handleRemoveSiteBundleAdapter(ba)) {
					success = false;
				}
			}
		}
		return success;
	}

	private boolean handleRemoveCategoryDefinition(SiteCategoryDefinitionAdapter catDef) {
		try {
			Object[] children = ((CategoryContentProvider) fCategoryViewer.getContentProvider()).getChildren(catDef);
			for (Object element : children) {
				if (element instanceof SiteCategoryDefinitionAdapter) {
					SiteCategoryDefinitionAdapter adapter = (SiteCategoryDefinitionAdapter) element;
					removeCategory(adapter.category, catDef.category.getName());
					handleRemoveCategoryDefinition(adapter);
				} else if (element instanceof SiteFeatureAdapter) {
					SiteFeatureAdapter adapter = (SiteFeatureAdapter) element;
					ISiteCategory[] cats = adapter.feature.getCategories();
					for (ISiteCategory cat : cats) {
						if (adapter.category.equals(cat.getName()))
							adapter.feature.removeCategories(new ISiteCategory[] {cat});
					}
					if (adapter.feature.getCategories().length == 0) {
						fModel.getSite().removeFeatures(new ISiteFeature[] {adapter.feature});
					}
				} else if (element instanceof SiteBundleAdapter) {
					SiteBundleAdapter adapter = (SiteBundleAdapter) element;
					ISiteCategory[] cats = adapter.bundle.getCategories();
					for (ISiteCategory cat : cats) {
						if (adapter.category.equals(cat.getName()))
							adapter.bundle.removeCategories(new ISiteCategory[] {cat});
					}
					if (adapter.bundle.getCategories().length == 0) {
						fModel.getSite().removeBundles(new ISiteBundle[] {adapter.bundle});
					}
				} else {
					return false;
				}
			}
			fModel.getSite().removeCategoryDefinitions(new ISiteCategoryDefinition[] { catDef.category });
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

	private boolean handleRemoveSiteBundleAdapter(SiteBundleAdapter adapter) {
		try {
			ISiteBundle bundle = adapter.bundle;
			if (adapter.category == null) {
				fModel.getSite().removeBundles(new ISiteBundle[] {bundle});
			} else {
				removeCategory(bundle, adapter.category);
				if (bundle.getCategories().length == 0)
					fModel.getSite().removeBundles(new ISiteBundle[] {bundle});
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
			for (ISiteCategory cat : cats) {
				if (catName.equals(cat.getName()))
					aFeature.removeCategories(new ISiteCategory[] {cat});
			}
		} catch (CoreException e) {
		}
	}

	private void removeCategory(ISiteBundle aBundle, String catName) {
		try {
			if (aBundle == null)
				return;
			ISiteCategory[] cats = aBundle.getCategories();
			for (ISiteCategory cat : cats) {
				if (catName.equals(cat.getName()))
					aBundle.removeCategories(new ISiteCategory[] {cat});
			}
		} catch (CoreException e) {
		}
	}

	private void removeCategory(final ISiteCategoryDefinition category, final String catName) {
		try {
			if (category == null) {
				return;
			}
			ISiteCategory[] cats = category.getCategories();
			for (ISiteCategory cat : cats) {
				if (catName.equals(cat.getName())) {
					category.removeCategories(new ISiteCategory[] { cat });
				}
			}
		} catch (CoreException e) {
		}
	}

	private ISiteFeature findRealFeature(SiteFeatureAdapter adapter) {
		ISiteFeature featureCopy = adapter.feature;
		ISiteFeature[] features = fModel.getSite().getFeatures();
		for (ISiteFeature feature : features) {
			if (Objects.equals(feature.getId(), featureCopy.getId())
					&& Objects.equals(feature.getVersion(), featureCopy.getVersion())) {
				return feature;
			}
		}
		return null;
	}

	private ISiteBundle findRealBundle(SiteBundleAdapter adapter) {
		ISiteBundle featureCopy = adapter.bundle;
		ISiteBundle[] bundles = fModel.getSite().getBundles();
		for (ISiteBundle bundle : bundles) {
			if (bundle.getId().equals(featureCopy.getId()) && bundle.getVersion().equals(featureCopy.getVersion())) {
				return bundle;
			}
		}
		return null;
	}

	private ISiteCategoryDefinition findRealCategoryDefinition(SiteCategoryDefinitionAdapter adapter) {
		ISiteCategoryDefinition categoryDefCopy = adapter.category;
		ISiteCategoryDefinition[] categoryDefinitions = fModel.getSite().getCategoryDefinitions();
		for (ISiteCategoryDefinition categoryDefinition : categoryDefinitions) {
			if (categoryDefinition.getName().equals(categoryDefCopy.getName())) {
				return categoryDefinition;
			}
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		FeatureModelManager mng = PDECore.getDefault().getFeatureModelManager();
		mng.removeFeatureModelListener(this);
		fModel.removeModelChangedListener(this);
		if (fSiteLabelProvider != null)
			fSiteLabelProvider.dispose();
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		Action removeAction = new Action(PDEUIMessages.CategorySection_remove) {
			@Override
			public void run() {
				doGlobalAction(ActionFactory.DELETE.getId());
			}
		};
		removeAction.setEnabled(isEditable());
		manager.add(removeAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	@Override
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
		return super.doGlobalAction(actionId);
	}

	@Override
	public void refresh() {
		fCategoryViewer.refresh();
		super.refresh();
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			fCategoryViewer.setSelection(StructuredSelection.EMPTY);
		}
		markStale();
	}

	public void initialize() {
		refresh();
		FeatureModelManager mng = PDECore.getDefault().getFeatureModelManager();
		mng.addFeatureModelListener(this);
	}

	@Override
	protected void doPaste(Object target, Object[] objects) {
		for (Object object : objects) {
			if (object instanceof SiteFeatureAdapter) {
				copyFeature((SiteFeatureAdapter) object, target);
			} else if (object instanceof SiteBundleAdapter) {
				copyBundle((SiteBundleAdapter) object, target);
			} else if (object instanceof ISiteCategoryDefinition) {
				copyCategory((SiteCategoryDefinitionAdapter) object, target);
			}
		}
	}

	@Override
	protected boolean canPaste(Object target, Object[] objects) {
		if (target == null || target instanceof ISiteCategoryDefinition) {
			for (Object object : objects) {
				if (object instanceof SiteFeatureAdapter) {
					return true;
				}
				if (object instanceof SiteBundleAdapter) {
					return true;
				}
				if (object instanceof ISiteCategoryDefinition) {
					String name = ((ISiteCategoryDefinition) object).getName();
					ISiteCategoryDefinition[] defs = fModel.getSite().getCategoryDefinitions();
					for (ISiteCategoryDefinition def : defs) {
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
	private IFeature findFeature(ISiteFeature siteFeature) {
		IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(siteFeature.getId(), siteFeature.getVersion());
		if (model != null)
			return model.getFeature();
		return null;
	}

	private void handleNewFeature() {
		final Control control = fCategoryViewer.getControl();
		BusyIndicator.showWhile(control.getDisplay(), () -> {
			IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
			ArrayList<IFeatureModel> newModels = new ArrayList<>();
			for (IFeatureModel allModel : allModels) {
				if (canAdd(allModel))
					newModels.add(allModel);
			}
			IFeatureModel[] candidateModels = newModels.toArray(new IFeatureModel[newModels.size()]);
			FeatureSelectionDialog dialog = new FeatureSelectionDialog(fCategoryViewer.getTree().getShell(), candidateModels, true);
			if (dialog.open() == Window.OK) {
				Object[] models = dialog.getResult();
				try {
					doAdd(models);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		});
	}

	private void handleNewBundle() {
		final Control control = fCategoryViewer.getControl();
		BusyIndicator.showWhile(control.getDisplay(), () -> {
			IPluginModelBase[] allModels = PluginRegistry.getAllModels();
			ArrayList<IPluginModelBase> newModels = new ArrayList<>();
			for (IPluginModelBase allModel : allModels) {
				if (canAdd(allModel))
					newModels.add(allModel);
			}
			IPluginModelBase[] candidateModels = newModels.toArray(new IPluginModelBase[newModels.size()]);
			PluginSelectionDialog dialog = new PluginSelectionDialog(fCategoryViewer.getTree().getShell(), candidateModels, true);
			if (dialog.open() == Window.OK) {
				Object[] models = dialog.getResult();
				try {
					doAddBundles(models);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		});
	}

	private boolean canAdd(IFeatureModel candidate) {
		ISiteFeature[] features = fModel.getSite().getFeatures();
		IFeature cfeature = candidate.getFeature();
		String id = cfeature.getId();
		String version = cfeature.getVersion();

		for (ISiteFeature bfeature : features) {
			boolean idEquals = Objects.equals(bfeature.getId(), id);
			if (idEquals && (bfeature.getVersion() == null || Objects.equals(bfeature.getVersion(), version))) {
				return false;
			}
		}
		return true;
	}

	private boolean canAdd(IPluginModelBase candidate) {
		ISiteBundle[] currentBundles = fModel.getSite().getBundles();
		IPluginBase candidateBundle = candidate.getPluginBase();

		String candidateId = candidateBundle.getId();
		String candidateVersion = candidateBundle.getVersion();

		for (ISiteBundle currentBundle : currentBundles) {
			if (currentBundle.getId().equals(candidateId)
					&& (currentBundle.getVersion() == null || currentBundle.getVersion().equals(candidateVersion)))
				return false;
		}
		return true;
	}

	public static ISiteFeature createSiteFeature(ISiteModel model, IFeatureModel featureModel) throws CoreException {
		IFeature feature = featureModel.getFeature();
		ISiteFeature sfeature = model.getFactory().createFeature();
		sfeature.setId(feature.getId());
		sfeature.setOS(feature.getOS());
		sfeature.setWS(feature.getWS());
		sfeature.setArch(feature.getArch());
		sfeature.setNL(feature.getNL());
		sfeature.setIsPatch(isFeaturePatch(feature));
		return sfeature;
	}

	private ISiteBundle createSiteBundle(ISiteModel model, IPluginModelBase candidate) throws CoreException {
		ISiteBundle newBundle = model.getFactory().createBundle();
		newBundle.setId(candidate.getPluginBase().getId());
		return newBundle;
	}

	private static boolean isFeaturePatch(IFeature feature) {
		IFeatureImport[] imports = feature.getImports();
		for (IFeatureImport import1 : imports) {
			if (import1.isPatch())
				return true;
		}
		return false;
	}

	public ISiteModel getModel() {
		return fModel;
	}

	/**
	 * @param candidates Array of IFeatureModel
	 */
	public void doAdd(Object[] candidates) throws CoreException {
		// Category to add features to
		String categoryName = getCategoryName();
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

	private String getCategoryName() {
		String categoryName = null;
		Object element = fCategoryViewer.getStructuredSelection().getFirstElement();
		if (element instanceof SiteCategoryDefinitionAdapter) {
			categoryName = ((SiteCategoryDefinitionAdapter) element).category.getName();
		} else if (element instanceof SiteFeatureAdapter) {
			categoryName = ((SiteFeatureAdapter) element).category;
		} else if (element instanceof SiteBundleAdapter) {
			categoryName = ((SiteBundleAdapter) element).category;
		}
		return categoryName;
	}

	/**
	 * @param candidates Array of IPluginModelBase
	 */
	public void doAddBundles(Object[] candidates) throws CoreException {
		// Category to add features to
		String categoryName = getCategoryName();

		ISiteBundle[] added = new ISiteBundle[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IPluginModelBase candidate = (IPluginModelBase) candidates[i];
			ISiteBundle child = createSiteBundle(fModel, candidate);
			if (categoryName != null) {
				addCategory(child, categoryName);
			}
			added[i] = child;
		}

		// Update model
		fModel.getSite().addBundles(added);
		// Select last added feature
		if (added.length > 0) {
			if (categoryName != null) {
				expandCategory(categoryName);
			}
			fCategoryViewer.setSelection(new StructuredSelection(new SiteBundleAdapter(categoryName, added[added.length - 1])), true);
		}
	}

	void fireSelection() {
		fCategoryViewer.setSelection(fCategoryViewer.getSelection());
	}

	@Override
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
			for (ISiteCategoryDefinition definition : catDefs) {
				if (category.equals(definition.getName())) {
					String parentCategoryName = definition.getCategories().length == 0 ? null
							: definition.getCategories()[0].getName();
					SiteCategoryDefinitionAdapter adapter = new SiteCategoryDefinitionAdapter(parentCategoryName, definition);
					fCategoryViewer.expandToLevel(adapter, 1);
					break;
				}
			}
		}
	}

	@Override
	public void modelsChanged(IFeatureModelDelta delta) {
		markStale();
	}
}
