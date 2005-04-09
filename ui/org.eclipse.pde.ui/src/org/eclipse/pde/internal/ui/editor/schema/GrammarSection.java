/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.*;

public class GrammarSection extends PDESection implements IPartSelectionListener {
	private TreeViewer treeViewer;
	private Text dtdLabel;
	private PropertiesAction propertiesAction;
	class GrammarContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public Object[] getChildren(Object parent) {
			Object[] children = null;
			if (parent instanceof ISchemaElement
				&& !(parent instanceof SchemaElementReference)) {
				ISchemaType type = ((ISchemaElement) parent).getType();
				if (type instanceof ISchemaComplexType) {
					Object compositor =
						((ISchemaComplexType) type).getCompositor();
					if (compositor != null) {
						children = new Object[1];
						children[0] = compositor;
					}
				}
			} else if (parent instanceof ISchemaCompositor) {
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
			return PDEPlugin.getDefault().getLabelProvider().getText(o);
		}
		public Image getImage(Object o) {
			if (o instanceof ISchemaObjectReference) {
				ISchemaObjectReference ref = (ISchemaObjectReference) o;
				int flags =
					ref.getReferencedObject() == null
						? PDELabelProvider.F_ERROR
						: 0;
				return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_ELREF_SC_OBJ,
					flags);
			}
			return PDEPlugin.getDefault().getLabelProvider().getImage(o);
		}
	}

	public GrammarSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEUIMessages.SchemaEditor_GrammarSection_title);
		getSection().setDescription(PDEUIMessages.SchemaEditor_GrammarSection_desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	
	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 2;
		layout.verticalSpacing = toolkit.getBorderStyle()==SWT.BORDER?0:1;
		container.setLayout(layout);
		
		SashForm sash = new SashForm(container, SWT.VERTICAL);
		toolkit.adapt(sash, false, false);
		GridData gd = new GridData(GridData.FILL_BOTH);		
		sash.setLayoutData(gd);

		Composite sashCell = sash;
		if (toolkit.getBorderStyle()==SWT.NULL)
			sashCell = createSashCell(sash, toolkit, 1);
		createTree(sashCell, toolkit);
		
		sashCell = sash;
		if (toolkit.getBorderStyle()==SWT.NULL)
			sashCell = createSashCell(sash, toolkit, 2);
		dtdLabel =
			toolkit.createText(
				sashCell,
				"", //$NON-NLS-1$
				SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		//dtdLabel.setData(
			//FormToolkit.KEY_DRAW_BORDER,
			//FormToolkit.TREE_BORDER);
		dtdLabel.setEditable(false);
		dtdLabel.setForeground(
			toolkit.getColors().getColor(FormColors.TITLE));
		//gd = new GridData(GridData.FILL_BOTH);
		//dtdLabel.setLayoutData(gd);
		updateDTDLabel(null);
		
		sash.setWeights(new int[] {3, 1});	

		toolkit.paintBordersFor(container);
		section.setClient(container);
		propertiesAction = new PropertiesAction(getPage().getPDEEditor());		
		initialize();
	}

	private Composite createSashCell(Composite parent, FormToolkit toolkit, int marginHeight) {
		Composite cell = toolkit.createComposite(parent);
		FillLayout layout = new FillLayout();
		layout.marginHeight = marginHeight;
		layout.marginWidth = 1;
		cell.setLayout(layout);
		toolkit.paintBordersFor(cell);
		return cell;
	}
	
	private Control createTree(Composite parent, FormToolkit toolkit) {
		Tree tree = toolkit.createTree(parent, SWT.SINGLE);

		treeViewer = new TreeViewer(tree);
		treeViewer.setLabelProvider(new GrammarLabelProvider());
		treeViewer.setContentProvider(new GrammarContentProvider());
		treeViewer.setAutoExpandLevel(999);
		treeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				getPage().getPDEEditor().setSelection(e.getSelection());
			}
		});
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.setRemoveAllWhenShown(true);
		popupMenuManager.addMenuListener(listener);
		Menu menu = popupMenuManager.createContextMenu(tree);
		tree.setMenu(menu);
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				propertiesAction.run();
			}
		});
		return tree;
	}
	public void dispose() {
		ISchema schema = (ISchema) getPage().getModel();
		if (schema!= null) schema.removeModelChangedListener(this);
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
		return false;
	}
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = treeViewer.getSelection();
		final Object object =
			((IStructuredSelection) selection).getFirstElement();
		ISchemaElement sourceElement = (ISchemaElement) treeViewer.getInput();

		if (sourceElement != null) {
			ISchema schema = sourceElement.getSchema();

			MenuManager submenu =
				new MenuManager(PDEUIMessages.Menus_new_label);
			MenuManager cmenu =
				new MenuManager(
					PDEUIMessages.SchemaEditor_GrammarSection_compositor);

			cmenu.add(
				new NewCompositorAction(
					sourceElement,
					object,
					ISchemaCompositor.ALL));
			cmenu.add(
				new NewCompositorAction(
					sourceElement,
					object,
					ISchemaCompositor.CHOICE));
			cmenu.add(
				new NewCompositorAction(
					sourceElement,
					object,
					ISchemaCompositor.SEQUENCE));
			cmenu.add(
				new NewCompositorAction(
					sourceElement,
					object,
					ISchemaCompositor.GROUP));
			submenu.add(cmenu);

			if (schema.getResolvedElementCount() > 1
				&& object != null
				&& object instanceof SchemaCompositor) {
				MenuManager refMenu =
					new MenuManager(
						PDEUIMessages.SchemaEditor_GrammarSection_reference);
				ISchemaElement[] elements = schema.getResolvedElements();
				for (int i = 0; i < elements.length; i++) {
					ISchemaElement element = elements[i];
					//if (element == sourceElement)
					//continue;
					refMenu.add(
						new NewReferenceAction(sourceElement, object, element));
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
				deleteAction.setText(PDEUIMessages.Actions_delete_label);
				deleteAction.setEnabled(schema.isEditable());
				manager.add(deleteAction);
			}
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		manager.add(propertiesAction);
	}
	private void handleDelete(Object object) {
		if (object instanceof SchemaCompositor) {
			SchemaCompositor compositor = (SchemaCompositor) object;
			ISchemaObject parent = compositor.getParent();
			if (parent instanceof ISchemaElement) {
				// root
				SchemaElement element = (SchemaElement) parent;
				SchemaComplexType complexType =
					(SchemaComplexType) element.getType();
				if (complexType.getAttributeCount() == 0)
					element.setType(
						new SchemaSimpleType(element.getSchema(), "string")); //$NON-NLS-1$
				else
					complexType.setCompositor(null);
			} else if (parent instanceof SchemaCompositor) {
				((SchemaCompositor) parent).removeChild(compositor);
			}
		} else if (object instanceof SchemaElementReference) {
			SchemaCompositor compositor =
				(SchemaCompositor) ((SchemaElementReference) object)
					.getCompositor();
			compositor.removeChild((SchemaElementReference) object);
		}
	}
	public void initialize() {
		ISchema schema = (ISchema) getPage().getModel();
		schema.addModelChangedListener(this);
	}
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof ISchemaCompositor
			|| obj instanceof ISchemaObjectReference) {
			final ISchemaObject sobj = (ISchemaObject) obj;
			ISchemaObject parent = sobj.getParent();
			if (e.getChangeType() == IModelChangedEvent.CHANGE) {
				treeViewer.update(sobj, null);
			} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
				treeViewer.add(parent, sobj);
				treeViewer.getTree().getDisplay().asyncExec(new Runnable() {
					public void run() {
						treeViewer.setSelection(
							new StructuredSelection(sobj),
							true);
					}
				});

			} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
				treeViewer.remove(sobj);
				treeViewer.setSelection(new StructuredSelection(parent), true);
			}
		} else if (obj instanceof ISchemaComplexType) {
			// first compositor added/removed
			treeViewer.refresh();
			if (e.getChangeType() == IModelChangedEvent.INSERT) {
				ISchemaComplexType type = (ISchemaComplexType) obj;
				final ISchemaCompositor compositor = type.getCompositor();
				treeViewer.getTree().getDisplay().asyncExec(new Runnable() {
					public void run() {
						treeViewer.setSelection(
							new StructuredSelection(compositor),
							true);
					}
				});
			}
		} else if (obj instanceof ISchemaElement) {
			if (e.getChangeType() == IModelChangedEvent.CHANGE
				&& e.getChangedProperty() == SchemaElement.P_TYPE) {
				treeViewer.refresh();
			}
		}

		updateDTDLabel((ISchemaObject) treeViewer.getInput());
	}
	public void refresh() {
		treeViewer.refresh();
		super.refresh();
	}
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (!(part instanceof ElementSection))
			return;
		Object changeObject = ((IStructuredSelection)selection).getFirstElement();
		if (changeObject instanceof ISchemaAttribute) {
			changeObject = ((ISchemaAttribute) changeObject).getParent();
		}
		if (changeObject == treeViewer.getInput())
			return;
		ISchemaObject element = (ISchemaObject) changeObject;
		updateDTDLabel(element);
		treeViewer.setInput(changeObject);
	}
	private void updateDTDLabel(ISchemaObject object) {
		String prefix = PDEUIMessages.SchemaEditor_GrammarSection_dtd + "\n"; //$NON-NLS-1$
		String text = ""; //$NON-NLS-1$
		if (object != null) {
			ISchemaElement element = (ISchemaElement) object;
			text = element.getDTDRepresentation(false);
		}
		dtdLabel.setText(prefix + text);
	}
	protected void handleDoubleClick(IStructuredSelection selection) {
		propertiesAction.run();
	}
}
