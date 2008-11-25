/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 214511
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.util.ResourceBundle;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.actions.*;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.outline.IOutlineContentCreator;
import org.eclipse.pde.internal.ui.editor.outline.IOutlineSelectionHandler;
import org.eclipse.pde.internal.ui.editor.plugin.ExtensionHyperLink;
import org.eclipse.pde.internal.ui.editor.text.PDESelectAnnotationRulerAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.*;

public abstract class PDESourcePage extends TextEditor implements IFormPage, IGotoMarker, ISelectionChangedListener, IOutlineContentCreator, IOutlineSelectionHandler {

	private static String RES_BUNDLE_LOCATION = "org.eclipse.pde.internal.ui.editor.text.ConstructedPDEEditorMessages"; //$NON-NLS-1$
	private static ResourceBundle fgBundleForConstructedKeys = ResourceBundle.getBundle(RES_BUNDLE_LOCATION);

	public static ResourceBundle getBundleForConstructedKeys() {
		return fgBundleForConstructedKeys;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#initializeKeyBindingScopes()
	 */
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] {"org.eclipse.pde.ui.pdeEditorContext"}); //$NON-NLS-1$
	}

	/**
	 * Updates the OutlinePage selection and this editor's range indicator.
	 * 
	 * @since 3.0
	 */
	private class PDESourcePageChangedListener implements ISelectionChangedListener {

		/**
		 * Installs this selection changed listener with the given selection
		 * provider. If the selection provider is a post selection provider,
		 * post selection changed events are the preferred choice, otherwise
		 * normal selection changed events are requested.
		 * 
		 * @param selectionProvider
		 */
		public void install(ISelectionProvider selectionProvider) {
			if (selectionProvider != null) {
				if (selectionProvider instanceof IPostSelectionProvider) {
					IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
					provider.addPostSelectionChangedListener(this);
				} else {
					selectionProvider.addSelectionChangedListener(this);
				}
			}
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			handleSelectionChangedSourcePage(event);
		}

		/**
		 * Removes this selection changed listener from the given selection
		 * provider.
		 * 
		 * @param selectionProviderstyle
		 */
		public void uninstall(ISelectionProvider selectionProvider) {
			if (selectionProvider != null) {
				if (selectionProvider instanceof IPostSelectionProvider) {
					IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
					provider.removePostSelectionChangedListener(this);
				} else {
					selectionProvider.removeSelectionChangedListener(this);
				}
			}
		}

	}

	/**
	 * The editor selection changed listener.
	 * 
	 * @since 3.0
	 */
	private PDESourcePageChangedListener fEditorSelectionChangedListener;
	private PDEFormEditor fEditor;
	private Control fControl;
	private int fIndex;
	private String fId;
	private InputContext fInputContext;
	private ISortableContentOutlinePage fOutlinePage;
	private ISelectionChangedListener fOutlineSelectionChangedListener;
	private Object fSelection;

	public PDESourcePage(PDEFormEditor editor, String id, String title) {
		fId = id;
		initialize(editor);
		IPreferenceStore[] stores = new IPreferenceStore[2];
		stores[0] = PDEPlugin.getDefault().getPreferenceStore();
		stores[1] = EditorsUI.getPreferenceStore();
		setPreferenceStore(new ChainedPreferenceStore(stores));
		setRangeIndicator(new DefaultRangeIndicator());
		if (isSelectionListener())
			getEditor().getSite().getSelectionProvider().addSelectionChangedListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#initialize(org.eclipse.ui.forms.editor.FormEditor)
	 */
	public void initialize(FormEditor editor) {
		fEditor = (PDEFormEditor) editor;
	}

	public void dispose() {
		if (fEditorSelectionChangedListener != null) {
			fEditorSelectionChangedListener.uninstall(getSelectionProvider());
			fEditorSelectionChangedListener = null;
		}
		if (fOutlinePage != null) {
			fOutlinePage.dispose();
			fOutlinePage = null;
		}
		if (isSelectionListener())
			getEditor().getSite().getSelectionProvider().removeSelectionChangedListener(this);

		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.outline.IOutlineContentCreator#createOutlineLabelProvider()
	 */
	public abstract ILabelProvider createOutlineLabelProvider();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.outline.IOutlineContentCreator#createOutlineContentProvider()
	 */
	public abstract ITreeContentProvider createOutlineContentProvider();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.outline.IOutlineContentCreator#createOutlineComparator()
	 */
	public abstract ViewerComparator createOutlineComparator();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.outline.IOutlineSelectionHandler#updateSelection(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void updateSelection(SelectionChangedEvent event) {
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) sel;
			updateSelection(structuredSelection.getFirstElement());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.outline.IOutlineSelectionHandler#updateSelection(java.lang.Object)
	 */
	public abstract void updateSelection(Object object);

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.outline.IOutlineContentCreator#createDefaultOutlineComparator()
	 */
	public ViewerComparator createDefaultOutlineComparator() {
		return null;
	}

	protected ISortableContentOutlinePage createOutlinePage() {
		SourceOutlinePage sourceOutlinePage = new SourceOutlinePage(fEditor, (IEditingModel) getInputContext().getModel(), createOutlineLabelProvider(), createOutlineContentProvider(), createDefaultOutlineComparator(), createOutlineComparator());
		fOutlinePage = sourceOutlinePage;
		fOutlineSelectionChangedListener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection(event);
			}
		};
		fOutlinePage.addSelectionChangedListener(fOutlineSelectionChangedListener);
		getSelectionProvider().addSelectionChangedListener(sourceOutlinePage);
		fEditorSelectionChangedListener = new PDESourcePageChangedListener();
		fEditorSelectionChangedListener.install(getSelectionProvider());
		return fOutlinePage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.outline.IOutlineSelectionHandler#getContentOutline()
	 */
	public ISortableContentOutlinePage getContentOutline() {
		if (fOutlinePage == null)
			fOutlinePage = createOutlinePage();
		return fOutlinePage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getEditor()
	 */
	public FormEditor getEditor() {
		return fEditor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getManagedForm()
	 */
	public IManagedForm getManagedForm() {
		// not a form page
		return null;
	}

	protected void firePropertyChange(int type) {
		if (type == PROP_DIRTY) {
			fEditor.fireSaveNeeded(getEditorInput(), true);
		}
		super.firePropertyChange(type);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#setActive(boolean)
	 */
	public void setActive(boolean active) {
		fInputContext.setSourceEditingMode(active);
		// Update the text selection if this page is being activated
		if (active) {
			updateTextSelection();
		}
	}

	public boolean canLeaveThePage() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#isActive()
	 */
	public boolean isActive() {
		return this.equals(fEditor.getActivePageInstance());
	}

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		Control[] children = parent.getChildren();
		fControl = children[children.length - 1];

		PlatformUI.getWorkbench().getHelpSystem().setHelp(fControl, IHelpContextIds.MANIFEST_SOURCE_PAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getPartControl()
	 */
	public Control getPartControl() {
		return fControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#getIndex()
	 */
	public int getIndex() {
		return fIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#setIndex(int)
	 */
	public void setIndex(int index) {
		fIndex = index;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#isSource()
	 */
	public boolean isEditor() {
		return true;
	}

	/**
	 * @return Returns the inputContext.
	 */
	public InputContext getInputContext() {
		return fInputContext;
	}

	/**
	 * @param inputContext The inputContext to set.
	 */
	public void setInputContext(InputContext inputContext) {
		fInputContext = inputContext;
		setDocumentProvider(inputContext.getDocumentProvider());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.IFormPage#focusOn(java.lang.Object)
	 */
	public boolean selectReveal(Object object) {
		if (object instanceof IMarker) {
			IDE.gotoMarker(this, (IMarker) object);
			return true;
		}
		return false;
	}

	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		return null;
	}

	public void setHighlightRange(IDocumentRange range, boolean moveCursor) {
		int offset = range.getOffset();
		if (offset == -1) {
			resetHighlightRange();
			return;
		}

		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return;

		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		int length = range.getLength();
		setHighlightRange(offset, length == -1 ? 1 : length, moveCursor);
	}

	public void setSelectedRange(IDocumentRange range, boolean fullNodeSelection) {
		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return;

		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		int offset;
		int length;
		if (range instanceof IDocumentElementNode && !fullNodeSelection) {
			length = ((IDocumentElementNode) range).getXMLTagName().length();
			offset = range.getOffset() + 1;
		} else {
			length = range.getLength();
			offset = range.getOffset();
		}
		sourceViewer.setSelectedRange(offset, length);
	}

	public int getOrientation() {
		return SWT.LEFT_TO_RIGHT;
	}

	protected void createActions() {
		super.createActions();
		PDESelectAnnotationRulerAction action = new PDESelectAnnotationRulerAction(getBundleForConstructedKeys(), "PDESelectAnnotationRulerAction.", this, getVerticalRuler()); //$NON-NLS-1$
		setAction(ITextEditorActionConstants.RULER_CLICK, action);
		PDEFormEditorContributor contributor = fEditor == null ? null : fEditor.getContributor();
		if (contributor instanceof PDEFormTextEditorContributor) {
			PDEFormTextEditorContributor textContributor = (PDEFormTextEditorContributor) contributor;
			setAction(PDEActionConstants.OPEN, textContributor.getHyperlinkAction());
			setAction(PDEActionConstants.FORMAT, textContributor.getFormatAction());
		}

		// Create the quick outline action
		createQuickOutlineAction();
	}

	/**
	 * 
	 */
	private void createQuickOutlineAction() {
		// Quick Outline Action
		ResourceAction action = new TextOperationAction(getBundleForConstructedKeys(), "QuickOutline.", this, //$NON-NLS-1$
				PDEProjectionViewer.QUICK_OUTLINE, true);
		action.setActionDefinitionId(PDEActionConstants.COMMAND_ID_QUICK_OUTLINE);
		action.setText(PDEUIMessages.PDESourcePage_actionTextQuickOutline);
		action.setId(PDEActionConstants.COMMAND_ID_QUICK_OUTLINE);
		action.setImageDescriptor(PDEPluginImages.DESC_OVERVIEW_OBJ);
		setAction(PDEActionConstants.COMMAND_ID_QUICK_OUTLINE, action);
	}

	public final void selectionChanged(SelectionChangedEvent event) {
		if (event.getSource() == getSelectionProvider())
			return;
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection) {

			IStructuredSelection structuredSel = (IStructuredSelection) sel;

			// Store the selected object to save us having to do this again.
			setSelectedObject(structuredSel.getFirstElement());

		} else if (sel instanceof ITextSelection) {

			ITextSelection textSel = (ITextSelection) sel;

			setSelectedObject(getRangeElement(textSel.getOffset(), false));

		} else
			fSelection = null;
	}

	/*
	 * Locate an IDocumentRange, subclasses that want to 
	 * highlight text components based on site selection
	 * should override this method.
	 */
	protected IDocumentRange findRange() {
		return null;
	}

	public void updateTextSelection() {
		IDocumentRange range = findRange();
		if (range == null)
			return;
		IBaseModel model = getInputContext().getModel();
		if (!(model instanceof AbstractEditingModel))
			return;

		if (range.getOffset() == -1 || isDirty()) {
			try {
				((AbstractEditingModel) model).adjustOffsets(((AbstractEditingModel) model).getDocument());
			} catch (CoreException e) {
			}
			range = findRange();
		}
		setHighlightRange(range, true);
		setSelectedRange(range, false);
	}

	/*
	 * Subclasses that wish to provide PDEFormPage -> PDESourcePage
	 * selection persistence should override this and return true.
	 */
	protected boolean isSelectionListener() {
		return false;
	}

	public ISourceViewer getViewer() {
		return getSourceViewer();
	}

	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		PDEFormEditorContributor contributor = fEditor == null ? null : fEditor.getContributor();
		if (contributor instanceof PDEFormTextEditorContributor) {
			PDEFormTextEditorContributor textContributor = (PDEFormTextEditorContributor) contributor;
			HyperlinkAction action = textContributor.getHyperlinkAction();
			if ((action != null) && action.isEnabled() && ((action.getHyperLink() instanceof ExtensionHyperLink) == false)) {
				// Another detector handles this the extension hyperlink case
				// org.eclipse.pde.internal.ui.editor.plugin.ExtensionAttributePointDectector.java
				// Implemented at a higher level.  As a result, need to disable
				// the action here to prevent duplicate entries in the context menu
				menu.add(action);
			}
			FormatAction formatManifestAction = textContributor.getFormatAction();
			if (isEditable() && formatManifestAction != null && formatManifestAction.isEnabled())
				menu.add(formatManifestAction);
		}
		super.editorContextMenuAboutToShow(menu);
	}

	public Object getSelection() {
		return fSelection;
	}

	/**
	 * Allow for programmatic selection of the currently selected object by
	 * subclasses
	 */
	protected void setSelectedObject(Object selectedObject) {
		fSelection = selectedObject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.outline.IOutlineContentCreator#getOutlineInput()
	 */
	public Object getOutlineInput() {
		return getInputContext().getModel();
	}

	/**
	 * @param rangeElement
	 */
	protected void updateOutlinePageSelection(Object rangeElement) {
		// Set selection in source outline page if the 'Link with Editor' 
		// feature is on
		if (PDEPlugin.getDefault().getPreferenceStore().getBoolean("ToggleLinkWithEditorAction.isChecked")) { //$NON-NLS-1$
			// Ensure we have a source outline page
			if ((fOutlinePage instanceof SourceOutlinePage) == false) {
				return;
			}
			SourceOutlinePage outlinePage = (SourceOutlinePage) fOutlinePage;
			// Temporarily remove the listener to prevent a selection event being fired 
			// back at this page			
			outlinePage.removeAllSelectionChangedListeners();
			if (rangeElement != null) {
				outlinePage.setSelection(new StructuredSelection(rangeElement));
			} else {
				outlinePage.setSelection(StructuredSelection.EMPTY);
			}
			outlinePage.addAllSelectionChangedListeners();
		}
	}

	/**
	 * Handles selection events generated from the source page
	 * @param event The selection changed event
	 */
	protected void handleSelectionChangedSourcePage(SelectionChangedEvent event) {

		ISelection selection = event.getSelection();
		if (!selection.isEmpty() && selection instanceof ITextSelection) {

			ITextSelection textSel = (ITextSelection) selection;

			// Get the current element from the offset
			int offset = textSel.getOffset();
			IDocumentRange rangeElement;
			rangeElement = this.getRangeElement(offset, false);

			// store this as the current selection
			setSelectedObject(rangeElement);

			// Synchronize the outline page
			synchronizeOutlinePage(rangeElement);
		}
	}

	/**
	 * @param rangeElement
	 */
	protected void updateHighlightRange(IDocumentRange rangeElement) {
		if (rangeElement != null) {
			setHighlightRange(rangeElement, false);
		} else {
			resetHighlightRange();
		}
	}

	/**
	 * Synchronize the outline page to show a relevant element given the 
	 * current offset.
	 * 
	 * @param offset The current offset within the source page
	 */
	protected void synchronizeOutlinePage(int offset) {
		IDocumentRange rangeElement = getRangeElement(offset, false);

		synchronizeOutlinePage(rangeElement);
	}

	/**
	 * Synchronize the outline page to show the specified element
	 * @param rangeElement The element to show in the outline page 
	 */
	private void synchronizeOutlinePage(IDocumentRange rangeElement) {
		updateHighlightRange(rangeElement);
		updateOutlinePageSelection(rangeElement);
	}

	/**
	 * Triggered by toggling the 'Link with Editor' button in the outline
	 * view
	 * @param offset
	 */
	public void synchronizeOutlinePage() {
		// Get the current position of the cursor in this page
		int current_offset = getSourceViewer().getSelectedRange().x;
		synchronizeOutlinePage(current_offset);
	}

	/**
	 * Utility method for getRangeElement(int, boolean)
	 * @param node
	 * @param offset
	 * @param searchChildren
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#findNode(Object[], int, boolean)
	 */
	protected IDocumentRange findNode(IDocumentElementNode node, int offset, boolean searchChildren) {
		return findNode(new Object[] {node}, offset, searchChildren);
	}

	/**
	 * Utility method for getRangeElement(int, boolean)
	 * @param nodes All entries should be instances of IDocumentElementNode
	 * @param offset The offset the cursor is currently on in the document
	 * @param searchChildren <code>true</code> to search child nodes; <code>false</code> otherwise.
	 * @return Node the offset is in
	 */
	protected IDocumentRange findNode(Object[] nodes, int offset, boolean searchChildren) {
		for (int i = 0; i < nodes.length; i++) {
			IDocumentElementNode node = (IDocumentElementNode) nodes[i];
			if (node.getOffset() <= offset && offset < node.getOffset() + node.getLength()) {

				if (!searchChildren)
					return node;

				if (node.getOffset() < offset && offset <= node.getOffset() + node.getXMLTagName().length() + 1)
					return node;

				IDocumentAttributeNode[] attrs = node.getNodeAttributes();
				if (attrs != null)
					for (int a = 0; a < attrs.length; a++)
						if (attrs[a].getNameOffset() <= offset && offset <= attrs[a].getValueOffset() + attrs[a].getValueLength())
							return attrs[a];

				IDocumentTextNode textNode = node.getTextNode();
				if (textNode != null && textNode.getOffset() <= offset && offset < textNode.getOffset() + textNode.getLength())
					return textNode;

				IDocumentElementNode[] children = node.getChildNodes();
				if (children != null)
					for (int c = 0; c < children.length; c++)
						if (children[c].getOffset() <= offset && offset < children[c].getOffset() + children[c].getLength())
							return findNode(children[c], offset, searchChildren);

				// not contained inside any sub elements, must be inside node
				return node;
			}
		}
		return null;
	}

	/**
	 * Override the getAdapter function to return a list of targets
	 * for the "Show In >" action in the context menu.
	 *  
	 * @param adapter
	 * @return A list of targets (IShowInTargetList) for the "Show In >"
	 * submenu if the appropriate adapter is passed in and the editor
	 * is not read-only. Returns <code>super.getAdapter(adapter)</code>
	 * otherwise.
	 */
	public Object getAdapter(Class adapter) {
		if ((adapter == IShowInTargetList.class) && (fEditor != null) && (fEditor.getEditorInput() instanceof IFileEditorInput)) {
			return getShowInTargetList();
		}
		return super.getAdapter(adapter);
	}

	/**
	 * Returns the <code>IShowInTargetList</code> for this view.
	 * @return the <code>IShowInTargetList</code> 
	 */
	protected IShowInTargetList getShowInTargetList() {
		return new IShowInTargetList() {
			public String[] getShowInTargetIds() {
				return new String[] {JavaUI.ID_PACKAGES, IPageLayout.ID_RES_NAV};
			}
		};
	}

	/**
	 * @param range
	 */
	public IDocumentRange adaptRange(IDocumentRange range) {
		// Subclasses to override
		return range;
	}

}
