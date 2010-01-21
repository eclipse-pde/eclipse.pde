/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.compare;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;

public class PluginStructureCreator extends StructureCreator {

	public static final int ROOT = 0;
	public static final int LIBRARY = 1;
	public static final int IMPORT = 2;
	public static final int EXTENSION_POINT = 3;
	public static final int EXTENSION = 4;

	static class PluginNode extends DocumentRangeNode implements ITypedElement {

		private final Image image;

		public PluginNode(DocumentRangeNode parent, int type, String id, Image image, IDocument doc, int start, int length) {
			super(parent, type, id, doc, start, length);
			this.image = image;
			if (parent != null) {
				parent.addChild(PluginNode.this);
			}
		}

		public String getName() {
			return this.getId();
		}

		public String getType() {
			return "PLUGIN2"; //$NON-NLS-1$
		}

		public Image getImage() {
			return image;
		}
	}

	public PluginStructureCreator() {
		// Nothing to do
	}

	protected IStructureComparator createStructureComparator(Object input, IDocument document, ISharedDocumentAdapter adapter, IProgressMonitor monitor) throws CoreException {
		final boolean isEditable;
		if (input instanceof IEditableContent)
			isEditable = ((IEditableContent) input).isEditable();
		else
			isEditable = false;

		// Create a label provider to provide the text of the elements
		final PDELabelProvider labelProvider = new PDELabelProvider();
		// Create a resource manager to manage the images.
		// We can't use the label provider because an image could be disposed that is still in use.
		// By using a resource manager, we ensure that the image is not disposed until no resource 
		// managers reference it.
		final ResourceManager resources = new LocalResourceManager(JFaceResources.getResources());
		DocumentRangeNode rootNode = new StructureRootNode(document, input, this, adapter) {
			public boolean isEditable() {
				return isEditable;
			}

			public void dispose() {
				// Dispose the label provider and the local resource manager
				labelProvider.dispose();
				resources.dispose();
				super.dispose();
			}
		};
		try {
			parsePlugin(input, rootNode, document, labelProvider, resources, monitor);
		} catch (CoreException ex) {
			if (adapter != null)
				adapter.disconnect(input);
			throw ex;
		}

		return rootNode;
	}

	public String getContents(Object node, boolean ignoreWhitespace) {
		if (node instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca = (IStreamContentAccessor) node;
			try {
				return ManifestStructureCreator.readString(sca);
			} catch (CoreException ex) {
			}
		}
		return null;
	}

	public String getName() {
		return PDEUIMessages.PluginStructureCreator_name;
	}

	private void parsePlugin(Object input, DocumentRangeNode rootNode, IDocument document, PDELabelProvider labelProvider, ResourceManager resources, IProgressMonitor monitor) throws CoreException {
		boolean isFragment = isFragment(input);
		PluginModelBase model = createModel(input, document, isFragment);
		if (!model.isLoaded() && model.getStatus().getSeverity() == IStatus.ERROR)
			throw new CoreException(model.getStatus());

		try {
			String id = isFragment ? "fragment" : "plugin"; //$NON-NLS-1$ //$NON-NLS-2$
			ImageDescriptor icon = isFragment ? PDEPluginImages.DESC_FRAGMENT_MF_OBJ : PDEPluginImages.DESC_PLUGIN_MF_OBJ;
			PluginNode parent = new PluginNode(rootNode, ROOT, id, resources.createImage(icon), document, 0, document.getLength());
			createChildren(parent, model, labelProvider, resources);
		} finally {
			model.dispose();
		}
	}

	private boolean isFragment(Object input) {
		if (input instanceof ITypedElement && ((ITypedElement) input).getName().equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR))
			return true;
		return false;
	}

	private PluginModelBase createModel(Object input, IDocument document, boolean isFragment) throws CoreException {
		PluginModelBase model = null;
		if (isFragment) {
			model = new FragmentModel(document, false /* isReconciling */);
		} else {
			model = new PluginModel(document, false /* isReconciling */);
		}
		model.setCharset(getCharset(input));
		model.load();
		return model;
	}

	private String getCharset(Object input) throws CoreException {
		if (input instanceof IEncodedStreamContentAccessor)
			return ((IEncodedStreamContentAccessor) input).getCharset();
		return ResourcesPlugin.getEncoding();
	}

	private void createChildren(DocumentRangeNode rootNode, PluginModelBase model, PDELabelProvider labelProvider, ResourceManager resources) {
		createLibraries(rootNode, model, labelProvider, resources);
		createImports(rootNode, model, labelProvider, labelProvider, resources);
		createExtensionPoints(rootNode, model, labelProvider, resources);
		createExtensions(rootNode, model, labelProvider, resources);
	}

	private void createLibraries(DocumentRangeNode parent, PluginModelBase model, PDELabelProvider labelProvider, ResourceManager resources) {
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		int type = LIBRARY;
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary pluginLibrary = libraries[i];
			createNode(parent, type, pluginLibrary, labelProvider, resources);
		}
	}

	private void createImports(DocumentRangeNode parent, PluginModelBase model, PDELabelProvider labelProvider, PDELabelProvider labelProvider2, ResourceManager resources) {
		IPluginImport[] imports = model.getPluginBase().getImports();
		int type = IMPORT;
		for (int i = 0; i < imports.length; i++) {
			IPluginImport pluginImport = imports[i];
			createNode(parent, type, pluginImport, labelProvider, resources);
		}
	}

	private void createExtensionPoints(DocumentRangeNode parent, PluginModelBase model, PDELabelProvider labelProvider, ResourceManager resources) {
		IPluginExtensionPoint[] extensionPoints = model.getPluginBase().getExtensionPoints();
		int type = EXTENSION_POINT;
		for (int i = 0; i < extensionPoints.length; i++) {
			IPluginExtensionPoint extensionPoint = extensionPoints[i];
			createNode(parent, type, extensionPoint, labelProvider, resources);
		}
	}

	private void createExtensions(DocumentRangeNode parent, PluginModelBase model, PDELabelProvider labelProvider, ResourceManager resources) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		int type = EXTENSION;
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension extension = extensions[i];
			createNode(parent, type, extension, labelProvider, resources);
		}
	}

	private void createNode(DocumentRangeNode parent, int type, Object element, PDELabelProvider labelProvider, ResourceManager resources) {
		if (element instanceof IDocumentElementNode) {
			IDocumentElementNode node = (IDocumentElementNode) element;
			ImageDescriptor imageDescriptor = getImageDescriptor(element);
			Image image = null;
			if (imageDescriptor != null) {
				image = resources.createImage(imageDescriptor);
			}
			new PluginNode(parent, type, labelProvider.getText(element), image, parent.getDocument(), node.getOffset(), node.getLength());
		}
	}

	private ImageDescriptor getImageDescriptor(Object element) {
		if (element instanceof IPluginImport) {
			return PDEPluginImages.DESC_REQ_PLUGIN_OBJ;
		}
		if (element instanceof IPluginLibrary) {
			return PDEPluginImages.DESC_JAVA_LIB_OBJ;
		}
		if (element instanceof IPluginExtension) {
			return PDEPluginImages.DESC_EXTENSION_OBJ;
		}
		if (element instanceof IPluginExtensionPoint) {
			return PDEPluginImages.DESC_EXT_POINT_OBJ;
		}
		return null;
	}
}
