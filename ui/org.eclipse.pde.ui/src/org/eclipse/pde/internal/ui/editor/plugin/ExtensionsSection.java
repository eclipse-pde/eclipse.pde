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
package org.eclipse.pde.internal.ui.editor.plugin;
import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.extension.*;
import org.eclipse.pde.ui.IExtensionEditorWizard;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.part.*;

/**
 *
 */
public class ExtensionsSection extends TreeSection
		implements
			IModelChangedListener {
	//private TableTreeViewer extensionTree;
	private TreeViewer extensionTree;
	private Image extensionImage;
	public static final String SECTION_TITLE = "ManifestEditor.DetailExtension.title"; //$NON-NLS-1$
	public static final String SECTION_NEW = "ManifestEditor.DetailExtension.new"; //$NON-NLS-1$
	public static final String SECTION_EDIT = "ManifestEditor.DetailExtension.edit"; //$NON-NLS-1$
	public static final String SECTION_DOWN = "ManifestEditor.DetailExtension.down"; //$NON-NLS-1$
	public static final String SECTION_UP = "ManifestEditor.DetailExtension.up"; //$NON-NLS-1$
	public static final String SECTION_SHOW_CHILDREN = "ManifestEditor.DetailExtension.showAllChildren"; //$NON-NLS-1$
	public static final String POPUP_NEW = "Menus.new.label"; //$NON-NLS-1$
	public static final String POPUP_EDIT = "Menus.edit.label"; //$NON-NLS-1$
	public static final String POPUP_NEW_EXTENSION = "ManifestEditor.DetailExtension.newExtension"; //$NON-NLS-1$
	public static final String POPUP_COLLAPSE_ALL = "ManifestEditor.DetailExtension.collapseAll"; //$NON-NLS-1$
	public static final String POPUP_GO_TO = "Menus.goTo.label"; //$NON-NLS-1$
	public static final String POPUP_DELETE = "Actions.delete.label"; //$NON-NLS-1$
	private Image genericElementImage;

	private SchemaRegistry schemaRegistry;
	private ExternalModelManager pluginInfoRegistry;
	private DrillDownAdapter drillDownAdapter;
	private Action newExtensionAction;
	private Action collapseAllAction;
	private Hashtable editorWizards;
	private static final String[] COMMON_LABEL_PROPERTIES = {"label", "name", //$NON-NLS-1$ //$NON-NLS-2$
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
				PDEPlugin.getResourceString(SECTION_NEW), 
				PDEPlugin.getResourceString(SECTION_EDIT),
				null,
				PDEPlugin.getResourceString(SECTION_UP),
				PDEPlugin.getResourceString(SECTION_DOWN)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		pluginInfoRegistry = PDECore.getDefault().getExternalModelManager();
		handleDefaultButton = false;
	}
	private static void addItemsForExtensionWithSchema(MenuManager menu,
			IPluginExtension extension, IPluginParent parent) {
		ISchema schema = getSchema(extension);
		String tagName = (parent == extension ? "extension" : parent.getName()); //$NON-NLS-1$
		ISchemaElement elementInfo = schema.findElement(tagName);
		if (elementInfo == null)
			return;
		ISchemaElement[] candidates = schema.getCandidateChildren(elementInfo);
		for (int i = 0; i < candidates.length; i++) {
			ISchemaElement candidateInfo = candidates[i];
			Action action = new NewElementAction(candidateInfo, parent);
			menu.add(action);
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
		extensionTree = treePart.getTreeViewer();
		extensionTree.setContentProvider(new ExtensionContentProvider());
		extensionTree.setLabelProvider(new ExtensionLabelProvider());
		drillDownAdapter = new DrillDownAdapter(extensionTree);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize((IPluginModelBase) getPage().getModel());
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
		editorWizards = null;
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
			extensionTree.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = extensionTree.getSelection();
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
			delAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
			manager.add(delAction);
			manager.add(new Separator());
			delAction.setEnabled(isEditable());
		}
		if (newMenu == null) {
			newMenu = new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));
			manager.add(newMenu);
		}
		if (!newMenu.isEmpty())
			newMenu.add(new Separator());
		newMenu.add(newExtensionAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		manager.add(collapseAllAction);
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().addClipboardActions(manager);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager, false);
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
		MenuManager menu = new MenuManager(PDEPlugin
				.getResourceString(POPUP_NEW));
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
			Action deleteAction = new Action(PDEPlugin
					.getResourceString(POPUP_DELETE)) {
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
		IStructuredSelection sel = (IStructuredSelection) extensionTree
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
		BusyIndicator.showWhile(extensionTree.getTree().getDisplay(),
				new Runnable() {
					public void run() {
						NewExtensionWizard wizard = new NewExtensionWizard(
								project, (IPluginModelBase) getPage()
										.getModel(), (ManifestEditor)getPage().getPDEEditor());
						WizardDialog dialog = new WizardDialog(PDEPlugin
								.getActiveWorkbenchShell(), wizard) {
							protected void finishPressed() {
								((ManifestEditor)getPage().getEditor()).ensurePluginContextPresence();
								super.finishPressed();
							}
						};
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
			BusyIndicator.showWhile(extensionTree.getTree().getDisplay(),
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
		final IStructuredSelection selection = (IStructuredSelection)extensionTree.getSelection();
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
			BusyIndicator.showWhile(extensionTree.getTree().getDisplay(),
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
		if (editorWizards==null)
			loadExtensionWizards();
		return (ArrayList)editorWizards.get(pointId);
	}

	private void loadExtensionWizards() {
		editorWizards = new Hashtable();
		IConfigurationElement [] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.pde.ui.newExtension"); //$NON-NLS-1$
		for (int i=0; i<elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("editorWizard")) { //$NON-NLS-1$
				String pointId = element.getAttribute("point"); //$NON-NLS-1$
				if (pointId==null) continue;
				ArrayList list = (ArrayList)editorWizards.get(pointId);
				if (list==null) {
					list = new ArrayList();
					editorWizards.put(pointId, list);
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
	void handleCollapseAll() {
		getTreePart().getTreeViewer().collapseAll();
	}
	public void initialize(IPluginModelBase model) {
		extensionTree.setInput(model.getPluginBase());
		selectFirstExtension();
		boolean editable = model.isEditable();
		TreePart treePart = getTreePart();
		treePart.setButtonEnabled(0, editable);
		treePart.setButtonEnabled(1, false);
		treePart.setButtonEnabled(3, false);
		treePart.setButtonEnabled(4, false);
		model.addModelChangedListener(this);
		newExtensionAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newExtensionAction.setText(PDEPlugin
				.getResourceString(POPUP_NEW_EXTENSION));
		newExtensionAction
				.setImageDescriptor(PDEPluginImages.DESC_EXTENSION_OBJ);
		newExtensionAction.setEnabled(editable);
		collapseAllAction = new Action() {
			public void run() {
				handleCollapseAll();
			}
		};
		collapseAllAction.setText(PDEPlugin
				.getResourceString(POPUP_COLLAPSE_ALL));
	}
	private void selectFirstExtension() {
		Tree tree = extensionTree.getTree();
		TreeItem [] items = tree.getItems();
		if (items.length==0) return;
		TreeItem firstItem = items[0];
		Object obj = firstItem.getData();
		extensionTree.setSelection(new StructuredSelection(obj));
	}
	void fireSelection() {
		extensionTree.setSelection(extensionTree.getSelection());
	}
	public void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		extensionImage = provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
		genericElementImage = provider
				.get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	}
	public void refresh() {
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		extensionTree.setInput(model.getPluginBase());
		selectFirstExtension();
		getManagedForm().fireSelectionChanged(ExtensionsSection.this,
						extensionTree.getSelection());
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
						IPluginBase.P_EXTENSION_ORDER)) {
			IStructuredSelection sel = (IStructuredSelection) extensionTree
					.getSelection();
			IPluginExtension extension = (IPluginExtension) sel
					.getFirstElement();
			extensionTree.refresh();
			extensionTree.setSelection(new StructuredSelection(extension));
			return;
		}
		if (changeObject instanceof IPluginExtension
				|| (changeObject instanceof IPluginElement && ((IPluginElement)changeObject).getParent() instanceof IPluginParent)) {
			IPluginObject pobj = (IPluginObject) changeObject;
			IPluginObject parent = changeObject instanceof IPluginExtension
					? ((IPluginModelBase) getPage().getModel()).getPluginBase()
					: pobj.getParent();
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				extensionTree.add(parent, pobj);
				extensionTree.setSelection(
						new StructuredSelection(changeObject), true);
				extensionTree.getTree().setFocus();
				// defect 16606: update property sheet
				//asyncResendSelection(getPage().getSelection());
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				extensionTree.remove(pobj);
			} else {
				if (event.getChangedProperty().equals(
						IPluginParent.P_SIBLING_ORDER)) {
					IStructuredSelection sel = (IStructuredSelection) extensionTree
							.getSelection();
					IPluginObject child = (IPluginObject) sel.getFirstElement();
					extensionTree.refresh(child.getParent());
					extensionTree.setSelection(new StructuredSelection(child));
				} else {
					extensionTree.update(changeObject, null);
					/*
					 * if (extensionTree.getTree().isFocusControl()) {
					 * ISelection sel = getFormPage().getSelection(); if (sel !=
					 * null && sel instanceof IStructuredSelection) {
					 * IStructuredSelection ssel = (IStructuredSelection) sel;
					 * if (!ssel.isEmpty() &&
					 * ssel.getFirstElement().equals(changeObject)) { // update
					 * property sheet asyncResendSelection(sel); } } }
					 */
				}
			}
		}
	}
	/*
	 * private void asyncResendSelection(final ISelection sel) {
	 * extensionTree.getControl().getDisplay().asyncExec(new Runnable() {
	 * public void run() { getFormPage().setSelection(sel); } }); }
	 */
	private Image resolveObjectImage(Object obj) {
		if (obj instanceof IPluginExtension) {
			return extensionImage;
		}
		Image elementImage = genericElementImage;
		if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			Image customImage = getCustomImage(element);
			if (customImage != null)
				elementImage = customImage;
			String bodyText = element.getText();
			boolean hasBodyText = bodyText!=null&&bodyText.length()>0;
			if (hasBodyText) {
				elementImage = PDEPlugin.getDefault().getLabelProvider().get(
						elementImage, PDELabelProvider.F_EDIT);
			}
		}
		return elementImage;
	}
	static Image getCustomImage(IPluginElement element) {
		if (isStorageModel(element)) return null;
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
	private static boolean isStorageModel(IPluginObject object) {
		IPluginModelBase modelBase = object.getPluginModel();
		return modelBase.getInstallLocation()==null;
	}
	
	private static Image getImageFromPlugin(IPluginElement element,
			String iconPathName) {
		IPluginModelBase model = element.getPluginModel();
		if (model == null)
			return null;
		// 39283 - ignore icon paths that
		// point at plugin.properties
		if (iconPathName.startsWith("%")) //$NON-NLS-1$
			return null;
		URL modelURL = null;
		String path = model.getInstallLocation();
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IPath realPath = resource.getLocation().removeLastSegments(1);
			path = realPath.toOSString();
		}
		try {
			if (!path.startsWith("file:")) //$NON-NLS-1$
				path = "file:" + path; //$NON-NLS-1$
			modelURL = new URL(path + File.separator);
			return PDEPlugin.getDefault().getLabelProvider().getImageFromURL(
					modelURL, iconPathName);
		} catch (MalformedURLException e) {
			return null;
		}
	}
	private String resolveObjectName(Object obj) {
		return resolveObjectName(getSchemaRegistry(), pluginInfoRegistry, obj);
	}
	
	private SchemaRegistry getSchemaRegistry() {
		if (schemaRegistry == null)
			schemaRegistry = PDECore.getDefault().getSchemaRegistry();
		return schemaRegistry;
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
			else
				return fullName != null
						? (fullName + " (" + baseName + ")") //$NON-NLS-1$ //$NON-NLS-2$
						: baseName;
		}
		return obj.toString();
	}
	public void setFocus() {
		if (extensionTree != null)
			extensionTree.getTree().setFocus();
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
		IStructuredSelection sel = (IStructuredSelection) extensionTree
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
			IPluginBase plugin = (IPluginBase) extension.getPluginBase();
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
		boolean upEnabled = false;
		boolean downEnabled = false;
		if (item != null) {
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
		}
		getTreePart().setButtonEnabled(3, upEnabled);
		getTreePart().setButtonEnabled(4, downEnabled);
	}
}
