package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;

public class URLSection extends PDEFormSection {
	public static final String SECTION_TITLE = "ComponentEditor.URLSection.title";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String POPUP_UPDATE_URL = "ComponentEditor.URLSection.updateURL";
	public static final String POPUP_DISCOVERY_URL = "ComponentEditor.URLSection.discoveryURL";
	public static final String SECTION_DESC = "ComponentEditor.URLSection.desc";
	public static final String NEW_UPDATE_SITE = "ComponentEditor.URLSection.newUpdateSite";
	public static final String NEW_DISCOVERY_SITE = "ComponentEditor.URLSection.newDiscoverySite";
	public static final String KEY_UPDATE_URLS = "ComponentEditor.URLSection.updateURLs";
	public static final String KEY_DISCOVERY_URLS = "ComponentEditor.URLSection.discoveryURLs";
	public static final String NEW_URL = "ComponentEditor.URLSection.newURL";

	private boolean updateNeeded;
	private TreeViewer urlTree;
	private Image urlFolderImage;
	private Image urlImage;
	private URLFolder[] folders =
		new URLFolder[] {
			new URLFolder(IComponentURLElement.UPDATE),
			new URLFolder(IComponentURLElement.DISCOVERY)};

	class URLFolder {
		int type;

		URLFolder(int type) {
			this.type = type;
		}
		IComponentURL getURL() {
			IComponentModel model = (IComponentModel) getFormPage().getModel();
			IComponent component = model.getComponent();
			return component.getURL();
		}
	}

	class URLContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			return getChildren(parent).length > 0;
		}

		public Object[] getChildren(Object parent) {
			if (parent instanceof IComponentURL) {
				return folders;
			}
			if (parent instanceof URLFolder) {
				URLFolder folder = (URLFolder) parent;
				IComponentURL url = folder.getURL();
				if (url != null) {
					if (folder.type == IComponentURLElement.UPDATE)
						return url.getUpdates();
					if (folder.type == IComponentURLElement.DISCOVERY)
						return url.getDiscoveries();
				}
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof URLFolder) {
				return ((URLFolder) child).getURL();
			}
			if (child instanceof IComponentURLElement) {
				IComponentURLElement element = (IComponentURLElement)child;
				if (element.getElementType()==IComponentURLElement.UPDATE)
				return folders[0];
				else return folders[1];
			}
			return null;
		}
		public Object[] getElements(Object parent) {
			return folders;
		}
	}

	class URLLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof URLFolder) {
				URLFolder folder = (URLFolder) obj;
				if (folder.type == IComponentURLElement.UPDATE) {
					return PDEPlugin.getResourceString(KEY_UPDATE_URLS);
				}
				if (folder.type == IComponentURLElement.DISCOVERY) {
					return PDEPlugin.getResourceString(KEY_DISCOVERY_URLS);
				}
			}
			return super.getText(obj);
		}

		public Image getImage(Object obj) {
			if (obj instanceof URLFolder) {
				return urlFolderImage;
			}
			if (obj instanceof IComponentURLElement) {
				return urlImage;
			}
			return null;
		}

	}

