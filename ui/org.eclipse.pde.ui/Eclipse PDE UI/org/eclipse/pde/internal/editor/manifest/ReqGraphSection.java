package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.part.*;
import org.eclipse.core.resources.*;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.ui.*;
import java.io.*;
import org.eclipse.jface.action.*;
import java.util.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;


public class ReqGraphSection extends PDEFormSection {
	private FormWidgetFactory factory;
	private IPluginModel model;
	private DependencyGraph graph;
	private Label cyclesLabel;
	private FormButton forwardButton;
	private FormButton backButton;
	private FormButton homeButton;

	private Image backwardImage;
	private Image backwardHoverImage;
	private Image backwardDisabledImage;
	private Image forwardImage;
	private Image forwardHoverImage;
	private Image forwardDisabledImage;
	private Image homeImage;
	private Image homeHoverImage;
	private Image homeDisabledImage;
	private Composite browser;

	public static final String ROOT_BACKGROUND = "rootBg";
	public static final String SECTION_TITLE = "ManifestEditor.ReqGraphSection.title";
	public static final String SECTION_DESC = "ManifestEditor.ReqGraphSection.desc";
	public static final String KEY_SHOW_REQUIRED = "ManifestEditor.ReqGraphSection.showRequired";
	public static final String KEY_SHOW_REFERENCING = "ManifestEditor.ReqGraphSection.showReferencing";
	public static final String KEY_BACK = "ManifestEditor.ReqGraphSection.back";
	public static final String POPUP_OPEN = "Actions.open.label";
	public static final String KEY_FORWARD = "ManifestEditor.ReqGraphSection.forward";
	public static final String KEY_HOME = "ManifestEditor.ReqGraphSection.home";
	public static final String KEY_CYCLES = "ManifestEditor.ReqGraphSection.cycles";
	
	public static final String ERROR_BACKGROUND = "errorBg";
	public static final String NODE_BACKGROUND = "nodeBg";
	public static final String ACTIVE_NODE_BACKGROUND = "activeNodeBg";
	public static final String ACTIVE_ROOT_BACKGROUND = "activeRootBg";
	public static final String NODE_BORDER = "nodeBorder";
	public static final String NODE_SHADOW = "nodeShadow";

	abstract class BaseNode implements IDependencyGraphNode {
		IDependencyGraphNode lastChild;

		public IDependencyGraphNode getLastChild() {
			return lastChild;
		}
		public void setLastChild(IDependencyGraphNode node) {
			lastChild = node;
		}
		public boolean isCyclical() { return false; }
	}

	class RootNode extends BaseNode {
		private String id;
		private String name;
		private Vector children;
		private Vector references;
		public RootNode(String id, String name) {
			this.id = id;
			this.name = name;
			children = new Vector();
		}
		public void add(PluginInfoNode child) {
			children.addElement(child);
			child.setParent(this);
		}
		public void add(UnresolvedNode child) {
			children.addElement(child);
			child.setParent(this);
		}
		public void setChildren(Vector children) {
			this.children = children;
		}
		public Iterator getChildren(boolean flushCache) {
			if (graph.isReverse()) {
				if (references==null || flushCache) references = createReferencingNodes(this, id);
				return references.iterator();
			}
			else return children.iterator();
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
		private IPlugin pluginInfo;
		private Vector children;
		private IDependencyGraphNode parent;
		private boolean cyclical = false;
		
		public PluginInfoNode(IPlugin info) {
			this.pluginInfo = info;
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
			if (graph.isReverse())
				children = createReferencingNodes(this, pluginInfo);
			else
				children = createRequiredNodes(this, pluginInfo);
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
		public boolean isCyclical() {
			return cyclical;
		}
		public void setCyclical(boolean value) {
			cyclical = value;
		}
	}
	class UnresolvedNode extends BaseNode {
		private IPluginImport iimport;
		private IDependencyGraphNode parent;
		
		public UnresolvedNode(IPluginImport iimport) {
			this.iimport = iimport;
		}
		public void setParent(IDependencyGraphNode parent) {
			this.parent = parent;
		}
		public Iterator getChildren(boolean flushCashe) {
			return new Vector().iterator();
		}
		public IDependencyGraphNode getHomeNode() {
			return parent.getHomeNode();
		}
		public String getId() {
			return iimport.getId();
		}
		public String getName() {
			return getId();
		}
		public IDependencyGraphNode getParent() {
			return parent;
		}
		public boolean isHomeNode() {
			return false;
		}
		public boolean isCyclical() {
			return true;
		}
	}
	

public ReqGraphSection(ManifestDependenciesPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public static boolean checkForCycles(String referenceId, IPlugin pluginInfo) {
	IPluginImport[] imports = pluginInfo.getImports();

	for (int i = 0; i < imports.length; i++) {
		IPluginImport reqInfo = imports[i];
		if (reqInfo.getId().equals(referenceId))
			return true;
		IPlugin requiredPlugin = PDEPlugin.getDefault().findPlugin(reqInfo.getId());
		if (requiredPlugin != null) {
			if (checkForCycles(referenceId, requiredPlugin))
				return true;
		}
	}
	// no cycles
	return false;
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.model = (IPluginModel) getFormPage().getModel();
	this.factory = factory;
	initializeColors();
	initializeImages();
	browser = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.verticalSpacing = 0;
	layout.horizontalSpacing = 0;
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	browser.setLayout(layout);

	Composite buttonContainer = factory.createComposite(browser);
	RowLayout rlayout = new RowLayout();
	rlayout.marginBottom = 10;
	buttonContainer.setLayout(rlayout);

	Button button = factory.createButton(buttonContainer, "Home", SWT.PUSH);
	button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			graph.doOperation(DependencyGraph.OP_HOME);
		}
	});
	homeButton = new FormButton(button, factory);
	homeButton.setImage(homeImage);
	homeButton.setHoverImage(homeHoverImage);
	homeButton.setDisabledImage(homeDisabledImage);

