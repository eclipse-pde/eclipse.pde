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

import javax.xml.parsers.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.w3c.dom.Node;
import org.xml.sax.*;

/**
 * PluginXMLDocumentModel.java
 */
public class DocumentModel implements IDocumentNode {

	private SAXParser fParser;
	private DocumentModelHandler fHandler;
	private TicketManager fTicketManager = new TicketManager();
	private IDocumentNode fRootNode;
	private AbstractPluginModelBase fPluginModelBase;
	private Map fContentLineTable;

	public DocumentModel(AbstractPluginModelBase pluginModelBase) {
		super();
		fPluginModelBase = pluginModelBase;
	}

	public void setRootNode(IDocumentNode root) {
		fRootNode = root;
		if (fRootNode != null)
			fRootNode.setParent(this);
	}

	public IDocumentNode getRootNode() {
		return fRootNode;
	}

	public IDocumentNode[] getChildren() {
		return new IDocumentNode[] { fRootNode };
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
					fPluginModelBase.throwParseErrorsException(null);
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
			boolean success = true;
			try {
				if (fParser == null)
					fParser = SAXParserFactory.newInstance().newSAXParser();
				if (fHandler == null)
					fHandler = new DocumentModelHandler(stream);
				else
					fHandler.reset(stream);
				fParser.setProperty("http://xml.org/sax/properties/lexical-handler", fHandler);
				fParser.parse(new InputSource(new StringReader(fHandler.getText())), fHandler);
			} catch (Exception e) {
				success = false;
			}
			

			final PluginBase tmpPluginBase;
			tmpPluginBase = (PluginBase) fPluginModelBase.createPluginBase();
			tmpPluginBase.setModel(fPluginModelBase);
			final Node documentElement =
				fHandler.getDocumentElement();
			if (documentElement != null) {
				tmpPluginBase.load(
					documentElement,
					fHandler.getSchemaVersion(),
					fHandler.getLineTable());
			}
			DocumentModel.load(tmpPluginBase, fHandler.getModelRoot());

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

					setRootNode(fHandler.getModelRoot());
					fContentLineTable = fHandler.getLineTable();
					XMLCore.getDefault().notifyDocumentModelListeners(
						new DocumentModelChangeEvent(DocumentModel.this));
				}
			};

			return result;
		}
	}

	static void load(PluginBase tmpPluginBase, IDocumentNode docNode) {
		IDocumentNode docRootNode = docNode;
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
							currentDocNode.getDOMNode().getNodeName().toLowerCase();
						if (name.equals("extension")) {
							currentDocNode.setPluginObjectNode(
								tmpPluginBase.getExtensions()[iExtension++]);
						} else if (name.equals("extension-point")) {
							currentDocNode.setPluginObjectNode(
								tmpPluginBase.getExtensionPoints()[iExtensionPoint++]);
						} else if (name.equals("runtime")) {
							iLibrary =
								processRuntime(tmpPluginBase, currentDocNode, iLibrary);
						} else if (name.equals("requires")) {
							iImport =
								processRequires(tmpPluginBase, currentDocNode, iImport);
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
					PluginDocumentNode currentDocNode = (PluginDocumentNode) children[i];
					String name = currentDocNode.getDOMNode().getNodeName().toLowerCase();
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
					PluginDocumentNode currentDocNode = (PluginDocumentNode) children[i];
					String name = currentDocNode.getDOMNode().getNodeName().toLowerCase();
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
