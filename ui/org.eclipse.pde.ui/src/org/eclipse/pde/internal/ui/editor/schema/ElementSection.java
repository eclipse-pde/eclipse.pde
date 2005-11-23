package org.eclipse.pde.internal.ui.editor.schema;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.ischema.ISchemaRootElement;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaComplexType;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.core.schema.SchemaSimpleType;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ElementSection extends TreeSection {
	private TreeViewer treeViewer;
	private Schema schema;
	private NewElementAction newElementAction = new NewElementAction();
	private NewAttributeAction newAttributeAction = new NewAttributeAction();
	private Clipboard clipboard;

	class ContentProvider extends DefaultContentProvider implements	ITreeContentProvider {
		public Object[] getElements(Object object) {
			if (object instanceof Schema) {
				Schema schema = (Schema) object;
				return schema.getElements();
			}
			return new Object[0];
		}

		public Object[] getChildren(Object parent) {
			Object[] children = new Object[0];
			if (parent instanceof ISchemaElement) {
				Object[] types = new Object[0];
				Object[] attributes = ((ISchemaElement) parent).getAttributes();
				ISchemaType type = ((ISchemaElement) parent).getType();
				if (type instanceof ISchemaComplexType) {
					Object compositor = ((ISchemaComplexType) type).getCompositor();
					if (compositor != null)
						types = new Object[] { compositor };
				}
				children = new Object[types.length + attributes.length];
				System.arraycopy(types, 0, children, 0, types.length);
				System.arraycopy(attributes, 0, children, types.length,	attributes.length);
			} else if (parent instanceof ISchemaCompositor) {
				children = ((ISchemaCompositor) parent).getChildren();
			}
			return children;
		}

		public Object getParent(Object child) {
			if (child instanceof ISchemaObject)
				return ((ISchemaObject) child).getParent();
			return null;
		}

		public boolean hasChildren(Object parent) {
			if (parent instanceof ISchemaAttribute
					|| parent instanceof ISchemaObjectReference)
				return false;
			return getChildren(parent).length > 0;
		}
	}

	public ElementSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {
				PDEUIMessages.SchemaEditor_ElementSection_newElement,
				PDEUIMessages.SchemaEditor_ElementSection_newAttribute });
		getSection().setText(PDEUIMessages.SchemaEditor_ElementSection_title);
		getSection().setDescription(PDEUIMessages.SchemaEditor_ElementSection_desc);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createTree(container, toolkit);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		treeViewer = treePart.getTreeViewer();
		treeViewer.setContentProvider(new ContentProvider());
		treeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
