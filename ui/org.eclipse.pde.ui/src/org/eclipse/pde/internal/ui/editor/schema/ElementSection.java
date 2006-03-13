package org.eclipse.pde.internal.ui.editor.schema;
import java.util.Iterator;

import org.eclipse.core.runtime.Path;
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
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.ischema.ISchemaRootElement;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
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
	private TreeViewer fTreeViewer;
	private Schema fSchema;
	private NewElementAction fNewElementAction = new NewElementAction();
	private NewAttributeAction fNewAttributeAction = new NewAttributeAction();
	private Clipboard fClipboard;
	private SchemaRearranger fRearranger;

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
			if (parent instanceof ISchemaAttribute || parent instanceof ISchemaObjectReference)
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
		fTreeViewer = treePart.getTreeViewer();
		fTreeViewer.setContentProvider(new ContentProvider());
		fTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		initDragAndDrop();
	}

	protected void initDragAndDrop() {
		fClipboard = new Clipboard(fTreeViewer.getControl().getDisplay());
		int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] transfers = new Transfer[] {ModelDataTransfer.getInstance(), TextTransfer.getInstance() };
		ElementSectionDragAdapter dragAdapter = new ElementSectionDragAdapter(fTreeViewer);
		fTreeViewer.addDragSupport(ops, transfers, dragAdapter);
		fTreeViewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, new ElementSectionDropAdapter(dragAdapter, this));
	}

	protected TreeViewer getTreeViewer() {
		return fTreeViewer;
	}

	public void refresh() {
		fTreeViewer.refresh();
		super.refresh();
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
		if (fClipboard != null) {
			fClipboard.dispose();
			fClipboard = null;
		}
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		boolean cut = actionId.equals(ActionFactory.CUT.getId());
		if (cut || actionId.equals(ActionFactory.DELETE.getId())) {
			ISelection sel = fTreeViewer.getSelection();
			Object obj = ((IStructuredSelection) sel).getFirstElement();
			if (obj != null)
				handleDelete(obj);
			// if cutting delete here and let the editor transfer
			// the selection to the clipboard
			return !cut;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}

	public boolean setFormInput(Object object) {
		if (object instanceof ISchemaElement
				|| object instanceof ISchemaAttribute
				|| object instanceof ISchemaCompositor) {
			fTreeViewer.setSelection(new StructuredSelection(object), true);
			
			ISelection selection = fTreeViewer.getSelection();
			if (selection != null && !selection.isEmpty())
				return true;
			if (object instanceof ISchemaElement) {
				ISchemaElement found = fSchema.findElement(((ISchemaElement)object).getName());
				if (found != null)
					fTreeViewer.setSelection(new StructuredSelection(found), true);
				return found != null;
			}
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		final ISelection selection = fTreeViewer.getSelection();
		final Object object = ((IStructuredSelection) selection).getFirstElement();

		MenuManager submenu = new MenuManager(PDEUIMessages.Menus_new_label);
		if (object == null || object instanceof SchemaElement) {
			fNewElementAction.setSchema(fSchema);
			fNewElementAction.setEnabled(fSchema.isEditable());
			submenu.add(fNewElementAction);
		}
		if (object != null) {
			ISchemaElement element = null;
			if (object instanceof SchemaElement)
				element = (SchemaElement) object;
			else if (object instanceof SchemaAttribute)
				element = (SchemaElement) ((SchemaAttribute) object).getParent();
			
			if (element != null	&& !(element instanceof ISchemaRootElement)
					&& !(element instanceof ISchemaObjectReference)) { //$NON-NLS-1$
				fNewAttributeAction.setElement((SchemaElement) element);
				fNewAttributeAction.setEnabled(fSchema.isEditable());
				submenu.add(fNewAttributeAction);
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
				deleteAction.setEnabled(fSchema.isEditable());
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

	private void handleDelete(Object object) {
		if (object instanceof SchemaElementReference) {
			fRearranger.deleteReference((SchemaElementReference)object);
		} else if (object instanceof ISchemaElement) {
			fRearranger.deleteElement((ISchemaElement)object);
		} else if (object instanceof ISchemaAttribute) {
			fRearranger.deleteAttribute((ISchemaAttribute)object);
		} else if (object instanceof ISchemaCompositor) {
			fRearranger.deleteCompositor((ISchemaCompositor)object);
		}
	}

	private void handleNewAttribute() {
		Object object = ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
		if (object != null) {
			SchemaElement element = null;
			if (object instanceof SchemaElement)
				element = (SchemaElement) object;
			else if (object instanceof SchemaAttribute)
				element = (SchemaElement) ((SchemaAttribute) object).getParent();
			
			if (element != null && !(element instanceof ISchemaRootElement)) { //$NON-NLS-1$
				fNewAttributeAction.setElement(element);
				fNewAttributeAction.run();
			}
		}
	}

	private void handleNewElement() {
		fNewElementAction.setSchema(fSchema);
		fNewElementAction.run();
	}

	public void initialize() {
		this.fSchema = (Schema) getPage().getModel();
		fRearranger = new SchemaRearranger(fSchema);
		fTreeViewer.setInput(fSchema);
		getTreePart().setButtonEnabled(0, fSchema.isEditable());
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
				fTreeViewer.refresh(((SchemaElementReference)obj).getCompositor());
				if (e.getChangeType() == IModelChangedEvent.INSERT)
					fTreeViewer.setSelection(new StructuredSelection(obj), true);
			} else if (obj instanceof ISchemaElement || obj instanceof ISchemaAttribute) {
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					String changeProp = e.getChangedProperty();
					if (changeProp != null
							&& (changeProp.equals(ISchemaObject.P_NAME) 
							|| changeProp.equals(SchemaAttribute.P_KIND)))
						fTreeViewer.update(obj, null);
					Object typeCheck = e.getNewValue();
					if (typeCheck instanceof ISchemaComplexType 
							&& changeProp.equals(SchemaElement.P_TYPE)
							&& obj instanceof ISchemaElement) {
						fTreeViewer.refresh(typeCheck);
						fTreeViewer.setSelection(new StructuredSelection(typeCheck), true);
					} else {
						fTreeViewer.refresh(obj);
					}
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					ISchemaObject sobj = (ISchemaObject) obj;
					ISchemaObject parent = sobj.getParent();
					fTreeViewer.refresh(parent);
					fTreeViewer.setSelection(new StructuredSelection(obj), true);
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					ISchemaObject sobj = (ISchemaObject) obj;
					ISchemaObject parent = sobj.getParent();
					fTreeViewer.remove(obj);
					fTreeViewer.setSelection(new StructuredSelection(parent), true);
				}
			} else if (obj instanceof ISchemaCompositor || obj instanceof ISchemaObjectReference) {
				final ISchemaObject sobj = (ISchemaObject) obj;
				ISchemaObject parent = sobj.getParent();
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					fTreeViewer.refresh(sobj);
				} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
					fTreeViewer.add(parent, sobj);
					fTreeViewer.getTree().getDisplay().asyncExec(new Runnable() {
						public void run() {
							fTreeViewer.setSelection(new StructuredSelection(sobj), true);
						}
					});
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					fTreeViewer.remove(sobj);
					fTreeViewer.setSelection(new StructuredSelection(parent), true);
				}
			} else if (obj instanceof ISchemaComplexType) {
				// first compositor added/removed
				ISchemaCompositor comp = ((ISchemaComplexType)obj).getCompositor();
				fTreeViewer.refresh(comp);
				if (comp != null)
					fTreeViewer.refresh(comp.getParent());
				
				if (e.getChangeType() == IModelChangedEvent.INSERT ||
						e.getChangeType() == IModelChangedEvent.CHANGE) {
					ISchemaComplexType type = (ISchemaComplexType) obj;
					final ISchemaCompositor compositor = type.getCompositor();
					if (compositor != null) {
						fTreeViewer.getTree().getDisplay().asyncExec(new Runnable() {
							public void run() {
								fTreeViewer.setSelection(new StructuredSelection(compositor), true);
							}
						});
					}
				}
			} else if (obj instanceof ISchema) {
				fTreeViewer.refresh();
			}
		}
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getManagedForm().fireSelectionChanged(this, selection);
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

	public void setFocus() {
		fTreeViewer.getTree().setFocus();
		getPage().getPDEEditor().setSelection(fTreeViewer.getSelection());
	}

	private void updateButtons() {
		if (!fSchema.isEditable())
			return;
		Object object = ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
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

	private ISchemaObject getSibling(Object target, Object object) {
		if (target instanceof ISchemaElement && object instanceof ISchemaElement)
			return (ISchemaElement)target;
		if (target instanceof ISchemaAttribute && object instanceof ISchemaAttribute)
			return (ISchemaAttribute)target;
		if (target instanceof SchemaElementReference && object instanceof ISchemaElement)
			return (SchemaElementReference)target;
		return null;
	}

	private ISchemaObject getRealTarget(Object target, Object object) {
		if (object instanceof ISchemaElement || object instanceof ISchemaObjectReference) {
			if (target instanceof SchemaElementReference)
				return ((SchemaElementReference)target).getCompositor();
			if (target instanceof ISchemaCompositor)
				return (ISchemaCompositor)target;
			if (object instanceof ISchemaElement)
				return fSchema;
		}
		if (object instanceof ISchemaAttribute) {
			if (target instanceof ISchemaAttribute) {
				// add it to the parent of the selected attribute
				return ((ISchemaAttribute) target).getParent();
			}
			if (target instanceof ISchemaElement)
				return (ISchemaElement)target;
		}
		if (object instanceof ISchemaCompositor) {
			if (target instanceof SchemaElementReference) 
				return ((SchemaElementReference)target).getCompositor();
			if (target instanceof ISchemaElement)
				return (ISchemaElement)target;
			if (target instanceof ISchemaCompositor)
				return (ISchemaCompositor)target;
		}
		return null;
	}

	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			Object obj = objects[i];
			if (obj instanceof ISchemaAttribute && target instanceof ISchemaAttribute) {
				continue;
			} else if (obj instanceof ISchemaObjectReference && target instanceof ISchemaCompositor) {
				continue;
			} else if (target instanceof ISchemaElement 
					&& !(target instanceof ISchemaObjectReference)
					&& !(obj instanceof ISchemaRootElement)) {
				continue;
			}
			return false;
		}
		return true;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		Object object = selection.getFirstElement();
		if (object instanceof SchemaElementReference) {
			ISchemaElement element = ((SchemaElementReference) object).getReferencedElement();
			ISchema schema = element.getSchema();
			if (schema.equals(fSchema))
				fireSelection(new StructuredSelection(element));
			else {
				ISchemaInclude[] includes = fSchema.getIncludes();
				for (int i = 0; i < includes.length; i++) {
					if (includes[i].getIncludedSchema().equals(schema)) {
						String location = includes[i].getLocation();
						SchemaEditor.openToElement(new Path(location), element);
						break;
					}
				}
			}
		}
	}
	protected void fireSelection(ISelection selection) {
		if (selection == null) selection = fTreeViewer.getSelection();
		fTreeViewer.setSelection(selection);
	}

	public void handleCollapseAll() {
		fTreeViewer.collapseAll();
	}

	protected void doPaste(Object target, Object[] objects) {
		handleOp(target, objects, DND.DROP_COPY);
	}

	public void handleOp(Object currentTarget, Object[] objects, int currentOperation) {
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof ISchemaObject))
				continue;
			ISchemaObject object = (ISchemaObject)objects[i];
			ISchemaObject realTarget = getRealTarget(currentTarget, object);
			ISchemaObject sibling = getSibling(currentTarget, object);
			if (realTarget == null)
				continue;
			switch (currentOperation) {
			case DND.DROP_COPY:
				doPaste(realTarget, sibling, object);
				break;
			case DND.DROP_MOVE:
				doMove(realTarget, sibling, object);
				break;
			case DND.DROP_LINK:
				doLink(realTarget, sibling, object);
				break;
			}
		}
	}

	private void doLink(ISchemaObject realTarget, ISchemaObject sibling, ISchemaObject object) {
		if (realTarget instanceof ISchemaCompositor
				&& object instanceof ISchemaElement) {
			fRearranger.linkReference(
					(ISchemaCompositor)realTarget,
					(ISchemaElement)object,
					sibling);
		}
	}
	
	private void doMove(ISchemaObject realTarget, ISchemaObject sibling, ISchemaObject object) {
		if (object instanceof ISchemaCompositor) {
			fRearranger.moveCompositor(
					realTarget,
					(ISchemaCompositor)object);
		} else if (object instanceof SchemaElementReference) {
			fRearranger.moveReference(
					(SchemaElementReference)object,
					(ISchemaCompositor)realTarget,
					sibling);
		} else if (object instanceof ISchemaElement) {
			fRearranger.moveElement(
					realTarget,
					(ISchemaElement)object,
					sibling != null ? (ISchemaAttribute)sibling : null);
		} else if (object instanceof ISchemaAttribute) {
			fRearranger.moveAttribute(
					(ISchemaElement)realTarget,
					(ISchemaAttribute)object,
					sibling != null ? (ISchemaAttribute)sibling : null);
		}
	}
	
	private void doPaste(ISchemaObject realTarget, ISchemaObject sibling, ISchemaObject object) {
		if (object instanceof ISchemaCompositor) {
			fRearranger.pasteCompositor(
					realTarget,
					(ISchemaCompositor)object,
					sibling);
		} else if (object instanceof SchemaElementReference) {
			fRearranger.pasteReference(
					realTarget,
					(SchemaElementReference)object,
					sibling);
		} else if (object instanceof ISchemaElement) {
			fRearranger.pasteElement(
					(ISchemaElement)object,
					sibling);
		} else if (object instanceof ISchemaAttribute) {
			fRearranger.pasteAttribute(
					(ISchemaElement)realTarget,
					(ISchemaAttribute)object,
					sibling);
		}
	}
}
