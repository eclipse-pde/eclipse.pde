package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.part.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.*;
import java.io.*;
import org.eclipse.jface.action.*;
import java.util.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.base.model.plugin.*;


public class PointGraphSection extends PDEFormSection {
	public static final String SECTION_TITLE = "ManifestEditor.PointGraphSection.title";
	public static final String SECTION_DESC = "ManifestEditor.PointGraphSection.desc";
	public static final String SECTION_FDESC = "ManifestEditor.PointGraphSection.fdesc";
	public static final String POPUP_OPEN = "Actions.open.label";
	private FormWidgetFactory factory;
	private IPluginModelBase model;
	private DependencyGraph graph;

	public static final String ROOT_BACKGROUND = "rootBg";
	public static final String ERROR_BACKGROUND = "errorBg";
	public static final String NODE_BACKGROUND = "nodeBg";
	public static final String ACTIVE_NODE_BACKGROUND = "activeNodeBg";
	public static final String ACTIVE_ROOT_BACKGROUND = "activeRootBg";
	public static final String NODE_BORDER = "nodeBorder";
	public static final String NODE_SHADOW = "nodeShadow";

	class ReferenceGraph extends DependencyGraph {
		public ReferenceGraph(Composite parent, int style) {
			super(parent, style);
		}
		public void linkActivated(Label link) {
			IDependencyGraphNode node = (IDependencyGraphNode)link.getData();
			openEditor(node);
		}
	}

	abstract class BaseNode implements IDependencyGraphNode {
		IDependencyGraphNode lastChild;

		public IDependencyGraphNode getLastChild() {
			return lastChild;
		}
		public void setLastChild(IDependencyGraphNode node) {
			lastChild = node;
		}
		public boolean isCyclical() {
			return false;
		}
	}

	class RootNode extends BaseNode {
		private String id;
		private String name;
		private Vector children;
		public RootNode(String id, String name) {
			this.id = id;
			this.name = name;
			children = new Vector();
		}
		public void add(PluginInfoNode child) {
			children.addElement(child);
			child.setParent(this);
		}
		public void setChildren(Vector children) {
			this.children = children;
		}
		public Iterator getChildren(boolean flushCache) {
			return children.iterator();
		}
		public String getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public IDependencyGraphNode getParent() {
			return null;
		}
		public IDependencyGraphNode getHomeNode() {
			return this;
		}
		public boolean isHomeNode() {
			return true;
		}

	}

	class PluginInfoNode extends BaseNode {
		private IPluginBase pluginInfo;
		private Vector children;
		private IDependencyGraphNode parent;

		public PluginInfoNode(IPluginBase info) {
			this.pluginInfo = info;
		}

