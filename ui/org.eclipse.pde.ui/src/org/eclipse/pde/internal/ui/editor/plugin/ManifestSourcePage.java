/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionContext;

public class ManifestSourcePage extends XMLSourcePage {
	
	private Object fLibraries = new Object();
	private Object fImports = new Object();
	private Object fExtensionPoints = new Object();
	private Object fExtensions = new Object();
	private ExtensionAttributePointDectector fDetector;
	private PluginSearchActionGroup fActionGroup;
	
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
				return PDEUIMessages.ManifestSourcePage_libraries; 
			if (obj == fImports)
				return PDEUIMessages.ManifestSourcePage_dependencies; 
			if (obj == fExtensionPoints)
				return PDEUIMessages.ManifestSourcePage_extensionPoints; 
			if (obj == fExtensions)
				return PDEUIMessages.ManifestSourcePage_extensions; 
			String text = fProvider.getText(obj);
			if ((text == null || text.trim().length() == 0) && obj instanceof IDocumentNode)
				text = ((IDocumentNode)obj).getXMLTagName();
			return text;
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
			int flags = ((IDocumentNode)obj).isErrorNode() ? SharedLabelProvider.F_ERROR : 0;
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
		
	class OutlineComparator extends ViewerComparator{
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
		fDetector = new ExtensionAttributePointDectector();
		fActionGroup = new PluginSearchActionGroup();
	}

	public ILabelProvider createOutlineLabelProvider() {
		return new OutlineLabelProvider();
	}
	public ITreeContentProvider createOutlineContentProvider() {
		return new ContentProvider();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(java.lang.Object)
	 */
	public void updateSelection(Object object) {
		if ((object instanceof IDocumentNode) && 
				!((IDocumentNode)object).isErrorNode()) {
			fSelection = object;
			setHighlightRange((IDocumentNode)object, true);
			setSelectedRange((IDocumentNode)object, false);
		} else {
			//resetHighlightRange();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineSorter()
	 */
	public ViewerComparator createOutlineComparator() {
		return new OutlineComparator();
	}
	
	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		IPluginBase base = ((IPluginModelBase)getInputContext().getModel()).getPluginBase();
		IDocumentRange 
			node = findNode(base.getLibraries(), offset, searchChildren);
		if (node == null)
			node = findNode(base.getImports(), offset, searchChildren);
		if (node == null)
			node = findNode(base.getExtensionPoints(), offset, searchChildren);
		if (node == null)
			node = findNode(base.getExtensions(), offset, searchChildren);
		if (node == null)
			node = findNode(new IPluginObject[] { base }, offset, searchChildren);
		
		return node;
	}

	private IDocumentRange findNode(Object[] nodes, int offset, boolean searchChildren) {
		for (int i = 0; i < nodes.length; i++) {
			IDocumentNode node = (IDocumentNode)nodes[i];
			if (node.getOffset() <= offset 
					&& offset < node.getOffset() + node.getLength()) {
				
				if (!searchChildren)
					return node;
				
				if (node.getOffset() < offset && 
						offset <= node.getOffset() + node.getXMLTagName().length() + 1)
					return node;
				
				IDocumentAttribute[] attrs = node.getNodeAttributes();
				if (attrs != null)
					for (int a = 0; a < attrs.length; a++)
						if (attrs[a].getNameOffset() <= offset &&
								offset <= attrs[a].getValueOffset() + attrs[a].getValueLength())
							return (IDocumentNode)attrs[a];
				
				IDocumentTextNode textNode = node.getTextNode();
				if (textNode != null && 
						textNode.getOffset() <= offset &&
						offset < textNode.getOffset() + textNode.getLength())
					return textNode;
				
				IDocumentNode[] children = node.getChildNodes();
				if (children != null)
					for (int c = 0; c < children.length; c++)
						if (children[c].getOffset() <= offset &&
								offset < children[c].getOffset() + children[c].getLength())
							return findNode(new Object[] {children[c]}, offset, searchChildren);
				
				// not contained inside any sub elements, must be inside node
				return node;
			}
		}
		return null;
	}
	
	public IDocumentRange findRange() {
		if (fSelection instanceof ImportObject)
			fSelection = ((ImportObject)fSelection).getImport();
		if (fSelection instanceof IDocumentNode)
			return (IDocumentNode)fSelection;
		return null;
	}
	
	protected boolean isSelectionListener() {
		return true;
	}
	
	public Object getAdapter(Class adapter) {
		if (IHyperlinkDetector.class.equals(adapter))
			return new ManifestHyperlinkDetector(this);
		return super.getAdapter(adapter);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		
		ISelection selection = fDetector.getSelection();
		if (selection != null) {
			fActionGroup.setContext(new ActionContext(selection));
			fActionGroup.fillContextMenu(menu);
		}
		super.editorContextMenuAboutToShow(menu);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		// At this point the source page is fully initialized including the 
		// underlying text viewer
		fDetector.setTextEditor(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#isQuickOutlineEnabled()
	 */
	public boolean isQuickOutlineEnabled() {
		return true;
	}
	
}