public URLSection(ComponentFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	urlImage = PDEPluginImages.DESC_LINK_OBJ.createImage();
	urlFolderImage = PDEPluginImages.DESC_LINKS_OBJ.createImage();
}
public void commitChanges(boolean onSave) {
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	container.setLayout(layout);
	Tree tree = factory.createTree(container, SWT.NULL);
	urlTree = new TreeViewer(tree);
	urlTree.setContentProvider(new URLContentProvider());
	urlTree.setLabelProvider(new URLLabelProvider());
	urlTree.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
	urlTree.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent e) {
			getFormPage().setSelection(e.getSelection());
		}
	});
	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener() {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.addMenuListener(listener);
	popupMenuManager.setRemoveAllWhenShown(true);
	Menu menu = popupMenuManager.createContextMenu(urlTree.getTree());
	urlTree.getTree().setMenu(menu);

	GridData gd = new GridData(GridData.FILL_BOTH);
	gd.heightHint = 100;
	tree.setLayoutData(gd);
	factory.paintBordersFor(container);
	return container;
}
public void dispose() {
	IComponentModel model = (IComponentModel) getFormPage().getModel();
	model.removeModelChangedListener(this);
	urlImage.dispose();
	urlFolderImage.dispose();
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
	}
}
public void expandTo(Object object) {
	urlTree.setSelection(new StructuredSelection(object), true);
}
private void fillContextMenu(IMenuManager manager) {
	if (!(getFormPage().getModel() instanceof IEditable))
		return;
	ISelection selection = urlTree.getSelection();
	Object object = ((IStructuredSelection) selection).getFirstElement();

	MenuManager submenu = new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));

	submenu.add(new Action(PDEPlugin.getResourceString(POPUP_UPDATE_URL)) {
		public void run() {
			handleNewURL(IComponentURLElement.UPDATE);
		}
	});
	submenu.add(new Action(PDEPlugin.getResourceString(POPUP_DISCOVERY_URL)) {
		public void run() {
			handleNewURL(IComponentURLElement.DISCOVERY);
		}
	});
	manager.add(submenu);

	if (object != null && object instanceof IComponentURLElement) {
		manager.add(new Separator());
		manager.add(new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
			public void run() {
				handleDelete();
			}
		});
	}
	manager.add(new Separator());
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}
private void handleDelete() {
	Object object =
		((IStructuredSelection) urlTree.getSelection()).getFirstElement();
	if (object != null && object instanceof IComponentURLElement) {
		IComponentURLElement urlElement = (IComponentURLElement) object;
		IComponent component = urlElement.getComponent();
		IComponentURL url = component.getURL();
		try {
			if (urlElement.getElementType() == IComponentURLElement.UPDATE)
				url.removeUpdate(urlElement);
			else
				url.removeDiscovery(urlElement);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
private void handleNewURL(int type) {
	IComponentModel model = (IComponentModel) getFormPage().getModel();
	IComponent component = model.getComponent();
	IComponentURL url = component.getURL();

	if (url == null) {
		url = model.getFactory().createURL();
		try {
			component.setURL(url);
		} catch (CoreException e) {
			return;
		}
	}
	try {
		IComponentURLElement element = model.getFactory().createURLElement(url, type);
		String label =
			type == IComponentURLElement.UPDATE
				? PDEPlugin.getResourceString(NEW_UPDATE_SITE)
				: PDEPlugin.getResourceString(NEW_DISCOVERY_SITE);
		element.setLabel(label);
		element.setURL(new URL(PDEPlugin.getResourceString(NEW_URL)));
		if (type == IComponentURLElement.UPDATE)
			url.addUpdate(element);
		else
			url.addDiscovery(element);

	} catch (CoreException e) {
		PDEPlugin.logException(e);
	} catch (MalformedURLException e) {
		PDEPlugin.logException(e);
	}
}
public void initialize(Object input) {
	IComponentModel model = (IComponentModel)input;
	update(input);
	if (model.isEditable()==false) {
	}
	model.addModelChangedListener(this);
}
public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
		updateNeeded = true;
		if (getFormPage().isVisible())
			update();
	} else {
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof IComponentURLElement) {
			if (e.getChangeType() == IModelChangedEvent.INSERT) {
				Object parent = null;
				IComponentURLElement element = (IComponentURLElement) obj;
				if (element.getElementType() == IComponentURLElement.UPDATE) {
					parent = folders[0];
				} else
					parent = folders[1];
				urlTree.add(parent, element);
				urlTree.setSelection(new StructuredSelection(element), true);
			} else
				if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					urlTree.remove(obj);
				} else {
					urlTree.update(obj, null);
				}
		}
	}
}
public void setFocus() {
}
public void update() {
	if (updateNeeded) {
		this.update(getFormPage().getModel());
	}
}
public void update(Object input) {
	IComponentModel model = (IComponentModel)input;
	IComponent component = model.getComponent();
	urlTree.setInput(model);
	updateNeeded=false;
}
}
