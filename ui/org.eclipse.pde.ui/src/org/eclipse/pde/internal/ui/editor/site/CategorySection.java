package org.eclipse.pde.internal.ui.editor.site;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author melhem
 *  
 */
public class CategorySection extends TreeSection {
	private ISiteModel fModel;
	private TreePart fCategoryTreePart;
	private TreeViewer fCategoryViewer;

	class CategoryContentProvider extends DefaultContentProvider
			implements
				ITreeContentProvider {
		public Object[] getElements(Object inputElement) {
			ISite model = (ISite) inputElement;
			ArrayList result = new ArrayList();
			ISiteCategoryDefinition[] catDefs = model.getCategoryDefinitions();
			for (int i = 0; i < catDefs.length; i++) {
				result.add(catDefs[i]);
			}
			ISiteFeature[] features = model.getFeatures();
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
						if (cats[j].getDefinition() != null
								&& cats[j].getDefinition().equals(catDef)) {
							result.add(new SiteFeatureAdapter(
									cats[j].getName(), features[i]));
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
						if (cats[j].getDefinition() != null
								&& cats[j].getDefinition().equals(catDef)){
							return true;
						}
					}
				}
			}
			return false;
		}
	}
	class CategoryLabelProvider extends LabelProvider {
		private Image siteFeatureImage;
		private Image catDefImage;
		public CategoryLabelProvider() {
			siteFeatureImage = PDEPluginImages.DESC_FEATURE_OBJ.createImage();
			catDefImage = PDEPluginImages.DESC_CATEGORY_OBJ.createImage();
		}
		public Image getImage(Object element) {
			if (element instanceof ISiteCategoryDefinition)
				return catDefImage;
			if (element instanceof SiteFeatureAdapter)
				return siteFeatureImage;
			return super.getImage(element);
		}
		public String getText(Object element) {
			if (element instanceof ISiteCategoryDefinition)
				return ((ISiteCategoryDefinition) element).getName();
			if (element instanceof SiteFeatureAdapter) {
				ISiteFeature feature = ((SiteFeatureAdapter) element).feature;
				if (feature.getId() != null && feature.getVersion() != null)
					return feature.getId() + " (" + feature.getVersion() + ")";
				return feature.getURL();
			}
			return super.getText(element);
		}
		public void dispose() {
			super.dispose();
			catDefImage.dispose();
			siteFeatureImage.dispose();
		}
	}
	public CategorySection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION,
				new String[]{"New Category..."});
		//TODO text not translated
		getSection().setText("Features To Publish");
		getSection()
				.setDescription(
						"Categorize the features to facilitate the browsing and searching of the update site.  A feature may appear in 0 or more categories. "); //$NON-NLS-1$
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
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		fCategoryTreePart = getTreePart();
		fCategoryViewer = fCategoryTreePart.getTreeViewer();
		fCategoryViewer.setContentProvider(new CategoryContentProvider());
		fCategoryViewer.setLabelProvider(new CategoryLabelProvider());
		fCategoryViewer.setInput(fModel.getSite());
		int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK
				| DND.DROP_DEFAULT;
		Transfer[] transfers = new Transfer[]{ModelDataTransfer.getInstance()};
		fCategoryViewer.addDropSupport(ops, transfers, new ViewerDropAdapter(
				fCategoryViewer) {
			public void dragEnter(DropTargetEvent event) {
				if ((event.operations & DND.DROP_LINK) != 0)
					event.detail = DND.DROP_LINK;
				else if (event.detail != DND.DROP_COPY)
					event.detail = DND.DROP_MOVE;
			}
			public boolean performDrop(Object data) {
				if (!(data instanceof Object[]))
					return false;
				Object target = getCurrentTarget();
				int op = getCurrentOperation();
				Object[] objects = (Object[]) data;
				if (op == DND.DROP_LINK) {
					for (int i = 0; i < objects.length; i++) {
						if (objects[i] instanceof ISiteBuildFeature)
							linkFeature((ISiteBuildFeature) objects[i], target);
					}
					return true;
				} else if (objects.length > 0
						&& objects[0] instanceof SiteFeatureAdapter) {
					if (op == DND.DROP_COPY) {
						copyFeature((SiteFeatureAdapter) objects[0], target);
					} else {
						moveFeature((SiteFeatureAdapter) objects[0], target);
					}
					return true;
				}
				return false;
			}
			public boolean validateDrop(Object target, int operation,
					TransferData transferType) {
				if (target == null && operation == DND.DROP_COPY)
					return false;
				return (target instanceof ISiteCategoryDefinition || target == null);
			}
		});
		fCategoryViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY,
				transfers, new DragSourceListener() {
					public void dragStart(DragSourceEvent event) {
						IStructuredSelection ssel = (IStructuredSelection) fCategoryViewer
								.getSelection();
						if (ssel == null
								|| ssel.isEmpty()
								|| ssel.getFirstElement() instanceof ISiteCategoryDefinition) {
							event.doit = false;
						}
					}
					public void dragSetData(DragSourceEvent event) {
						IStructuredSelection ssel = (IStructuredSelection) fCategoryViewer
								.getSelection();
						event.data = ssel.toArray();
					}
					public void dragFinished(DragSourceEvent event) {
					}
				});
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}
	private void linkFeature(ISiteBuildFeature sbFeature, Object target) {
		try {
			ISiteFeature feature = FeatureSection.findMatchingSiteFeature(
					fModel, sbFeature);
			if (feature == null) {
				feature = FeatureSection.createSiteFeature(fModel, sbFeature);
				fModel.getSite().addFeatures(new ISiteFeature[]{feature});
			}
			if (target != null && target instanceof ISiteCategoryDefinition)
				addCategory(feature, false, (ISiteCategoryDefinition) target);
		} catch (CoreException e) {
		}
	}
	private void copyFeature(SiteFeatureAdapter adapter, Object target) {
		if (adapter.category == null) {
			moveFeature(adapter, target);
		} else if (target instanceof ISiteCategoryDefinition) {
			addCategory(adapter.feature, true, (ISiteCategoryDefinition) target);
		}
	}
	private void addCategory(ISiteFeature aFeature, boolean isCopy,
			ISiteCategoryDefinition target) {
		try {
			ISiteFeature feature = isCopy
					? findRealFeature(aFeature)
					: aFeature;
			if (feature == null)
				return;
			ISiteCategoryDefinition catDef = (ISiteCategoryDefinition) target;
			ISiteCategory[] cats = feature.getCategories();
			int j = 0;
			for (; j < cats.length; j++) {
				if (cats[j].getName().equals(catDef.getName()))
					break;
			}
			if (j == cats.length) {
				ISiteCategory cat = fModel.getFactory().createCategory(feature);
				cat.setName(catDef.getName());
				feature.addCategories(new ISiteCategory[]{cat});
			}
		} catch (CoreException e) {
		}
	}
	private void moveFeature(SiteFeatureAdapter adapter, Object target) {
		if (adapter.category != null)
			removeCategory(adapter.feature, true, adapter.category);
		if (target instanceof ISiteCategoryDefinition)
			addCategory(adapter.feature, true, (ISiteCategoryDefinition) target);
	}
	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleAddCategoryDefinition();
		}
	}
	protected void handleDoubleClick(IStructuredSelection ssel) {
		Object selected = ssel.getFirstElement();
		if (selected instanceof ISiteCategoryDefinition)
			handleEditCategoryDefinition();
		else if (selected instanceof SiteFeatureAdapter)
			handleEditFeatureProperties((SiteFeatureAdapter) selected);
	}
	private void handleEditFeatureProperties(SiteFeatureAdapter adapter) {
		final ISiteFeature feature = adapter.feature;
		BusyIndicator.showWhile(fCategoryViewer.getControl().getDisplay(),
				new Runnable() {
					public void run() {
						FeaturePropertiesDialog dialog = new FeaturePropertiesDialog(
								fCategoryViewer.getControl().getShell(),
								fModel, feature);
						dialog.create();
						if (dialog.open() == FeaturePropertiesDialog.OK) {
						}
					}
				});
	}
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}
	private void handleEditCategoryDefinition() {
		IStructuredSelection ssel = (IStructuredSelection) fCategoryViewer
				.getSelection();
		if (ssel != null && ssel.size() == 1)
			showCategoryDialog((ISiteCategoryDefinition) ssel.getFirstElement());
	}
	private void handleAddCategoryDefinition() {
		showCategoryDialog(null);
	}
	private boolean handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fCategoryViewer
				.getSelection();
		Object object = ssel.getFirstElement();
		if (object == null)
			return true;
		if (object instanceof ISiteCategoryDefinition)
			return handleRemoveCategoryDefinition((ISiteCategoryDefinition) object);
		return handleRemoveSiteFeatureAdapter((SiteFeatureAdapter) object);
	}
	private boolean handleRemoveCategoryDefinition(
			ISiteCategoryDefinition catDef) {
		try {
			Object[] children = ((CategoryContentProvider) fCategoryViewer
					.getContentProvider()).getChildren(catDef);
			for (int i = 0; i < children.length; i++) {
				SiteFeatureAdapter adapter = (SiteFeatureAdapter) children[i];
				ISiteCategory[] cats = adapter.feature.getCategories();
				for (int j = 0; j < cats.length; j++) {
					if (adapter.category.equals(cats[j].getName()))
						adapter.feature
								.removeCategories(new ISiteCategory[]{cats[j]});
				}
				if (adapter.feature.getCategories().length == 0)
					fModel.getSite().removeFeatures(
							new ISiteFeature[]{adapter.feature});
			}
			fModel.getSite().removeCategoryDefinitions(
					new ISiteCategoryDefinition[]{catDef});
			return true;
		} catch (CoreException e) {
		}
		return false;
	}
	private boolean handleRemoveSiteFeatureAdapter(SiteFeatureAdapter adapter) {
		try {
			ISiteFeature feature = adapter.feature;
			if (adapter.category == null) {
				fModel.getSite().removeFeatures(new ISiteFeature[]{feature});
			} else {
				removeCategory(feature, false, adapter.category);
				if (feature.getCategories().length == 0)
					fModel.getSite()
							.removeFeatures(new ISiteFeature[]{feature});
			}
			return true;
		} catch (CoreException e) {
		}
		return false;
	}
	private void removeCategory(ISiteFeature aFeature, boolean isCopy,
			String catName) {
		try {
			ISiteFeature feature = isCopy
					? findRealFeature(aFeature)
					: aFeature;
			if (feature == null)
				return;
			ISiteCategory[] cats = feature.getCategories();
			for (int i = 0; i < cats.length; i++) {
				if (catName.equals(cats[i].getName()))
					feature.removeCategories(new ISiteCategory[]{cats[i]});
			}
		} catch (CoreException e) {
		}
	}
	private ISiteFeature findRealFeature(ISiteFeature aCopy) {
		ISiteFeature[] features = fModel.getSite().getFeatures();
		for (int i = 0; i < features.length; i++) {
			if (features[i].getURL().equals(aCopy.getURL()))
				return features[i];
		}
		return null;
	}
	private void showCategoryDialog(final ISiteCategoryDefinition def) {
		BusyIndicator.showWhile(fCategoryViewer.getControl().getDisplay(),
				new Runnable() {
					public void run() {
						NewCategoryDefinitionDialog dialog = new NewCategoryDefinitionDialog(
								fCategoryViewer.getControl().getShell(),
								fModel, def);
						dialog.create();
						if (dialog.open() == NewCategoryDefinitionDialog.OK) {
						}
					}
				});
	}
	public void dispose() {
		super.dispose();
		fModel.removeModelChangedListener(this);
	}
	protected void fillContextMenu(IMenuManager manager) {
		manager
				.add(new Action(PDEPlugin
						.getResourceString("SiteEditor.remove")) {
					public void run() {
						doGlobalAction(ActionFactory.DELETE.getId());
					}
				});
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
		manager.add(new Separator());
		manager.add(new Action(PDEPlugin
				.getResourceString("SiteEditor.properties")) {
			public void run() {
				handleDoubleClick((IStructuredSelection) fCategoryViewer
						.getSelection());
			}
		});
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
		return actionId.equals(ActionFactory.DELETE.getId())
				? handleRemove()
				: false;
	}
	public void refresh() {
		fCategoryViewer.refresh();
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		refresh();
	}

	public void initialize() {
		refresh();
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
				if (objects[i] instanceof ISiteBuildFeature) {
					linkFeature((ISiteBuildFeature) objects[i], target);
				} else if (objects[i] instanceof SiteFeatureAdapter) {
					copyFeature((SiteFeatureAdapter) objects[i], target);
				} else if (objects[i] instanceof ISiteCategoryDefinition) {
					fModel
							.getSite()
							.addCategoryDefinitions(
									new ISiteCategoryDefinition[]{(ISiteCategoryDefinition) objects[i]});
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
				if (objects[i] instanceof ISiteBuildFeature
						|| objects[i] instanceof SiteFeatureAdapter
						|| objects[i] instanceof ISiteCategoryDefinition)
					return true;
			}
		}
		return false;
	}
}