package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;

public class URLSection extends PDEFormSection {
	public static final String SECTION_TITLE = "FeatureEditor.URLSection.title";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String POPUP_UPDATE_URL = "FeatureEditor.URLSection.updateURL";
	public static final String POPUP_DISCOVERY_URL = "FeatureEditor.URLSection.discoveryURL";
	public static final String SECTION_DESC = "FeatureEditor.URLSection.desc";
	public static final String NEW_UPDATE_SITE = "FeatureEditor.URLSection.newUpdateSite";
	public static final String NEW_DISCOVERY_SITE = "FeatureEditor.URLSection.newDiscoverySite";
	public static final String KEY_UPDATE_URLS = "FeatureEditor.URLSection.updateURLs";
	public static final String KEY_DISCOVERY_URLS = "FeatureEditor.URLSection.discoveryURLs";
	public static final String NEW_URL = "FeatureEditor.URLSection.newURL";

	private boolean updateNeeded;
	private TreeViewer urlTree;
	private Image urlFolderImage;
	private Image urlImage;
	private URLFolder[] folders =
		new URLFolder[] {
			new URLFolder(IFeatureURLElement.UPDATE),
			new URLFolder(IFeatureURLElement.DISCOVERY)};

	class URLFolder {
		int type;

		URLFolder(int type) {
			this.type = type;
		}
		IFeatureURL getURL() {
			IFeatureModel model = (IFeatureModel) getFormPage().getModel();
			IFeature component = model.getFeature();
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
			if (parent instanceof IFeatureURL) {
				return folders;
			}
			if (parent instanceof URLFolder) {
				URLFolder folder = (URLFolder) parent;
				IFeatureURL url = folder.getURL();
				if (url != null) {
					if (folder.type == IFeatureURLElement.UPDATE)
						return url.getUpdates();
					if (folder.type == IFeatureURLElement.DISCOVERY)
						return url.getDiscoveries();
				}
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof URLFolder) {
				return ((URLFolder) child).getURL();
			}
			if (child instanceof IFeatureURLElement) {
				IFeatureURLElement element = (IFeatureURLElement)child;
				if (element.getElementType()==IFeatureURLElement.UPDATE)
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
				if (folder.type == IFeatureURLElement.UPDATE) {
					return PDEPlugin.getResourceString(KEY_UPDATE_URLS);
				}
				if (folder.type == IFeatureURLElement.DISCOVERY) {
					return PDEPlugin.getResourceString(KEY_DISCOVERY_URLS);
				}
			}
			return super.getText(obj);
		}

		public Image getImage(Object obj) {
			if (obj instanceof URLFolder) {
				return urlFolderImage;
			}
			if (obj instanceof IFeatureURLElement) {
				return urlImage;
			}
			return null;
		}

	}

public URLSection(FeatureFormPage page) {
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
	IFeatureModel model = (IFeatureModel) getFormPage().getModel();
	model.removeModelChangedListener(this);
	urlImage.dispose();
	urlFolderImage.dispose();
	super.dispose();
}
public boolean doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
		return true;
	}
	return false;
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
			handleNewURL(IFeatureURLElement.UPDATE);
		}
	});
	submenu.add(new Action(PDEPlugin.getResourceString(POPUP_DISCOVERY_URL)) {
		public void run() {
			handleNewURL(IFeatureURLElement.DISCOVERY);
		}
	});
	manager.add(submenu);

	if (object != null && object instanceof IFeatureURLElement) {
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
	if (object != null && object instanceof IFeatureURLElement) {
		IFeatureURLElement urlElement = (IFeatureURLElement) object;
		IFeature component = urlElement.getFeature();
		IFeatureURL url = component.getURL();
		try {
			if (urlElement.getElementType() == IFeatureURLElement.UPDATE)
				url.removeUpdate(urlElement);
			else
				url.removeDiscovery(urlElement);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
private void handleNewURL(int type) {
	IFeatureModel model = (IFeatureModel) getFormPage().getModel();
	IFeature component = model.getFeature();
	IFeatureURL url = component.getURL();

	if (url == null) {
		url = model.getFactory().createURL();
		try {
			component.setURL(url);
		} catch (CoreException e) {
			return;
		}
	}
	try {
		IFeatureURLElement element = model.getFactory().createURLElement(url, type);
		String label =
			type == IFeatureURLElement.UPDATE
				? PDEPlugin.getResourceString(NEW_UPDATE_SITE)
				: PDEPlugin.getResourceString(NEW_DISCOVERY_SITE);
		element.setLabel(label);
		element.setURL(new URL(PDEPlugin.getResourceString(NEW_URL)));
		if (type == IFeatureURLElement.UPDATE)
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
	IFeatureModel model = (IFeatureModel)input;
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
		if (obj instanceof IFeatureURLElement) {
			if (e.getChangeType() == IModelChangedEvent.INSERT) {
				Object parent = null;
				IFeatureURLElement element = (IFeatureURLElement) obj;
				if (element.getElementType() == IFeatureURLElement.UPDATE) {
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
	IFeatureModel model = (IFeatureModel)input;
	IFeature component = model.getFeature();
	urlTree.setInput(model);
	updateNeeded=false;
}
}
