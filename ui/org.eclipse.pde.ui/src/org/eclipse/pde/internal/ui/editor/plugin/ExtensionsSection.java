/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@gentleware.com> - bug 194529, bug 196867
 *     Sascha Becher <s.becher@qualitype.com> - bug 360894
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.plugin.PluginBaseNode;
import org.eclipse.pde.internal.core.text.plugin.PluginExtensionNode;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.*;
import org.eclipse.pde.internal.ui.editor.contentassist.XMLElementProposalComputer;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.pde.internal.ui.search.ExtensionsPatternFilter;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.extension.ExtensionEditorWizard;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionWizard;
import org.eclipse.pde.ui.IExtensionEditorWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.BidiUtil;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.WorkbenchJob;

public class ExtensionsSection extends TreeSection implements IModelChangedListener, IPropertyChangeListener {
	private static final int REFRESHJOB_DELAY_TIME = 1200; // milliseconds to wait
	private static final int ACCELERATED_SCROLLING = 15; // lines to skip

	// All constants changed for removal of search button
	private static final int BUTTON_MOVE_DOWN = 4;
	private static final int BUTTON_MOVE_UP = 3;
	private static final int BUTTON_EDIT = 2;
	private static final int BUTTON_REMOVE = 1;
	private static final int BUTTON_ADD = 0;
	private TreeViewer fExtensionTree;
	private Image fExtensionImage;
	private Image fGenericElementImage;
	private FormFilteredTree fFilteredTree;
	private ExtensionsPatternFilter fPatternFilter;
	private SchemaRegistry fSchemaRegistry;
	private Hashtable fEditorWizards;
	private SortAction fSortAction;
	private CollapseAction fCollapseAction;
	private ToggleExpandStateAction fExpandAction;
	private FilterRelatedExtensionsAction fFilterRelatedAction;
	private boolean fBypassFilterDelay = false;

	/**
	 * <code>label, name, class, id, commandId, property, activityId, attribute, value</code>
	 * <br>
	 * While adding elements to the array at the end is possible without concern, changing 
	 * previous elements requires to refactor occurrences with indexed access to the array.
	 */
	// TODO common label properties might be configured through preferences
	public static final String[] COMMON_LABEL_ATTRIBUTES = {"label", //$NON-NLS-1$
			"name", "locationURI", "class", "id", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"commandId", "property", "activityId", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"attribute", "value"}; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String[] VALID_IMAGE_TYPES = {"png", "bmp", "ico", "gif", "jpg", "tiff"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	private static final String MENU_NEW_ID = "NewMenu"; //$NON-NLS-1$

	class ExtensionContentProvider extends DefaultContentProvider implements ITreeContentProvider {
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
			if (child instanceof IPluginExtension) {
				return ((IPluginModelBase) getPage().getModel()).getPluginBase();
			}
			if (child instanceof IPluginObject)
				return ((IPluginObject) child).getParent();
			return null;
		}

		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
	}

	class ExtensionLabelProvider extends LabelProvider implements IFontProvider {
		public String getText(Object obj) {
			return resolveObjectName(obj);
		}

		public Image getImage(Object obj) {
			return resolveObjectImage(obj);
		}

		public Font getFont(Object element) {
			if (fFilteredTree.isFiltered() && fPatternFilter.getMatchingLeafs().contains(element)) {
				return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
			}
			return null;
		}
	}

