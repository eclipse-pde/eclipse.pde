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

package org.eclipse.pde.internal.core.plugin;

import java.io.*;
import java.util.Map;

import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.w3c.dom.Node;

/**
 * PluginXMLDocumentModel.java
 */
public class DocumentModel implements IDocumentNode {

	private XMLDocumentModelBuilder fParser;
	private XEErrorHandler fErrorHandler;
	private TicketManager fTicketManager = new TicketManager();
	private IDocumentNode fRootNode;
	private IDocumentNode[] fCachedChildren;
	private AbstractPluginModelBase fPluginModelBase;
	private Map fContentLineTable;

	public DocumentModel(AbstractPluginModelBase pluginModelBase) {
		super();
		fPluginModelBase = pluginModelBase;
		fParser =
			XMLCore.getDefault().createXMLModelBuilder(
				new PluginDocumentModelFactory());
		fErrorHandler = new XEErrorHandler(null);
		fParser.setErrorHandler(fErrorHandler);
	}

	public void setRootNode(IDocumentNode root) {
		fRootNode = root;
		fCachedChildren = null;
		if (fRootNode != null)
			fRootNode.setParent(this);
	}

	public IDocumentNode getRootNode() {
		return fRootNode;
	}

	public IDocumentNode[] getChildren() {
		if (fCachedChildren == null && fRootNode != null) {
			fCachedChildren = new IDocumentNode[1];
			fCachedChildren[0] = fRootNode;
		}
		return fCachedChildren;
	}

	public IDocumentNode getParent() {
		return null;
	}

	public void setParent(IDocumentNode parentNode) {
		throw new UnsupportedOperationException();
	}

	public ISourceRange getSourceRange() {
		throw new UnsupportedOperationException();
	}

	public ISourceRange getSourceRange(IDocumentNode node) {
		ISourceRange result = null;
		if (fContentLineTable != null) {
			result = (ISourceRange) fContentLineTable.get(node);
		}
		return result;
	}

	public TicketManager getTicketManager() {
		return fTicketManager;
	}