//		initDragAndDrop();
	}

	protected void initDragAndDrop() {
		clipboard = new Clipboard(treeViewer.getControl().getDisplay());
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] {ModelDataTransfer.getInstance(), TextTransfer.getInstance() };
		treeViewer.addDragSupport(ops, transfers, new ElementSectionDragAdapter(treeViewer, this));
		treeViewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, new ElementSectionDropAdapter(this));
	}

	TreeViewer getTreeViewer() {
		return treeViewer;
	}

	protected void buttonSelected(int index) {
		switch (index) {
		case 0:
			handleNewElement();
			break;
		case 1:
			handleNewAttribute();
			break;
		}
	}

	public void dispose() {
		if (clipboard != null) {
			clipboard.dispose();
			clipboard = null;
		}
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		boolean cut = actionId.equals(ActionFactory.CUT.getId());
		if (cut || actionId.equals(ActionFactory.DELETE.getId())) {
			ISelection sel = treeViewer.getSelection();
			Object obj = ((IStructuredSelection) sel).getFirstElement();
			if (obj != null)
				handleDelete(obj);
			// if cutting delete here and let the editor transfer
			// the selection to the clipboard
			return !cut;
		}
//		if (actionId.equals(ActionFactory.PASTE.getId())) {
//			doPaste();
//			return true;
//		}
		return false;
	}

	public boolean setFormInput(Object object) {
		if (object instanceof ISchemaElement
				|| object instanceof ISchemaAttribute
				|| object instanceof ISchemaCompositor) {
			treeViewer.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		final ISelection selection = treeViewer.getSelection();
		final Object object = ((IStructuredSelection) selection).getFirstElement();

		MenuManager submenu = new MenuManager(PDEUIMessages.Menus_new_label);
		if (object == null || object instanceof SchemaElement) {
			newElementAction.setSchema(schema);
			newElementAction.setEnabled(schema.isEditable());
			submenu.add(newElementAction);
		}
		if (object != null) {
			ISchemaElement element = null;
			if (object instanceof SchemaElement)
				element = (SchemaElement) object;
			else if (object instanceof SchemaAttribute)
				element = (SchemaElement) ((SchemaAttribute) object).getParent();
			
			if (element != null	&& !(element instanceof ISchemaRootElement)
					&& !(element instanceof ISchemaObjectReference)) { //$NON-NLS-1$
				newAttributeAction.setElement((SchemaElement) element);
				newAttributeAction.setEnabled(schema.isEditable());
				submenu.add(newAttributeAction);
			}
		}
		if (object instanceof SchemaElement || object instanceof SchemaCompositor) {
			ISchemaElement sourceElement = null;
			ISchemaObject schemaObject = (ISchemaObject) object;
			while (schemaObject != null) {
				if (schemaObject instanceof ISchemaElement) {
					sourceElement = (ISchemaElement) schemaObject;
					break;
				}
				schemaObject = schemaObject.getParent();
			}
			if (sourceElement != null) {
				ISchema schema = sourceElement.getSchema();
				MenuManager cmenu = new MenuManager(PDEUIMessages.ElementSection_compositorMenu);
				cmenu.add(new NewCompositorAction(sourceElement, object, ISchemaCompositor.CHOICE));
				cmenu.add(new NewCompositorAction(sourceElement, object, ISchemaCompositor.SEQUENCE));
				if (submenu.getItems().length > 0)
					submenu.add(new Separator());
				submenu.add(cmenu);
				if (object instanceof SchemaCompositor) {
					MenuManager refMenu = new MenuManager(PDEUIMessages.ElementSection_referenceMenu);
					ISchemaElement[] elements = schema.getResolvedElements();
					for (int i = 0; i < elements.length; i++) {
						refMenu.add(new NewReferenceAction(sourceElement,object, elements[i]));
					}
					if (!refMenu.isEmpty())
						submenu.add(refMenu);
				}
			}
		}
		manager.add(submenu);
		if (object != null) {
			if (!(object instanceof ISchemaRootElement)) { //$NON-NLS-1$
				manager.add(new Separator());
				Action deleteAction = new Action() {
					public void run() {
						handleDelete((IStructuredSelection) selection);
					}
				};
				deleteAction.setText(PDEUIMessages.Actions_delete_label);
				deleteAction.setEnabled(schema.isEditable());
				manager.add(deleteAction);
			}
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());
	}

	private void handleDelete(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			handleDelete(iter.next());
		}
	}

	void handleDelete(Object object) {
		ISchemaObject sobject = (ISchemaObject) object;
		ISchemaObject parent = sobject.getParent();

		if (sobject instanceof SchemaElementReference) {
			SchemaCompositor compositor = (SchemaCompositor) ((SchemaElementReference) sobject).getCompositor();
			compositor.removeChild(sobject);
		} else if (sobject instanceof ISchemaElement) {
			if (!(sobject instanceof ISchemaRootElement)) {
				Schema schema = (Schema) parent;
				schema.removeElement((ISchemaElement) sobject);
				schema.updateReferencesFor((ISchemaElement) sobject, ISchema.REFRESH_DELETE);
			}
		} else if (sobject instanceof ISchemaAttribute) {
			SchemaElement element = (SchemaElement) parent;
			SchemaComplexType type = (SchemaComplexType) element.getType();
			type.removeAttribute((ISchemaAttribute) sobject);
		} else if (sobject instanceof SchemaCompositor) {
			SchemaCompositor compositor = (SchemaCompositor) sobject;
			ISchemaObject cparent = compositor.getParent();
			if (cparent instanceof ISchemaElement) {
				SchemaElement element = (SchemaElement) cparent;
				SchemaComplexType complexType = (SchemaComplexType) element.getType();
				if (complexType.getAttributeCount() == 0)
					element.setType(new SchemaSimpleType(element.getSchema(), "string")); //$NON-NLS-1$
				else
					complexType.setCompositor(null);
			} else if (cparent instanceof SchemaCompositor) {
				((SchemaCompositor) cparent).removeChild(compositor);
			}
		}
	}

	private void handleNewAttribute() {
		Object object = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
		if (object != null) {
			SchemaElement element = null;
			if (object instanceof SchemaElement)
				element = (SchemaElement) object;
			else if (object instanceof SchemaAttribute)
				element = (SchemaElement) ((SchemaAttribute) object).getParent();
			
			if (element != null && !(element instanceof ISchemaRootElement)) { //$NON-NLS-1$
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
		getTreePart().setButtonEnabled(0, schema.isEditable());
		getTreePart().setButtonEnabled(1, false);
	}

	public void handleModelChanged(IModelChangedEvent e) {
		if (e.getChangedProperty() != null
				&& e.getChangedProperty().equals(ISchemaObject.P_DESCRIPTION))
			return;
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object[] objects = e.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			Object obj = objects[0];
			if (obj instanceof SchemaElementReference) {
				treeViewer.refresh(((SchemaElementReference)obj).getCompositor());
				if (e.getChangeType() == IModelChangedEvent.INSERT)
					treeViewer.setSelection(new StructuredSelection(obj), true);
			} else if (obj instanceof ISchemaElement || obj instanceof ISchemaAttribute) {
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					String changeProp = e.getChangedProperty();
					if (changeProp.equals(ISchemaObject.P_NAME) 
							|| changeProp.equals(SchemaAttribute.P_KIND))
						treeViewer.update(obj, null);
					Object typeCheck = e.getNewValue();
					if (typeCheck instanceof ISchemaComplexType 
							&& changeProp.equals(SchemaElement.P_TYPE)
							&& obj instanceof ISchemaElement) {
						treeViewer.refresh(typeCheck);
						treeViewer.setSelection(new StructuredSelection(typeCheck), true);
					}
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					ISchemaObject sobj = (ISchemaObject) obj;
					ISchemaObject parent = sobj.getParent();
					treeViewer.add(parent, sobj);
					treeViewer.setSelection(new StructuredSelection(obj), true);
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					ISchemaObject sobj = (ISchemaObject) obj;
					ISchemaObject parent = sobj.getParent();
					treeViewer.remove(obj);
					treeViewer.setSelection(new StructuredSelection(parent), true);
				}
			} else if (obj instanceof ISchemaCompositor || obj instanceof ISchemaObjectReference) {
				final ISchemaObject sobj = (ISchemaObject) obj;
				ISchemaObject parent = sobj.getParent();
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					treeViewer.update(sobj, null);
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					treeViewer.add(parent, sobj);
					treeViewer.getTree().getDisplay().asyncExec(new Runnable() {
						public void run() {
							treeViewer.setSelection(new StructuredSelection(sobj), true);
						}
					});
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					treeViewer.remove(sobj);
					treeViewer.setSelection(new StructuredSelection(parent), true);
				}
			} else if (obj instanceof ISchemaComplexType) {
				// first compositor added/removed
				treeViewer.refresh();
				if (e.getChangeType() == IModelChangedEvent.INSERT ||
						e.getChangeType() == IModelChangedEvent.CHANGE) {
					ISchemaComplexType type = (ISchemaComplexType) obj;
					final ISchemaCompositor compositor = type.getCompositor();
					if (compositor != null) {
						treeViewer.getTree().getDisplay().asyncExec(new Runnable() {
							public void run() {
								treeViewer.setSelection(new StructuredSelection(compositor), true);
							}
						});
					}
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
		if (!schema.isEditable())
			return;
		Object object = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
		ISchemaObject sobject = (ISchemaObject) object;

		boolean canAddAttribute = false;
		if (sobject != null) {
			if (sobject instanceof ISchemaElement) {
				if (!(sobject instanceof ISchemaRootElement)
						&& !(sobject instanceof ISchemaObjectReference))
					canAddAttribute = true;
			} else if (sobject instanceof ISchemaAttribute) {
				ISchemaElement element = (ISchemaElement) (sobject.getParent());
				if (!(element instanceof ISchemaRootElement))
					canAddAttribute = true;
			}
		}
		getTreePart().setButtonEnabled(1, canAddAttribute);
	}

//	public void doPaste(Object target, Object[] objects) {
//		for (int i = 0; i < objects.length; i++) {
//			Object object = objects[i];
//			Object realTarget = getRealTarget(target, object);
//			Object sibling = getSibling(target, object);
//			if (realTarget == null)
//				continue;
//			doPaste(realTarget, sibling, object);
//		}
//	}

//	private Object getSibling(Object target, Object object) {
//		if (target instanceof ISchemaElement && object instanceof ISchemaElement)
//			return target;
//		if (target instanceof ISchemaAttribute && object instanceof ISchemaAttribute)
//			return target;
//		return null;
//	}

//	private Object getRealTarget(Object target, Object object) {
//		if (object instanceof ISchemaObjectReference) {
//			return target;
//		}
//		if (object instanceof ISchemaElement) {
//			return schema;
//		}
//		if (object instanceof ISchemaAttribute) {
//			if (target instanceof ISchemaAttribute) {
//				// add it to the parent of the selected attribute
//				return ((ISchemaAttribute) target).getParent();
//			}
//			if (target instanceof ISchemaElement)
//				return target;
//		}
//		return null;
//	}

//	private void doPaste(Object realTarget, Object sibling, Object object) {
//		if (object instanceof ISchemaRootElement) {
//			// do not paste root elements
//		} else if (realTarget instanceof ISchemaObjectReference) {
//			
//		} else if (object instanceof ISchemaObjectReference) {
//			
//		} else if (object instanceof ISchemaElement) {
//			SchemaElement element = (SchemaElement) object;
//			element.setParent(schema);
//			schema.addElement(element, (ISchemaElement) sibling);
//			schema.updateReferencesFor(element, ISchema.REFRESH_ADD);
//		} else if (object instanceof ISchemaAttribute) {
//			SchemaElement element = (SchemaElement) realTarget;
//			SchemaAttribute attribute = (SchemaAttribute) object;
//			attribute.setParent(element);
//			ISchemaType type = element.getType();
//			SchemaComplexType complexType = null;
//			if (!(type instanceof ISchemaComplexType)) {
//				complexType = new SchemaComplexType(element.getSchema());
//				element.setType(complexType);
//			} else {
//				complexType = (SchemaComplexType) type;
//			}
//			complexType.addAttribute(attribute, (ISchemaAttribute) sibling);
//		}
//	}

//	protected boolean canPaste(Object target, Object[] objects) {
//		for (int i = 0; i < objects.length; i++) {
//			Object obj = objects[i];
//			if (obj instanceof ISchemaAttribute && target instanceof ISchemaAttribute) {
//				continue;
//			} else if (obj instanceof ISchemaObjectReference && target instanceof ISchemaCompositor) {
//				continue;
//			} else if (target instanceof ISchemaElement 
//					&& !(target instanceof ISchemaObjectReference)
//					&& !(obj instanceof ISchemaRootElement)) {
//				continue;
//			}
//			return false;
//		}
//		return true;
//	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		Object object = selection.getFirstElement();
		if (object instanceof SchemaElementReference)
			treeViewer.setSelection(new StructuredSelection(((SchemaElementReference) object).getReferencedObject()));
	}
	void fireSelection(ISelection selection) {
		if (selection == null) selection = treeViewer.getSelection();
		treeViewer.setSelection(selection);
	}

	public void handleCollapseAll() {
		treeViewer.collapseAll();
	}
}
