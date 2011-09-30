/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import java.util.Arrays;
import java.util.Iterator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.*;
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
	private CollapseAction fCollapseAction;

	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {
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
						types = new Object[] {compositor};
				}
				children = new Object[types.length + attributes.length];
				System.arraycopy(types, 0, children, 0, types.length);
				System.arraycopy(attributes, 0, children, types.length, attributes.length);
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
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.SchemaEditor_ElementSection_newElement, PDEUIMessages.SchemaEditor_ElementSection_newAttribute, PDEUIMessages.SchemaEditor_ElementSection_newChoice, PDEUIMessages.SchemaEditor_ElementSection_newSequence, PDEUIMessages.SchemaEditor_ElementSection_remove});
		getSection().setText(PDEUIMessages.SchemaEditor_ElementSection_title);
		getSection().setDescription(PDEUIMessages.SchemaEditor_ElementSection_desc);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createTree(container, toolkit);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
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
				if ((handCursor != null) && (handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});
		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fTreeViewer, PDEUIMessages.ExtensionsPage_collapseAll);
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fTreeViewer = treePart.getTreeViewer();
		fTreeViewer.setContentProvider(new ContentProvider());
		fTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		initDragAndDrop();
	}

	protected void initDragAndDrop() {
		fClipboard = new Clipboard(fTreeViewer.getControl().getDisplay());
		int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] transfers = new Transfer[] {ModelDataTransfer.getInstance(), TextTransfer.getInstance()};
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

		if (fTreeViewer.getSelection().isEmpty() && fSchema.getElementCount() > 0) {
			fTreeViewer.setSelection(new StructuredSelection(fSchema.getElements()[0]));
		}
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNewElement();
				break;
			case 1 :
				handleNewAttribute();
				break;
			case 2 :
				addCompositor(ISchemaCompositor.CHOICE);
				break;
			case 3 :
				addCompositor(ISchemaCompositor.SEQUENCE);
				break;
			case 4 :
				final ISelection selection = fTreeViewer.getSelection();
				handleDelete((IStructuredSelection) selection);
				break;
		}
	}

	private void addCompositor(int kind) {
		Object selection = ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
		ISchemaElement sourceElement = null;
		Object current = selection;
		while (current instanceof ISchemaCompositor)
			current = ((ISchemaCompositor) current).getParent();
		if (current instanceof ISchemaElement)
			sourceElement = (ISchemaElement) current;
		if (sourceElement != null)
			new NewCompositorAction(sourceElement, selection, kind).run();
	}

	public void dispose() {
		if (fClipboard != null) {
			fClipboard.dispose();
			fClipboard = null;
		}
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		boolean cut = actionId.equals(ActionFactory.CUT.getId());
		if (cut || actionId.equals(ActionFactory.DELETE.getId())) {
			// Get the current selection
			IStructuredSelection sel = (IStructuredSelection) fTreeViewer.getSelection();
			// Get the first selected object
			Object selectedObject = sel.getFirstElement();
			// Ensure we have a selection
			if (selectedObject == null) {
				return true;
			}
			handleDelete(sel);
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
		if (object instanceof ISchemaElement || object instanceof ISchemaAttribute || object instanceof ISchemaCompositor) {
			fTreeViewer.setSelection(new StructuredSelection(object), true);

			ISelection selection = fTreeViewer.getSelection();
			if (selection != null && !selection.isEmpty())
				return true;
			if (object instanceof ISchemaElement) {
				ISchemaElement found = fSchema.findElement(((ISchemaElement) object).getName());
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
		if (object == null) {
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

			if (element != null && !(element instanceof ISchemaRootElement) && !(element instanceof ISchemaObjectReference)) {
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
				if (object instanceof SchemaCompositor || sourceElement.getType() instanceof ISchemaSimpleType || ((ISchemaComplexType) sourceElement.getType()).getCompositor() == null) {
					if (submenu.getItems().length > 0)
						submenu.add(new Separator());
					submenu.add(new NewCompositorAction(sourceElement, object, ISchemaCompositor.CHOICE));
					submenu.add(new NewCompositorAction(sourceElement, object, ISchemaCompositor.SEQUENCE));
				}
				if (object instanceof SchemaCompositor) {
					boolean seperatorAdded = false;
					ISchemaElement[] elements = sourceElement.getSchema().getResolvedElements();
					Arrays.sort(elements);
					for (int i = 0; i < elements.length; i++) {
						if (!(elements[i] instanceof SchemaRootElement)) {
							if (!seperatorAdded) {
								submenu.add(new Separator());
								seperatorAdded = true;
							}
							submenu.add(new NewReferenceAction(sourceElement, object, elements[i]));
						}
					}
				}
			}
		}
		manager.add(submenu);
		if (object != null) {
			if (!(object instanceof ISchemaRootElement)) {
				if (manager.getItems().length > 0)
					manager.add(new Separator());
				if (!(object instanceof ISchemaAttribute && ((ISchemaAttribute) object).getParent() instanceof ISchemaRootElement)) {
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
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());
	}

	private void handleDelete(IStructuredSelection selection) {
		IStructuredSelection nextSelection = null;
		Object selectionSource = null;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object thisObject = iter.next();
			// Do the delete and generate a new selection in one of the following cases:
			//   1. No selection has been generated
			//   2. This object is higher up in the hierarchy than the previous
			//      object used to generate the selection
			//   3. The object selected for deletion is currently set as the next selection
			IStructuredSelection result = handleDelete(thisObject, nextSelection == null || schemaObjectHigherThan(thisObject, selectionSource) || nextSelection.getFirstElement().equals(thisObject));
			if (result != null) {
				nextSelection = result;
				selectionSource = thisObject;
			}
		}
		if (nextSelection != null)
			getTreeViewer().setSelection(nextSelection);
	}

	private IStructuredSelection handleDelete(Object object, boolean generateSelection) {
		IStructuredSelection newSelection = null;
		if (!isEditable()) {
			Display.getCurrent().beep();
		} else if (object instanceof ISchemaRootElement) {
			// Semantic rule: The root "extension" element of a schema
			// cannot be removed

			// Produce audible beep
			Display.getCurrent().beep();
		} else if (object instanceof SchemaElementReference) {
			newSelection = handleReferenceDelete((SchemaElementReference) object, generateSelection);
		} else if (object instanceof ISchemaElement) {
			newSelection = handleElementDelete((ISchemaElement) object, generateSelection);
		} else if (object instanceof ISchemaAttribute) {
			ISchemaAttribute att = (ISchemaAttribute) object;
			if (!(att.getParent() instanceof ISchemaRootElement)) {
				newSelection = handleAttributeDelete(att, generateSelection);
			} else {
				// Semantic rule: Attributes of the root "extension" element
				// of a schema cannot be removed

				// Produce audible beep
				Display.getCurrent().beep();
			}
		} else if (object instanceof ISchemaCompositor) {
			newSelection = handleCompositorDelete((ISchemaCompositor) object, generateSelection);
		}
		return newSelection;
	}

	private IStructuredSelection handleReferenceDelete(SchemaElementReference ref, boolean generateSelection) {
		IStructuredSelection newSelection = null;
		if (generateSelection) {
			SchemaCompositor parent = (SchemaCompositor) ref.getParent();
			ISchemaObject[] children = parent.getChildren();
			int index = getNewSelectionIndex(getArrayIndex(children, ref), children.length);
			if (index == -1)
				newSelection = new StructuredSelection(parent);
			else
				newSelection = new StructuredSelection(children[index]);
		}
		fRearranger.deleteReference(ref);
		return newSelection;
	}

	private IStructuredSelection handleElementDelete(ISchemaElement element, boolean generateSelection) {
		IStructuredSelection newSelection = null;
		if (generateSelection) {
			ISchema parent = element.getSchema();
			ISchemaElement[] children = parent.getElements();
			int index = getNewSelectionIndex(getArrayIndex(children, element), children.length);
			if (index != -1)
				newSelection = new StructuredSelection(children[index]);
		}
		fRearranger.deleteElement(element);
		return newSelection;
	}

	private IStructuredSelection handleAttributeDelete(ISchemaAttribute att, boolean generateSelection) {
		IStructuredSelection newSelection = null;
		if (generateSelection) {
			ISchemaElement parent = (ISchemaElement) att.getParent();
			ISchemaAttribute[] children = parent.getAttributes();
			int index = getNewSelectionIndex(getArrayIndex(children, att), children.length);
			if (index == -1) {
				ISchemaType type = parent.getType();
				if (type instanceof ISchemaComplexType) {
					ISchemaCompositor comp = ((ISchemaComplexType) type).getCompositor();
					if (comp != null)
						newSelection = new StructuredSelection(comp);
					else
						newSelection = new StructuredSelection(parent);
				}
			} else
				newSelection = new StructuredSelection(children[index]);
		}
		fRearranger.deleteAttribute(att);
		return newSelection;
	}

	private IStructuredSelection handleCompositorDelete(ISchemaCompositor comp, boolean generateSelection) {
		IStructuredSelection newSelection = null;
		if (generateSelection) {
			ISchemaObject parent = comp.getParent();
			if (parent instanceof ISchemaElement) {
				ISchemaElement element = (ISchemaElement) parent;
				ISchemaAttribute[] attributes = element.getAttributes();
				if (attributes.length > 0)
					newSelection = new StructuredSelection(attributes[0]);
				else
					newSelection = new StructuredSelection(element);
			} else {
				ISchemaCompositor parentComp = (ISchemaCompositor) parent;
				ISchemaObject[] children = parentComp.getChildren();
				int index = getNewSelectionIndex(getArrayIndex(children, comp), children.length);
				if (index == -1)
					newSelection = new StructuredSelection(parent);
				else
					newSelection = new StructuredSelection(children[index]);
			}
		}
		fRearranger.deleteCompositor(comp);
		return newSelection;
	}

	// returns true if object a is a SchemaObject higher in the hierarchy than object b
	// returns false if b is higher or they are equal
	private boolean schemaObjectHigherThan(Object a, Object b) {
		if (!(b instanceof ISchemaObject))
			return true;
		if (!(a instanceof ISchemaObject))
			return false;
		return (computeNestLevel((ISchemaObject) a) < computeNestLevel((ISchemaObject) b));
	}

	// determines how deeply nested an ISchemaObject is
	// returns 0 if this is an element, 1 if it's a direct child, etc.
	private int computeNestLevel(ISchemaObject o) {
		int result = 0;
		while ((o instanceof SchemaElementReference) || !(o instanceof ISchemaElement)) {
			o = o.getParent();
			result++;
		}
		return result;
	}

	private void handleNewAttribute() {
		Object object = ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
		if (object != null) {
			SchemaElement element = null;
			if (object instanceof SchemaElement)
				element = (SchemaElement) object;
			else if (object instanceof SchemaAttribute)
				element = (SchemaElement) ((SchemaAttribute) object).getParent();

			if (element != null && !(element instanceof ISchemaRootElement)) {
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
		boolean isEditable = fSchema.isEditable();
		getTreePart().setButtonEnabled(0, isEditable);
		getTreePart().setButtonEnabled(1, false);
		getTreePart().setButtonEnabled(2, isEditable);
		getTreePart().setButtonEnabled(3, isEditable);
		getTreePart().setButtonEnabled(4, isEditable);
	}

	public void handleModelChanged(IModelChangedEvent e) {
		if (e.getChangedProperty() != null && e.getChangedProperty().equals(ISchemaObject.P_DESCRIPTION))
			return;
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
			return;
		}
		Object[] objects = e.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			Object obj = objects[0];
			if (obj instanceof SchemaElementReference) {
				fTreeViewer.refresh(((SchemaElementReference) obj).getCompositor());
				if (e.getChangeType() == IModelChangedEvent.INSERT)
					fTreeViewer.setSelection(new StructuredSelection(obj), true);
			} else if (obj instanceof ISchemaElement || obj instanceof ISchemaAttribute) {
				if (e.getChangeType() == IModelChangedEvent.CHANGE) {
					String changeProp = e.getChangedProperty();
					if (changeProp != null && (changeProp.equals(ISchemaObject.P_NAME) || changeProp.equals(SchemaAttribute.P_KIND)))
						fTreeViewer.update(obj, null);
					Object typeCheck = e.getNewValue();
					if (typeCheck instanceof ISchemaComplexType && changeProp.equals(SchemaElement.P_TYPE) && obj instanceof ISchemaElement) {
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
					fTreeViewer.remove(obj);
					// the new selection is handled by the handleDelete method for cuts and deletes,
					// for moves it is handled by the subsequent insert
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
					// the new selection is handled by the handleDelete method for cuts and deletes,
					// for moves it is handled by the subsequent insert
				}
			} else if (obj instanceof ISchemaComplexType) {
				// first compositor added/removed
				ISchemaCompositor comp = ((ISchemaComplexType) obj).getCompositor();
				fTreeViewer.refresh(comp);
				if (comp != null)
					fTreeViewer.refresh(comp.getParent());

				if (e.getChangeType() == IModelChangedEvent.INSERT || e.getChangeType() == IModelChangedEvent.CHANGE) {
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
				if (!ISchemaObject.P_NAME.equals(e.getChangedProperty()))
					fTreeViewer.refresh();
			}
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Note:  Cannot use event.  There are no changed objects within it
		// This method acts like a refresh
		initialize();
		// TODO: MP: REVERT: LOW: Update initialize with this once Bug #171897 is fixed
		ISchemaElement root = fSchema.getSchema().findElement(ICoreConstants.EXTENSION_NAME);
		// Ensure the root element is present
		if (root == null) {
			return;
		}
		// Select the root extension element
		fTreeViewer.setSelection(new StructuredSelection(root), true);
		// Collapse tree to the first level
		fTreeViewer.expandToLevel(1);
	}

	protected void selectionChanged(IStructuredSelection selection) {
//		getPage().getManagedForm().fireSelectionChanged(this, selection);
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#setFocus()
	 */
	public void setFocus() {
		if (fTreeViewer != null) {
			fTreeViewer.getTree().setFocus();
			getPage().getPDEEditor().setSelection(fTreeViewer.getSelection());
		}
	}

	private void updateButtons() {
		if (!fSchema.isEditable())
			return;
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		ISchemaObject sobject = (ISchemaObject) selection.getFirstElement();

		boolean canAddAttribute = false;
		if (sobject instanceof ISchemaElement) {
			if (!(sobject instanceof ISchemaRootElement) && !(sobject instanceof ISchemaObjectReference))
				canAddAttribute = true;
		} else if (sobject instanceof ISchemaAttribute) {
			ISchemaElement element = (ISchemaElement) (sobject.getParent());
			if (!(element instanceof ISchemaRootElement))
				canAddAttribute = true;
		}
		getTreePart().setButtonEnabled(1, canAddAttribute);

		boolean canAddCompositor = false;
		if (sobject instanceof ISchemaCompositor || (sobject instanceof ISchemaElement && !(sobject instanceof SchemaElementReference) && (((ISchemaElement) sobject).getType() instanceof ISchemaSimpleType || ((ISchemaComplexType) ((ISchemaElement) sobject).getType()).getCompositor() == null)))
			canAddCompositor = true;
		getTreePart().setButtonEnabled(2, canAddCompositor);
		getTreePart().setButtonEnabled(3, canAddCompositor);

		boolean canRemove = false;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			sobject = (ISchemaObject) iter.next();
			if (sobject != null && !(sobject instanceof ISchemaRootElement) && !(sobject instanceof ISchemaAttribute && sobject.getParent() instanceof ISchemaRootElement)) {
				canRemove = true;
				break;
			}
		}
		getTreePart().setButtonEnabled(4, canRemove);
	}

	private ISchemaObject getSibling(Object target, Object object) {
		if (target instanceof ISchemaElement && object instanceof ISchemaElement)
			return (ISchemaElement) target;
		if (target instanceof ISchemaAttribute && object instanceof ISchemaAttribute)
			return (ISchemaAttribute) target;
		if (target instanceof SchemaElementReference && object instanceof ISchemaElement)
			return (SchemaElementReference) target;
		return null;
	}

	private ISchemaObject getRealTarget(Object target, Object object) {
		if (object instanceof ISchemaElement || object instanceof ISchemaObjectReference) {
			if (target instanceof SchemaElementReference)
				return ((SchemaElementReference) target).getCompositor();
			if (target instanceof ISchemaCompositor)
				return (ISchemaCompositor) target;
			if (object instanceof ISchemaElement)
				return fSchema;
		}
		if (object instanceof ISchemaAttribute) {
			if (target instanceof ISchemaAttribute) {
				// add it to the parent of the selected attribute
				return ((ISchemaAttribute) target).getParent();
			}
			if (target instanceof ISchemaElement)
				return (ISchemaElement) target;
		}
		if (object instanceof ISchemaCompositor) {
			if (target instanceof SchemaElementReference)
				return ((SchemaElementReference) target).getCompositor();
			if (target instanceof ISchemaElement)
				return (ISchemaElement) target;
			if (target instanceof ISchemaCompositor)
				return (ISchemaCompositor) target;
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
			} else if (target instanceof ISchemaElement && !(target instanceof ISchemaObjectReference) && !(obj instanceof ISchemaRootElement)) {
				continue;
			}
			return false;
		}
		return true;
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		super.handleDoubleClick(selection);
		Object object = selection.getFirstElement();
		if (object instanceof SchemaElementReference) {
			ISchemaElement element = ((SchemaElementReference) object).getReferencedElement();
			if (element == null) {
				String name = ((SchemaElementReference) object).getName();
				MessageDialog.openWarning(getPage().getSite().getShell(), PDEUIMessages.ElementSection_missingRefElement, NLS.bind(PDEUIMessages.SchemaIncludesSection_missingWarningMessage, name));
				return;
			}
			ISchema schema = element.getSchema();
			if (schema.equals(fSchema))
				fireSelection(new StructuredSelection(element));
			else {
				ISchemaInclude[] includes = fSchema.getIncludes();
				for (int i = 0; i < includes.length; i++) {
					ISchema includedSchema = includes[i].getIncludedSchema();
					if (includedSchema != null && includedSchema.equals(schema)) {
						String location = includes[i].getLocation();
						SchemaEditor.openToElement(new Path(location), element);
						break;
					}
				}
			}
		}
	}

	protected void fireSelection(ISelection selection) {
		if (selection == null)
			selection = fTreeViewer.getSelection();
		fTreeViewer.setSelection(selection);
	}

	protected void doPaste(Object target, Object[] objects) {
		handleOp(target, objects, DND.DROP_COPY);
	}

	public void handleOp(Object currentTarget, Object[] objects, int currentOperation) {
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof ISchemaObject))
				continue;
			ISchemaObject object = (ISchemaObject) objects[i];
			ISchemaObject realTarget = getRealTarget(currentTarget, object);
			ISchemaObject sibling = getSibling(currentTarget, object);
			if (realTarget == null)
				continue;
			switch (currentOperation) {
				case DND.DROP_COPY :
					doPaste(realTarget, sibling, object);
					break;
				case DND.DROP_MOVE :
					doMove(realTarget, sibling, object);
					break;
				case DND.DROP_LINK :
					doLink(realTarget, sibling, object);
					break;
			}
		}
	}

	private void doLink(ISchemaObject realTarget, ISchemaObject sibling, ISchemaObject object) {
		if (realTarget instanceof ISchemaCompositor && object instanceof ISchemaElement) {
			fRearranger.linkReference((ISchemaCompositor) realTarget, (ISchemaElement) object, sibling);
		}
	}

	private void doMove(ISchemaObject realTarget, ISchemaObject sibling, ISchemaObject object) {
		if (object instanceof ISchemaCompositor) {
			fRearranger.moveCompositor(realTarget, (ISchemaCompositor) object);
		} else if (object instanceof SchemaElementReference) {
			fRearranger.moveReference((SchemaElementReference) object, (ISchemaCompositor) realTarget, sibling);
		} else if (object instanceof ISchemaElement) {
			fRearranger.moveElement(realTarget, (ISchemaElement) object, sibling);
		} else if (object instanceof ISchemaAttribute) {
			fRearranger.moveAttribute((ISchemaElement) realTarget, (ISchemaAttribute) object, sibling != null ? (ISchemaAttribute) sibling : null);
		}
	}

	private void doPaste(ISchemaObject realTarget, ISchemaObject sibling, ISchemaObject object) {
		if (object instanceof ISchemaCompositor) {
			fRearranger.pasteCompositor(realTarget, (ISchemaCompositor) object, sibling);
		} else if (object instanceof SchemaElementReference) {
			fRearranger.pasteReference(realTarget, (SchemaElementReference) object, sibling);
		} else if (object instanceof ISchemaElement) {
			fRearranger.pasteElement((ISchemaElement) object, sibling);
		} else if (object instanceof ISchemaAttribute) {
			fRearranger.pasteAttribute((ISchemaElement) realTarget, (ISchemaAttribute) object, sibling);
		}
	}
}