	public void load(InputStream stream, boolean outOfSync)
		throws CoreException {
		synchronized (fPluginModelBase) {
			getTicketManager().buyTicket();
			load(stream, outOfSync, true);
		}
	}

	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		synchronized (fPluginModelBase) {
			load(stream, outOfSync, false);
		}
	}

	private void load(
		InputStream stream,
		boolean outOfSync,
		boolean initialRun)
		throws CoreException {
		synchronized (fPluginModelBase) {
			AbstractModelUpdateStrategy updateStrategy =
				reconcile(stream, outOfSync, initialRun);
			if (updateStrategy != null) {
				updateStrategy.run();
				if (!updateStrategy.isSuccessful()) {
					fPluginModelBase.throwParseErrorsException();
				}
			}
		}
	}

	public AbstractModelUpdateStrategy reconcile(
		InputStream stream,
		final boolean outOfSync,
		final boolean initialRun) { //called from main and reconciler thread
		synchronized (fPluginModelBase) {
			if (!getTicketManager().isTicketValid()) {
				getTicketManager().removeTicket();
				return null;
			}

			AbstractModelUpdateStrategy result;
			boolean success;
			try {
				XMLInputSource source =
					new XMLInputSource(null, null, null, stream, null);
				fErrorHandler.reset();
				fParser.parse(source);
				success =
					fErrorHandler.getErrorCount() == 0
						&& fErrorHandler.getFatalErrorCount() == 0;
			} catch (XNIException e) {
				success = false;
			} catch (IOException e) {
				success = false;
			}

			final PluginBase tmpPluginBase;
			tmpPluginBase = (PluginBase) fPluginModelBase.createPluginBase();
			tmpPluginBase.setModel(fPluginModelBase);
			final Node documentElement =
				fParser.getDocument().getDocumentElement();
			if (documentElement != null) {
				tmpPluginBase.load(
					documentElement,
					fParser.getSchemaVersion(),
					fParser.getLineTable());
			}
			DocumentModel.load(tmpPluginBase, fParser.getModelRoot());

			result = new AbstractModelUpdateStrategy(this, success) {
				public void update() {
					PluginBase pluginBase = fPluginModelBase.pluginBase;
					if (pluginBase == null) {
						pluginBase =
							(PluginBase) fPluginModelBase.createPluginBase();
						pluginBase.setModel(fPluginModelBase);
						fPluginModelBase.pluginBase = pluginBase;
					}
					pluginBase.reset();
					if (isSuccessful()) {
						pluginBase.load(tmpPluginBase);
					}
					if (!outOfSync) {
						fPluginModelBase.updateTimeStamp();
					}
					if (!initialRun) {
						fPluginModelBase.fireModelChanged(
							new ModelChangedEvent(
								IModelChangedEvent.WORLD_CHANGED,
								new Object[] { pluginBase },
								null));
						if (fPluginModelBase instanceof IEditable
							&& fPluginModelBase.isEditable())
							 ((IEditable) fPluginModelBase).setDirty(false);
					}
					fPluginModelBase.setLoaded(isSuccessful());

					IDocumentNode modelRoot = fParser.getModelRoot();
					if (modelRoot != null) {
						IDocumentNode[] children = modelRoot.getChildren();
						modelRoot = null;
						if (children != null) {
							for (int i = 0; i < children.length; i++) {
								IDocumentNode node = children[i];
								if (node instanceof PluginDocumentNode
									&& ((PluginDocumentNode) node).getDOMNode()
										== documentElement) {
									modelRoot = node;
									break;
								}
							}
						}
					}
					setRootNode(modelRoot);
					fContentLineTable = fParser.getLineTable();
					XMLCore.getDefault().notifyDocumentModelListeners(
						new DocumentModelChangeEvent(DocumentModel.this));
				}
			};

			return result;
		}
	}

	static void load(PluginBase tmpPluginBase, IDocumentNode docNode) {
		IDocumentNode docRootNode = null;
		if (docNode != null && docNode.getChildren() != null) {
			IDocumentNode[] children = docNode.getChildren();
			for (int i = 0; i < children.length; i++) {
				String name =
					children[i] instanceof PluginDocumentNode
						? ((PluginDocumentNode) children[i])
							.getDOMNode()
							.getNodeName()
							.toLowerCase()
						: null;
				if ("plugin".equals(name) || "fragment".equals(name)) {
					docRootNode = children[i];
					break;
				}
			}
		}

		if (docRootNode instanceof PluginDocumentNode) {
			PluginDocumentNode pluginDocNode = (PluginDocumentNode) docRootNode;
			pluginDocNode.setPluginObjectNode(tmpPluginBase);

			if (pluginDocNode.getChildren() != null) {
				int iExtension = 0;
				int iExtensionPoint = 0;
				int iImport = 0;
				int iLibrary = 0;
				IDocumentNode[] children = pluginDocNode.getChildren();
				for (int i = 0; i < children.length; i++) {
					if (children[i] instanceof PluginDocumentNode) {
						PluginDocumentNode currentDocNode =
							(PluginDocumentNode) children[i];
						String name =
							currentDocNode
								.getDOMNode()
								.getNodeName()
								.toLowerCase();
						if (name.equals("extension")) {
							currentDocNode.setPluginObjectNode(
								tmpPluginBase.getExtensions()[iExtension++]);
						} else if (name.equals("extension-point")) {
							currentDocNode.setPluginObjectNode(
								tmpPluginBase
									.getExtensionPoints()[iExtensionPoint++]);
						} else if (name.equals("runtime")) {
							iLibrary =
								processRuntime(
									tmpPluginBase,
									currentDocNode,
									iLibrary);
						} else if (name.equals("requires")) {
							iImport =
								processRequires(
									tmpPluginBase,
									currentDocNode,
									iImport);
						}
					}
				}
			}
		}
	}

	static int processRuntime(
		PluginBase pluginBase,
		PluginDocumentNode docNode,
		int iLibrary) {
		if (docNode.getChildren() != null) {
			IDocumentNode[] children = docNode.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof PluginDocumentNode) {
					PluginDocumentNode currentDocNode =
						(PluginDocumentNode) children[i];
					String name =
						currentDocNode.getDOMNode().getNodeName().toLowerCase();
					if (name.equals("library")) {
						currentDocNode.setPluginObjectNode(
							pluginBase.getLibraries()[iLibrary++]);
					}
				}
			}
		}
		return iLibrary;
	}

	static int processRequires(
		PluginBase pluginBase,
		PluginDocumentNode docNode,
		int iImport) {
		if (docNode.getChildren() != null) {
			IDocumentNode[] children = docNode.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof PluginDocumentNode) {
					PluginDocumentNode currentDocNode =
						(PluginDocumentNode) children[i];
					String name =
						currentDocNode.getDOMNode().getNodeName().toLowerCase();
					if (name.equals("import")) {
						currentDocNode.setPluginObjectNode(
							pluginBase.getImports()[iImport++]);
					}
				}
			}
		}
		return iImport;
	}

	public IPluginModelBase getModel() {
		return fPluginModelBase;
	}
}
