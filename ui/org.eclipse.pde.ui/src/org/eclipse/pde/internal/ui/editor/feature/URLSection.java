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
package org.eclipse.pde.internal.ui.editor.feature;

import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class URLSection extends PDESection {
	public static final String SECTION_TITLE = "FeatureEditor.URLSection.title"; //$NON-NLS-1$
	public static final String POPUP_NEW = "Menus.new.label"; //$NON-NLS-1$
	public static final String POPUP_DELETE = "Actions.delete.label"; //$NON-NLS-1$
	public static final String POPUP_UPDATE_URL =
		"FeatureEditor.URLSection.updateURL"; //$NON-NLS-1$
	public static final String POPUP_DISCOVERY_URL =
		"FeatureEditor.URLSection.discoveryURL"; //$NON-NLS-1$
	public static final String SECTION_DESC = "FeatureEditor.URLSection.desc"; //$NON-NLS-1$
	public static final String NEW_UPDATE_SITE =
		"FeatureEditor.URLSection.newUpdateSite"; //$NON-NLS-1$
	public static final String NEW_DISCOVERY_SITE =
		"FeatureEditor.URLSection.newDiscoverySite"; //$NON-NLS-1$
	public static final String KEY_UPDATE_URLS =
		"FeatureEditor.URLSection.updateURLs"; //$NON-NLS-1$
	public static final String KEY_DISCOVERY_URLS =
		"FeatureEditor.URLSection.discoveryURLs"; //$NON-NLS-1$
	public static final String NEW_URL = "FeatureEditor.URLSection.newURL"; //$NON-NLS-1$

	private TreeViewer urlTree;
	private Image urlImage;
	private Image urlFolderImage;
	private PropertiesAction propertiesAction;
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
			IFeatureModel model = (IFeatureModel) getPage().getModel();
			IFeature feature = model.getFeature();
			return feature.getURL();
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
				IFeatureURLElement element = (IFeatureURLElement) child;
				if (element.getElementType() == IFeatureURLElement.UPDATE)
					return folders[0];
				else
					return folders[1];
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

	public URLSection(FeatureFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		urlImage = provider.get(PDEPluginImages.DESC_LINK_OBJ);
		urlFolderImage = provider.get(PDEPluginImages.DESC_LINKS_OBJ);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	public void commit(boolean onSave) {
		super.commit(onSave);
	}
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		Tree tree = toolkit.createTree(container, SWT.NULL);
		urlTree = new TreeViewer(tree);
		urlTree.setContentProvider(new URLContentProvider());
		urlTree.setLabelProvider(new URLLabelProvider());
		urlTree.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		urlTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				getPage().getPDEEditor().setSelection(e.getSelection());
			}
		});
		urlTree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				propertiesAction.run();
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
		toolkit.paintBordersFor(container);
		propertiesAction = new PropertiesAction(getPage().getPDEEditor());
		section.setClient(container);
		initialize();
	}
	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null) 
			model.removeModelChangedListener(this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
			if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
	return false;
	}
	public boolean setFormInput(Object object) {
		urlTree.setSelection(new StructuredSelection(object), true);
		return true;
	}
	private void fillContextMenu(IMenuManager manager) {
		IModel model = (IModel)getPage().getModel();
		ISelection selection = urlTree.getSelection();
		Object object = ((IStructuredSelection) selection).getFirstElement();

		MenuManager submenu = new MenuManager(PDEPlugin.getResourceString(POPUP_NEW));

		Action updateUrl = new Action(PDEPlugin.getResourceString(POPUP_UPDATE_URL)) {
			public void run() {
				handleNewURL(IFeatureURLElement.UPDATE);
			}
		};
		updateUrl.setEnabled(model.isEditable());
		submenu.add(updateUrl);
		Action discoveryUrl = new Action(PDEPlugin.getResourceString(POPUP_DISCOVERY_URL)) {
			public void run() {
				handleNewURL(IFeatureURLElement.DISCOVERY);
			}
		};
		discoveryUrl.setEnabled(model.isEditable());
		submenu.add(discoveryUrl);
		manager.add(submenu);

		if (object != null && object instanceof IFeatureURLElement) {
			manager.add(new Separator());
			Action deleteAction = new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					handleDelete();
				}
			};
			deleteAction.setEnabled(model.isEditable());
			manager.add(deleteAction);
		}
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());
		manager.add(propertiesAction);
	}
	private void handleDelete() {
		Object object =
			((IStructuredSelection) urlTree.getSelection()).getFirstElement();
		if (object != null && object instanceof IFeatureURLElement) {
			IFeatureURLElement urlElement = (IFeatureURLElement) object;
			IFeature feature = urlElement.getFeature();
			IFeatureURL url = feature.getURL();
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
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IFeatureURL url = feature.getURL();

		if (url == null) {
			url = model.getFactory().createURL();
			try {
				feature.setURL(url);
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
	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		model.addModelChangedListener(this);
	}
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
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
				} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
					urlTree.remove(obj);
				} else {
					urlTree.update(obj, null);
				}
			}
		}
	}
	public void setFocus() {
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		urlTree.setInput(model);
		super.refresh();
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		IStructuredSelection ssel = (IStructuredSelection)urlTree.getSelection();
		if (ssel.size() != 1) return false;
		
		Object target = ssel.getFirstElement();
		if (target instanceof URLFolder) {
			Object [] objects = (Object[])clipboard.getContents(ModelDataTransfer.getInstance());
			if (objects != null && objects.length > 0) {
				return canPaste((URLFolder)target, objects);
			}
		}
		return false;
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object, Object[])
	 */
	protected boolean canPaste(URLFolder target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof FeatureURLElement) || 
				((FeatureURLElement)objects[i]).getElementType() != target.type)
				return false;
		}
		return true;
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	protected void doPaste() {
		IStructuredSelection ssel = (IStructuredSelection)urlTree.getSelection();
		if (ssel.size() != 1) return;
		
		Object target = ssel.getFirstElement();
		if (target instanceof URLFolder) {
			Clipboard clipboard = getPage().getPDEEditor().getClipboard();
			Object [] objects = (Object[])clipboard.getContents(ModelDataTransfer.getInstance());
			if (objects != null) {
				doPaste((URLFolder)target, objects);
			}
		}
	}
	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object, Object[])
	 */
	protected void doPaste(URLFolder target, Object[] objects) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof FeatureURLElement) {
				FeatureURLElement element = (FeatureURLElement)objects[i];
				if (element.getElementType() == target.type) {
					element.setModel(model);
					element.setParent(feature);
					try {
						if (target.type == IFeatureURLElement.UPDATE) 
							feature.getURL().addUpdate(element);
						else
							feature.getURL().addDiscovery(element);
					} catch (CoreException e) {
						PDECore.logException(e);
					}
				}
			}
		}
	}
}