	public ExtensionsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {/*PDEUIMessages.Actions_search_targetplatform,*/PDEUIMessages.ManifestEditor_DetailExtension_new, PDEUIMessages.ManifestEditor_DetailExtension_remove, PDEUIMessages.ManifestEditor_DetailExtension_edit, PDEUIMessages.ManifestEditor_DetailExtension_up, PDEUIMessages.ManifestEditor_DetailExtension_down});
		fHandleDefaultButton = false;
	}

	private static void addItemsForExtensionWithSchema(MenuManager menu, IPluginExtension extension, IPluginParent parent) {
		ISchema schema = getSchema(extension);
		// Bug 213457 - look up elements based on the schema in which the parent is found
		ISchemaElement elementInfo = null;
		if (schema.getIncludes().length == 0 || parent == extension) {
			String tagName = (parent == extension ? "extension" : parent.getName()); //$NON-NLS-1$
			elementInfo = schema.findElement(tagName);
		} else {
			Stack stack = new Stack();
			IPluginParent parentParent = parent;
			while (parentParent != extension && parentParent != null) {
				stack.push(parentParent.getName());
				parentParent = (IPluginParent) parentParent.getParent();
			}
			while (!stack.isEmpty()) {
				elementInfo = schema.findElement((String) stack.pop());
				schema = elementInfo.getSchema();
			}
		}

		if ((elementInfo != null) && (elementInfo.getType() instanceof ISchemaComplexType) && (parent instanceof IDocumentElementNode)) {
			// We have a schema complex type.  Either the element has attributes
			// or the element has children.
			// Generate the list of element proposals
			TreeSet elementSet = XMLElementProposalComputer.computeElementProposal(elementInfo, (IDocumentElementNode) parent);

			// Create a corresponding menu entry for each element proposal
			Iterator iterator = elementSet.iterator();
			while (iterator.hasNext()) {
				Action action = new NewElementAction((ISchemaElement) iterator.next(), parent);
				menu.add(action);
			}
		}
	}

	/**
	 * @param parent
	 */
	private static ISchema getSchema(IPluginParent parent) {
		if (parent instanceof IPluginExtension) {
			return getSchema((IPluginExtension) parent);
		} else if (parent instanceof IPluginElement) {
			return getSchema((IPluginElement) parent);
		} else {
			return null;
		}
	}

	private static ISchema getSchema(IPluginExtension extension) {
		String point = extension.getPoint();
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		return registry.getSchema(point);
	}

	/**
	 * @param element
	 */
	static ISchemaElement getSchemaElement(IPluginElement element) {
		ISchema schema = getSchema(element);
		if (schema != null) {
			return schema.findElement(element.getName());
		}
		return null;
	}

	/**
	 * @param element
	 */
	private static ISchema getSchema(IPluginElement element) {
		IPluginObject parent = element.getParent();
		while (parent != null && !(parent instanceof IPluginExtension)) {
			parent = parent.getParent();
		}
		if (parent != null) {
			return getSchema((IPluginExtension) parent);
		}
		return null;
	}

	public void createClient(Section section, FormToolkit toolkit) {
		initializeImages();
		Composite container = createClientContainer(section, 2, toolkit);
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI | SWT.BORDER, 2, toolkit);

		fExtensionTree = treePart.getTreeViewer();
		fExtensionTree.setContentProvider(new ExtensionContentProvider());
		fExtensionTree.setLabelProvider(new ExtensionLabelProvider());
		toolkit.paintBordersFor(container);
		section.setClient(container);
		section.setDescription(PDEUIMessages.ExtensionsSection_sectionDescExtensionsMaster);
		// See Bug # 160554: Set text before text client
		section.setText(PDEUIMessages.ManifestEditor_DetailExtension_title);
		initialize((IPluginModelBase) getPage().getModel());
		createSectionToolbar(section, toolkit);
		// accelerated tree scrolling enabled
		fFilteredTree.addMouseWheelListener(new AcceleratedTreeScrolling(fExtensionTree.getTree(), ACCELERATED_SCROLLING));
		toolkit.paintBordersFor(fFilteredTree.getParent());
		// Create the adapted listener for the filter entry field
		fFilteredTree.createUIListenerEntryFilter(this);
		final Text filterText = fFilteredTree.getFilterControl();
		if (filterText != null) {
			filterText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					StructuredViewer viewer = getStructuredViewerPart().getViewer();
					IStructuredSelection ssel = (IStructuredSelection) viewer.getSelection();
					updateButtons(ssel.size() != 1 ? null : ssel);
				}
			});
		}
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
				handCursor.dispose();
			}
		});
		// Add sort action to the tool bar
		fSortAction = new SortAction(fExtensionTree, PDEUIMessages.ExtensionsPage_sortAlpha, null, null, this) {
			public void run() {
				Object[] expanded = fFilteredTree.getViewer().getVisibleExpandedElements();
				try {
					fFilteredTree.setRedraw(false);
					super.run();
					// bugfix: retain tree expand state after sort action
					fFilteredTree.getViewer().setExpandedElements(expanded);
				} finally {
					fFilteredTree.setRedraw(true);
				}
			}
		};
		toolBarManager.add(fSortAction);
		// Add expand selected leafs action to the toolbar
		fExpandAction = new ToggleExpandStateAction(fFilteredTree, fExtensionTree);
		toolBarManager.add(fExpandAction);
		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fExtensionTree, PDEUIMessages.ExtensionsPage_collapseAll);
		toolBarManager.add(fCollapseAction);

		// Create filter action for context menu and global find keybinding
		fFilterRelatedAction = new FilterRelatedExtensionsAction(fExtensionTree, fFilteredTree, this);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons(selection);
		getTreePart().getButton(BUTTON_EDIT).setVisible(isSelectionEditable(selection));
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case BUTTON_ADD :
				handleNew();
				break;
			case BUTTON_REMOVE :
				handleDelete();
				break;
			case BUTTON_EDIT :
				handleEdit();
				break;
			case BUTTON_MOVE_UP :
				handleMove(true);
				break;
			case BUTTON_MOVE_DOWN :
				handleMove(false);
				break;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// Explicitly call the dispose method on the extensions tree
		if (fFilteredTree != null) {
			fFilteredTree.dispose();
		}
		fEditorWizards = null;
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor().getAggregateModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.FIND.getId()) && fFilterRelatedAction != null) {
			fFilterRelatedAction.run();
			return true;
		}
		if (!isEditable()) {
			return false;
		}
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			if (isSingleSelection()) {
				handleDelete();
			}
			return true;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			if (isSingleSelection()) {
				doPaste();
			}
			return true;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			handleSelectAll();
			return true;
		}

		return false;
	}

	public boolean setFormInput(Object object) {
		if (object instanceof IPluginExtension || object instanceof IPluginElement) {
			fExtensionTree.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fExtensionTree.getSelection();
		final IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			Object object = ssel.getFirstElement();
			if (object instanceof IPluginParent) {
				IPluginParent parent = (IPluginParent) object;
				if (parent.getModel().getUnderlyingResource() != null) {
					boolean removeEnabled = !fFilteredTree.isFiltered() || isRemoveEnabled(ssel);
					fillContextMenu(getPage(), parent, manager, false, removeEnabled);
					manager.add(new Separator());
				}
			}
			manager.add(new Separator());
			if (object instanceof IPluginExtension) {
				PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				manager.add(new Separator());
			}
		} else if (ssel.size() > 1) {
			// Add delete action
			Action delAction = new Action() {
				public ImageDescriptor getImageDescriptor() {
					return PDEPluginImages.DESC_DELETE;
				}

				public ImageDescriptor getDisabledImageDescriptor() {
					return PDEPluginImages.DESC_REMOVE_ATT_DISABLED;
				}

				public void run() {
					handleDelete();
				}
			};
			delAction.setText(PDEUIMessages.ExtensionsSection_Remove);
			manager.add(delAction);
			manager.add(new Separator());
			delAction.setEnabled(isEditable() && isRemoveEnabled(ssel));
		}
		if (ssel.size() > 0) {
			if (ExtensionsFilterUtil.isFilterRelatedEnabled(ssel)) {
				manager.add(fFilterRelatedAction);
			}
		}
		if (fFilteredTree.isFiltered()) {
			// Add action to clear the current filtering
			manager.add(new Action() {
				public String getText() {
					return PDEUIMessages.ShowAllExtensionsAction_label;
				}

				public void run() {
					Text filterText = fFilteredTree.getFilterControl();
					setBypassFilterDelay(true);
					filterText.setText(""); //$NON-NLS-1$
				}

			});
		}

		manager.add(new Separator());
		if (ssel.size() < 2) { // only cut things when the selection is one
			getPage().getPDEEditor().getContributor().addClipboardActions(manager);
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager, false);
		this.fFilteredTree.update();
	}

	static IMenuManager fillContextMenu(PDEFormPage page, final IPluginParent parent, IMenuManager manager) {
		return fillContextMenu(page, parent, manager, false);
	}

	static IMenuManager fillContextMenu(PDEFormPage page, final IPluginParent parent, IMenuManager manager, boolean addSiblingItems) {
		return fillContextMenu(page, parent, manager, addSiblingItems, true);
	}

	static IMenuManager fillContextMenu(PDEFormPage page, final IPluginParent parent, IMenuManager manager, boolean addSiblingItems, boolean fullMenu) {
		MenuManager menu = new MenuManager(PDEUIMessages.Menus_new_label, MENU_NEW_ID);
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
			Action deleteAction = new Action(PDEUIMessages.ExtensionsSection_Remove) {
				public void run() {
					try {
						IPluginObject parentsParent = parent.getParent();
						if (parent instanceof IPluginExtension) {
							IPluginBase plugin = (IPluginBase) parentsParent;
							plugin.remove((IPluginExtension) parent);
						} else {
							IPluginParent parentElement = (IPluginParent) parent.getParent();
							parentElement.remove(parent);
						}
					} catch (CoreException e) {
					}
				}

				public ImageDescriptor getImageDescriptor() {
					return PDEPluginImages.DESC_DELETE;
				}

				public ImageDescriptor getDisabledImageDescriptor() {
					return PDEPluginImages.DESC_REMOVE_ATT_DISABLED;
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
		IStructuredSelection sel = (IStructuredSelection) fExtensionTree.getSelection();
		if (sel.isEmpty())
			return;
		for (Iterator iter = sel.iterator(); iter.hasNext();) {
			IPluginObject object = (IPluginObject) iter.next();
			try {
				IStructuredSelection newSelection = null;
				boolean sorted = fSortAction != null && fSortAction.isChecked();
				if (object instanceof IPluginElement) {
					IPluginElement ee = (IPluginElement) object;
					IPluginParent parent = (IPluginParent) ee.getParent();
					if (!sorted) {
						int index = getNewSelectionIndex(parent.getIndexOf(ee), parent.getChildCount());
						newSelection = index == -1 ? new StructuredSelection(parent) : new StructuredSelection(parent.getChildren()[index]);
					} else {
						IPluginObject original[] = parent.getChildren();
						IPluginObject objects[] = new IPluginObject[original.length];
						for (int i = 0; i < original.length; i++)
							objects[i] = original[i];
						fExtensionTree.getComparator().sort(fExtensionTree, objects);
						int index = getNewSelectionIndex(getArrayIndex(objects, ee), objects.length);
						newSelection = index == -1 ? new StructuredSelection(parent) : new StructuredSelection(objects[index]);
					}
					parent.remove(ee);
				} else if (object instanceof IPluginExtension) {
					IPluginExtension extension = (IPluginExtension) object;
					IPluginBase plugin = extension.getPluginBase();
					if (!sorted) {
						int index = getNewSelectionIndex(plugin.getIndexOf(extension), plugin.getExtensions().length);
						if (index != -1)
							newSelection = new StructuredSelection(plugin.getExtensions()[index]);
					} else {
						IPluginExtension original[] = plugin.getExtensions();
						IPluginExtension extensions[] = new IPluginExtension[original.length];
						for (int i = 0; i < original.length; i++)
							extensions[i] = original[i];
						fExtensionTree.getComparator().sort(fExtensionTree, extensions);
						int index = getNewSelectionIndex(getArrayIndex(extensions, extension), extensions.length);
						if (index != -1)
							newSelection = new StructuredSelection(extensions[index]);
					}
					plugin.remove(extension);
				}
				if (newSelection != null)
					fExtensionTree.setSelection(newSelection);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	public FormFilteredTree getFormFilteredTree() {
		return fFilteredTree;
	}

	/**
	 * Adds another value to filter text and a preceding separator character if necessary.
	 * Empty values as well as <code>true</code> and <code>false</code> are omitted.
	 * 
	 * @param attributeValue Value to be trimmed and added to the filter text
	 * @param clearFilterText When <code>true</code> the filter text is replaced with the attribute value, appended otherwise.
	 */
	public void addAttributeToFilter(String attributeValue, boolean clearFilterText) {
		Text filterControl = fFilteredTree.getFilterControl();
		if (filterControl != null && attributeValue != null) {
			String trimmedValue = attributeValue.trim();
			if (trimmedValue.length() > 0 && !ExtensionsFilterUtil.isBoolean(trimmedValue)) {
				if (trimmedValue.startsWith("%")) {//$NON-NLS-1$
					IPluginModelBase model = getPluginModelBase();
					trimmedValue = ((model != null) ? model.getResourceString(trimmedValue) : trimmedValue).replaceAll("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				String filterPattern;
				if (clearFilterText) {
					filterPattern = trimmedValue;
				} else {
					filterPattern = filterControl.getText();
					if (filterPattern.length() > 0 && !filterPattern.endsWith("/")) { //$NON-NLS-1$
						filterPattern += "/"; //$NON-NLS-1$
					}
					filterPattern += trimmedValue;
				}
				if (filterPattern.indexOf('/') != -1) { // quote when
					filterPattern = "\"" + filterPattern + "\""; //$NON-NLS-1$ //$NON-NLS-2$
				}
				setBypassFilterDelay(true); // force immediate job run
				filterControl.setText(filterPattern);
			}
		}
	}

	private void handleNew() {
		final IProject project = getPage().getPDEEditor().getCommonProject();
		BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(), new Runnable() {
			public void run() {
				((ManifestEditor) getPage().getEditor()).ensurePluginContextPresence();
				NewExtensionWizard wizard = new NewExtensionWizard(project, (IPluginModelBase) getPage().getModel(), (ManifestEditor) getPage().getPDEEditor()) {
					public boolean performFinish() {
						return super.performFinish();
					}
				};
				WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 500, 500);
				dialog.open();
			}
		});
	}

	private void handleEdit(IConfigurationElement element, IStructuredSelection selection) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		try {
			final IExtensionEditorWizard wizard = (IExtensionEditorWizard) element.createExecutableExtension("class"); //$NON-NLS-1$
			wizard.init(project, model, selection);
			BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(), new Runnable() {
				public void run() {
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 500, 500);
					dialog.open();
				}
			});
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleEdit() {
		final IStructuredSelection selection = (IStructuredSelection) fExtensionTree.getSelection();
		ArrayList editorWizards = getEditorWizards(selection);
		if (editorWizards == null)
			return;
		if (editorWizards.size() == 1) {
			// open the wizard directly			
			handleEdit((IConfigurationElement) editorWizards.get(0), selection);
		} else {
			IProject project = getPage().getPDEEditor().getCommonProject();
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			final ExtensionEditorWizard wizard = new ExtensionEditorWizard(project, model, selection);
			BusyIndicator.showWhile(fExtensionTree.getTree().getDisplay(), new Runnable() {
				public void run() {
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 500, 500);
					dialog.open();
				}
			});
		}
	}

	private void handleSelectAll() {
		fExtensionTree.getTree().selectAll();
		updateButtons(fFilteredTree.getViewer().getSelection());
	}

	private ArrayList getEditorWizards(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;
		Object obj = selection.getFirstElement();
		String pointId = null;
		if (obj instanceof IPluginExtension) {
			pointId = ((IPluginExtension) obj).getPoint();
		} else if (obj instanceof IPluginElement) {
			IPluginObject parent = ((IPluginElement) obj).getParent();
			while (parent != null) {
				if (parent instanceof IPluginExtension) {
					pointId = ((IPluginExtension) parent).getPoint();
					break;
				}
				parent = parent.getParent();
			}
		}
		if (pointId == null)
			return null;
		if (fEditorWizards == null)
			loadExtensionWizards();
		return (ArrayList) fEditorWizards.get(pointId);
	}

	private void loadExtensionWizards() {
		fEditorWizards = new Hashtable();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.pde.ui.newExtension"); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equals("editorWizard")) { //$NON-NLS-1$
				String pointId = element.getAttribute("point"); //$NON-NLS-1$
				if (pointId == null)
					continue;
				ArrayList list = (ArrayList) fEditorWizards.get(pointId);
				if (list == null) {
					list = new ArrayList();
					fEditorWizards.put(pointId, list);
				}
				list.add(element);
			}
		}
	}

	private boolean isSelectionEditable(IStructuredSelection selection) {
		if (!getPage().getModel().isEditable())
			return false;
		return getEditorWizards(selection) != null;
	}

	public void initialize(IPluginModelBase model) {
		fExtensionTree.setInput(model.getPluginBase());
		selectFirstExtension();
		boolean editable = model.isEditable();
		TreePart treePart = getTreePart();
		treePart.setButtonEnabled(BUTTON_ADD, editable);
		treePart.setButtonEnabled(BUTTON_REMOVE, false);
		treePart.setButtonEnabled(BUTTON_EDIT, false);
		treePart.setButtonEnabled(BUTTON_MOVE_UP, false);
		treePart.setButtonEnabled(BUTTON_MOVE_DOWN, false);
		model.addModelChangedListener(this);
	}

	private void selectFirstExtension() {
		Tree tree = fExtensionTree.getTree();
		TreeItem[] items = tree.getItems();
		if (items.length == 0)
			return;
		TreeItem firstItem = items[0];
		Object obj = firstItem.getData();
		fExtensionTree.setSelection(new StructuredSelection(obj));
	}

	void fireSelection() {
		fExtensionTree.setSelection(fExtensionTree.getSelection());
	}

	public void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		fExtensionImage = provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
		fGenericElementImage = provider.get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	}

	public void refresh() {
		// The model changed but the editor is still open, we should try to retain expansion, selection will be retained on its own
		Object[] expanded = fExtensionTree.getExpandedElements();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		int[] indexPath = getTreeIndexPath(fExtensionTree.getTree());
		try {
			fExtensionTree.getControl().setRedraw(false);
			fExtensionTree.setInput(model.getPluginBase());
			fExtensionTree.setExpandedElements(expanded);

			reportMissingExtensionPointSchemas(model.getPluginBase());
			getManagedForm().fireSelectionChanged(ExtensionsSection.this, fExtensionTree.getSelection());
			super.refresh();

			if (indexPath != null) {
				// fix for Bug 371066
				revealTopItem(fExtensionTree.getTree(), indexPath);
			}
		} finally {
			fExtensionTree.getControl().setRedraw(true);
		}
	}

	private static int[] getTreeIndexPath(Tree tree) {
		int[] indexPath = null;
		if (tree != null) {
			TreeItem item = tree.getTopItem();
			int count = 1;
			while (item != null && (item = item.getParentItem()) != null) {
				count++;
			}
			indexPath = new int[count];
			int index = 0;
			item = tree.getTopItem();
			while (item != null && index < count) {
				TreeItem parent = item.getParentItem();
				if (parent != null) {
					indexPath[index++] = parent.indexOf(item);
				} else {
					indexPath[index++] = tree.indexOf(item);
				}
				item = parent;
			}
		}
		return indexPath;
	}

	private static void revealTopItem(Tree tree, int[] indexPath) {
		TreeItem itemFound = null;
		for (int i = indexPath.length - 1; i >= 0; i--) {
			int index = indexPath[i];
			if (itemFound != null) {
				itemFound = (itemFound.getItemCount() > index) ? itemFound.getItem(indexPath[i]) : null;
			} else if (i == indexPath.length - 1) {
				itemFound = (tree.getItemCount() > index) ? tree.getItem(indexPath[i]) : null;
			}
		}
		if (itemFound != null) {
			tree.setTopItem(itemFound);
		}
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginBase && event.getChangeType() == IModelChangedEvent.CHANGE && event.getChangedProperty().equals(IExtensions.P_EXTENSION_ORDER)) {
			IStructuredSelection sel = (IStructuredSelection) fExtensionTree.getSelection();
			IPluginExtension extension = (IPluginExtension) sel.getFirstElement();
			fExtensionTree.refresh();
			fExtensionTree.setSelection(new StructuredSelection(extension));
			return;
		}
		if (changeObject instanceof IPluginExtension || (changeObject instanceof IPluginElement && ((IPluginElement) changeObject).getParent() instanceof IPluginParent)) {
			IPluginObject pobj = (IPluginObject) changeObject;
			IPluginObject parent = changeObject instanceof IPluginExtension ? ((IPluginModelBase) getPage().getModel()).getPluginBase() : pobj.getParent();
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				// enables adding extensions while tree is filtered
				if (fFilteredTree.isFiltered()) {
					Object[] inserted = event.getChangedObjects();
					for (int i = 0; i < inserted.length; i++) {
						fPatternFilter.addElement(inserted[i]);
					}
					if (inserted.length == 1) {
						fFilteredTree.getViewer().setSelection(new StructuredSelection(inserted[0]));
					}
				}

				//
				fExtensionTree.refresh(parent);
				if (changeObject instanceof IPluginExtension) {
					IPluginExtension ext = (IPluginExtension) changeObject;
					if (ext.getSchema() == null)
						reportMissingExtensionPointSchema(ext.getPoint());
				}
				fExtensionTree.setSelection(new StructuredSelection(changeObject), true);
				fExtensionTree.getTree().setFocus();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				if (changeObject instanceof IPluginExtension) {
					IPluginExtension ext = (IPluginExtension) changeObject;
					IPluginExtension[] extensions = ((IPluginBase) parent).getExtensions();
					boolean found = false;
					// search if there is at least another extension extending the same point than the one being removed
					for (int i = 0; i < extensions.length; i++) {
						String point = extensions[i].getPoint();
						if (ext.getPoint().equals(point)) {
							found = true;
							break;
						}
					}
					if (!found)
						getManagedForm().getMessageManager().removeMessage(ext.getPoint());
				}
				fExtensionTree.remove(pobj);
			} else {
				if (event.getChangedProperty().equals(IPluginParent.P_SIBLING_ORDER)) {
					IStructuredSelection sel = (IStructuredSelection) fExtensionTree.getSelection();
					IPluginObject child = (IPluginObject) sel.getFirstElement();
					fExtensionTree.refresh(child.getParent());
					fExtensionTree.setSelection(new StructuredSelection(child));
				} else {
					fExtensionTree.update(changeObject, null);
				}
			}
		}
	}

	private Image resolveObjectImage(Object obj) {
		if (obj instanceof IPluginExtension) {
			return fExtensionImage;
		}
		Image elementImage = fGenericElementImage;
		if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			Image customImage = getCustomImage(element);
			if (customImage != null)
				elementImage = customImage;
		}
		return elementImage;
	}

	private static boolean isStorageModel(IPluginObject object) {
		IPluginModelBase modelBase = object.getPluginModel();
		return modelBase.getInstallLocation() == null;
	}

	static Image getCustomImage(IPluginElement element) {
		if (isStorageModel(element))
			return null;
		ISchemaElement elementInfo = getSchemaElement(element);
		if (elementInfo != null && elementInfo.getIconProperty() != null) {
			String iconProperty = elementInfo.getIconProperty();
			IPluginAttribute att = element.getAttribute(iconProperty);
			String iconPath = null;
			if (att != null && att.getValue() != null) {
				iconPath = att.getValue();
			}
			// we have a value from a resource attribute
			if (iconPath != null) {
				String ext = new Path(iconPath).getFileExtension();
				// if the resource targets a folder, the file extension will be null
				if (ext == null)
					return null;
				boolean valid = false;
				// ensure the resource is an image
				for (int i = 0; i < VALID_IMAGE_TYPES.length; i++) {
					if (ext.equalsIgnoreCase(VALID_IMAGE_TYPES[i])) {
						valid = true;
						break;
					}
				}
				// if the resource is an image, get the image, otherwise return null
				return valid ? getImageFromPlugin(element, iconPath) : null;
			}
		}
		return null;
	}

	private static Image getImageFromPlugin(IPluginElement element, String iconPathName) {
		// 39283 - ignore icon paths that
		// point at plugin.properties
		if (iconPathName.startsWith("%")) //$NON-NLS-1$
			return null;

		IPluginModelBase model = element.getPluginModel();
		if (model == null)
			return null;

		return PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(model, iconPathName);
	}

	private String resolveObjectName(Object obj) {
		return resolveObjectName(getSchemaRegistry(), obj);
	}

	private SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = PDECore.getDefault().getSchemaRegistry();
		return fSchemaRegistry;
	}

	public static String resolveObjectName(SchemaRegistry schemaRegistry, Object obj) {
		boolean fullNames = PDEPlugin.isFullNameModeEnabled();
		if (obj instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) obj;
			if (!fullNames) {
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
			return extension.getPoint();
		} else if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			String baseName = element.getName();
			String fullName = null;
			ISchemaElement elementInfo = getSchemaElement(element);
			IPluginAttribute labelAtt = null;
			if (elementInfo != null && elementInfo.getLabelProperty() != null) {
				labelAtt = element.getAttribute(elementInfo.getLabelProperty());
			}
			if (labelAtt == null) {
				// try some hard-coded attributes that
				// are used frequently
				for (int i = 0; i < COMMON_LABEL_ATTRIBUTES.length; i++) {
					labelAtt = element.getAttribute(COMMON_LABEL_ATTRIBUTES[i]);
					if (labelAtt != null && labelAtt.getValue().length() > 0)
						break;
				}
				if (labelAtt == null) {
					// Last try - if there is only one attribute,
					// use that
					if (element.getAttributeCount() == 1)
						labelAtt = element.getAttributes()[0];
				}
			}
			if (labelAtt != null && labelAtt.getValue() != null) {
				fullName = stripShortcuts(labelAtt.getValue());
				if (labelAtt.getName().equals(COMMON_LABEL_ATTRIBUTES[3])) { // remove package from handler class 
					fullName = fullName.substring(fullName.lastIndexOf('.') + 1, fullName.length());
				}
			}
			fullName = element.getResourceString(fullName);

			if (fullNames)
				return fullName != null ? fullName : baseName;
			if (fullName == null)
				return baseName;
			// Bug 183417 - Bidi3.3: Elements' labels in the extensions page in the fragment manifest characters order is incorrect
			// add RTL zero length character just before the ( and the LTR character just after to ensure:
			// 1. The leading parenthesis takes proper orientation when running in BiDi configuration
			// Assumption: baseName (taken from the schema definition), is only Latin characters and is therefore always displayed LTR
			if (BidiUtil.isBidiPlatform())
				return fullName + " \u200f(\u200e" + baseName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			return fullName + " (" + baseName + ')'; //$NON-NLS-1$
		}
		if (obj != null) {
			return obj.toString();
		}
		return new String();
	}

	public void setFocus() {
		if (fExtensionTree != null)
			fExtensionTree.getTree().setFocus();
	}

	/**
	 * Temporarily bypasses default {@link FormFilteredTree#getRefreshJobDelay()} for several actions to immediately start tree 
	 * filtering. Only the next job to call <code>getRefreshJobDelay()</code> will be affected and reset this value.
	 * 
	 * @param bypassFilterDelay <code>true</code> bypasses the refresh job delay by overriding it with <code>0</code> 
	 */
	public void setBypassFilterDelay(boolean bypassFilterDelay) {
		fBypassFilterDelay = bypassFilterDelay;
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

	public boolean canCopy(ISelection selection) {
		// Partial fix for Bug 360079, enables Ctrl+C in filter text if plugin model is editable
		if (fFilteredTree.getFilterControl().isFocusControl() && !selection.isEmpty()) {
			return true;
		}
		// TODO enable copy also when plug-in model is not editable
		return super.canCopy(selection);
	}

	public boolean canPaste(Clipboard clipboard) {
		// Partial fix for Bug 360079, enables Ctrl+V in filter text if plugin model is editable
		if (fFilteredTree.getFilterControl().isFocusControl()) {
			return true;
		}
		// TODO enable paste also when plug-in model is not editable
		return super.canPaste(clipboard);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		// Note: Multi-select in is enabled and this function can support 
		// multiple source object but it needs to be investigated
		// Rule:  Element source objects are always pasted as children of the
		// target object (if allowable)
		// Rule:  Extension source objects are always pasted and are independent
		// of the target object
		// Ensure all the sourceObjects are either extensions or elements
		boolean allExtensions = true;
		boolean allElements = true;
		for (int i = 0; i < sourceObjects.length; i++) {
			if (sourceObjects[i] instanceof IPluginExtension) {
				allElements = false;
			} else if (sourceObjects[i] instanceof IPluginElement) {
				allExtensions = false;
			} else {
				return false;
			}
		}
		// Because of the extension rule, we can paste all extension source
		// objects
		if (allExtensions) {
			return true;
		}
		// Pasting a mixture of elements and extensions is not supported
		// (or wise from the users perspective)
		if (allElements == false) {
			return false;
		}
		// Ensure the target object can have children 
		if ((targetObject instanceof IPluginParent) == false) {
			return false;
		} else if ((targetObject instanceof IDocumentElementNode) == false) {
			return false;
		}
		// Retrieve the schema corresponding to the target object		
		IPluginParent targetParent = (IPluginParent) targetObject;
		ISchema schema = getSchema(targetParent);
		// If there is no schema, then a source object can be pasted as a 
		// child of any target object
		if (schema == null) {
			return true;
		}
		// Determine the element name of the target object
		String tagName = ((IDocumentElementNode) targetParent).getXMLTagName();
		// Retrieve the element schema for the target object
		ISchemaElement schemaElement = schema.findElement(tagName);
		// Ensure we found a schema element and it is a schema complex type
		if (schemaElement == null) {
			// Something is seriously wrong, we have a schema
			return false;
		} else if ((schemaElement.getType() instanceof ISchemaComplexType) == false) {
			// Something is seriously wrong, we are a plugin parent
			return false;
		}
		// We have a schema complex type.  Either the target object has 
		// attributes or the element has children.
		// Generate the list of element proposals
		TreeSet elementSet = XMLElementProposalComputer.computeElementProposal(schemaElement, (IDocumentElementNode) targetObject);
		// Determine whether we can paste the source elements as children of
		// the target object
		if (sourceObjects.length > 1) {
			IPluginElement[] sourcePluginElements = new IPluginElement[sourceObjects.length];
			System.arraycopy(sourceObjects, 0, sourcePluginElements, 0, sourceObjects.length);
			return canPasteSourceElements(sourcePluginElements, elementSet);
		}
		return canPasteSourceElement((IPluginElement) sourceObjects[0], elementSet);
	}

	/**
	 * @param sourceElements
	 * @param targetElementSet
	 */
	private boolean canPasteSourceElements(IPluginElement[] sourceElements, TreeSet targetElementSet) {
		// Performance optimisation
		// HashSet of schema elements is not comparable for the source
		// objects (schema elements are transient)
		// Create a new HashSet with element names for comparison		
		HashSet targetElementNameSet = new HashSet();
		Iterator iterator = targetElementSet.iterator();
		while (iterator.hasNext()) {
			targetElementNameSet.add(((ISchemaElement) iterator.next()).getName());
		}
		// Paste will be enabled only if all source objects can be pasted 
		// as children into the target element
		// Limitation:  Multiplicity checks will be compromised because we
		// are pasting multiple elements as a single transaction.  The 
		// multiplicity check is computed on the current static state of the
		// target object with the assumption one new element will be added.
		// Obviously, adding more than one element can invalidate the check
		// due to choice, sequence multiplicity constraints.  Even if source
		// elements that are pasted violate multiplicity constraints the 
		// extensions builder will flag them with errors
		for (int i = 0; i < sourceElements.length; i++) {
			String sourceTagName = sourceElements[i].getName();
			if (targetElementNameSet.contains(sourceTagName) == false) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param sourceElement
	 * @param targetElementSet
	 */
	private boolean canPasteSourceElement(IPluginElement sourceElement, TreeSet targetElementSet) {
		boolean canPaste = false;
		// Get the source element tag name
		String sourceTagName = sourceElement.getName();
		// Iterate over set of valid element proposals
		Iterator iterator = targetElementSet.iterator();
		while (iterator.hasNext()) {
			// Get the proposal element tag name
			String targetTagName = ((ISchemaElement) iterator.next()).getName();
			// Only a source element that is found within the set of element 
			// proposals can be pasted
			if (sourceTagName.equals(targetTagName)) {
				canPaste = true;
				break;
			}
		}
		return canPaste;
	}

	private IPluginModelBase getPluginModelBase() {
		// Note:  This method will work with fragments as long as a fragment.xml
		// is defined first.  Otherwise, paste will not work out of the box.
		// Get the model
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		// Ensure the model is a bundle plug-in model
		if ((model instanceof IBundlePluginModelBase) == false) {
			return null;
		}
		// Get the extension model
		ISharedExtensionsModel extensionModel = ((IBundlePluginModelBase) model).getExtensionsModel();
		// Ensure the extension model is defined
		if ((extensionModel == null) || ((extensionModel instanceof IPluginModelBase) == false)) {
			return null;
		}
		return ((IPluginModelBase) extensionModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// By default, fragment.xml does not exist until the first extension
		// or extension point is created.  
		// Ensure the file exists before pasting because the model will be 
		// null and the paste will fail if it does not exist
		((ManifestEditor) getPage().getEditor()).ensurePluginContextPresence();
		// Note:  Multi-select in tree viewer is disabled; but, this function
		// can support multiple source objects
		// Get the model
		IPluginModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		IPluginBase pluginBase = model.getPluginBase();
		try {
			// Paste all source objects into the target object
			for (int i = 0; i < sourceObjects.length; i++) {
				Object sourceObject = sourceObjects[i];

				if ((sourceObject instanceof IPluginExtension) && (pluginBase instanceof IDocumentElementNode)) {
					// Extension object
					IDocumentElementNode extension = (IDocumentElementNode) sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					extension.reconnect((IDocumentElementNode) pluginBase, model);
					// Add the extension to the plug-in parent (plug-in)
					pluginBase.add((IPluginExtension) extension);

				} else if ((sourceObject instanceof IPluginElement) && (targetObject instanceof IPluginParent) && (targetObject instanceof IDocumentElementNode)) {
					// Element object
					IDocumentElementNode element = (IDocumentElementNode) sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					element.reconnect((IDocumentElementNode) targetObject, model);
					// Add the element to the plug-in parent (extension or
					// element)
					((IPluginParent) targetObject).add((IPluginElement) element);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleMove(boolean up) {
		IStructuredSelection sel = (IStructuredSelection) fExtensionTree.getSelection();
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
			IPluginBase plugin = extension.getPluginBase();
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

	private void updateButtons(Object item) {
		if (fExpandAction != null) {
			fExpandAction.setEnabled(ToggleExpandStateAction.isExpandable((IStructuredSelection) fExtensionTree.getSelection()));
		}
		if (fFilterRelatedAction != null) {
			boolean filterRelatedEnabled = false;
			if (fExtensionTree != null) {
				filterRelatedEnabled = ExtensionsFilterUtil.isFilterRelatedEnabled((IStructuredSelection) fExtensionTree.getSelection());
			}
			fFilterRelatedAction.setEnabled(filterRelatedEnabled);
		}

		if (getPage().getModel().isEditable() == false)
			return;
		boolean sorted = fSortAction != null && fSortAction.isChecked();
		if (sorted) {
			getTreePart().setButtonEnabled(BUTTON_MOVE_UP, false);
			getTreePart().setButtonEnabled(BUTTON_MOVE_DOWN, false);
			return;
		}
		IStructuredSelection selection = (item instanceof IStructuredSelection) ? (IStructuredSelection) item : null;

		boolean filtered = fFilteredTree.isFiltered();
		boolean addEnabled = true;
		boolean removeEnabled = true;
		boolean upEnabled = false;
		boolean downEnabled = false;

		if (filtered) {
			// Fix for bug 194529 and bug 194828
			// Update: adding during filtering enabled by additional filter capability
			addEnabled = true;
			upEnabled = false;
			downEnabled = false;
			removeEnabled = isRemoveEnabled(selection);
		} else {
			if (selection != null && selection.size() == 1) {
				Object selected = selection.getFirstElement();
				if (selected instanceof IPluginElement) {
					IPluginElement element = (IPluginElement) selected;
					IPluginParent parent = (IPluginParent) element.getParent();
					// check up
					int index = parent.getIndexOf(element);
					if (index > 0)
						upEnabled = true;
					if (index < parent.getChildCount() - 1)
						downEnabled = true;
				} else if (selected instanceof IPluginExtension) {
					IPluginExtension extension = (IPluginExtension) selected;
					IExtensions extensions = (IExtensions) extension.getParent();
					int index = extensions.getIndexOf(extension);
					int size = extensions.getExtensions().length;
					if (index > 0)
						upEnabled = true;
					if (index < size - 1)
						downEnabled = true;
				}
			}
		}
		getTreePart().setButtonEnabled(BUTTON_ADD, addEnabled);
		getTreePart().setButtonEnabled(BUTTON_REMOVE, removeEnabled);
		getTreePart().setButtonEnabled(BUTTON_MOVE_UP, upEnabled);
		getTreePart().setButtonEnabled(BUTTON_MOVE_DOWN, downEnabled);
	}

	/**
	 * Since filtering potentially hides children of extensions, removing them when they still have children is intransparent.
	 * Needs to be called only when the tree is filtered.
	 *  
	 * @param selection selection to be tested
	 * @return whether removing the selected elements is enabled
	 */
	boolean isRemoveEnabled(IStructuredSelection selection) {
		if (selection != null) {
			for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
				Object element = iterator.next();
				if (element instanceof PluginExtensionNode) {
					return ((PluginExtensionNode) element).getChildCount() == 0;
				}
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		fPatternFilter = new ExtensionsPatternFilter();
		fFilteredTree = new FormFilteredTree(parent, style, fPatternFilter) {
			protected WorkbenchJob doCreateRefreshJob() {
				final WorkbenchJob job = super.doCreateRefreshJob();
				job.addJobChangeListener(new JobChangeAdapter() {
					private ISelection selection;
					private boolean aboutToRunPassed = false;

					public void scheduled(IJobChangeEvent event) {
						((ExtensionsPatternFilter) fFilteredTree.getPatternFilter()).clearMatchingLeafs();
						selection = fExtensionTree.getSelection();
					}

					public void aboutToRun(IJobChangeEvent event) {
						aboutToRunPassed = true;
					}

					/* 
					 * Restores selection after tree refresh and expands tree up to matching leafs only
					 */
					public void done(IJobChangeEvent event) {
						if (aboutToRunPassed) { // restoring is only required if the job actually ran
							try {
								fFilteredTree.setRedraw(false);
								ExtensionsPatternFilter extensionsPatternFilter = ((ExtensionsPatternFilter) fFilteredTree.getPatternFilter());
								fExtensionTree.collapseAll();
								Object[] leafs = extensionsPatternFilter.getMatchingLeafsAsArray();
								for (int i = 0; i < leafs.length; i++) {
									fExtensionTree.expandToLevel(leafs[i], 0);
								}
								if (selection != null && !(selection.isEmpty())) {
									fExtensionTree.setSelection(selection, true);
								}
							} finally {
								fFilteredTree.setRedraw(true);
							}
						}
					}
				});
				return job;
			}

			protected long getRefreshJobDelay() {
				// Prolonged job delay time is required because of the attribute search being more costly in nature.
				// This can block input to the filter text severely. Thus it shouldn't happen when typing slowly.
				// The delay of 1500ms is bypassed by some actions that use the filter text to initiate searches or clear the text.
				long delay = (fBypassFilterDelay) ? 0 : REFRESHJOB_DELAY_TIME;
				setBypassFilterDelay(false); // reset afterwards
				return delay;
			}

			protected void clearText() {
				// bugfix: additional notification with textChanged() would cause a needless 2nd refresh job run
				// which in turn would have a longer delay time than the 1st run.
				setFilterText(""); //$NON-NLS-1$
			}

			protected void textChanged() {
				String filterText = getFilterString();
				if (filterText != null && filterText.length() == 0) {
					// clearing the filter text doesn't require a refresh job delay
					setBypassFilterDelay(true);
				}
				super.textChanged();
			}
		};
		parent.setData("filtered", Boolean.TRUE); //$NON-NLS-1$
		return fFilteredTree.getViewer();
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			StructuredViewer viewer = getStructuredViewerPart().getViewer();
			IStructuredSelection ssel = (IStructuredSelection) viewer.getSelection();
			updateButtons(ssel);
		}
	}

	protected void selectExtensionElement(ISelection selection) {
		fExtensionTree.setSelection(selection, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#isDragAndDropEnabled()
	 */
	protected boolean isDragAndDropEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDragMove(java.lang.Object[])
	 */
	public boolean canDragMove(Object[] sourceObjects) {
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		} else if (fFilteredTree.isFiltered()) {
			return false;
		} else if (isTreeViewerSorted()) {
			return false;
		}
		return true;
	}

	/**
	 * @param targetObject
	 * @param sourceObjects
	 */
	private boolean validateDropMoveSanity(Object targetObject, Object[] sourceObjects) {
		// Validate target object
		if ((targetObject instanceof IPluginParent) == false) {
			return false;
		} else if ((targetObject instanceof IDocumentElementNode) == false) {
			return false;
		}
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourceObjects
	 */
	private boolean validateDragMoveSanity(Object[] sourceObjects) {
		// Validate source
		if (sourceObjects == null) {
			// No objects
			return false;
		} else if (sourceObjects.length != 1) {
			// Multiple selection not supported
			return false;
		} else if ((sourceObjects[0] instanceof IDocumentElementNode) == false) {
			// Must be the right type
			return false;
		} else if ((sourceObjects[0] instanceof IPluginParent) == false) {
			// Must be the right type
			return false;
		}
		return true;
	}

	/**
	 * @param sourcePluginObject
	 * @param targetPluginObject
	 */
	private boolean validateDropMoveModel(IPluginParent sourcePluginObject, IPluginParent targetPluginObject) {
		// Objects have to be from the same model
		ISharedPluginModel sourceModel = sourcePluginObject.getModel();
		ISharedPluginModel targetModel = targetPluginObject.getModel();
		if (sourceModel.equals(targetModel)) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			return false;
		}
		// Multiple selection not supported
		IPluginParent sourcePluginObject = (IPluginParent) sourceObjects[0];
		IPluginParent targetPluginObject = (IPluginParent) targetObject;
		// Validate model
		if (validateDropMoveModel(sourcePluginObject, targetPluginObject) == false) {
			return false;
		}
		// Validate move
		if (sourcePluginObject instanceof IPluginExtension) {
			IPluginExtension sourceExtensionObject = (IPluginExtension) sourcePluginObject;
			if (targetPluginObject instanceof IPluginExtension) {
				// Source:  Extension
				// Target:  Extension
				IPluginExtension targetExtensionObject = (IPluginExtension) targetPluginObject;
				return canDropMove(targetExtensionObject, sourceExtensionObject, targetLocation);
			} else if (targetPluginObject instanceof IPluginElement) {
				// Source:  Extension
				// Target:  Element
				return false;
			}
		} else if (sourcePluginObject instanceof IPluginElement) {
			IPluginElement sourceElementObject = (IPluginElement) sourcePluginObject;
			if (targetPluginObject instanceof IPluginExtension) {
				// Source:  Element
				// Target:  Extension
				IPluginExtension targetExtensionObject = (IPluginExtension) targetPluginObject;
				return canDropMove(targetExtensionObject, sourceElementObject, targetLocation);
			} else if (targetPluginObject instanceof IPluginElement) {
				// Source:  Element
				// Target:  Element
				IPluginElement targetElementObject = (IPluginElement) targetPluginObject;
				return canDropMove(targetElementObject, sourceElementObject, targetLocation);
			}
		}
		return false;
	}

	/**
	 * @param targetElementObject
	 * @param sourceElementObject
	 * @param targetLocation
	 */
	private boolean canDropMove(IPluginElement targetElementObject, IPluginElement sourceElementObject, int targetLocation) {

		// Verify that the source is not the parent of the target
		if (validateDropMoveParent(targetElementObject, sourceElementObject) == false) {
			return false;
		}

		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			IDocumentElementNode previousNode = ((IDocumentElementNode) targetElementObject).getPreviousSibling();
			if (sourceElementObject.equals(previousNode)) {
				return false;
			}
			IPluginObject targetParentObject = targetElementObject.getParent();
			if ((targetParentObject instanceof IPluginParent) == false) {
				return false;
			}
			// Paste element as a sibling of the other element (before)
			return validateDropMoveSchema((IPluginParent) targetParentObject, sourceElementObject);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			IDocumentElementNode nextNode = ((IDocumentElementNode) sourceElementObject).getPreviousSibling();
			if (targetElementObject.equals(nextNode)) {
				return false;
			}
			IPluginObject targetParentObject = targetElementObject.getParent();
			if ((targetParentObject instanceof IPluginParent) == false) {
				return false;
			}
			// Paste element as a sibling of the other element (after)
			return validateDropMoveSchema((IPluginParent) targetParentObject, sourceElementObject);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			IDocumentElementNode targetExtensionNode = (IDocumentElementNode) targetElementObject;
			int childCount = targetExtensionNode.getChildCount();
			if (childCount != 0) {
				IDocumentElementNode lastNode = targetExtensionNode.getChildAt(childCount - 1);
				if (sourceElementObject.equals(lastNode)) {
					return false;
				}
			}
			// Paste element as the last child of the element
			return validateDropMoveSchema(targetElementObject, sourceElementObject);
		}
		return false;
	}

	/**
	 * @param targetElementObject
	 * @param sourceElementObject
	 */
	private boolean validateDropMoveParent(IPluginElement targetElementObject, IPluginElement sourceElementObject) {

		IPluginObject currentParent = targetElementObject.getParent();
		while (true) {
			if (currentParent == null) {
				return true;
			} else if ((currentParent instanceof IPluginElement) == false) {
				return true;
			} else if (sourceElementObject.equals(currentParent)) {
				return false;
			}
			currentParent = currentParent.getParent();
		}
	}

	/**
	 * @param targetExtensionObject
	 * @param sourceElementObject
	 * @param targetLocation
	 */
	private boolean canDropMove(IPluginExtension targetExtensionObject, IPluginElement sourceElementObject, int targetLocation) {

		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			return false;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			return false;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			IDocumentElementNode targetExtensionNode = (IDocumentElementNode) targetExtensionObject;
			int childCount = targetExtensionNode.getChildCount();
			if (childCount != 0) {
				IDocumentElementNode lastNode = targetExtensionNode.getChildAt(childCount - 1);
				if (sourceElementObject.equals(lastNode)) {
					return false;
				}
			}
			// Paste element as the last child of the extension
			return validateDropMoveSchema(targetExtensionObject, sourceElementObject);
		}
		return false;
	}

	/**
	 * @param targetPluginObject
	 * @param sourcePluginObject
	 */
	private boolean validateDropMoveSchema(IPluginParent targetPluginObject, IPluginParent sourcePluginObject) {
		IDocumentElementNode targetPluginNode = (IDocumentElementNode) targetPluginObject;
		// If the target is the source's parent, then the move is always 
		// valid.  No need to check the schema.  Order does not matter
		if (targetPluginObject.equals(sourcePluginObject.getParent())) {
			return true;
		}
		// Retrieve the schema corresponding to the target object		
		ISchema schema = getSchema(targetPluginObject);
		// If there is no schema, then a source object can be pasted as a 
		// child of any target object
		if (schema == null) {
			return true;
		}
		// Determine the element name of the target object
		String targetNodeTagName = targetPluginNode.getXMLTagName();
		// Retrieve the element schema for the target object
		ISchemaElement schemaElement = schema.findElement(targetNodeTagName);
		// Ensure we found a schema element and it is a schema complex type
		if (schemaElement == null) {
			// Something is seriously wrong, we have a schema
			return false;
		} else if ((schemaElement.getType() instanceof ISchemaComplexType) == false) {
			// Something is seriously wrong, we are a plug-in parent
			return false;
		}
		// We have a schema complex type.  Either the target object has 
		// attributes or the element has children.
		// Generate the list of element proposals
		TreeSet elementSet = XMLElementProposalComputer.computeElementProposal(schemaElement, targetPluginNode);
		// Iterate over set of valid element proposals
		Iterator iterator = elementSet.iterator();
		while (iterator.hasNext()) {
			// Get the proposal element tag name
			String targetTagName = ((ISchemaElement) iterator.next()).getName();
			// Only a source element that is found within the set of element 
			// proposals can be pasted
			String sourceNodeTagName = ((IDocumentElementNode) sourcePluginObject).getXMLTagName();
			if (sourceNodeTagName.equals(targetTagName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param targetExtensionObject
	 * @param sourceExtensionObject
	 * @param targetLocation
	 */
	private boolean canDropMove(IPluginExtension targetExtensionObject, IPluginExtension sourceExtensionObject, int targetLocation) {

		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			IDocumentElementNode previousNode = ((IDocumentElementNode) targetExtensionObject).getPreviousSibling();
			if (sourceExtensionObject.equals(previousNode)) {
				return false;
			}
			// Paste extension as sibling of extension (before)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			IDocumentElementNode nextNode = ((IDocumentElementNode) sourceExtensionObject).getPreviousSibling();
			if (targetExtensionObject.equals(nextNode)) {
				return false;
			}
			// Paste extension as sibling of extension (after)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			return false;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDragRemove(java.lang.Object[])
	 */
	public void doDragRemove(Object[] sourceObjects) {
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return;
		}
		IPluginParent pluginParentObject = (IPluginParent) sourceObjects[0];
		// Remove the object from the model
		try {
			if (pluginParentObject instanceof IPluginExtension) {
				IPluginExtension extension = (IPluginExtension) pluginParentObject;
				IPluginBase pluginBase = pluginParentObject.getPluginBase();
				if (pluginBase != null) {
					pluginBase.remove(extension);
				}
			} else if (pluginParentObject instanceof IPluginElement) {
				IPluginElement element = (IPluginElement) pluginParentObject;
				IPluginObject object = element.getParent();
				if (object instanceof IPluginParent) {
					((IPluginParent) object).remove(element);
				}
			}
			// Applicable for move operations
			// Flush the text edit operations associated with the move operation
			// to the source page
			// Move involves add new cloned object x and remove of original object
			// x 
			// Without flushing, multiple move operations up and down cause the
			// text edit operations to get completely screwed up (e.g. mark-up
			// in wrong position or getting lost)
			// TODO: MP: Undo: What are the implications of this?
			((PDEFormEditor) getPage().getEditor()).getContextManager().getPrimaryContext().flushEditorInput();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			Display.getDefault().beep();
			return;
		}
		// Multiple selection not supported
		IPluginParent sourcePluginObject = (IPluginParent) sourceObjects[0];
		IPluginParent targetPluginObject = (IPluginParent) targetObject;
		// Validate move
		try {
			if (sourcePluginObject instanceof IPluginExtension) {
				IPluginExtension sourceExtensionObject = (IPluginExtension) sourcePluginObject;
				if (targetPluginObject instanceof IPluginExtension) {
					// Source:  Extension
					// Target:  Extension
					IPluginExtension targetExtensionObject = (IPluginExtension) targetPluginObject;
					doDropMove(targetExtensionObject, sourceExtensionObject, targetLocation);
				} else if (targetPluginObject instanceof IPluginElement) {
					// Source:  Extension
					// Target:  Element
					return;
				}
			} else if (sourcePluginObject instanceof IPluginElement) {
				IPluginElement sourceElementObject = (IPluginElement) sourcePluginObject;
				if (targetPluginObject instanceof IPluginExtension) {
					// Source:  Element
					// Target:  Extension
					IPluginExtension targetExtensionObject = (IPluginExtension) targetPluginObject;
					doDropMove(targetExtensionObject, sourceElementObject, targetLocation);
				} else if (targetPluginObject instanceof IPluginElement) {
					// Source:  Element
					// Target:  Element
					IPluginElement targetElementObject = (IPluginElement) targetPluginObject;
					doDropMove(targetElementObject, sourceElementObject, targetLocation);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	/**
	 * @param targetExtensionObject
	 * @param sourceExtensionObject
	 * @param targetLocation
	 */
	private void doDropMove(IPluginExtension targetExtensionObject, IPluginExtension sourceExtensionObject, int targetLocation) throws CoreException {
		// Get the model
		IPluginModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		// Get the plug-in base
		IPluginBase pluginBase = model.getPluginBase();
		// Ensure the plug-in base is a document node
		if ((pluginBase instanceof IDocumentElementNode) == false) {
			return;
		} else if ((pluginBase instanceof PluginBaseNode) == false) {
			return;
		}
		// Plug-in base node
		IDocumentElementNode pluginBaseNode = (IDocumentElementNode) pluginBase;
		// Source extension node
		IDocumentElementNode sourceExtensionNode = (IDocumentElementNode) sourceExtensionObject;
		// Target extension node
		IDocumentElementNode targetExtensionNode = (IDocumentElementNode) targetExtensionObject;
		// Do drop move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Adjust all the source object transient field values to
			// acceptable values
			sourceExtensionNode.reconnect(pluginBaseNode, model);
			// Get index of target extension
			int index = (pluginBaseNode.indexOf(targetExtensionNode));
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Paste extension as sibling of extension (before)
			((PluginBaseNode) pluginBaseNode).add(sourceExtensionObject, index);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Adjust all the source object transient field values to
			// acceptable values
			sourceExtensionNode.reconnect(pluginBaseNode, model);
			// Get index of target extension
			int index = (pluginBaseNode.indexOf(targetExtensionNode));
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Paste extension as sibling of extension (after)
			((PluginBaseNode) pluginBaseNode).add(sourceExtensionObject, index + 1);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// NO-OP
		}
	}

	/**
	 * @param targetExtensionObject
	 * @param sourceElementObject
	 * @param targetLocation
	 */
	private void doDropMove(IPluginExtension targetExtensionObject, IPluginElement sourceElementObject, int targetLocation) throws CoreException {
		// Get the model
		IPluginModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		// Target extension node
		IDocumentElementNode targetExtensionNode = (IDocumentElementNode) targetExtensionObject;
		// Source extension node
		IDocumentElementNode sourceElementNode = (IDocumentElementNode) sourceElementObject;
		// Do drop move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// NO-OP
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// NO-OP
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Adjust all the source object transient field values to
			// acceptable values
			sourceElementNode.reconnect(targetExtensionNode, model);
			// Paste element as the last child of the extension
			targetExtensionObject.add(sourceElementObject);
		}
	}

	/**
	 * @param targetElementObject
	 * @param sourceElementObject
	 * @param targetLocation
	 */
	private void doDropMove(IPluginElement targetElementObject, IPluginElement sourceElementObject, int targetLocation) throws CoreException {
		// Get the model
		IPluginModelBase model = getPluginModelBase();
		// Ensure the model is defined
		if (model == null) {
			return;
		}
		// Target extension node
		IDocumentElementNode targetElementNode = (IDocumentElementNode) targetElementObject;
		// Source extension node
		IDocumentElementNode sourceElementNode = (IDocumentElementNode) sourceElementObject;
		// Do drop move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the target's parent
			IPluginObject targetParentObject = targetElementObject.getParent();
			if ((targetParentObject instanceof IPluginParent) == false) {
				return;
			} else if ((targetParentObject instanceof IDocumentElementNode) == false) {
				return;
			}
			IDocumentElementNode targetParentNode = (IDocumentElementNode) targetParentObject;
			// Adjust all the source object transient field values to
			// acceptable values
			sourceElementNode.reconnect(targetParentNode, model);
			// Get index of target element
			int index = (targetParentNode.indexOf(targetElementNode));
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Paste element as a sibling of the other element (before)
			((IPluginParent) targetParentObject).add(index, sourceElementObject);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the target's parent
			IPluginObject targetParentObject = targetElementObject.getParent();
			if ((targetParentObject instanceof IPluginParent) == false) {
				return;
			} else if ((targetParentObject instanceof IDocumentElementNode) == false) {
				return;
			}
			IDocumentElementNode targetParentNode = (IDocumentElementNode) targetParentObject;
			// Adjust all the source object transient field values to
			// acceptable values
			sourceElementNode.reconnect(targetParentNode, model);
			// Get index of target element
			int index = (targetParentNode.indexOf(targetElementNode));
			// Ensure the target index was found
			if (index == -1) {
				return;
			}
			// Paste element as a sibling of the other element (after)
			((IPluginParent) targetParentObject).add(index + 1, sourceElementObject);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Adjust all the source object transient field values to
			// acceptable values
			sourceElementNode.reconnect(targetElementNode, model);
			// Paste element as the last child of the element
			targetElementObject.add(sourceElementObject);
		}

	}

	private boolean isTreeViewerSorted() {
		if (fSortAction == null) {
			return false;
		}
		return fSortAction.isChecked();
	}

	private boolean isSingleSelection() {
		IStructuredSelection selection = (IStructuredSelection) fExtensionTree.getSelection();
		return selection.size() == 1;
	}

	private void reportMissingExtensionPointSchemas(IPluginBase pluginBase) {
		IPluginExtension[] extensions = pluginBase.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension ext = extensions[i];
			if (ext.getSchema() == null)
				reportMissingExtensionPointSchema(ext.getPoint());
		}
	}

	private void reportMissingExtensionPointSchema(String point) {
		getManagedForm().getMessageManager().addMessage(point, NLS.bind(PDEUIMessages.ManifestEditor_DetailExtension_missingExtPointSchema, point), null, IMessageProvider.WARNING);
	}
}