	button = factory.createButton(buttonContainer, "Back", SWT.PUSH);
	button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			graph.doOperation(DependencyGraph.OP_BACK);
		}
	});
	backButton = new FormButton(button, factory);
	backButton.setImage(backwardImage);
	backButton.setHoverImage(backwardHoverImage);
	backButton.setDisabledImage(backwardDisabledImage);
	button = factory.createButton(buttonContainer, "Forward", SWT.PUSH);
	button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			graph.doOperation(DependencyGraph.OP_FORWARD);
		}
	});
	forwardButton = new FormButton(button, factory);
	forwardButton.setImage(forwardImage);
	forwardButton.setHoverImage(forwardHoverImage);
	forwardButton.setDisabledImage(forwardDisabledImage);

	Composite radioContainer = factory.createComposite(buttonContainer);
	button =
		factory.createButton(
			radioContainer,
			PDEPlugin.getResourceString(KEY_SHOW_REQUIRED),
			SWT.RADIO);
	button.setSelection(true);
	button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			graph.setReverse(false);
		}
	});
	button.setLayoutData(
		new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
	button =
		factory.createButton(
			radioContainer,
			PDEPlugin.getResourceString(KEY_SHOW_REFERENCING),
			SWT.RADIO);
	button.setLayoutData(
		new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
	button.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			graph.setReverse(true);
		}
	});
	GridLayout radioLayout = new GridLayout();
	radioLayout.marginHeight = 0;
	radioLayout.marginWidth = 20;
	radioContainer.setLayout(radioLayout);
	//radioContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));

	GridData gd =
		new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
	buttonContainer.setLayoutData(gd);

	//factory.createSeparator(browser, SWT.HORIZONTAL);
	cyclesLabel = factory.createLabel(browser, "");
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	cyclesLabel.setLayoutData(gd);
	cyclesLabel.setForeground(browser.getDisplay().getSystemColor(SWT.COLOR_RED));

	graph = new DependencyGraph(browser, SWT.NONE);
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
	gd = new GridData(GridData.FILL_BOTH);
	graph.setLayoutData(gd);
	//viewport.setLayoutData(gd);
	graph.addRootNodeListener(new IRootNodeListener() {
		public void rootNodeChanged(IDependencyGraphNode node) {
			updateButtons(node);
			updateLayout();
		}
	});
	graph.setPopupListener(new IMenuListener() {
		public void menuAboutToShow(IMenuManager manager) {
			manager.removeAll();
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
private void createReferencingNodes(
	IDependencyGraphNode parent,
	IPluginModel [] models,
	String referenceId,
	Vector children) {
	for (int i = 0; i < models.length; i++) {
		IPlugin candidate = models[i].getPlugin();
		if (candidate.getId().equals(referenceId))
			continue; // skip ourselves
		IPluginImport [] imports = candidate.getImports();
		for (int j=0; j<imports.length; j++) {
			IPluginImport ri = imports[j];
			if (ri.getId().equals(referenceId)) {
				PluginInfoNode child = new PluginInfoNode(candidate);
				child.setParent(parent);
				children.addElement(child);
			}
		}
	}
}
private Vector createReferencingNodes(
	IDependencyGraphNode parent,
	String referenceId) {
	Vector children = new Vector();
	ExternalModelManager registry = PDEPlugin.getDefault().getExternalModelManager();
	WorkspaceModelManager manager =
		(WorkspaceModelManager) PDEPlugin.getDefault().getWorkspaceModelManager();
	createReferencingNodes(
		parent,
		manager.getWorkspacePluginModels(),
		referenceId,
		children);
	if (registry.hasEnabledModels())
		createReferencingNodes(parent, registry.getModels(), referenceId, children);
	return children;
}
private Vector createReferencingNodes(IDependencyGraphNode parent, IPlugin info) {
	return createReferencingNodes(parent, info.getId());
}
private Vector createRequiredNodes(
	IDependencyGraphNode parent,
	IPlugin pluginInfo) {
	Vector children = new Vector();
	IPluginImport[] imports = pluginInfo.getImports();
	for (int i = 0; i < imports.length; i++) {
		IPluginImport ri = imports[i];
		IPlugin info = PDEPlugin.getDefault().findPlugin(ri.getId());
		if (info == null) {
			UnresolvedNode child = new UnresolvedNode(ri);
			child.setParent(parent);
			children.addElement(child);
		} else {
			PluginInfoNode child = new PluginInfoNode(info);
			child.setParent(parent);
			children.addElement(child);
		}
	}
	return children;
}
public void dispose() {
	backwardImage.dispose();
	backwardHoverImage.dispose();
	backwardDisabledImage.dispose();
	forwardImage.dispose();
	forwardHoverImage.dispose();
	forwardDisabledImage.dispose();
	homeImage.dispose();
	homeHoverImage.dispose();
	homeDisabledImage.dispose();
	super.dispose();
}
private String getPluginName() {
	return model.getPlugin().getName();
}
public void initialize(Object input) {
	updateButtons(null);
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
private void initializeImages() {
	homeImage = PDEPluginImages.DESC_GRAPH_HOME_TOOL.createImage();
	homeHoverImage = PDEPluginImages.DESC_GRAPH_HOME_TOOL_HOVER.createImage();
	homeDisabledImage = PDEPluginImages.DESC_GRAPH_HOME_TOOL_DISABLED.createImage();

	backwardImage = PDEPluginImages.DESC_GRAPH_BACK_TOOL.createImage();
	backwardHoverImage = PDEPluginImages.DESC_GRAPH_BACK_TOOL_HOVER.createImage();
	backwardDisabledImage =
		PDEPluginImages.DESC_GRAPH_BACK_TOOL_DISABLED.createImage();

	forwardImage = PDEPluginImages.DESC_GRAPH_FORWARD_TOOL.createImage();
	forwardHoverImage = PDEPluginImages.DESC_GRAPH_FORWARD_TOOL_HOVER.createImage();
	forwardDisabledImage =
		PDEPluginImages.DESC_GRAPH_FORWARD_TOOL_DISABLED.createImage();
}
private void openEditor(IDependencyGraphNode node) {
	String id = node.getId();
	ManifestEditor meditor = (ManifestEditor) getFormPage().getEditor();
	meditor.openPluginEditor(id);
}
public void sectionChanged(FormSection source, int changeType, Object changeObject) {
	IPlugin plugin = model.getPlugin();
	updateGraph(plugin.getId(), plugin.getResourceString(plugin.getName()), (PluginListSection)source);
}
private void updateButtons(IDependencyGraphNode currentRoot) {
	backButton.setEnabled(graph.canDoOperation(DependencyGraph.OP_BACK));
	forwardButton.setEnabled(graph.canDoOperation(DependencyGraph.OP_FORWARD));
	homeButton.setEnabled(graph.canDoOperation(DependencyGraph.OP_HOME));
	String backToolTip = null;
	String forwardToolTip = null;

	if (currentRoot != null) {
		if (currentRoot.getParent() != null)
			backToolTip = PDEPlugin.getFormattedMessage(KEY_BACK, currentRoot.getParent().getName());
		if (currentRoot.getLastChild() != null)
			forwardToolTip = PDEPlugin.getFormattedMessage(KEY_FORWARD, currentRoot.getLastChild().getName());
	}
	homeButton.getButton().setToolTipText(PDEPlugin.getFormattedMessage(KEY_HOME, getPluginName()));
	backButton.getButton().setToolTipText(backToolTip);
	forwardButton.getButton().setToolTipText(forwardToolTip);
}
private void updateGraph(
	String id,
	String name,
	PluginListSection pluginSection) {
	Iterator selectedPlugins = pluginSection.getSelectedPlugins();
	String cyclesText = null;

	RootNode rootNode = new RootNode(id, name);

	if (graph.isReverse()) {
		rootNode.setChildren(createReferencingNodes(rootNode, id));
	} else {
		while (selectedPlugins.hasNext()) {
			Object data = selectedPlugins.next();
			if (data instanceof IPlugin) {
				IPlugin info = (IPlugin) data;
				PluginInfoNode child = new PluginInfoNode(info);
				child.setCyclical(checkForCycles(id, info));
				if (child.isCyclical()) {
					if (cyclesText != null)
						cyclesText += ", " + child.getName();
					else
						cyclesText = child.getName();
				}
				rootNode.add(child);
			} else
				if (data instanceof IPluginImport) {
					// unresolved reference
					IPluginImport iimport = (IPluginImport) data;
					UnresolvedNode child = new UnresolvedNode(iimport);
					rootNode.add(child);
				}

		}
	}
	if (cyclesText != null)
		cyclesLabel.setText(PDEPlugin.getFormattedMessage(KEY_CYCLES, cyclesText));
	else
		cyclesLabel.setText("");
	graph.setRoot(rootNode);
}
private void updateLayout() {
	(getFormPage().getForm()).update();
}
}
