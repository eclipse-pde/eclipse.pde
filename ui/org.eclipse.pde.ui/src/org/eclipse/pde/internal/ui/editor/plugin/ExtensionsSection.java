/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.editor.contentassist.XMLElementProposalComputer;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.wizards.extension.ExtensionEditorWizard;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionWizard;
import org.eclipse.pde.ui.IExtensionEditorWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.DrillDownAdapter;

public class ExtensionsSection extends TreeSection implements IModelChangedListener, IPropertyChangeListener {
	private TreeViewer fExtensionTree;
	private Image fExtensionImage;
	private Image fGenericElementImage;

	private SchemaRegistry fSchemaRegistry;
	private ExternalModelManager fPluginInfoRegistry;
	private DrillDownAdapter fDrillDownAdapter;
	private Action fNewExtensionAction;
	private Hashtable fEditorWizards;
	private SortAction fSortAction;
	private CollapseAction fCollapseAction;

	private static final String[] COMMON_LABEL_PROPERTIES = {
		"label", //$NON-NLS-1$
		"name", //$NON-NLS-1$
		"id"}; //$NON-NLS-1$

	class ExtensionContentProvider extends DefaultContentProvider
	implements
	ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			Object[] children = null;
			if (parent instanceof IPluginBase)
				children = ((IPluginBase) parent).getExtensions();
			else if (parent instanceof IPluginExtension) {
				children = ((IPluginExtension) parent).getChildren();
			} else if (parent instanceof IPluginElement) {
				children = ((IPluginElement) parent).getChildren();
			}
			if (children == null)
				children = new Object[0];
			return children;
		}
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginExtension) {
				return ((IPluginModelBase)getPage().getModel()).getPluginBase();
			}
			if (child instanceof IPluginObject)
				return ((IPluginObject) child).getParent();
			return null;
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
	}
	class ExtensionLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return resolveObjectName(obj);
		}
		public Image getImage(Object obj) {
			return resolveObjectImage(obj);
		}
	}
	public ExtensionsSection(PDEFormPage page, Composite parent) {
		super(page, parent, 0, new String[]{
				PDEUIMessages.ManifestEditor_DetailExtension_new, 
				PDEUIMessages.ManifestEditor_DetailExtension_edit,
				null,
				PDEUIMessages.ManifestEditor_DetailExtension_up,
				PDEUIMessages.ManifestEditor_DetailExtension_down});
		fPluginInfoRegistry = PDECore.getDefault().getExternalModelManager();
		fHandleDefaultButton = false;
	}
	private static void addItemsForExtensionWithSchema(MenuManager menu,
			IPluginExtension extension, IPluginParent parent) {
		ISchema schema = getSchema(extension);
		String tagName = (parent == extension ? "extension" : parent.getName()); //$NON-NLS-1$
		ISchemaElement elementInfo = schema.findElement(tagName);
		
		if ((elementInfo != null) &&
				(elementInfo.getType() instanceof ISchemaComplexType) &&
				(parent instanceof IDocumentNode)) {
			// We have a schema complex type.  Either the element has attributes
			// or the element has children.
			// Generate the list of element proposals
			HashSet elementSet = XMLElementProposalComputer
					.computeElementProposal(elementInfo, (IDocumentNode)parent);
			// Create a corresponding menu entry for each element proposal
			Iterator iterator = elementSet.iterator();
			while (iterator.hasNext()) {
				Action action = new NewElementAction((ISchemaElement) iterator
						.next(), parent);
				menu.add(action);	
			}			
		}
	}
	private static ISchema getSchema(IPluginExtension extension) {
		String point = extension.getPoint();
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		return registry.getSchema(point);
	}
	static ISchemaElement getSchemaElement(IPluginElement element) {
		IPluginObject parent = element.getParent();
		while (parent != null && !(parent instanceof IPluginExtension)) {
			parent = parent.getParent();
		}
		if (parent != null) {
			ISchema schema = getSchema((IPluginExtension) parent);
			if (schema != null) {
				return schema.findElement(element.getName());
			}
		}
		return null;
	}
	public void createClient(Section section, FormToolkit toolkit) {
		initializeImages();
		Composite container = createClientContainer(section, 2, toolkit);
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fExtensionTree = treePart.getTreeViewer();
		fExtensionTree.setContentProvider(new ExtensionContentProvider());
		fExtensionTree.setLabelProvider(new ExtensionLabelProvider());
		fDrillDownAdapter = new DrillDownAdapter(fExtensionTree);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		// See Bug # 160554: Set text before text client
		section.setText(PDEUIMessages.ManifestEditor_DetailExtension_title);
		initialize((IPluginModelBase) getPage().getModel());
		createSectionToolbar(section, toolkit);
	}
	
	/**
	 * @param section
	 * @param toolkit
	 */
	private void createSectionToolbar(Section section, FormToolkit toolkit) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) &&
						(handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});			
		// Add sort action to the tool bar
		fSortAction = new SortAction(fExtensionTree, 
				PDEUIMessages.ExtensionsPage_sortAlpha, null, this, true);
		toolBarManager.add(fSortAction);
		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fExtensionTree, 
				PDEUIMessages.ExtensionsPage_collapseAll);
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateUpDownButtons(selection.getFirstElement());
		getTreePart().setButtonEnabled(1, isSelectionEditable(selection));
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
		/*
		 * PropertiesAction action = new
		 * PropertiesAction(getFormPage().getEditor()); action.run();
		 */
	}
	protected void buttonSelected(int index) {
		switch (index) {
		case 0 :
			handleNew();
			break;
		case 1 :
			handleEdit();
			break;
		case 2:
			// blank
			break;
		case 3 :
			handleMove(true);
			break;
		case 4 :
			handleMove(false);
			break;
		}
	}
	public void dispose() {
		fEditorWizards = null;
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
		.getAggregateModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
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
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginExtension
				|| object instanceof IPluginElement) {
			fExtensionTree.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fExtensionTree.getSelection();
		IStructuredSelection ssel = (IStructuredSelection) selection;
		IMenuManager newMenu = null;
		if (ssel.size() == 1) {
			Object object = ssel.getFirstElement();
			if (object instanceof IPluginParent) {
				IPluginParent parent = (IPluginParent) object;
				if (parent.getModel().getUnderlyingResource() != null) {
					newMenu = fillContextMenu(getPage(), parent, manager);
					manager.add(new Separator());
				}
			}
		} else if (ssel.size() > 1) {
			// multiple
			Action delAction = new Action() {
				public void run() {
					handleDelete();
				}
			};
			delAction.setText(PDEUIMessages.Actions_delete_label);
			manager.add(delAction);
			manager.add(new Separator());
			delAction.setEnabled(isEditable());
		}
		if (newMenu == null) {
			newMenu = new MenuManager(PDEUIMessages.Menus_new_label);
			manager.add(newMenu);
		}
		if (ssel.size() == 1) {
			manager.add(new Separator());
			Object object = ssel.getFirstElement();
			if (object instanceof IPluginExtension) {
				PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				manager.add(new Separator());
			}
			//manager.add(new PropertiesAction(getFormPage().getEditor()));
		}		
		if (!newMenu.isEmpty())
			newMenu.add(new Separator());
		newMenu.add(fNewExtensionAction);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().addClipboardActions(manager);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager, false);

	}
	static IMenuManager fillContextMenu(PDEFormPage page,
			final IPluginParent parent, IMenuManager manager) {
		return fillContextMenu(page, parent, manager, false);
	}
	static IMenuManager fillContextMenu(PDEFormPage page,
			final IPluginParent parent, IMenuManager manager,
			boolean addSiblingItems) {
		return fillContextMenu(page, parent, manager, addSiblingItems, true);
	}
	static IMenuManager fillContextMenu(PDEFormPage page,
			final IPluginParent parent, IMenuManager manager,
			boolean addSiblingItems, boolean fullMenu) {
		MenuManager menu = new MenuManager(PDEUIMessages.Menus_new_label);
		IPluginExtension extension = getExtension(parent);
		ISchema schema = getSchema(extension);
		if (schema == null) {
			menu.add(new NewElementAction(null, parent));
		} else {
			addItemsForExtensionWithSchema(menu, extension, parent);
			if (addSiblingItems) {
				IPluginObject parentsParent = parent.getParent();
				if (!(parentsParent instanceof IPluginExtension)) {
					IPluginParent pparent = (IPluginParent) parentsParent;
					menu.add(new Separator());
					addItemsForExtensionWithSchema(menu, extension, pparent);
				}
			}
		}
		manager.add(menu);
		manager.add(new Separator());
		if (fullMenu) {
			Action deleteAction = new Action(PDEUIMessages.Actions_delete_label) {
				public void run() {
					try {
						IPluginObject parentsParent = parent.getParent();
						if (parent instanceof IPluginExtension) {
							IPluginBase plugin = (IPluginBase) parentsParent;
							plugin.remove((IPluginExtension) parent);
						} else {
							IPluginParent parentElement = (IPluginParent) parent
							.getParent();
							parentElement.remove(parent);
						}
					} catch (CoreException e) {
					}
				}
			};
			deleteAction.setEnabled(page.getModel().isEditable());
			manager.add(deleteAction);
		}
		return menu;
	}
	static IPluginExtension getExtension(IPluginParent parent) {
		while (parent != null && !(parent instanceof IPluginExtension)) {
			parent = (IPluginParent) parent.getParent();
		}
		return (IPluginExtension) parent;
	}
	private void handleDelete() {
		IStructuredSelection sel = (IStructuredSelection) fExtensionTree
		.getSelection();
		if (sel.isEmpty())
			return;
		for (Iterator iter = sel.iterator(); iter.hasNext();) {
			IPluginObject object = (IPluginObject) iter.next();
			try {
				if (object instanceof IPluginElement) {
					IPluginElement ee = (IPluginElement) object;
					IPluginParent parent = (IPluginParent) ee.getParent();
					parent.remove(ee);
				} else if (object instanceof IPluginExtension) {
					IPluginExtension extension = (IPluginExtension) object;
					IPluginBase plugin = extension.getPluginBase();
					plugin.remove(extension);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void handleNew() {
		final IProject project = getPage().getPDEEditor().getCommonProject();
		BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(),
				new Runnable() {
			public void run() {
				((ManifestEditor)getPage().getEditor()).ensurePluginContextPresence();
				NewExtensionWizard wizard = new NewExtensionWizard(
						project, (IPluginModelBase) getPage()
						.getModel(), (ManifestEditor) getPage()
						.getPDEEditor()) {
					public boolean performFinish() {
						return super.performFinish();
					}
				};
				WizardDialog dialog = new WizardDialog(PDEPlugin
						.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 500, 500);
				dialog.open();
			}
		});
	}
	private void handleEdit(IConfigurationElement element, IStructuredSelection selection) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		try {
			final IExtensionEditorWizard wizard = (IExtensionEditorWizard)element.createExecutableExtension("class"); //$NON-NLS-1$
			wizard.init(project, model, selection);
			BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(),
					new Runnable() {
				public void run() {
					WizardDialog dialog = new WizardDialog(PDEPlugin
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 500, 500);
					dialog.open();
				}
			});
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void handleEdit() {
		final IStructuredSelection selection = (IStructuredSelection)fExtensionTree.getSelection();
		ArrayList editorWizards = getEditorWizards(selection);
		if (editorWizards==null) return;
		if (editorWizards.size()==1) {
			// open the wizard directly			
			handleEdit((IConfigurationElement)editorWizards.get(0), selection);
		}
		else {
			IProject project = getPage().getPDEEditor().getCommonProject();
			IPluginModelBase model = (IPluginModelBase)getPage().getModel();
			final ExtensionEditorWizard wizard = new ExtensionEditorWizard(project, model, selection);
			BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(),
					new Runnable() {
				public void run() {
					WizardDialog dialog = new WizardDialog(PDEPlugin
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 500, 500);
					dialog.open();
				}
			});
		}
	}
	private ArrayList getEditorWizards(IStructuredSelection selection) {
		if (selection.size()!=1) return null;
		Object obj = selection.getFirstElement();
		String pointId = null;
		if (obj instanceof IPluginExtension) {
			pointId = ((IPluginExtension)obj).getPoint();
		}
		else if (obj instanceof IPluginElement) {
			IPluginObject parent = ((IPluginElement)obj).getParent();
			while (parent!=null) {
				if (parent instanceof IPluginExtension) {
					pointId = ((IPluginExtension)parent).getPoint();
					break;
				}
				parent = parent.getParent();
			}
		}
		if (pointId==null) return null;
		if (fEditorWizards==null)
			loadExtensionWizards();
		return (ArrayList)fEditorWizards.get(pointId);
	}

	private void loadExtensionWizards() {
		fEditorWizards = new Hashtable();
		IConfigurationElement [] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.pde.ui.newExtension"); //$NON-NLS-1$
		for (int i=0; i<elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("editorWizard")) { //$NON-NLS-1$
				String pointId = element.getAttribute("point"); //$NON-NLS-1$
				if (pointId==null) continue;
				ArrayList list = (ArrayList)fEditorWizards.get(pointId);
				if (list==null) {
					list = new ArrayList();
					fEditorWizards.put(pointId, list);
				}
				list.add(element);
			}
		}
	}
	private boolean isSelectionEditable(IStructuredSelection selection) {
		if (!getPage().getModel().isEditable())
			return false;
		return getEditorWizards(selection)!=null;
	}

	public void initialize(IPluginModelBase model) {
		fExtensionTree.setInput(model.getPluginBase());
		selectFirstExtension();
		boolean editable = model.isEditable();
		TreePart treePart = getTreePart();
		treePart.setButtonEnabled(0, editable);
		treePart.setButtonEnabled(1, false);
		treePart.setButtonEnabled(3, false);
		treePart.setButtonEnabled(4, false);
		model.addModelChangedListener(this);
		fNewExtensionAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		fNewExtensionAction.setText(PDEUIMessages.ManifestEditor_DetailExtension_newExtension);
		fNewExtensionAction
		.setImageDescriptor(PDEPluginImages.DESC_EXTENSION_OBJ);
		fNewExtensionAction.setEnabled(editable);
	}
	private void selectFirstExtension() {
		Tree tree = fExtensionTree.getTree();
		TreeItem [] items = tree.getItems();
		if (items.length==0) return;
		TreeItem firstItem = items[0];
		Object obj = firstItem.getData();
		fExtensionTree.setSelection(new StructuredSelection(obj));
	}
	void fireSelection() {
		fExtensionTree.setSelection(fExtensionTree.getSelection());
	}
	public void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		fExtensionImage = provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
		fGenericElementImage = provider
		.get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	}
	public void refresh() {
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		fExtensionTree.setInput(model.getPluginBase());
		selectFirstExtension();
		getManagedForm().fireSelectionChanged(ExtensionsSection.this,
				fExtensionTree.getSelection());
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginBase
				&& event.getChangeType() == IModelChangedEvent.CHANGE
				&& event.getChangedProperty().equals(
						IExtensions.P_EXTENSION_ORDER)) {
			IStructuredSelection sel = (IStructuredSelection) fExtensionTree
			.getSelection();
			IPluginExtension extension = (IPluginExtension) sel
			.getFirstElement();
			fExtensionTree.refresh();
			fExtensionTree.setSelection(new StructuredSelection(extension));
			return;
		}
		if (changeObject instanceof IPluginExtension
				|| (changeObject instanceof IPluginElement && ((IPluginElement)changeObject).getParent() instanceof IPluginParent)) {
			IPluginObject pobj = (IPluginObject) changeObject;
			IPluginObject parent = changeObject instanceof IPluginExtension
			? ((IPluginModelBase) getPage().getModel()).getPluginBase()
					: pobj.getParent();
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				fExtensionTree.add(parent, pobj);
				fExtensionTree.setSelection(
						new StructuredSelection(changeObject), true);
				fExtensionTree.getTree().setFocus();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				fExtensionTree.remove(pobj);
			} else {
				if (event.getChangedProperty().equals(
						IPluginParent.P_SIBLING_ORDER)) {
					IStructuredSelection sel = (IStructuredSelection) fExtensionTree
					.getSelection();
					IPluginObject child = (IPluginObject) sel.getFirstElement();
					fExtensionTree.refresh(child.getParent());
					fExtensionTree.setSelection(new StructuredSelection(child));
				} else {
					fExtensionTree.update(changeObject, null);
				}
			}
		}
	}

	private Image resolveObjectImage(Object obj) {
		if (obj instanceof IPluginExtension) {
			return fExtensionImage;
		}
		Image elementImage = fGenericElementImage;
		if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			Image customImage = getCustomImage(element);
			if (customImage != null)
				elementImage = customImage;
			String bodyText = element.getText();
			boolean hasBodyText = bodyText!=null&&bodyText.length()>0;
			if (hasBodyText) {
				elementImage = PDEPlugin.getDefault().getLabelProvider().get(
						elementImage, SharedLabelProvider.F_EDIT);
			}
		}
		return elementImage;
	}

	private static boolean isStorageModel(IPluginObject object) {
		IPluginModelBase modelBase = object.getPluginModel();
		return modelBase.getInstallLocation()==null;
	}

	static Image getCustomImage(IPluginElement element) {
		if (isStorageModel(element))return null;
		ISchemaElement elementInfo = getSchemaElement(element);
		if (elementInfo != null && elementInfo.getIconProperty() != null) {
			String iconProperty = elementInfo.getIconProperty();
			IPluginAttribute att = element.getAttribute(iconProperty);
			String iconPath = null;
			if (att != null && att.getValue() != null) {
				iconPath = att.getValue();
			}
			if (iconPath != null) {
				//OK, we have an icon path relative to the plug-in
				return getImageFromPlugin(element, iconPath);
			}
		}
		return null;
	}

	private static Image getImageFromPlugin(IPluginElement element,
			String iconPathName) {
		// 39283 - ignore icon paths that
		// point at plugin.properties
		if (iconPathName.startsWith("%")) //$NON-NLS-1$
			return null;

		IPluginModelBase model = element.getPluginModel();
		if (model == null)
			return null;

		return PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(model, iconPathName);
	}
	private String resolveObjectName(Object obj) {
		return resolveObjectName(getSchemaRegistry(), fPluginInfoRegistry, obj);
	}

	private SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = PDECore.getDefault().getSchemaRegistry();
		return fSchemaRegistry;
	}

	public static String resolveObjectName(SchemaRegistry schemaRegistry,
			ExternalModelManager pluginInfoRegistry, Object obj) {
		boolean fullNames = PDEPlugin.isFullNameModeEnabled();
		if (obj instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) obj;
			if (!fullNames) {
				return extension.getPoint();
			}
			if (extension.getName() != null)
				return extension.getTranslatedName();
			ISchema schema = schemaRegistry.getSchema(extension.getPoint());
			// try extension point schema definition
			if (schema != null) {
				// exists
				return schema.getName();
			}
			return extension.getPoint();		
		} else if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			String baseName = element.getName();			
			String fullName = null;
			ISchemaElement elementInfo = getSchemaElement(element);
			IPluginAttribute labelAtt = null;
			if (elementInfo != null && elementInfo.getLabelProperty() != null) {
				labelAtt = element.getAttribute(elementInfo.getLabelProperty());
			}
			if (labelAtt == null) {
				// try some hard-coded attributes that
				// are used frequently
				for (int i = 0; i < COMMON_LABEL_PROPERTIES.length; i++) {
					labelAtt = element.getAttribute(COMMON_LABEL_PROPERTIES[i]);
					if (labelAtt != null)
						break;
				}
				if (labelAtt == null) {
					// Last try - if there is only one attribute,
					// use that
					if (element.getAttributeCount() == 1)
						labelAtt = element.getAttributes()[0];
				}
			}
			if (labelAtt != null && labelAtt.getValue() != null)
				fullName = stripShortcuts(labelAtt.getValue());
			fullName = element.getResourceString(fullName);
			if (fullNames)
				return fullName != null ? fullName : baseName;
			return fullName != null
			? (fullName + " (" + baseName + ")") //$NON-NLS-1$ //$NON-NLS-2$
					: baseName;
		}
		return obj.toString();
	}
	public void setFocus() {
		if (fExtensionTree != null)
			fExtensionTree.getTree().setFocus();
	}
	public static String stripShortcuts(String input) {
		StringBuffer output = new StringBuffer();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '&')
				continue;
			else if (c == '@')
				break;
			output.append(c);
		}
		return output.toString();
	}
	protected boolean canPaste(Object target, Object[] objects) {
		if (objects[0] instanceof IPluginExtension)
			return true;
		if (objects[0] instanceof IPluginElement
				&& target instanceof IPluginParent)
			return true;
		return false;
	}
	protected void doPaste(Object target, Object[] objects) {
		/*IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof IPluginExtension) {
					IPluginExtension extension = (IPluginExtension) obj;
					plugin.add(extension);
					((PluginParent) extension).reconnect();
				} else if (obj instanceof IPluginElement
						&& target instanceof IPluginParent) {
					IPluginElement element = (IPluginElement) obj;
					((IPluginParent) target).add(element);
					if (element instanceof PluginParent)
						((PluginParent) element).reconnect();
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}*/
	}
	private void handleMove(boolean up) {
		IStructuredSelection sel = (IStructuredSelection) fExtensionTree
		.getSelection();
		IPluginObject object = (IPluginObject) sel.getFirstElement();
		if (object instanceof IPluginElement) {
			IPluginParent parent = (IPluginParent) object.getParent();
			IPluginObject[] children = parent.getChildren();
			int index = parent.getIndexOf(object);
			int newIndex = up ? index - 1 : index + 1;
			IPluginObject child2 = children[newIndex];
			try {
				parent.swap(object, child2);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		} else if (object instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) object;
			IPluginBase plugin = extension.getPluginBase();
			IPluginExtension[] extensions = plugin.getExtensions();
			int index = plugin.getIndexOf(extension);
			int newIndex = up ? index - 1 : index + 1;
			IPluginExtension e2 = extensions[newIndex];
			try {
				plugin.swap(extension, e2);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void updateUpDownButtons(Object item) {
		if (getPage().getModel().isEditable() == false)
			return;
		boolean sorted = fSortAction != null && fSortAction.isChecked();
		if (sorted) {
			getTreePart().setButtonEnabled(3, false);
			getTreePart().setButtonEnabled(4, false);
			return;
		}
		
		boolean upEnabled = false;
		boolean downEnabled = false;
		if (item instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) item;
			IPluginParent parent = (IPluginParent) element.getParent();
			// check up
			int index = parent.getIndexOf(element);
			if (index > 0)
				upEnabled = true;
			if (index < parent.getChildCount() - 1)
				downEnabled = true;
		} else if (item instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) item;
			IExtensions extensions = (IExtensions) extension.getParent();
			int index = extensions.getIndexOf(extension);
			int size = extensions.getExtensions().length;
			if (index > 0)
				upEnabled = true;
			if (index < size - 1)
				downEnabled = true;
		}
		
		getTreePart().setButtonEnabled(3, upEnabled);
		getTreePart().setButtonEnabled(4, downEnabled);
	}

	protected TreeViewer createTreeViewer(Composite parent, int style) {
		FilteredTree tree = new FormFilteredTree(parent, style, new PatternFilter());
		parent.setData("filtered", Boolean.TRUE); //$NON-NLS-1$
		return tree.getViewer();
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource())) {
			StructuredViewer viewer = getStructuredViewerPart().getViewer();
			IStructuredSelection ssel = (IStructuredSelection)viewer.getSelection();
			updateUpDownButtons(ssel.size() != 1 ? null : ssel.getFirstElement());
		}
	}

	protected void selectExtensionElement(ISelection selection) {
		fExtensionTree.setSelection(selection, true);
	}
}