		public IPluginBase getPluginInfo() {
			return pluginInfo;
		}
		public void setParent(IDependencyGraphNode parent) {
			this.parent = parent;
		}
		public Iterator getChildren(boolean flushCache) {
			if (children == null || flushCache) {
				createChildren();
			}
			return children.iterator();

		}
		private void createChildren() {
			children = new Vector();
		}
		public IDependencyGraphNode getHomeNode() {
			return parent.getHomeNode();
		}
		public String getId() {
			return pluginInfo.getId();
		}
		public String getName() {
			return pluginInfo.getResourceString(pluginInfo.getName());
		}
		public IDependencyGraphNode getParent() {
			return parent;
		}
		public boolean isHomeNode() {
			return false;
		}
	}

public PointGraphSection(ManifestExtensionPointPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
	if (fragment)
		setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
	else
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
private void addReferencingNodes(
	IDependencyGraphNode parent,
	String pluginId,
	String fullPointId,
	IPluginModel[] models,
	Vector result) {
	for (int i = 0; i < models.length; i++) {
		IPluginModel model = models[i];
		if (model.isEnabled() == false)
			continue;
		IPlugin plugin = model.getPlugin();
		if (plugin.getId().equals(pluginId))
			continue;
		if (testUsage(plugin, fullPointId)) {
			PluginInfoNode child = new PluginInfoNode(plugin);
			child.setParent(parent);
			result.addElement(child);
		}
	}
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	initializeColors();
	Composite browser = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	GridData gd;
	layout.verticalSpacing = 0;
	layout.horizontalSpacing = 0;
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	browser.setLayout(layout);

	graph = new ReferenceGraph(browser, SWT.NONE);
	graph.setBackground(factory.getBackgroundColor());
	graph.setHomeBackground(factory.getColor(ROOT_BACKGROUND));
	graph.setNodeBackground(factory.getColor(NODE_BACKGROUND));
	graph.setShadowColor(factory.getColor(NODE_SHADOW));
	graph.setActiveNodeBackground(factory.getColor(ACTIVE_NODE_BACKGROUND));
	graph.setActiveHomeBackground(factory.getColor(ACTIVE_ROOT_BACKGROUND));
	graph.setErrorBackground(factory.getColor(ERROR_BACKGROUND));
	graph.setShadowPainted(true);
	graph.setBorderPainted(true);
	graph.setBorderColor(factory.getColor(NODE_BORDER));
	graph.setReverse(true);
	graph.setTraversalEnabled(false);
	gd = new GridData(GridData.FILL_BOTH);
	graph.setLayoutData(gd);
	graph.addRootNodeListener(new IRootNodeListener() {
		public void rootNodeChanged(IDependencyGraphNode node) {
			((ScrollableSectionForm) getFormPage().getForm()).update();
		}
	});
	graph.setPopupListener(new IMenuListener() {
		public void menuAboutToShow(IMenuManager manager) {
			if (graph.getSelectedNode() != null) {
				manager.add(new Action(PDEPlugin.getResourceString(POPUP_OPEN)) {
					public void run() {
						openEditor(graph.getSelectedNode());
					}
				});
			}
			getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
		}
	});
	return browser;
}
private Vector createReferencingNodes(
	IDependencyGraphNode parent,
	String fullPointId) {
	Vector children = new Vector();

	// Test this plugin
	Object model = getFormPage().getModel();
	IPluginBase thisPluginBase = ((IPluginModelBase) model).getPluginBase();
	if (testUsage(thisPluginBase, fullPointId)) {
		PluginInfoNode child = new PluginInfoNode(thisPluginBase);
		child.setParent(parent);
		children.addElement(child);
	}

	WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
	addReferencingNodes(
		parent,
		thisPluginBase.getId(),
		fullPointId,
		manager.getWorkspacePluginModels(),
		children);

	ExternalModelManager registry = PDEPlugin.getDefault().getExternalModelManager();
	addReferencingNodes(
		parent,
		thisPluginBase.getId(),
		fullPointId,
		registry.getModels(),
		children);
	return children;
}
private String getPluginName() {
	return model.getPluginBase().getName();
}
public void initialize(Object input) {
	this.model = (IPluginModelBase)input;
}
private void initializeColors() {
	factory.registerColor(NODE_BACKGROUND, 255, 218, 121);
	factory.registerColor(ROOT_BACKGROUND, 193, 209, 253);
	factory.registerColor(ACTIVE_NODE_BACKGROUND, 255, 240, 202);
	factory.registerColor(ACTIVE_ROOT_BACKGROUND, 235, 240, 254); 
	factory.registerColor(NODE_BORDER, 0, 0, 0);
	factory.registerColor(NODE_SHADOW, 195, 191, 179);
	factory.registerColor(ERROR_BACKGROUND, 255, 0, 0);
}
private void openEditor(IDependencyGraphNode node) {
	if (!(node instanceof PluginInfoNode))
		return;
	String id = node.getId();
	IPlugin pluginToOpen = PDEPlugin.getDefault().findPlugin(id);
	if (pluginToOpen != null) {
		IResource underlyingResource = pluginToOpen.getModel().getUnderlyingResource();
		if (underlyingResource == null) {
			openExternalPlugin(pluginToOpen);
		} else {
			openWorkspacePlugin((IFile) underlyingResource);
		}
	} else {
		Display.getCurrent().beep();
	}
}
private void openExternalPlugin(IPlugin pluginInfo) {
	String fileName =
		pluginInfo.getModel().getInstallLocation() + File.separator + "plugin.xml";
	File file = new File(fileName);
	if (file.exists()) {
		String editorId = PDEPlugin.MANIFEST_EDITOR_ID;
		try {
			SystemFileEditorInput input = new SystemFileEditorInput(file);
			PDEPlugin.getActivePage().openEditor(input, editorId);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
}
private void openWorkspacePlugin(IFile pluginFile) {
	String editorId = PDEPlugin.MANIFEST_EDITOR_ID;
	try {
		FileEditorInput input = new FileEditorInput(pluginFile);
		PDEPlugin.getActivePage().openEditor(input, editorId);
	} catch (PartInitException e) {
		PDEPlugin.logException(e);
	}
}
public void sectionChanged(FormSection source, int changeType, Object changeObject) {
	IPluginBase pluginBase = model.getPluginBase();
	updateGraph(pluginBase.getId(), pluginBase.getName(), (IPluginExtensionPoint)changeObject);
}
private boolean testUsage(IPluginBase pluginBase, String pointId) {
	IPluginExtension [] extensions = pluginBase.getExtensions();
	for (int i=0; i<extensions.length; i++) {
		IPluginExtension extension = extensions[i];
		if (extension.getPoint().equals(pointId)) {
			return true;
		}
	}
	return false;
}
private void updateGraph(
	String id,
	String name,
	IPluginExtensionPoint extensionPoint) {
	if (extensionPoint == null) {
		graph.setRoot(null);
		return;
	}
	RootNode rootNode = new RootNode(id, extensionPoint.getResourceString(extensionPoint.getName()));
	rootNode.setChildren(
		createReferencingNodes(rootNode, extensionPoint.getFullId()));
	graph.setRoot(rootNode);
}
}
