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
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.*;


public class ElementSection
	extends PDEFormSection
	implements ISelectionChangedListener {
	private TreeViewer treeViewer;
	private Schema schema;
	private NewElementAction newElementAction = new NewElementAction();
	private NewAttributeAction newAttributeAction = new NewAttributeAction();
	private ElementList elements;
	private Button newAttributeButton;
	private Button newElementButton;
	private Image globalElementImage;
	public static final String SECTION_TITLE = "SchemaEditor.ElementSection.title";
	public static final String SECTION_DESC = "SchemaEditor.ElementSection.desc";
	public static final String SECTION_NEW_ELEMENT = "SchemaEditor.ElementSection.newElement";
	public static final String SECTION_NEW_ATTRIBUTE = "SchemaEditor.ElementSection.newAttribute";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private FormWidgetFactory factory;

	public class ElementLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getText(Object object) {
			return getColumnText(object, 1);
		}
		public String getColumnText(Object object, int column) {
			if (column != 1)
				return "";
			ISchemaObject sobj = (ISchemaObject) object;
			String text = sobj.getName();
			return text != null ? text : "";
		}
		public Image getImage(Object object) {
			return getColumnImage(object, 1);
		}
		public Image getColumnImage(Object object, int column) {
			if (column != 1)
				return null;
			if (object instanceof ISchemaElement)
				return globalElementImage;
			if (object instanceof ISchemaAttribute) {
				ISchemaAttribute att = (ISchemaAttribute) object;
				if (att.getKind() == ISchemaAttribute.JAVA)
					return PDEPluginImages.get(PDEPluginImages.IMG_ATT_CLASS_OBJ);
				if (att.getKind() == ISchemaAttribute.RESOURCE)
				    return PDEPluginImages.get(PDEPluginImages.IMG_ATT_FILE_OBJ);
				if (att.getUse() == ISchemaAttribute.REQUIRED)
					return PDEPluginImages.get(PDEPluginImages.IMG_ATT_REQ_OBJ);
				return PDEPluginImages.get(PDEPluginImages.IMG_ATT_IMPL_OBJ);
			}
			return null;
		}
	}

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
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	container.setLayout(layout);

	Control control = createTree(container, factory);
	GridData gd = new GridData(GridData.FILL_BOTH);
	control.setLayoutData(gd);

	Composite buttonContainer = factory.createComposite(container);
	layout = new GridLayout();
	layout.marginHeight = 0;
	buttonContainer.setLayout(layout);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);

	newElementButton =
		factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_NEW_ELEMENT), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	newElementButton.setLayoutData(gd);
	newElementButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleNewElement();
		}
	});
	newAttributeButton =
		factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_NEW_ATTRIBUTE), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	newAttributeButton.setLayoutData(gd);
	newAttributeButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleNewAttribute();
		}
	});

	factory.paintBordersFor(container);
	return container;
}
private Tree createTree(Composite container, FormWidgetFactory factory) {
	Tree tree = factory.createTree(container, SWT.NULL);

	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener () {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.setRemoveAllWhenShown(true);
	popupMenuManager.addMenuListener(listener);
	Menu menu=popupMenuManager.createContextMenu(tree);
	tree.setMenu(menu);

	treeViewer = new TreeViewer(tree);
	treeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
	treeViewer.setContentProvider(new ContentProvider()); 
	treeViewer.setLabelProvider(new ElementLabelProvider());
	treeViewer.addSelectionChangedListener(this);
	return tree;
}
public void dispose() {
	schema.removeModelChangedListener(this);
	globalElementImage.dispose();
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		ISelection sel = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) sel).getFirstElement();
		if (obj != null)
			handleDelete(obj);
	}
}
public void expandTo(Object object) {
	if (object instanceof ISchemaElement || object instanceof ISchemaAttribute) {
		treeViewer.setSelection(new StructuredSelection(object), true);
	}
}
protected void fillContextMenu(IMenuManager manager) {
	ISelection selection = treeViewer.getSelection();
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
	if (object != null) {
		if (!(object instanceof SchemaElement)
			|| ((SchemaElement) object).getName().equals("extension")==false) {
			manager.add(new Separator());
			Action deleteAction = new Action() {
				public void run() {
					handleDelete(object);
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
	initializeImages();
	this.schema = (Schema)input;
	treeViewer.setInput(input);
	schema.addModelChangedListener(this);
}
private void initializeImages() {
	globalElementImage = PDEPluginImages.DESC_GEL_SC_OBJ.createImage();
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
public void selectionChanged(SelectionChangedEvent event) {
	Object object = null;
	if (!event.getSelection().isEmpty()) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			object = ((IStructuredSelection)selection).getFirstElement();
		}
	}
	fireSelectionNotification(object);
	getFormPage().setSelection(event.getSelection());
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
	newAttributeButton.setEnabled(canAddAttribute);
}
}
