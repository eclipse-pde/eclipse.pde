package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.net.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.extension.NewExtensionWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class DetailExtensionSection
	extends TreeSection
	implements IModelChangedListener {
	//private TableTreeViewer extensionTree;
	private TreeViewer extensionTree;
	private FormWidgetFactory factory;
	private Image extensionImage;
	public static final String SECTION_TITLE =
		"ManifestEditor.DetailExtensionSection.title";
	public static final String SECTION_NEW =
		"ManifestEditor.DetailExtensionSection.new";
	public static final String SECTION_SHOW_CHILDREN =
		"ManifestEditor.DetailExtensionSection.showAllChildren";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_NEW_EXTENSION =
		"ManifestEditor.DetailExtensionSection.newExtension";
	public static final String POPUP_COLLAPSE_ALL =
		"ManifestEditor.DetailExtensionSection.collapseAll";
	public static final String POPUP_GO_TO = "Menus.goTo.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private static final String SETTING_SHOW_ALL =
		"DetailExtensionSection.showAllChildren";
	private Image genericElementImage;
	//private Button showAllChildrenButton;
	private SchemaRegistry schemaRegistry;
	private ExternalModelManager pluginInfoRegistry;
	private DrillDownAdapter drillDownAdapter;
	private Action newExtensionAction;
	private Action collapseAllAction;
	private static final String[] COMMON_LABEL_PROPERTIES =
		{ "label", "name", "id" };

	class ExtensionContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {

		public Object[] getChildren(Object parent) {
			Object[] children = null;
			if (parent instanceof IPluginBase)
				children = ((IPluginBase) parent).getExtensions();
			else if (parent instanceof IPluginExtension) {
				children = ((IPluginExtension) parent).getChildren();
			} else if (/*showAllChildrenButton.getSelection() && */
				parent instanceof IPluginElement) {
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
			if (child instanceof IPluginObject)
				return ((IPluginObject) child).getParent();
			return null;
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
	}

	class ExtensionLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Viewer v, Object obj, int index) {
			return getColumnText(obj, index);
		}
		public String getText(Object obj) {
			return getColumnText(obj, 1);
		}
		public Image getImage(Object obj) {
			return getColumnImage(obj, 1);
		}
		public String getColumnText(Object obj, int index) {
			if (index == 1) {
				return resolveObjectName(obj);
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 1) {
				return resolveObjectImage(obj);
			}
			return null;
		}

		public Image getColumnImage(Viewer v, Object obj, int index) {
			return getColumnImage(obj, index);
		}
	}

	class LocalToolBar
		extends NullToolBarManager
		implements IPropertyChangeListener, SelectionListener {
		private Composite container;
		private Hashtable map = new Hashtable();

		public Composite createControl(
			Composite parent,
			FormWidgetFactory factory) {
			container = factory.createComposite(parent);
			RowLayout rowLayout = new RowLayout();
			rowLayout.wrap = false;
			rowLayout.pack = false;
			rowLayout.justify = false;
			rowLayout.type = SWT.HORIZONTAL;
			rowLayout.marginLeft = 0;
			rowLayout.marginTop = 0;
			rowLayout.marginRight = 0;
			rowLayout.marginBottom = 0;
			rowLayout.spacing = 5;
			container.setLayout(rowLayout);
			return container;
		}

		public void add(IContributionItem item) {
			if (item instanceof Separator) {
				getFormPage().getForm().getFactory().createLabel(
					container,
					null);
			}
		}

		public void add(IAction action) {
			Button button = factory.createButton(container, null, SWT.PUSH);
			button.setToolTipText(action.getToolTipText());
			/*
			FormButton fbutton =
				new FormButton(button, getFormPage().getForm().getFactory());
			*/
			PDELabelProvider provider =
				PDEPlugin.getDefault().getLabelProvider();
			//			fbutton.setImage(provider.get(action.getImageDescriptor()));
			//			ImageDescriptor desc = action.getHoverImageDescriptor();
			//			if (desc != null)
			//				fbutton.setHoverImage(provider.get(desc));
			//			desc = action.getDisabledImageDescriptor();
			//			if (desc != null)
			//				fbutton.setDisabledImage(provider.get(desc));
			button.setImage(provider.get(action.getImageDescriptor()));
			button.setData(action);
			button.setEnabled(action.isEnabled());
			action.addPropertyChangeListener(this);
			button.addSelectionListener(this);
			map.put(action, button);
		}
		public void widgetSelected(SelectionEvent e) {
			IAction action = (IAction) e.widget.getData();
			action.run();
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void propertyChange(PropertyChangeEvent e) {
			String propertyName = e.getProperty();
			IAction action = (IAction) e.getSource();
			boolean tooltipTextChanged =
				propertyName == null
					|| propertyName.equals(IAction.TOOL_TIP_TEXT);
			boolean enableStateChanged =
				propertyName == null || propertyName.equals(IAction.ENABLED);
			Button button = (Button) map.get(action);
			if (tooltipTextChanged)
				button.setToolTipText(action.getToolTipText());
			if (enableStateChanged)
				button.setEnabled(action.isEnabled());
		}
	}

	public DetailExtensionSection(ManifestExtensionsPage page) {
		super(
			page,
			new String[] {
				PDEPlugin.getResourceString(SECTION_NEW),
				null,
				PDEPlugin.getResourceString(SECTION_UP),
				PDEPlugin.getResourceString(SECTION_DOWN)});
		this.setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		schemaRegistry = PDECore.getDefault().getSchemaRegistry();
		pluginInfoRegistry = PDECore.getDefault().getExternalModelManager();
		handleDefaultButton = false;
	}
	private static void addItemsForExtensionWithSchema(
		MenuManager menu,
		IPluginExtension extension,
		IPluginParent parent) {
		ISchema schema = ((PluginExtension) extension).getSchema();
		String tagName = (parent == extension ? "extension" : parent.getName());
		ISchemaElement elementInfo = schema.findElement(tagName);
		if (elementInfo == null)
			return;
		ISchemaElement[] candidates = schema.getCandidateChildren(elementInfo);
		for (int i = 0; i < candidates.length; i++) {
			ISchemaElement candidateInfo = candidates[i];
			Action action = new NewElementAction(candidateInfo, parent);
			menu.add(action);
		}

	}
	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		this.factory = factory;
		initializeImages();
		Composite container = createClientContainer(parent, 2, factory);

		//		LocalToolBar localToolBar = new LocalToolBar();
		//		Composite toolBar = localToolBar.createControl(container, factory);
		//		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		//		gd.horizontalSpan = 2;
		//		toolBar.setLayoutData(gd);

		//createAllChildrenButton(container, 2, factory);

		TreePart treePart = getTreePart();

		createViewerPartControl(container, SWT.MULTI, 2, factory);
		extensionTree = treePart.getTreeViewer();
		extensionTree.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		extensionTree.setContentProvider(new ExtensionContentProvider());
		extensionTree.setLabelProvider(new ExtensionLabelProvider());

		drillDownAdapter = new DrillDownAdapter(extensionTree);
		//drillDownAdapter.addNavigationActions(localToolBar);

		factory.paintBordersFor(container);
		return container;
	}
	/*
		private void createAllChildrenButton(Composite container, int span, FormWidgetFactory factory ) {
			showAllChildrenButton = factory.createButton(container, null, SWT.CHECK);
			showAllChildrenButton.setText(
				PDEPlugin.getResourceString(SECTION_SHOW_CHILDREN));
			final IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
			boolean showAll = pstore.getBoolean(SETTING_SHOW_ALL);
			showAllChildrenButton.setSelection(showAll);
			showAllChildrenButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					pstore.setValue(SETTING_SHOW_ALL, showAllChildrenButton.getSelection());
					BusyIndicator.showWhile(extensionTree.getTree().getDisplay(), new Runnable() {
						public void run() {
							extensionTree.refresh();
						}
					});
				}
			});	
			GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan = span;
			showAllChildrenButton.setLayoutData(gd);
		}
	*/

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		fireSelectionNotification(item);
		getFormPage().setSelection(selection);
		updateUpDownButtons(item);
	}

	protected void handleDoubleClick(IStructuredSelection selection) {
		PropertiesAction action =
			new PropertiesAction(getFormPage().getEditor());
		action.run();
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
			case 1 : // noop
			case 2 :
				handleMove(true);
				break;
			case 3 :
				handleMove(false);
				break;
		}
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.CUT)) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(IWorkbenchActionConstants.PASTE)) {
			doPaste();
			return true;
		}
		return false;
	}
	public void expandTo(Object object) {
		extensionTree.setSelection(new StructuredSelection(object), true);
	}
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = extensionTree.getSelection();
		IStructuredSelection ssel = (IStructuredSelection) selection;
		IMenuManager newMenu = null;

		if (ssel.size() == 1) {
			Object object = ssel.getFirstElement();
			if (object instanceof IPluginParent) {
				IPluginParent parent = (IPluginParent) object;
				if (parent.getModel().getUnderlyingResource() != null) {
					newMenu = fillContextMenu(getFormPage(), parent, manager);
					manager.add(new Separator());
				}
			}
		} else if (ssel.size() > 1) {
			// multiple
			Action delAction = new Action() {
				public void run() {
					handleDelete();
				}
			};
			delAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
			manager.add(delAction);
			manager.add(new Separator());
			delAction.setEnabled(!isReadOnly());
		}
		if (newMenu == null) {
			newMenu = new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));
			manager.add(newMenu);
		}
		if (!newMenu.isEmpty())
			newMenu.add(new Separator());
		newMenu.add(newExtensionAction);

		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator());
		manager.add(collapseAllAction);
		manager.add(new Separator());
		getFormPage().getEditor().getContributor().addClipboardActions(manager);
		//manager.add(new Separator());
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager,
			false);

		if (ssel.size() == 1) {
			manager.add(new Separator());
			Object object = ssel.getFirstElement();
			if (object instanceof IPluginExtension) {
				PluginSearchActionGroup actionGroup =
					new PluginSearchActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				manager.add(new Separator());
			}
			manager.add(new PropertiesAction(getFormPage().getEditor()));
		}
	}
	static IMenuManager fillContextMenu(
		PDEFormPage page,
		final IPluginParent parent,
		IMenuManager manager) {
		return fillContextMenu(page, parent, manager, false);
	}
	static IMenuManager fillContextMenu(
		PDEFormPage page,
		final IPluginParent parent,
		IMenuManager manager,
		boolean addSiblingItems) {
		return fillContextMenu(page, parent, manager, addSiblingItems, true);
	}
	static IMenuManager fillContextMenu(
		PDEFormPage page,
		final IPluginParent parent,
		IMenuManager manager,
		boolean addSiblingItems,
		boolean fullMenu) {
		MenuManager menu =
			new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));

		IPluginExtension extension = getExtension(parent);

		ISchema schema = ((PluginExtension) extension).getSchema();
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
		//if (menu.isEmpty() == false) {
		manager.add(menu);
		manager.add(new Separator());
		//}
		if (fullMenu) {
			Action deleteAction =
				new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					try {
						IPluginObject parentsParent = parent.getParent();
						if (parent instanceof IPluginExtension) {
							IPluginBase plugin = (IPluginBase) parentsParent;
							plugin.remove((IPluginExtension) parent);
						} else {
							IPluginParent parentElement =
								(IPluginParent) parent.getParent();
							parentElement.remove(parent);
						}
					} catch (CoreException e) {
					}
				}
			};
			deleteAction.setEnabled(((IModel) page.getModel()).isEditable());
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
		IStructuredSelection sel =
			(IStructuredSelection) extensionTree.getSelection();
		if (sel.isEmpty())
			return;
		for (Iterator iter = sel.iterator(); iter.hasNext();) {
			IPluginObject object = (IPluginObject) iter.next();
			try {
				if (object instanceof IPluginElement) {
					IPluginElement ee = (IPluginElement) object;
					IPluginParent parent = (IPluginParent) ee.getParent();
					parent.remove(ee);
				} else if (object instanceof IPluginExtension) {
					IPluginExtension extension = (IPluginExtension) object;
					IPluginBase plugin = extension.getPluginBase();
					plugin.remove(extension);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	void handleNew() {
		IFile file =
			((IFileEditorInput) getFormPage().getEditor().getEditorInput())
				.getFile();
		final IProject project = file.getProject();
		BusyIndicator
			.showWhile(extensionTree.getTree().getDisplay(), new Runnable() {
			public void run() {
				NewExtensionWizard wizard =
					new NewExtensionWizard(
						project,
						(IPluginModelBase) getFormPage().getModel());
				WizardDialog dialog =
					new WizardDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 500, 500);
				dialog.open();
			}
		});
	}

	private void handleCollapseAll() {
		getTreePart().getTreeViewer().collapseAll();
	}

	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		extensionTree.setInput(model.getPluginBase());
		boolean editable = model.isEditable();
		setReadOnly(!editable);
		TreePart treePart = getTreePart();
		treePart.setButtonEnabled(0, editable);
		treePart.setButtonEnabled(2, false);
		treePart.setButtonEnabled(3, false);
		model.addModelChangedListener(this);

		newExtensionAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newExtensionAction.setText(
			PDEPlugin.getResourceString(POPUP_NEW_EXTENSION));
		newExtensionAction.setImageDescriptor(
			PDEPluginImages.DESC_EXTENSION_OBJ);
		newExtensionAction.setEnabled(editable);
		collapseAllAction = new Action() {
			public void run() {
				handleCollapseAll();
			}
		};
		collapseAllAction.setText(
			PDEPlugin.getResourceString(POPUP_COLLAPSE_ALL));
	}
	public void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		extensionImage = provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
		genericElementImage =
			provider.get(PDEPluginImages.DESC_GENERIC_XML_OBJ);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			extensionTree.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginBase
			&& event.getChangeType() == IModelChangedEvent.CHANGE
			&& event.getChangedProperty().equals(IPluginBase.P_EXTENSION_ORDER)) {
			IStructuredSelection sel =
				(IStructuredSelection) extensionTree.getSelection();
			IPluginExtension extension =
				(IPluginExtension) sel.getFirstElement();
			extensionTree.refresh();
			extensionTree.setSelection(new StructuredSelection(extension));
			return;
		}
		if (changeObject instanceof IPluginExtension
			|| changeObject instanceof IPluginElement) {
			IPluginObject pobj = (IPluginObject) changeObject;
			IPluginObject parent = pobj.getParent();

			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				extensionTree.add(parent, pobj);
				extensionTree.setSelection(
					new StructuredSelection(changeObject),
					true);
				extensionTree.getTree().setFocus();
				// defect 16606: update property sheet
				asyncResendSelection(getFormPage().getSelection());
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				extensionTree.remove(pobj);
			} else {
				if (event
					.getChangedProperty()
					.equals(IPluginParent.P_SIBLING_ORDER)) {
					IStructuredSelection sel =
						(IStructuredSelection) extensionTree.getSelection();
					IPluginObject child = (IPluginObject) sel.getFirstElement();
					extensionTree.refresh(child.getParent());
					extensionTree.setSelection(new StructuredSelection(child));
				} else {
					extensionTree.update(changeObject, null);
					if (extensionTree.getTree().isFocusControl()) {
						ISelection sel = getFormPage().getSelection();
						if (sel != null
							&& sel instanceof IStructuredSelection) {
							IStructuredSelection ssel =
								(IStructuredSelection) sel;
							if (!ssel.isEmpty()
								&& ssel.getFirstElement().equals(changeObject)) {
								// update property sheet
								asyncResendSelection(sel);
							}
						}
					}
				}
			}
		}
	}

	private void asyncResendSelection(final ISelection sel) {
		extensionTree.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				getFormPage().setSelection(sel);
			}
		});
	}

	private Image resolveObjectImage(Object obj) {
		if (obj instanceof IPluginExtension) {
			return extensionImage;
		}
		Image elementImage = genericElementImage;
		if (obj instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) obj;
			Image customImage = getCustomImage(element);
			if (customImage != null)
				elementImage = customImage;
			boolean hasBodyText = element.getText() != null;
			if (hasBodyText) {
				elementImage =
					PDEPlugin.getDefault().getLabelProvider().get(
						elementImage,
						PDELabelProvider.F_EDIT);
			}
		}
		return elementImage;
	}

	static Image getCustomImage(IPluginElement element) {
		ISchemaElement elementInfo = ((PluginElement) element).getElementInfo();
		if (elementInfo != null && elementInfo.getIconProperty() != null) {
			String iconProperty = elementInfo.getIconProperty();
			IPluginAttribute att = element.getAttribute(iconProperty);
			String iconPath = null;
			if (att != null && att.getValue() != null) {
				iconPath = att.getValue();
			}
			if (iconPath != null) {
				//OK, we have an icon path relative to the plug-in
				return getImageFromPlugin(element, iconPath);
			}
		}
		return null;
	}

	private static Image getImageFromPlugin(
		IPluginElement element,
		String iconPathName) {
		IPluginModelBase model = element.getModel();
		if (model == null)
			return null;
			
		URL modelURL = null;
		String path = model.getInstallLocation();

		try {
			if (!path.startsWith("file:"))
				path = "file:" + path;
			modelURL = new URL(path + File.separator);
			return PDEPlugin.getDefault().getLabelProvider().getImageFromURL(
				modelURL,
				iconPathName);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	private String resolveObjectName(Object obj) {
		return resolveObjectName(schemaRegistry, pluginInfoRegistry, obj);
	}
	public static String resolveObjectName(
		SchemaRegistry schemaRegistry,
		ExternalModelManager pluginInfoRegistry,
		Object obj) {
		boolean fullNames = MainPreferencePage.isFullNameModeEnabled();
		if (obj instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) obj;
			if (!fullNames) {
				//defect 17026
				//if (extension.getName()!=null) return extension.getName();
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
			// try extension point declaration
			IPluginExtensionPoint pointInfo =
				pluginInfoRegistry.findExtensionPoint(extension.getPoint());
			if (pointInfo != null) {
				return pointInfo.getResourceString(pointInfo.getName());
			}
		} else if (obj instanceof IPluginElement) {
			String baseName = obj.toString();
			PluginElement element = (PluginElement) obj;
			String fullName = null;
			ISchemaElement elementInfo = element.getElementInfo();
			IPluginAttribute labelAtt = null;
			if (elementInfo != null
				&& elementInfo.getLabelProperty() != null) {
				labelAtt = element.getAttribute(elementInfo.getLabelProperty());
			}
			if (labelAtt == null) {
				// try some hard-coded attributes that 
				// are used frequently
				for (int i = 0; i < COMMON_LABEL_PROPERTIES.length; i++) {
					labelAtt = element.getAttribute(COMMON_LABEL_PROPERTIES[i]);
					if (labelAtt != null)
						break;
				}
				if (labelAtt == null) {
					// Last try - if there is only one attribute,
					// use that
					if (element.getAttributeCount() == 1)
						labelAtt = element.getAttributes()[0];
				}
			}
			if (labelAtt != null && labelAtt.getValue() != null)
				fullName = stripShortcuts(labelAtt.getValue());
			fullName = element.getResourceString(fullName);
			if (fullNames)
				return fullName != null ? fullName : baseName;
			else
				return fullName != null
					? (fullName + " (" + baseName + ")")
					: baseName;
		}
		return obj.toString();
	}
	public void setFocus() {
		if (extensionTree != null)
			extensionTree.getTree().setFocus();
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
	public static final String SECTION_DOWN =
		"ManifestEditor.DetailExtensionSection.down";
	public static final String SECTION_UP =
		"ManifestEditor.DetailExtensionSection.up";
	protected boolean canPaste(Object target, Object[] objects) {
		if (objects[0] instanceof IPluginExtension)
			return true;
		if (objects[0] instanceof IPluginElement
			&& target instanceof IPluginParent)
			return true;
		return false;
	}
	protected void doPaste(Object target, Object[] objects) {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();

		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof IPluginExtension) {
					IPluginExtension extension = (IPluginExtension) obj;
					((PluginExtension) extension).setModel(model);
					((PluginExtension) extension).setParent(plugin);
					plugin.add(extension);
					((PluginParent) extension).reconnect();
				} else if (
					obj instanceof IPluginElement
						&& target instanceof IPluginParent) {
					PluginElement element = (PluginElement) obj;
					element.setModel(model);
					element.setParent((IPluginParent) target);
					((IPluginParent) target).add(element);
					if (element instanceof PluginParent)
						 ((PluginParent) element).reconnect();
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void handleMove(boolean up) {
		IStructuredSelection sel =
			(IStructuredSelection) extensionTree.getSelection();
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
			PluginBase plugin = (PluginBase) extension.getPluginBase();
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
	private void updateUpDownButtons(Object item) {
		if (isReadOnly())
			return;
		boolean upEnabled = false;
		boolean downEnabled = false;
		if (item != null) {
			if (item instanceof IPluginElement) {
				IPluginElement element = (IPluginElement) item;
				IPluginParent parent = (IPluginParent) element.getParent();
				// check up
				int index = parent.getIndexOf(element);
				if (index > 0)
					upEnabled = true;
				if (index < parent.getChildCount() - 1)
					downEnabled = true;
			} else if (item instanceof IPluginExtension) {
				IPluginExtension extension = (IPluginExtension) item;
				PluginBase pluginBase = (PluginBase) extension.getParent();
				int index = pluginBase.getIndexOf(extension);
				int size = pluginBase.getExtensionCount();
				if (index > 0)
					upEnabled = true;
				if (index < size - 1)
					downEnabled = true;
			}
		}
		getTreePart().setButtonEnabled(2, upEnabled);
		getTreePart().setButtonEnabled(3, downEnabled);
	}
}