package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import java.util.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.custom.*;
import org.eclipse.core.runtime.PlatformObject;

public class ImportListSection
	extends PDEFormSection
	implements IModelChangedListener {
	private TreeViewer importTree;
	private FormWidgetFactory factory;
	private Image importImage;
	private Image errorImportImage;
	public static final String SECTION_TITLE = "ManifestEditor.ImportListSection.title";
	public static final String SECTION_DESC = "ManifestEditor.ImportListSection.desc";
	public static final String SECTION_NEW = "ManifestEditor.ImportListSection.new";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private Button newButton;
	private Vector imports;
	
	class ImportObject extends PlatformObject {
		private IPluginImport iimport;
		private IPlugin plugin;
		
		public ImportObject(IPluginImport iimport) {
			this.iimport = iimport;
			String id = iimport.getId();
			plugin = PDEPlugin.getDefault().findPlugin(id);
		}
		public ImportObject(IPluginImport iiport, IPlugin plugin) {
			this.iimport = iimport;
			this.plugin = plugin;
		}
		public IPluginImport getImport() {
			return iimport;
		}
		public IPlugin getPlugin() {
			return plugin;
		}
		public String toString() {
			if (plugin!=null) {
				return plugin.getTranslatedName();
			}
			return iimport.getId();
		}
		public boolean isResolved() {
			return plugin!=null;
		}
	}

	class ImportContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {

		public Object[] getChildren(Object parent) {
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			return false;
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object[] getElements(Object parent) {
			if (imports==null) {
				createImportObjects();
			}
			return imports.toArray();
		}
		private void createImportObjects() {
			imports = new Vector();
			IPluginModel model = (IPluginModel)getFormPage().getModel();
			IPluginImport [] iimports = model.getPlugin().getImports();
			for (int i=0; i<iimports.length; i++) {
				IPluginImport iimport = iimports[i];
				imports.add(new ImportObject(iimport));
			}
		}
	}

	class ImportLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return resolveObjectName(obj);
		}
		public Image getImage(Object obj) {
			return resolveObjectImage(obj);
		}
	}

public ImportListSection(ManifestDependenciesPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}

public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	initializeImages();
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;

	container.setLayout(layout);
	Tree tree = new Tree(container, factory.BORDER_STYLE);
	factory.hookDeleteListener(tree);

	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener () {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.setRemoveAllWhenShown(true);
	popupMenuManager.addMenuListener(listener);
	Menu menu = popupMenuManager.createContextMenu(tree);
	tree.setMenu(menu);

	//importTree = new TableTreeViewer(tree);
	importTree = new TreeViewer(tree);
	importTree.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
	importTree.setContentProvider(new ImportContentProvider());
	importTree.setLabelProvider(new ImportLabelProvider());
	factory.paintBordersFor(container);

	importTree.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			Object item = ((IStructuredSelection)event.getSelection()).getFirstElement();
			fireSelectionNotification(item);
			getFormPage().setSelection(event.getSelection());
		}
	});
	
	GridData gd = new GridData(GridData.FILL_BOTH);
	tree.setLayoutData(gd);

	Composite buttonContainer = factory.createComposite(container);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	layout = new GridLayout();
	layout.marginHeight = 0;
	buttonContainer.setLayout(layout);

	newButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_NEW), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.verticalAlignment= GridData.BEGINNING;
	newButton.setLayoutData(gd);
	newButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleNew();
			newButton.getShell().setDefaultButton(null);
		}
	});
	return container;
}
public void dispose() {
	importImage.dispose();
	errorImportImage.dispose();
	IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
	}
}

public void expandTo(Object object) {
	importTree.setSelection(new StructuredSelection(object), true);
}

private void fillContextMenu(IMenuManager manager) {
	ISelection selection = importTree.getSelection();
	Object object = ((IStructuredSelection) selection).getFirstElement();
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}

