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
import org.eclipse.pde.internal.model.ImportObject;

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
	public static final String POPUP_OPEN = "Actions.open.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private Button newButton;
	private Vector imports;
	private Action openAction;
	private Action newAction;
	private Action deleteAction;
	
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
	Tree tree = new Tree(container, SWT.MULTI | factory.BORDER_STYLE);
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
	importTree.addDoubleClickListener(new IDoubleClickListener() {
		public void doubleClick(DoubleClickEvent e) {
			handleOpen(e.getSelection());
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
	makeActions();
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
	if (object instanceof IPluginImport) {
		ImportObject iobj = new ImportObject((IPluginImport)object);
		importTree.setSelection(new StructuredSelection(iobj), true);
	}
}

private void fillContextMenu(IMenuManager manager) {
	ISelection selection = importTree.getSelection();
	manager.add(newAction);
	manager.add(new Separator());
	if (!selection.isEmpty()) {
		manager.add(openAction);
		manager.add(deleteAction);
	}
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}

private void handleDelete() {
	IStructuredSelection ssel = (IStructuredSelection)importTree.getSelection();

	if (ssel.isEmpty()) return;	
	IPluginModel model = (IPluginModel)getFormPage().getModel();
	IPlugin plugin = model.getPlugin();

	try {
		for (Iterator iter = ssel.iterator(); iter.hasNext();) {
			ImportObject iobj = (ImportObject)iter.next();
			plugin.remove(iobj.getImport());
		}
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}

private void handleNew() {
	final IPluginModel model = (IPluginModel)getFormPage().getModel();
	BusyIndicator.showWhile(importTree.getTree().getDisplay(), new Runnable() {
		public void run() {
			NewDependencyWizard wizard =
				new NewDependencyWizard(model);
			WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
			dialog.create();
			dialog.getShell().setSize(500, 500);
			dialog.open();
		}
	});
}

private void handleOpen(ISelection sel) {
	if (sel instanceof IStructuredSelection) {
		IStructuredSelection ssel = (IStructuredSelection)sel;
		if (ssel.size()==1) {
			handleOpen(ssel.getFirstElement());
		}
	}
}

private void handleOpen(Object obj) {
	if (obj instanceof ImportObject) {
		IPlugin plugin = ((ImportObject)obj).getPlugin();
		if (plugin!=null)
			((ManifestEditor)getFormPage().getEditor()).openPluginEditor(plugin);
	}
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

private void makeActions() {
	newAction = new Action() {
		public void run() {
			handleNew();
		}
	};
	newAction.setText(PDEPlugin.getResourceString(POPUP_NEW));
	openAction = new Action() {
		public void run() {
			handleOpen(importTree.getSelection());
		}
	};
	openAction.setText(PDEPlugin.getResourceString(POPUP_OPEN));

	deleteAction = new Action() {
		public void run() {
			handleDelete();
		}
	};
	deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
}

public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
		imports=null;
		importTree.refresh();
		return;
	}

	Object changeObject = event.getChangedObjects()[0];
	if (changeObject instanceof IPluginImport) {
		IPluginImport iimport = (IPluginImport)changeObject;
		if (event.getChangeType() == event.INSERT) {
			ImportObject iobj = new ImportObject(iimport);
			imports.add(iobj);
			importTree.add(iimport.getParent(), iobj);
			importTree.setSelection(new StructuredSelection(iobj), true);
			importTree.getTree().setFocus();
		} else {
			ImportObject iobj = findImportObject(iimport);
			if (iobj!=null) {
				if (event.getChangeType() == event.REMOVE) {
					imports.remove(iobj);
					importTree.remove(iobj);
				} else {
					importTree.update(iobj, null);
				}
			}
		}
	}
}

private ImportObject findImportObject(IPluginImport iimport) {
	if (imports==null) return null;
	for (int i=0; i<imports.size(); i++) {
		ImportObject iobj = (ImportObject)imports.get(i);
		if (iobj.getImport().equals(iimport))
		   return iobj;
	}
	return null;
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

public void setFocus() {
	if (importTree != null)
		importTree.getTree().setFocus();
}

}
