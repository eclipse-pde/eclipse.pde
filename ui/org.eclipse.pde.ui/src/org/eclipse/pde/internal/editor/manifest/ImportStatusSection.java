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
import org.eclipse.pde.internal.util.*;
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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.pde.internal.builders.*;

public class ImportStatusSection
	extends PDEFormSection
	implements IModelChangedListener {
	private TreeViewer statusTree;
	private FormWidgetFactory factory;
	private Image pluginImage;
	private Image importImage;
	private Image warningLoopImage;
	private Image loopNodeImage;
	private CCombo combo;
	private Vector references;
	private Object[] loops;
	private Action openAction;
	private Action refreshAction;

	public static final String SECTION_TITLE = "ManifestEditor.ImportStatusSection.title";
	public static final String SECTION_DESC = "ManifestEditor.ImportStatusSection.desc";
	public static final String COMBO_LABEL = "ManifestEditor.ImportStatusSection.comboLabel";
	public static final String COMBO_LOOPS = "ManifestEditor.ImportStatusSection.comboLoops";
	public static final String COMBO_REFS = "ManifestEditor.ImportStatusSection.comboRefs";
	public static final String KEY_OPEN_LABEL = "Actions.open.label";
	public static final String KEY_REFRESH_LABEL = "Actions.refresh.label";

	private static final int LOOP_MODE = 0;
	private static final int REFERENCE_MODE = 1;

	private int mode = LOOP_MODE;
	
	class StatusContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {

		public Object[] getChildren(Object parent) {
			if (parent instanceof DependencyLoop) {
				DependencyLoop loop = (DependencyLoop)parent;
				return loop.getMembers();
			}
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof DependencyLoop)
			   return true;
			return false;
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object[] getElements(Object parent) {
			if (mode==REFERENCE_MODE) {
				return getReferences();
			}
			if (mode==LOOP_MODE) {
				return getLoops();
			}
			return new Object[0];
		}
	}

	class StatusLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return resolveObjectName(obj);
		}
		public Image getImage(Object obj) {
			return resolveObjectImage(obj);
		}
	}

public ImportStatusSection(ManifestDependenciesPage page) {
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
	
	Label label = factory.createLabel(container, PDEPlugin.getResourceString(COMBO_LABEL));
	int comboStyle = SWT.READ_ONLY;
	if (SWT.getPlatform().equals("motif")==false)
	   comboStyle |= SWT.FLAT;
	else
	   comboStyle |= SWT.BORDER;
	combo = new CCombo(container, comboStyle);
	combo.setBackground(factory.getBackgroundColor());
	combo.setForeground(factory.getForegroundColor());
	combo.add(PDEPlugin.getResourceString(COMBO_LOOPS));
	combo.add(PDEPlugin.getResourceString(COMBO_REFS));
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	combo.setLayoutData(gd);
	combo.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			viewChanged();
		}
	});
	
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

	statusTree = new TreeViewer(tree);
	statusTree.setContentProvider(new StatusContentProvider());
	statusTree.setLabelProvider(new StatusLabelProvider());
	factory.paintBordersFor(container);

	statusTree.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			Object item = ((IStructuredSelection)event.getSelection()).getFirstElement();
			fireSelectionNotification(item);
			getFormPage().setSelection(event.getSelection());
		}
	});
	statusTree.addDoubleClickListener(new IDoubleClickListener() {
		public void doubleClick(DoubleClickEvent e) {
			handleOpen(e.getSelection());
		}
	});
	
	gd = new GridData(GridData.FILL_BOTH);
	gd.horizontalSpan = 2;
	tree.setLayoutData(gd);
	combo.select(0);
	makeActions();
	return container;
}
public void dispose() {
	importImage.dispose();
	pluginImage.dispose();
	warningLoopImage.dispose();
	loopNodeImage.dispose();
	IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
public boolean doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		//handleDelete();
		return true;
	}
	return false;
}

public void expandTo(Object object) {
	if (object instanceof IPluginImport) {
		ImportObject iobj = new ImportObject((IPluginImport)object);
		statusTree.setSelection(new StructuredSelection(iobj), true);
	}
}

