package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import java.util.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.editor.*;


public class GrammarSection extends PDEFormSection {
	private FormWidgetFactory factory;
	private TreeViewer treeViewer;
	private Label dtdLabel;
	private Image elementRefImage;
	private Image groupImage;
	public static final String SECTION_TITLE = "SchemaEditor.GrammarSection.title";
	public static final String SECTION_COMPOSITOR = "SchemaEditor.GrammarSection.compositor";
	public static final String SECTION_REFERENCE = "SchemaEditor.GrammarSection.reference";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String SECTION_DESC = "SchemaEditor.GrammarSection.desc";
	public static final String KEY_DTD = "SchemaEditor.GrammarSection.dtd";
	private Image sequenceImage;
	private Image choiceImage;
	private Image allImage;

	class GrammarContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			Object[] children = null;
			if (parent instanceof ISchemaElement
				&& !(parent instanceof SchemaElementReference)) {
				ISchemaType type = ((ISchemaElement) parent).getType();
				if (type instanceof ISchemaComplexType) {
					Object compositor = ((ISchemaComplexType) type).getCompositor();
					if (compositor != null) {
						children = new Object[1];
						children[0] = compositor;
					}
				}
			} else
				if (parent instanceof ISchemaCompositor) {
					children = ((ISchemaCompositor) parent).getChildren();
				}
			if (children == null)
				children = new Object[0];
			return children;
		}
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
	}
	class GrammarLabelProvider extends LabelProvider {
		public String getText(Object o) {
			String text = ((ISchemaObject) o).getName();
			if (o instanceof ISchemaRepeatable) {
				ISchemaRepeatable rso = (ISchemaRepeatable) o;
				boolean unbounded = rso.getMaxOccurs() == Integer.MAX_VALUE;
				int maxOccurs = rso.getMaxOccurs();
				int minOccurs = rso.getMinOccurs();
				if (maxOccurs != 1 || minOccurs != 1) {
					text += " (" + minOccurs + " - ";
					if (unbounded)
						text += "*)";
					else
						text += maxOccurs + ")";
				}
			}
			return text;
		}
		public Image getImage(Object o) {
			if (o instanceof ISchemaElement) {
				return elementRefImage;
			}
			if (o instanceof ISchemaCompositor) {
				ISchemaCompositor compositor = (ISchemaCompositor) o;
				switch (compositor.getKind()) {
					case ISchemaCompositor.ALL :
						return allImage;
					case ISchemaCompositor.CHOICE :
						return choiceImage;
					case ISchemaCompositor.SEQUENCE :
						return sequenceImage;
					case ISchemaCompositor.GROUP :
						return groupImage;
				}
			}
			return null;
		}
	}

