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
package org.eclipse.pde.internal.ui.neweditor.schema;

import java.util.Iterator;

import org.eclipse.jface.action.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.TreeSection;
import org.eclipse.pde.internal.ui.newparts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class ElementSection extends TreeSection {
	private TreeViewer treeViewer;
	private Schema schema;
	private NewElementAction newElementAction = new NewElementAction();
	private NewAttributeAction newAttributeAction = new NewAttributeAction();
	private Clipboard clipboard;
	public static final String SECTION_TITLE =
		"SchemaEditor.ElementSection.title";
	public static final String SECTION_DESC =
		"SchemaEditor.ElementSection.desc";
	public static final String SECTION_NEW_ELEMENT =
		"SchemaEditor.ElementSection.newElement";
	public static final String SECTION_NEW_ATTRIBUTE =
		"SchemaEditor.ElementSection.newAttribute";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	//private PropertiesAction propertiesAction;

	class ContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getElements(Object object) {
			Schema schema = (Schema) object;
			return schema.getElements();
		}
		public Object[] getChildren(Object parent) {
			if (parent instanceof ISchemaElement) {
				return ((ISchemaElement) parent).getAttributes();
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof ISchemaObject) {
				return ((ISchemaObject) child).getParent();
			}
			return null;
		}
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}
	}

	public ElementSection(PDEFormPage page, Composite parent) {
		super(
			page,
			parent,
			Section.DESCRIPTION,
			new String[] {
				PDEPlugin.getResourceString(SECTION_NEW_ELEMENT),
				PDEPlugin.getResourceString(SECTION_NEW_ATTRIBUTE)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createTree(container, toolkit);
		toolkit.paintBordersFor(container);
		//propertiesAction = new PropertiesAction(getFormPage().getEditor());
		section.setClient(container);
		initialize();
	}

	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		treeViewer = treePart.getTreeViewer();
		treeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		treeViewer.setContentProvider(new ContentProvider());
		treeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		initDragAndDrop();
	}
	
	private void initDragAndDrop() {
		clipboard = new Clipboard(treeViewer.getControl().getDisplay());
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers =
			new Transfer[] {
				ModelDataTransfer.getInstance(),
				TextTransfer.getInstance()};
		treeViewer.addDragSupport(
			ops,
			transfers,
			new ElementSectionDragAdapter((ISelectionProvider) treeViewer, this));
		treeViewer.addDropSupport(
			ops | DND.DROP_DEFAULT,
			transfers,
			new ElementSectionDropAdapter(this));
	}
	
	TreeViewer getTreeViewer() {
		return treeViewer;
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNewElement();
		else if (index == 1)
			handleNewAttribute();
	}

	public void dispose() {
		schema.removeModelChangedListener(this);
		if (clipboard!=null) {
			clipboard.dispose();
			clipboard = null;
		}
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			ISelection sel = treeViewer.getSelection();
			Object obj = ((IStructuredSelection) sel).getFirstElement();
			if (obj != null)
				handleDelete(obj);
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			ISelection sel = treeViewer.getSelection();
			Object obj = ((IStructuredSelection) sel).getFirstElement();
			if (obj != null)
				handleDelete(obj);
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}
	public void expandTo(Object object) {
		if (object instanceof ISchemaElement
			|| object instanceof ISchemaAttribute) {
			treeViewer.setSelection(new StructuredSelection(object), true);
		}
	}
	protected void fillContextMenu(IMenuManager manager) {
		final ISelection selection = treeViewer.getSelection();
		final Object object =
			((IStructuredSelection) selection).getFirstElement();

		MenuManager submenu =
			new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));
		if (object == null || object instanceof SchemaElement) {
			newElementAction.setSchema(schema);
			newElementAction.setEnabled(schema.isEditable());
			submenu.add(newElementAction);
		}
		if (object != null) {
			SchemaElement element;
			if (object instanceof SchemaElement)
				element = (SchemaElement) object;
			else
				element =
					(SchemaElement) ((SchemaAttribute) object).getParent();
			if (element.getName().equals("extension") == false) {
				newAttributeAction.setElement(element);
				newAttributeAction.setEnabled(schema.isEditable());
				submenu.add(newAttributeAction);
			}
		}
		manager.add(submenu);
		if (!selection.isEmpty()) {
			if (!(object instanceof SchemaElement)
				|| ((SchemaElement) object).getName().equals("extension")
					== false) {
				manager.add(new Separator());
				Action deleteAction = new Action() {
					public void run() {
						handleDelete((IStructuredSelection) selection);
					}
				};
				deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
				deleteAction.setEnabled(schema.isEditable());
				manager.add(deleteAction);
			}
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		//manager.add(propertiesAction);
	}

	private void handleDelete(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			handleDelete(iter.next());
		}
	}

	void handleDelete(Object object) {
		ISchemaObject sobject = (ISchemaObject) object;
		ISchemaObject parent = sobject.getParent();

		if (sobject instanceof ISchemaElement) {
			Schema schema = (Schema) parent;
			schema.removeElement((ISchemaElement) sobject);
			schema.updateReferencesFor((ISchemaElement)sobject, ISchema.REFRESH_DELETE);
		} else if (sobject instanceof ISchemaAttribute) {
			SchemaElement element = (SchemaElement) parent;
			SchemaComplexType type = (SchemaComplexType) element.getType();
			type.removeAttribute((ISchemaAttribute) sobject);
		}
	}

	private void handleNewAttribute() {
		Object object =
			((IStructuredSelection) treeViewer.getSelection())
				.getFirstElement();

		if (object != null) {
			SchemaElement element;
			if (object instanceof SchemaElement)
				element = (SchemaElement) object;
			else
				element =
					(SchemaElement) ((SchemaAttribute) object).getParent();
			if (element.getName().equals("extension") == false) {
				newAttributeAction.setElement(element);
				newAttributeAction.run();
			}
		}
	}

	private void handleNewElement() {
		newElementAction.setSchema(schema);
		newElementAction.run();
	}
	public void initialize() {
		this.schema = (Schema) getPage().getModel();
		treeViewer.setInput(schema);
		schema.addModelChangedListener(this);
		getTreePart().setButtonEnabled(0, schema.isEditable());
		getTreePart().setButtonEnabled(1, false);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof ISchemaObjectReference)
			return;
		if (obj instanceof ISchemaElement || obj instanceof ISchemaAttribute) {
			if (e.getChangeType() == IModelChangedEvent.CHANGE) {
				treeViewer.update(obj, null);
			} else {
				if (e.getChangeType() == IModelChangedEvent.INSERT) {
					ISchemaObject sobj = (ISchemaObject) obj;
					ISchemaObject parent = sobj.getParent();
					treeViewer.add(parent, sobj);
					treeViewer.setSelection(new StructuredSelection(obj), true);
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					ISchemaObject sobj = (ISchemaObject) obj;
					ISchemaObject parent = sobj.getParent();
					treeViewer.remove(obj);
					treeViewer.setSelection(
						new StructuredSelection(parent),
						true);
				}
			}
		}
	}
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getManagedForm().fireSelectionChanged(this, selection);
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

	public void setFocus() {
		treeViewer.getTree().setFocus();
		getPage().getPDEEditor().setSelection(treeViewer.getSelection());
	}
	private void updateButtons() {
		if (schema.isEditable() == false)
			return;
		Object object =
			((IStructuredSelection) treeViewer.getSelection())
				.getFirstElement();
		ISchemaObject sobject = (ISchemaObject) object;

		boolean canAddAttribute = false;
		if (sobject != null) {
			String name = sobject.getName();
			if (sobject instanceof ISchemaElement) {
				if (name.equals("extension") == false)
					canAddAttribute = true;
			} else if (sobject instanceof ISchemaAttribute) {
				ISchemaElement element = (ISchemaElement) (sobject.getParent());
				if (element.getName().equals("extension") == false)
					canAddAttribute = true;
			}
		}
		getTreePart().setButtonEnabled(1, canAddAttribute);
	}

	public void doPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			Object realTarget = getRealTarget(target, object);
			Object sibling = getSibling(target, object);
			if (realTarget == null)
				continue;
			doPaste(realTarget, sibling, object);
		}
	}
	
	private Object getSibling(Object target, Object object) {
		if (target instanceof ISchemaElement && object instanceof ISchemaElement)
			return target;
		if (target instanceof ISchemaAttribute && object instanceof ISchemaAttribute)
			return target;
		return null;
	}

	private Object getRealTarget(Object target, Object object) {
		if (object instanceof ISchemaElement) {
			return schema;
		}
		if (object instanceof ISchemaAttribute) {
			if (target instanceof ISchemaAttribute) {
				// add it to the parent of the selected attribute
				return ((ISchemaAttribute) target).getParent();
			}
			if (target instanceof ISchemaElement)
				return target;
		}
		return null;
	}

	private void doPaste(Object realTarget, Object sibling, Object object) {
		if (object instanceof ISchemaElement) {
			SchemaElement element = (SchemaElement) object;
			element.setParent(schema);
			schema.addElement(element, (ISchemaElement)sibling);
			schema.updateReferencesFor(element, ISchema.REFRESH_ADD);
		} else if (object instanceof ISchemaAttribute) {
			SchemaElement element = (SchemaElement) realTarget;
			SchemaAttribute attribute = (SchemaAttribute) object;
			attribute.setParent(element);
			ISchemaType type = element.getType();
			SchemaComplexType complexType = null;
			if (!(type instanceof ISchemaComplexType)) {
				complexType = new SchemaComplexType(element.getSchema());
				element.setType(complexType);
			} else {
				complexType = (SchemaComplexType) type;
			}
			complexType.addAttribute(attribute, (ISchemaAttribute)sibling);
		}
	}

	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			Object obj = objects[i];
			// Attributes can only paste into elements
			if (obj instanceof ISchemaAttribute) {
				if (target instanceof ISchemaAttribute
					|| target instanceof ISchemaElement)
					continue;
			} else if (obj instanceof ISchemaElement) {
				continue;
			}
			return false;
		}
		return true;
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
		//propertiesAction.run();
	}
}
