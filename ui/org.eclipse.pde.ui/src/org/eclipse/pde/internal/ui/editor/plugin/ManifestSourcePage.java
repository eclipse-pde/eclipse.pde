/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 214511
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.PDEActionConstants;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.refactoring.PDERefactoringAction;
import org.eclipse.pde.internal.ui.refactoring.RefactoringActionFactory;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionContext;

public class ManifestSourcePage extends XMLSourcePage {

	private Object fLibraries = new Object();
	private Object fImports = new Object();
	private Object fExtensionPoints = new Object();
	private Object fExtensions = new Object();
	private ExtensionAttributePointDectector fDetector;
	private PluginSearchActionGroup fActionGroup;
	private PDERefactoringAction fRenameAction;

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
			if ((text == null || text.trim().length() == 0) && obj instanceof IDocumentElementNode)
				text = ((IDocumentElementNode) obj).getXMLTagName();
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
			int flags = ((IDocumentElementNode) obj).isErrorNode() ? SharedLabelProvider.F_ERROR : 0;
			return (flags == 0) ? image : fProvider.get(image, flags);
		}
	}

	class ContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parent) {
			PluginModelBase model = (PluginModelBase) getInputContext().getModel();

			ArrayList result = new ArrayList();
			if (parent instanceof IPluginBase) {
				IPluginBase pluginBase = (IPluginBase) parent;
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
			if (element instanceof IDocumentElementNode)
				return ((IDocumentElementNode) element).getParentNode();
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof IPluginBase) {
				return ((IDocumentElementNode) element).getChildNodes().length > 0;
			}
			return element == fLibraries || element == fImports || element == fExtensionPoints || element == fExtensions;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IPluginModelBase) {
				return new Object[] {((IPluginModelBase) inputElement).getPluginBase()};
			}
			return new Object[0];
		}
	}

	class OutlineComparator extends ViewerComparator {
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
		if ((object instanceof IDocumentElementNode) && !((IDocumentElementNode) object).isErrorNode()) {
			setSelectedObject(object);
			setHighlightRange((IDocumentElementNode) object, true);
			setSelectedRange((IDocumentElementNode) object, false);
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
		IPluginBase base = ((IPluginModelBase) getInputContext().getModel()).getPluginBase(false);
		if (base == null)
			return null;

		IDocumentRange node = findNode(base.getLibraries(), offset, searchChildren);
		if (node == null)
			node = findNode(base.getImports(), offset, searchChildren);
		if (node == null)
			node = findNode(base.getExtensionPoints(), offset, searchChildren);
		if (node == null)
			node = findNode(base.getExtensions(), offset, searchChildren);
		if (node == null)
			node = findNode(new IPluginObject[] {base}, offset, searchChildren);

		return node;
	}

	public IDocumentRange findRange() {

		Object selectedObject = getSelection();

		if (selectedObject instanceof ImportObject) {
			selectedObject = ((ImportObject) selectedObject).getImport();
			setSelectedObject(selectedObject);
		}

		if (selectedObject instanceof IDocumentElementNode)
			return (IDocumentElementNode) selectedObject;

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

		StyledText text = getViewer().getTextWidget();
		Point p = text.getSelection();
		IDocumentRange element = getRangeElement(p.x, false);

		if (!(element instanceof IPluginExtensionPoint))
			return;

		if (isEditable()) {
			if (fRenameAction == null)
				fRenameAction = RefactoringActionFactory.createRefactorExtPointAction(PDEUIMessages.ManifestSourcePage_renameActionText);
			if (fRenameAction != null) {
				fRenameAction.setSelection(element);
				// add rename action after Outline. This is the same order as the hyperlink actions
				menu.insertAfter(PDEActionConstants.COMMAND_ID_QUICK_OUTLINE, fRenameAction);
			}
		}
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#setActive(boolean)
	 */
	public void setActive(boolean active) {
		super.setActive(active);
		// Update the text selection if this page is being activated
		if (active) {
			updateTextSelection();
		}
	}

	protected IFoldingStructureProvider getFoldingStructureProvider(IEditingModel model) {
		return new PluginFoldingStructureProvider(this, model);
	}
}
