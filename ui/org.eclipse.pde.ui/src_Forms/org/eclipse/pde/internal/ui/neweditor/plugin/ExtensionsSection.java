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
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.io.File;
import java.net.*;
import java.util.Iterator;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.manifest.NewElementAction;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.TreePart;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.part.DrillDownAdapter;
public class ExtensionsSection extends TreeSection
		implements
			IModelChangedListener {
	//private TableTreeViewer extensionTree;
	private TreeViewer extensionTree;
	private Image extensionImage;
	public static final String SECTION_TITLE = "ManifestEditor.DetailExtensionSection.title";
	public static final String SECTION_NEW = "ManifestEditor.DetailExtensionSection.new";
	public static final String SECTION_DOWN = "ManifestEditor.DetailExtensionSection.down";
	public static final String SECTION_UP = "ManifestEditor.DetailExtensionSection.up";
	public static final String SECTION_SHOW_CHILDREN = "ManifestEditor.DetailExtensionSection.showAllChildren";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_NEW_EXTENSION = "ManifestEditor.DetailExtensionSection.newExtension";
	public static final String POPUP_COLLAPSE_ALL = "ManifestEditor.DetailExtensionSection.collapseAll";
	public static final String POPUP_GO_TO = "Menus.goTo.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private Image genericElementImage;
	//private Button showAllChildrenButton;
	private SchemaRegistry schemaRegistry;
	private ExternalModelManager pluginInfoRegistry;
	private DrillDownAdapter drillDownAdapter;
	private Action newExtensionAction;
	private Action collapseAllAction;
	private static final String[] COMMON_LABEL_PROPERTIES = {"label", "name",
			"id"};
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
			if (child instanceof IPluginObject)
				return ((IPluginObject) child).getParent();
			return null;
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
	}
	class ExtensionLabelProvider extends LabelProvider
			implements
				ITableLabelProvider {
		public String getColumnText(Viewer v, Object obj, int index) {
			return getColumnText(obj, index);
		}
		public String getText(Object obj) {
			return getColumnText(obj, 1);
		}
		public Image getImage(Object obj) {
			return getColumnImage(obj, 1);
		}
		public String getColumnText(Object obj, int index) {
			if (index == 1) {
				return resolveObjectName(obj);
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 1) {
				return resolveObjectImage(obj);
			}
			return null;
		}
		public Image getColumnImage(Viewer v, Object obj, int index) {
			return getColumnImage(obj, index);
		}
	}
	public ExtensionsSection(PDEFormPage page, Composite parent) {
		super(page, parent, 0, new String[]{
				PDEPlugin.getResourceString(SECTION_NEW), null,
				PDEPlugin.getResourceString(SECTION_UP),
				PDEPlugin.getResourceString(SECTION_DOWN)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		schemaRegistry = PDECore.getDefault().getSchemaRegistry();
		pluginInfoRegistry = PDECore.getDefault().getExternalModelManager();
		handleDefaultButton = false;
	}
	private static void addItemsForExtensionWithSchema(MenuManager menu,
			IPluginExtension extension, IPluginParent parent) {
		ISchema schema = getSchema(extension);
		String tagName = (parent == extension ? "extension" : parent.getName());
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
		extensionTree.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		extensionTree.setContentProvider(new ExtensionContentProvider());
		extensionTree.setLabelProvider(new ExtensionLabelProvider());
		drillDownAdapter = new DrillDownAdapter(extensionTree);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize((IPluginModelBase) getPage().getModel());
	}
	protected void selectionChanged(IStructuredSelection selection) {
		//getFormPage().setSelection(selection);
		updateUpDownButtons(selection.getFirstElement());
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
			// noop
			case 2 :
				handleMove(true);
				break;
			case 3 :
				handleMove(false);
				break;
		}
	}
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
				.getAggregateModel();
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
	void handleNew() {
		final IProject project = getPage().getPDEEditor().getCommonProject();
		BusyIndicator.showWhile(extensionTree.getTree().getDisplay(),
				new Runnable() {
					public void run() {
						NewExtensionWizard wizard = new NewExtensionWizard(
								project, (IPluginModelBase) getPage()
										.getModel());
						WizardDialog dialog = new WizardDialog(PDEPlugin
								.getActiveWorkbenchShell(), wizard);
						dialog.create();
						SWTUtil.setDialogSize(dialog, 500, 500);
						dialog.open();
					}
				});
	}
	void handleCollapseAll() {
		getTreePart().getTreeViewer().collapseAll();
	}
	public void initialize(IPluginModelBase model) {
		extensionTree.setInput(model.getPluginBase());
		boolean editable = model.isEditable();
		TreePart treePart = getTreePart();
		treePart.setButtonEnabled(0, editable);
		treePart.setButtonEnabled(2, false);
		treePart.setButtonEnabled(3, false);
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
	public void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		extensionImage = provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
		genericElementImage = provider
				.get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	}
	public void refresh() {
		extensionTree.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				extensionTree.refresh();
				getForm().fireSelectionChanged(ExtensionsSection.this,
						extensionTree.getSelection());
			}
		});
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
				|| changeObject instanceof IPluginElement) {
			IPluginObject pobj = (IPluginObject) changeObject;
			IPluginObject parent = pobj.getParent();
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
			boolean hasBodyText = element.getText() != null;
			if (hasBodyText) {
				elementImage = PDEPlugin.getDefault().getLabelProvider().get(
						elementImage, PDELabelProvider.F_EDIT);
			}
		}
		return elementImage;
	}
	static Image getCustomImage(IPluginElement element) {
		// TODO this will break us with the source model
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
		IPluginModelBase model = element.getPluginModel();
		if (model == null)
			return null;
		// 39283 - ignore icon paths that
		// point at plugin.properties
		if (iconPathName.startsWith("%"))
			return null;
		URL modelURL = null;
		String path = model.getInstallLocation();
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IPath realPath = resource.getLocation().removeLastSegments(1);
			path = realPath.toOSString();
		}
		try {
			if (!path.startsWith("file:"))
				path = "file:" + path;
			modelURL = new URL(path + File.separator);
			return PDEPlugin.getDefault().getLabelProvider().getImageFromURL(
					modelURL, iconPathName);
		} catch (MalformedURLException e) {
			return null;
		}
	}
	private String resolveObjectName(Object obj) {
		return resolveObjectName(schemaRegistry, pluginInfoRegistry, obj);
	}
	public static String resolveObjectName(SchemaRegistry schemaRegistry,
			ExternalModelManager pluginInfoRegistry, Object obj) {
		boolean fullNames = PDEPlugin.isFullNameModeEnabled();
		if (obj instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) obj;
			if (!fullNames) {
				//defect 17026
				//if (extension.getName()!=null) return extension.getName();
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
			// try extension point declaration
			IPluginExtensionPoint pointInfo = pluginInfoRegistry
					.findExtensionPoint(extension.getPoint());
			if (pointInfo != null) {
				return pointInfo.getResourceString(pointInfo.getName());
			}
		} else if (obj instanceof IPluginElement) {
			String baseName = obj.toString();
			IPluginElement element = (IPluginElement) obj;
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
						? (fullName + " (" + baseName + ")")
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
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof IPluginExtension) {
					IPluginExtension extension = (IPluginExtension) obj;
					//TODO this will break with the XML model
					((PluginExtension) extension).setModel(model);
					((PluginExtension) extension).setParent(plugin);
					plugin.add(extension);
					((PluginParent) extension).reconnect();
				} else if (obj instanceof IPluginElement
						&& target instanceof IPluginParent) {
					PluginElement element = (PluginElement) obj;
					element.setModel(model);
					element.setParent((IPluginParent) target);
					((IPluginParent) target).add(element);
					if (element instanceof PluginParent)
						((PluginParent) element).reconnect();
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
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
			PluginBase plugin = (PluginBase) extension.getPluginBase();
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
		getTreePart().setButtonEnabled(2, upEnabled);
		getTreePart().setButtonEnabled(3, downEnabled);
	}
}