package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.jface.action.*;
import java.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.parts.TreePart;


public class ElementSection
	extends TreeSection {
	private TreeViewer treeViewer;
	private Schema schema;
	private NewElementAction newElementAction = new NewElementAction();
	private NewAttributeAction newAttributeAction = new NewAttributeAction();
	private ElementList elements;
	public static final String SECTION_TITLE = "SchemaEditor.ElementSection.title";
	public static final String SECTION_DESC = "SchemaEditor.ElementSection.desc";
	public static final String SECTION_NEW_ELEMENT = "SchemaEditor.ElementSection.newElement";
	public static final String SECTION_NEW_ATTRIBUTE = "SchemaEditor.ElementSection.newAttribute";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private FormWidgetFactory factory;

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

public ElementSection(PDEFormPage page) {
	super(page, new String [] { PDEPlugin.getResourceString(SECTION_NEW_ELEMENT), 
					PDEPlugin.getResourceString(SECTION_NEW_ATTRIBUTE) });
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	Composite container = createClientContainer(parent, 2, factory);
	createTree(container, factory);
	factory.paintBordersFor(container);
	return container;
}

private void createTree(Composite container, FormWidgetFactory factory) {
	TreePart treePart = getTreePart();
	createViewerPartControl(container, SWT.MULTI, 2, factory);
	treeViewer = treePart.getTreeViewer();
	treeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
	treeViewer.setContentProvider(new ContentProvider()); 
	treeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
}

protected void buttonSelected(int index) {
	if (index==0) handleNewElement();
	else if (index==1) handleNewAttribute();
}

public void dispose() {
	schema.removeModelChangedListener(this);
	super.dispose();
}

public boolean doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		ISelection sel = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) sel).getFirstElement();
		if (obj != null)
			handleDelete(obj);
		return true;
	}
	return false;
}
public void expandTo(Object object) {
	if (object instanceof ISchemaElement || object instanceof ISchemaAttribute) {
		treeViewer.setSelection(new StructuredSelection(object), true);
	}
}
protected void fillContextMenu(IMenuManager manager) {
	final ISelection selection = treeViewer.getSelection();
	final Object object = ((IStructuredSelection) selection).getFirstElement();

	MenuManager submenu = new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));
	if (object == null || object instanceof SchemaElement) {
		newElementAction.setSchema(schema);
		submenu.add(newElementAction);
	}
	if (object != null) {
		SchemaElement element;
		if (object instanceof SchemaElement)
			element = (SchemaElement) object;
		else
			element = (SchemaElement) ((SchemaAttribute) object).getParent();
		if (element.getName().equals("extension") == false) {
			newAttributeAction.setElement(element);
			submenu.add(newAttributeAction);
		}
	}
	manager.add(submenu);
	if (!selection.isEmpty()) {
		if (!(object instanceof SchemaElement)
			|| ((SchemaElement) object).getName().equals("extension")==false) {
			manager.add(new Separator());
			Action deleteAction = new Action() {
				public void run() {
					handleDelete(selection);
				}
			};
			deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
			manager.add(deleteAction);
		}
	}
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
	manager.add(new Separator());
	manager.add(new PropertiesAction(getFormPage().getEditor()));
}

private void handleDelete(IStructuredSelection selection) {
	for (Iterator iter=selection.iterator(); iter.hasNext();) {
		handleDelete(iter.next());
	}
}

private void handleDelete(Object object) {
	ISchemaObject sobject = (ISchemaObject)object;
	ISchemaObject parent = sobject.getParent();

	if (sobject instanceof ISchemaElement) {
		Schema schema = (Schema)parent;
		schema.removeElement((ISchemaElement)sobject);
	}
	else if (sobject instanceof ISchemaAttribute) {
		SchemaElement element = (SchemaElement)parent;
		SchemaComplexType type = (SchemaComplexType)element.getType();
		type.removeAttribute((ISchemaAttribute)sobject);
	}
}

private void handleNewAttribute() {
	Object object =
		((IStructuredSelection) treeViewer.getSelection()).getFirstElement();

	if (object != null) {
		SchemaElement element;
		if (object instanceof SchemaElement)
			element = (SchemaElement) object;
		else
			element = (SchemaElement) ((SchemaAttribute) object).getParent();
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
public void initialize(Object input) {
	this.schema = (Schema)input;
	treeViewer.setInput(input);
	schema.addModelChangedListener(this);
}

public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		treeViewer.refresh();
		return;
	}
	Object obj = e.getChangedObjects()[0];
	if (obj instanceof ISchemaObjectReference) return;
	if (obj instanceof ISchemaElement || obj instanceof ISchemaAttribute) {
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			treeViewer.update(obj, null);
		} else {
			if (e.getChangeType() == IModelChangedEvent.INSERT) {
				ISchemaObject sobj = (ISchemaObject) obj;
				ISchemaObject parent = sobj.getParent();
				treeViewer.add(parent, sobj);
				treeViewer.setSelection(new StructuredSelection(obj), true);
			} else
				if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					ISchemaObject sobj = (ISchemaObject) obj;
					ISchemaObject parent = sobj.getParent();
					treeViewer.remove(obj);
					treeViewer.setSelection(new StructuredSelection(parent), true);
				}
		}
	}
}
protected void selectionChanged(IStructuredSelection selection) {
	Object object = selection.getFirstElement();
	fireSelectionNotification(object);
	getFormPage().setSelection(selection);
	updateButtons();
}

public void setFocus() {
	treeViewer.getTree().setFocus();
	getFormPage().setSelection(treeViewer.getSelection());
}
private void updateButtons() {
	Object object =
		((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
	ISchemaObject sobject = (ISchemaObject) object;

	boolean canAddAttribute = false;
	if (sobject != null) {
		String name = sobject.getName();
		if (sobject instanceof ISchemaElement) {
			if (name.equals("extension") == false)
				canAddAttribute = true;
		} else
			if (sobject instanceof ISchemaAttribute) {
				ISchemaElement element = (ISchemaElement) (sobject.getParent());
				if (element.getName().equals("extension") == false)
					canAddAttribute = true;
			}
	}
	getTreePart().setButtonEnabled(1, canAddAttribute);
}
}
