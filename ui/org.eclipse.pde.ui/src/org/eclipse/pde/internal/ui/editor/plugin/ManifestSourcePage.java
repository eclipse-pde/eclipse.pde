/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.model.plugin.*;
import org.eclipse.swt.graphics.*;

public class ManifestSourcePage extends XMLSourcePage {
	
	private Object fLibraries = new Object();
	private Object fImports = new Object();
	private Object fExtensionPoints = new Object();
	private Object fExtensions = new Object();
	
	class OutlineLabelProvider extends LabelProvider {		
		private PDELabelProvider fProvider;
		
		public OutlineLabelProvider() {
			fProvider = PDEPlugin.getDefault().getLabelProvider();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
		 */
		
		public String getText(Object obj) {
			if (obj == fLibraries)
				return PDEPlugin.getResourceString("ManifestSourcePage.libraries"); //$NON-NLS-1$
			if (obj == fImports)
				return PDEPlugin.getResourceString("ManifestSourcePage.dependencies"); //$NON-NLS-1$
			if (obj == fExtensionPoints)
				return PDEPlugin.getResourceString("ManifestSourcePage.extensionPoints"); //$NON-NLS-1$
			if (obj == fExtensions)
				return PDEPlugin.getResourceString("ManifestSourcePage.extensions"); //$NON-NLS-1$
			return fProvider.getText(obj);
		}

		public Image getImage(Object obj) {
			if (obj == fLibraries)
				return fProvider.get(PDEPluginImages.DESC_RUNTIME_OBJ);
			if (obj == fImports)
				return fProvider.get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
			if (obj == fExtensionPoints)
				return fProvider.get(PDEPluginImages.DESC_EXT_POINTS_OBJ);
			if (obj == fExtensions)
				return fProvider.get(PDEPluginImages.DESC_EXTENSIONS_OBJ);
			
			Image image = fProvider.getImage(obj);
			int flags = ((IDocumentNode)obj).isErrorNode() ? PDELabelProvider.F_ERROR : 0;
			return (flags == 0) ? image : fProvider.get(image, flags);
		}
	}
	
	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parent) {
			PluginModelBase model = (PluginModelBase)getInputContext().getModel();

			ArrayList result = new ArrayList();			
			if (parent instanceof IPluginBase) {
				IPluginBase pluginBase = (IPluginBase)parent;
				if (pluginBase.getLibraries().length > 0)
					result.add(fLibraries);
				if (pluginBase.getImports().length > 0)
					result.add(fImports);
				if (pluginBase.getExtensionPoints().length > 0)
					result.add(fExtensionPoints);
				if (pluginBase.getExtensions().length > 0)
					result.add(fExtensions);
				return result.toArray();
			} 
			if (parent == fLibraries)
				return model.getPluginBase().getLibraries();
			
			if (parent == fImports)
				return model.getPluginBase().getImports();
			
			if (parent == fExtensionPoints)
				return model.getPluginBase().getExtensionPoints();
			
			if (parent == fExtensions)
				return model.getPluginBase().getExtensions();
			
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IDocumentNode)
				return ((IDocumentNode)element).getParentNode();
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof IPluginBase) {
				return ((IDocumentNode) element).getChildNodes().length > 0;
			}
			return element == fLibraries || element == fImports
			|| element == fExtensionPoints || element == fExtensions;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IPluginModelBase) {
				return new Object[] {((IPluginModelBase)inputElement).getPluginBase()};
			}
			return new Object[0];
		}
	}
		
	class OutlineSorter extends ViewerSorter{
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerSorter#category(java.lang.Object)
		 */
		public int category(Object element) {
			if (element == fLibraries)
				return 0;
			if (element == fImports)
				return 1;
			if (element == fExtensionPoints)
				return 2;
			if (element == fExtensions)
				return 3;
			return 4;
		}
	}
	
	public ManifestSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		
	}

	protected ILabelProvider createOutlineLabelProvider() {
		return new OutlineLabelProvider();
	}
	protected ITreeContentProvider createOutlineContentProvider() {
		return new ContentProvider();
	}

	protected void outlineSelectionChanged(SelectionChangedEvent event) {
		ISelection selection= event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			Object first= structuredSelection.getFirstElement();
			if (first instanceof IDocumentNode && !(first instanceof IPluginBase)) {
				setHighlightRange((IDocumentNode)first, true);
				setSelectedRange((IDocumentNode)first);				
			} else {
				resetHighlightRange();
			}
		}
	}
	
	public void setSelectedRange(IDocumentNode node) {
		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return;

		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		int offset = node.getOffset();
		sourceViewer.setSelectedRange(offset + 1, node.getXMLTagName().length());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineSorter()
	 */
	protected ViewerSorter createOutlineSorter() {
		return new OutlineSorter();
	}
	
	protected IDocumentRange getRangeElement(ITextSelection selection) {
		if (selection.isEmpty())
			return null;
		IPluginModelBase model = (IPluginModelBase) getInputContext().getModel();
		int offset = selection.getOffset();
		IDocumentRange node = findNode(model.getPluginBase().getLibraries(),
				offset);
		if (node == null)
			node = findNode(model.getPluginBase().getImports(), offset);
		if (node == null)
			node = findNode(model.getPluginBase().getExtensionPoints(), offset);
		if (node == null)
			node = findNode(model.getPluginBase().getExtensions(), offset);
		if (node == null) {
			node = findNode(new IPluginObject[] { model.getPluginBase() }, offset);
		}
		return node;
	}

	private IDocumentNode findNode(IPluginObject[] nodes, int offset) {
		for (int i = 0; i < nodes.length; i++) {
			IDocumentNode node = (IDocumentNode) nodes[i];
			if (offset >= node.getOffset()
					&& offset < node.getOffset() + node.getLength()) {
				return node;
			}
		}
		return null;
	}
}