private void fillContextMenu(IMenuManager manager) {
	ISelection selection = statusTree.getSelection();
	if (!selection.isEmpty()) {
		Object object = ((IStructuredSelection) selection).getFirstElement();
		if (object instanceof IPlugin) {
			manager.add(openAction);
		}
	}
	manager.add(refreshAction);
	manager.add(new Separator());
	((DependenciesForm)getFormPage().getForm()).fillContextMenu(manager);
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}

public void initialize(Object input) {
	IPluginModelBase model = (IPluginModelBase)input;
	statusTree.setInput(model.getPluginBase());
	setReadOnly(!model.isEditable());
	model.addModelChangedListener(this);
}

public void initializeImages() {
	importImage = PDEPluginImages.DESC_REQ_PLUGIN_OBJ.createImage();
	pluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
	loopNodeImage = PDEPluginImages.DESC_LOOP_NODE_OBJ.createImage();
	ImageDescriptor warningDesc = 
		new OverlayIcon(PDEPluginImages.DESC_LOOP_OBJ, 
		new ImageDescriptor[][] { {}, {}, { PDEPluginImages.DESC_WARNING_CO }
	});
	warningLoopImage = warningDesc.createImage();
}

private void makeActions() {
	openAction = new Action() {
		public void run() {
			handleOpen(statusTree.getSelection());
		}
	};
	openAction.setText(PDEPlugin.getResourceString(KEY_OPEN_LABEL));
	
	refreshAction = new Action() {
		public void run() {
			viewChanged();
		}
	};
	refreshAction.setText(PDEPlugin.getResourceString(KEY_REFRESH_LABEL));
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
	if (obj instanceof IPlugin) {
		IPlugin plugin = ((IPlugin)obj);
		((ManifestEditor)getFormPage().getEditor()).openPluginEditor(plugin);
	}
}

public void modelChanged(IModelChangedEvent event) {
	boolean fullRefresh=false;
	
	if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED)
		fullRefresh=true;
	
	else {
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginImport)
		   fullRefresh = true;
	}
	if (fullRefresh) {
		references = null;
		loops = null;
		statusTree.refresh();
		return;
	}
}

private void viewChanged() {
	int index = combo.getSelectionIndex();
	if (index==0) {
		mode = LOOP_MODE;
		statusTree.setAutoExpandLevel(0);
	}
	else {
		mode = REFERENCE_MODE;
		statusTree.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
	}
	statusTree.refresh();
}

private Object[] getReferences() {
	if (references==null) {
		references = new Vector();
		IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		String referenceId = plugin.getId();

		ExternalModelManager registry = PDEPlugin.getDefault().getExternalModelManager();
		WorkspaceModelManager manager =
			(WorkspaceModelManager) PDEPlugin.getDefault().getWorkspaceModelManager();
		
		createReferences(references, manager.getWorkspacePluginModels(), referenceId);
		if (registry.hasEnabledModels())
			createReferences(references, registry.getModels(), referenceId);
	}
	return references.toArray();
}

private void createReferences(Vector result, IPluginModel [] candidates, String id) {
	for (int i=0; i<candidates.length; i++) {
		IPluginModel candidate = candidates[i];
		if (isReferencing(candidate, id))
		   result.add(candidate.getPlugin());
	}
}

private boolean isReferencing(IPluginModel model, String id) {
	IPlugin plugin = model.getPlugin();
	IPluginImport [] imports = plugin.getImports();
	for (int i=0; i<imports.length; i++) {
		IPluginImport iimport = imports[i];
		if (iimport.getId().equals(id)) {
			return true;
		}
	}
	return false;
}

private Object[] getLoops() {
	if (loops==null) {
		IPlugin plugin = ((IPluginModel)getFormPage().getModel()).getPlugin();
		loops = DependencyLoopFinder.findLoops(plugin);
	}
	return loops;
}

private Image resolveObjectImage(Object obj) {
	if (obj instanceof IPlugin) {
		if (mode==REFERENCE_MODE)
			return pluginImage;
		else if (mode==LOOP_MODE)
			return loopNodeImage;
	}
	if (obj instanceof DependencyLoop) {
		return warningLoopImage;
	}
	return null;
}

private String resolveObjectName(Object obj) {
	if (mode==REFERENCE_MODE) {
		if (obj instanceof IPlugin) {
			IPlugin plugin = (IPlugin)obj;
			return plugin.getTranslatedName();
		}
	}
	return obj.toString();
}

public void setFocus() {
	if (statusTree != null)
		statusTree.getTree().setFocus();
}

}