/*
static void fillContextMenu(
	PDEFormPage page,
	final IPluginParent parent,
	IMenuManager manager,
	boolean addSiblingItems,
	boolean fullMenu) {
	MenuManager menu = new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));

	IPluginExtension extension = getExtension(parent);

	ISchema schema = extension.getSchema();
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
	if (menu.isEmpty() == false) {
		manager.add(menu);
		manager.add(new Separator());
	}
	if (fullMenu) {
		manager.add(new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
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
		});
		manager.add(new Separator());
		manager.add(new PropertiesAction(page.getEditor()));
	}
}
*/

private void handleDelete() {
/*
	IPluginObject object =
		(IPluginObject) ((IStructuredSelection) importTree.getSelection())
			.getFirstElement();
	if (object == null)
		return;

	try {
		if (object instanceof IPluginElement) {
			IPluginElement ee = (IPluginElement) object;
			IPluginParent parent = (IPluginParent) ee.getParent();
			parent.remove(ee);
		} else
			if (object instanceof IPluginExtension) {
				IPluginExtension extension = (IPluginExtension) object;
				IPluginBase plugin = extension.getPluginBase();
				plugin.remove(extension);
			}
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
*/
}

private void handleNew() {
/*
	IFile file = ((IFileEditorInput)getFormPage().getEditor().getEditorInput()).getFile();
	final IProject project = file.getProject();
	BusyIndicator.showWhile(importTree.getTree().getDisplay(), new Runnable() {
		public void run() {
			NewExtensionWizard wizard =
				new NewExtensionWizard(project, (IPluginModelBase) getFormPage().getModel());
			WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
			dialog.create();
			dialog.getShell().setSize(500, 500);
			dialog.open();
		}
	});
*/
}

public void initialize(Object input) {
	IPluginModelBase model = (IPluginModelBase)input;
	importTree.setInput(model.getPluginBase());
	setReadOnly(!model.isEditable());
	newButton.setEnabled(model.isEditable());
	model.addModelChangedListener(this);
}
public void initializeImages() {
	importImage = PDEPluginImages.DESC_REQ_PLUGIN_OBJ.createImage();
	ImageDescriptor errorDesc = 
		new OverlayIcon(PDEPluginImages.DESC_REQ_PLUGIN_OBJ, 
		new ImageDescriptor[][] { {}, {}, { PDEPluginImages.DESC_ERROR_CO }
	});
	errorImportImage = errorDesc.createImage();	
}

public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
		imports=null;
		importTree.refresh();
		return;
	}
/*
	Object changeObject = event.getChangedObjects()[0];
	if (changeObject instanceof IPluginExtension
		|| changeObject instanceof IPluginElement) {
		// We do not need to react to changes in element whose
		// parents are not extensions
		IPluginObject pobj = (IPluginObject) changeObject;
		if (!(pobj instanceof IPluginExtension)
			&& !(pobj.getParent() instanceof IPluginExtension))
			return;
		if (event.getChangeType() == event.INSERT) {
			importTree.refresh();
			importTree.setSelection(new StructuredSelection(changeObject), true);
			importTree.getTree().setFocus();
		} else
			if (event.getChangeType() == event.REMOVE) {
				importTree.refresh();
			} else {
				importTree.update(changeObject, null);
			}
	}
*/
}

private Image resolveObjectImage(Object obj) {
	ImportObject importObject = (ImportObject)obj;
	if (importObject.isResolved())
	   return importImage;
	return errorImportImage;
}

private String resolveObjectName(Object obj) {
	return obj.toString();
}

/*
public static String resolveObjectName(SchemaRegistry schemaRegistry, ExternalModelManager pluginInfoRegistry, Object obj) {
	if (obj instanceof IPluginExtension) {
		IPluginExtension extension = (IPluginExtension) obj;
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
	} else
		if (obj instanceof IPluginElement) {
			String name = obj.toString();
			IPluginElement element = (IPluginElement) obj;
			ISchemaElement elementInfo = element.getElementInfo();
			if (elementInfo!=null && elementInfo.getLabelProperty() != null) {
				IPluginAttribute att = element.getAttribute(elementInfo.getLabelProperty());
				if (att != null && att.getValue() != null)
					name = stripShortcuts(att.getValue());
					name = element.getResourceString(name);
			}
			return name;
		}
	return obj.toString();
}
*/

public void setFocus() {
	if (importTree != null)
		importTree.getTree().setFocus();
}

}