public GrammarSection(PDEFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	container.setLayout(layout);

	Control tree = createTree(container);
	GridData gd = new GridData(GridData.FILL_BOTH);
	if (SWT.getPlatform().equals("motif")==false)
	   gd.heightHint = 150;
	//gd.widthHint = 200;
	tree.setLayoutData(gd);

	dtdLabel = factory.createLabel(container, "", SWT.WRAP);
	dtdLabel.setForeground(factory.getColor(factory.DEFAULT_HEADER_COLOR));
	gd = new GridData(GridData.FILL_BOTH);
	dtdLabel.setLayoutData(gd);
	updateDTDLabel(null);

	initializeImages();
	factory.paintBordersFor(container);
	return container;
}
private Control createTree(Composite parent) {
	Tree tree = factory.createTree(parent, SWT.SINGLE);

	treeViewer = new TreeViewer(tree);
	treeViewer.setLabelProvider(new GrammarLabelProvider());
	treeViewer.setContentProvider(new GrammarContentProvider());
	treeViewer.setAutoExpandLevel(999);
	treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent e) {
			getFormPage().setSelection(e.getSelection());
		}
	});
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
	return tree;
}
public void dispose() {
	ISchema schema = (ISchema) getFormPage().getModel();
	schema.removeModelChangedListener(this);
	elementRefImage.dispose();
	groupImage.dispose();
	sequenceImage.dispose();
	choiceImage.dispose();
	allImage.dispose();
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		ISelection sel = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) sel).getFirstElement();
		if (obj != null)
			handleDelete(obj);
	}
}
protected void fillContextMenu(IMenuManager manager) {
	ISelection selection = treeViewer.getSelection();
	final Object object = ((IStructuredSelection) selection).getFirstElement();
	ISchemaElement sourceElement = (ISchemaElement) treeViewer.getInput();
	ISchema schema = sourceElement.getSchema();

	MenuManager submenu = new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));
	MenuManager cmenu = new MenuManager(PDEPlugin.getResourceString(SECTION_COMPOSITOR));

	cmenu.add(
		new NewCompositorAction(sourceElement, object, ISchemaCompositor.ALL));
	cmenu.add(
		new NewCompositorAction(sourceElement, object, ISchemaCompositor.CHOICE));
	cmenu.add(
		new NewCompositorAction(sourceElement, object, ISchemaCompositor.SEQUENCE));
	cmenu.add(
		new NewCompositorAction(sourceElement, object, ISchemaCompositor.GROUP));
	submenu.add(cmenu);

	if (schema.getElementCount() > 1
		&& object != null
		&& object instanceof SchemaCompositor) {
		MenuManager refMenu = new MenuManager(PDEPlugin.getResourceString(SECTION_REFERENCE));
		ISchemaElement[] elements = schema.getElements();
		for (int i = 0; i < elements.length; i++) {
			ISchemaElement element = elements[i];
			if (element == sourceElement)
				continue;
			refMenu.add(new NewReferenceAction(sourceElement, object, element));
		}
		submenu.add(refMenu);
	}
	if (object == null || object instanceof SchemaCompositor) {
		manager.add(submenu);
	}

	if (object != null) {
		manager.add(new Separator());
		Action deleteAction = new Action() {
			public void run() {
				handleDelete(object);
			}
		};
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
		manager.add(deleteAction);
	}
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
	manager.add(new Separator());
	manager.add(new PropertiesAction(getFormPage().getEditor()));
}
private void handleDelete(Object object) {
	if (object instanceof SchemaCompositor) {
		SchemaCompositor compositor = (SchemaCompositor)object;
		ISchemaObject parent = compositor.getParent();
		if (parent instanceof ISchemaElement) {
			// root
			SchemaElement element = (SchemaElement)parent;
			SchemaComplexType complexType = (SchemaComplexType)element.getType();
			if (complexType.getAttributeCount()==0) 
			   element.setType(new SchemaSimpleType(element.getSchema(), "string"));
			else
			   complexType.setCompositor(null);
		}
		else if (parent instanceof SchemaCompositor) {
			((SchemaCompositor)parent).removeChild(compositor);
		}
	}
	else if (object instanceof SchemaElementReference) {
		SchemaCompositor compositor = (SchemaCompositor)((SchemaElementReference)object).getCompositor();
		compositor.removeChild((SchemaElementReference)object);
	}
}
public void initialize(Object input) {
	ISchema schema = (ISchema)input;
	schema.addModelChangedListener(this);
}
private void initializeImages() {
	elementRefImage = PDEPluginImages.DESC_ELREF_SC_OBJ.createImage();
	groupImage = PDEPluginImages.DESC_GROUP_SC_OBJ.createImage();
	sequenceImage = PDEPluginImages.DESC_SEQ_SC_OBJ.createImage();
	choiceImage = PDEPluginImages.DESC_CHOICE_SC_OBJ.createImage();
	allImage = PDEPluginImages.DESC_ALL_SC_OBJ.createImage();
}
public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		treeViewer.refresh();
		return;
	}
	Object obj = e.getChangedObjects()[0];
	if (obj instanceof ISchemaCompositor
		|| obj instanceof ISchemaObjectReference) {
		ISchemaObject sobj = (ISchemaObject) obj;
		ISchemaObject parent = sobj.getParent();
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			treeViewer.update(sobj, null);
		} else
			if (e.getChangeType() == IModelChangedEvent.INSERT) {
				treeViewer.add(parent, sobj);
				treeViewer.setSelection(new StructuredSelection(obj), true);
			} else
				if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					treeViewer.setSelection(new StructuredSelection(parent), true);
					treeViewer.remove(sobj);
				}
	}
	updateDTDLabel((ISchemaObject) treeViewer.getInput());
}
public void sectionChanged(FormSection source, int changeType, Object changeObject) {
	if (!(source instanceof ElementSection)) return;
	if (changeType!=FormSection.SELECTION) return;
	if (changeObject instanceof ISchemaAttribute) {
		changeObject = ((ISchemaAttribute)changeObject).getParent();
	}
	if (changeObject==treeViewer.getInput()) return;
	ISchemaObject element = (ISchemaObject)changeObject;
	updateDTDLabel(element);
	treeViewer.setInput(changeObject);
}
private void updateDTDLabel(ISchemaObject object) {
	String prefix = PDEPlugin.getResourceString(KEY_DTD)+"\n";
	String text = "";
	if (object != null) {
		ISchemaElement element = (ISchemaElement) object;
		text = element.getDTDRepresentation();
	}
	dtdLabel.setText(prefix + text);
}
}
