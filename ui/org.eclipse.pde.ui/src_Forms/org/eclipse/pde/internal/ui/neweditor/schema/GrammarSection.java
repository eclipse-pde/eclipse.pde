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

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.SWT;
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
	public static final String SECTION_TITLE =
		"SchemaEditor.GrammarSection.title";
	public static final String SECTION_COMPOSITOR =
		"SchemaEditor.GrammarSection.compositor";
	public static final String SECTION_REFERENCE =
		"SchemaEditor.GrammarSection.reference";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String SECTION_DESC =
		"SchemaEditor.GrammarSection.desc";
	public static final String KEY_DTD = "SchemaEditor.GrammarSection.dtd";

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
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
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

		Control tree = createTree(container, toolkit);
		GridData gd = new GridData(GridData.FILL_BOTH);
		/*
		if (SWT.getPlatform().equals("motif") == false)
			gd.heightHint = 150;
		//gd.widthHint = 200;
		 */
		tree.setLayoutData(gd);

		dtdLabel =
			toolkit.createText(
				container,
				"",
				SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		dtdLabel.setData(
			FormToolkit.KEY_DRAW_BORDER,
			FormToolkit.TREE_BORDER);
		dtdLabel.setEditable(false);
		dtdLabel.setForeground(
			toolkit.getColors().getColor(FormColors.TITLE));
		gd = new GridData(GridData.FILL_BOTH);
		dtdLabel.setLayoutData(gd);
		updateDTDLabel(null);

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
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
		return tree;
	}
	public void dispose() {
		ISchema schema = (ISchema) getPage().getModel();
		schema.removeModelChangedListener(this);
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
				new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));
			MenuManager cmenu =
				new MenuManager(
					PDEPlugin.getResourceString(SECTION_COMPOSITOR));

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
						PDEPlugin.getResourceString(SECTION_REFERENCE));
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
				deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
				deleteAction.setEnabled(schema.isEditable());
				manager.add(deleteAction);
			}
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		manager.add(new PropertiesAction(getPage().getPDEEditor()));
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
						new SchemaSimpleType(element.getSchema(), "string"));
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
		String prefix = PDEPlugin.getResourceString(KEY_DTD) + "\n";
		String text = "";
		if (object != null) {
			ISchemaElement element = (ISchemaElement) object;
			text = element.getDTDRepresentation(false);
		}
		dtdLabel.setText(prefix + text);
	}
}