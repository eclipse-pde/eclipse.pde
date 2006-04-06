/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.context.IInputContextListener;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.search.ui.text.ISearchEditorAccess;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * A simple multi-page form editor that uses Eclipse Forms support. Example
 * plug-in is configured to create one instance of form colors that is shared
 * between multiple editor instances.
 */
public abstract class PDEFormEditor extends FormEditor
		implements
			IInputContextListener,
			IGotoMarker, ISearchEditorAccess {
	/**
	 * Updates the OutlinePage selection.
	 * 
	 * @since 3.0
	 */
	private class PDEFormEditorChangeListener implements
			ISelectionChangedListener {

		/**
		 * Installs this selection changed listener with the given selection
		 * provider. If the selection provider is a post selection provider,
		 * post selection changed events are the preferred choice, otherwise
		 * normal selection changed events are requested.
		 * 
		 * @param selectionProvider
		 */
		public void install(ISelectionProvider selectionProvider) {
			if (selectionProvider == null) {
				return;
			}

			if (selectionProvider instanceof IPostSelectionProvider) {
				IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
				provider.addPostSelectionChangedListener(this);
			} else {
				selectionProvider.addSelectionChangedListener(this);
			}
		}

		/*
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			if (PDEPlugin.getDefault().getPreferenceStore().getBoolean(
					"ToggleLinkWithEditorAction.isChecked")) //$NON-NLS-1$
				if (getFormOutline() != null) {
					getFormOutline().setSelection(event.getSelection());
				}
		}

		/**
		 * Removes this selection changed listener from the given selection
		 * provider.
		 * 
		 * @param selectionProviderstyle
		 */
		public void uninstall(ISelectionProvider selectionProvider) {
			if (selectionProvider == null) {
				return;
			}

			if (selectionProvider instanceof IPostSelectionProvider) {
				IPostSelectionProvider provider = (IPostSelectionProvider) selectionProvider;
				provider.removePostSelectionChangedListener(this);
			} else {
				selectionProvider.removeSelectionChangedListener(this);
			}
		}

	}

	/**
	 * The editor selection changed listener.
	 * 
	 * @since 3.0
	 */
	private PDEFormEditorChangeListener fEditorSelectionChangedListener;
	private Clipboard clipboard;
	private Menu fContextMenu;
	protected InputContextManager fInputContextManager;
	private ISortableContentOutlinePage fFormOutline;
	private PDEMultiPageContentOutline fContentOutline;
	private String fLastActivePageId;
	private boolean fLastDirtyState;
	private IEditorValidationStack fValidationStack;

	private static class PDEMultiPageEditorSite extends MultiPageEditorSite {
		public PDEMultiPageEditorSite(MultiPageEditorPart multiPageEditor,
				IEditorPart editor) {
			super(multiPageEditor, editor);
		}
		public IEditorActionBarContributor getActionBarContributor() {
			PDEFormEditor editor = (PDEFormEditor) getMultiPageEditor();
			PDEFormEditorContributor contributor = editor.getContributor();
			return contributor.getSourceContributor();
		}
		
		public IWorkbenchPart getPart() {
			return getMultiPageEditor();
		}
	}
	/**
	 *  
	 */
	public PDEFormEditor() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fInputContextManager = createInputContextManager();
		fValidationStack = new EditorValidationStack(this);
	}
	/**
	 * We must override nested site creation so that we properly pass the source
	 * editor contributor when asked.
	 */
	protected IEditorSite createSite(IEditorPart editor) {
		return new PDEMultiPageEditorSite(this, editor);
	}
	public IProject getCommonProject() {
		return fInputContextManager.getCommonProject();
	}
	public IBaseModel getAggregateModel() {
		if (fInputContextManager != null)
			return fInputContextManager.getAggregateModel();
		return null;
	}
	protected abstract InputContextManager createInputContextManager();
	/**
	 * Tests whether this editor has a context with a provided id. The test can
	 * be used to check whether to add certain pages.
	 * 
	 * @param contextId
	 * @return <code>true</code> if provided context is present,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasInputContext(String contextId) {
		return fInputContextManager.hasContext(contextId);
	}
	public InputContextManager getContextManager() {
		return fInputContextManager;
	}
	protected void createInputContexts(InputContextManager contextManager) {
		IEditorInput input = getEditorInput();
		if (input instanceof IFileEditorInput) {
			// resource - find the project
			createResourceContexts(contextManager, (IFileEditorInput) input);
		} else if (input instanceof SystemFileEditorInput) {
			// system file - find the file system folder
			createSystemFileContexts(contextManager,
					(SystemFileEditorInput) input);
		} else if (input instanceof IStorageEditorInput) {
			createStorageContexts(contextManager, (IStorageEditorInput) input);
		} else if (input instanceof ILocationProvider) {
			IPath path = ((ILocationProvider) input).getPath(input);
			File file = path.toFile();
			SystemFileEditorInput sinput = new SystemFileEditorInput(file);
			createSystemFileContexts(contextManager, sinput);
		}
	}
	protected abstract void createResourceContexts(
			InputContextManager contexts, IFileEditorInput input);
	protected abstract void createSystemFileContexts(
			InputContextManager contexts, SystemFileEditorInput input);
	protected abstract void createStorageContexts(InputContextManager contexts,
			IStorageEditorInput input);
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.editor.FormEditor#createToolkit(org.eclipse.swt.widgets.Display)
	 */
	protected FormToolkit createToolkit(Display display) {
		// Create a toolkit that shares colors between editors.
		return new FormToolkit(PDEPlugin.getDefault().getFormColors(display));
	}
	/*
	 * When subclassed, don't forget to call 'super'
	 */
	protected void createPages() {
		clipboard = new Clipboard(getContainer().getDisplay());
		MenuManager manager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				contextMenuAboutToShow(manager);
			}
		};
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(listener);
		fContextMenu = manager.createContextMenu(getContainer());
		getContainer().setMenu(fContextMenu);
		createInputContexts(fInputContextManager);
		super.createPages();
		fInputContextManager.addInputContextListener(this);
		String pageToShow = computeInitialPageId();
		if (pageToShow != null)
			setActivePage(pageToShow);
		updateTitle();
		PDEModelUtility.connect(this);
	}
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		IFormPage page = getActivePageInstance();
		updateContentOutline(page);
		if (page!=null)
			fLastActivePageId = page.getId();
	}
	public Clipboard getClipboard() {
		return clipboard;
	}
	protected void contextMenuAboutToShow(IMenuManager manager) {
		PDEFormEditorContributor contributor = getContributor();
		IFormPage page = getActivePageInstance();
		if (page instanceof PDEFormPage)
			((PDEFormPage) page).contextMenuAboutToShow(manager);
		if (contributor != null)
			contributor.contextMenuAboutToShow(manager);
	}
	public PDEFormEditorContributor getContributor() {
		return (PDEFormEditorContributor) getEditorSite()
				.getActionBarContributor();
	}
	protected String computeInitialPageId() {
		String firstPageId = null;
		String storedFirstPageId = loadDefaultPage();
		if (storedFirstPageId != null)
			firstPageId = storedFirstPageId;
		// Regardless what is the stored value,
		// use source page if model is not valid
		String invalidContextId = getFirstInvalidContextId();
		if (invalidContextId != null)
			return invalidContextId;
		return firstPageId;
	}

	private String getFirstInvalidContextId() {
		InputContext[] invalidContexts = fInputContextManager
				.getInvalidContexts();
		if (invalidContexts.length == 0)
			return null;
		// If primary context is among the invalid ones, return that.
		for (int i = 0; i < invalidContexts.length; i++) {
			if (invalidContexts[i].isPrimary())
				return invalidContexts[i].getId();
		}
		// Return the first one
		return invalidContexts[0].getId();
	}

	public String getTitle() {
		if (fInputContextManager == null)
			return super.getTitle();
		InputContext context = fInputContextManager.getPrimaryContext();
		if (context == null)
			return super.getTitle();
		return context.getInput().getName();
	}
	
	public void updateTitle() {
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	public String getTitleProperty() {
		return ""; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		commitFormPages(true);
		fInputContextManager.save(monitor);
		editorDirtyStateChanged();
	}
	public void doRevert() {
		IFormPage currentPage = getActivePageInstance();
		if (currentPage != null && currentPage instanceof PDEFormPage)
			((PDEFormPage) currentPage).cancelEdit();
		IFormPage[] pages = getPages();
		for (int i = 0; i < pages.length; i++) {
			if (pages[i] instanceof PDESourcePage) {
				((PDESourcePage) pages[i]).doRevertToSaved();
			}
		}
		editorDirtyStateChanged();
	}
	public void doRevert(IEditorInput input) {
		IFormPage currentPage = getActivePageInstance();
		if (currentPage != null && currentPage instanceof PDEFormPage)
			((PDEFormPage) currentPage).cancelEdit();
		InputContext context = fInputContextManager.getContext(input);
		IFormPage page = findPage(context.getId());
		if (page!=null && page instanceof PDESourcePage) {
			PDESourcePage spage = (PDESourcePage) page;
		    spage.doRevertToSaved();
		}
		editorDirtyStateChanged();
	}
	private void commitFormPages(boolean onSave) {
		IFormPage[] pages = getPages();
		for (int i = 0; i < pages.length; i++) {
			IFormPage page = pages[i];
			IManagedForm mform = page.getManagedForm();
			if (mform != null && mform.isDirty())
				mform.commit(true);
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

	private void storeDefaultPage() {
		IEditorInput input = getEditorInput();
		String pageId = fLastActivePageId;
		if (pageId == null)
			return;
		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			if (file != null) {
				//set the settings on the resouce
				try {
					file
							.setPersistentProperty(
									IPDEUIConstants.DEFAULT_EDITOR_PAGE_KEY_NEW,
									pageId);
				} catch (CoreException e) {
				}
			}
		} else if (input instanceof SystemFileEditorInput) {
			File file = (File) ((SystemFileEditorInput) input)
					.getAdapter(File.class);
			if (file == null)
				return;
			IDialogSettings section = getSettingsSection();
			section.put(file.getPath(), pageId);
		}
	}
	private String loadDefaultPage() {
		IEditorInput input = getEditorInput();
		if (input instanceof IFileEditorInput) {
			// load the setting from the resource
			IFile file = ((IFileEditorInput) input).getFile();
			try {
				return file
						.getPersistentProperty(IPDEUIConstants.DEFAULT_EDITOR_PAGE_KEY_NEW);
			} catch (CoreException e) {
				return null;
			}
		} else if (input instanceof SystemFileEditorInput) {
			File file = (File) ((SystemFileEditorInput) input)
					.getAdapter(File.class);
			if (file == null)
				return null;
			IDialogSettings section = getSettingsSection();
			String key = file.getPath();
			return section.get(key);
		}
		return null;
	}
	public void dispose() {
		storeDefaultPage();
		if (fEditorSelectionChangedListener != null)  {
			fEditorSelectionChangedListener.uninstall(getSite().getSelectionProvider());
			fEditorSelectionChangedListener= null;
		}
		//setSelection(new StructuredSelection());
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		PDEModelUtility.disconnect(this);
		if (clipboard != null) {
			clipboard.dispose();
			clipboard = null;
		}
		super.dispose();
		fInputContextManager.dispose();
		fInputContextManager = null;
	}
	public boolean isDirty() {
		fLastDirtyState = computeDirtyState();
		return fLastDirtyState;
	}
	
	private boolean computeDirtyState() {
		IFormPage page = getActivePageInstance();
		if ((page != null && page.isDirty())
				|| (fInputContextManager != null && fInputContextManager.isDirty()))
			return true;
		return super.isDirty();
	}
	
	public boolean getLastDirtyState() {
		return fLastDirtyState;
	}

	public void fireSaveNeeded(String contextId, boolean notify) {
		if (contextId == null)
			return;
		InputContext context = fInputContextManager.findContext(contextId);
		if (context != null)
			fireSaveNeeded(context.getInput(), notify);
	}
	public void fireSaveNeeded(IEditorInput input, boolean notify) {
		if (notify)
			editorDirtyStateChanged();
		if (isDirty())
			validateEdit(input);
	}
	public void editorDirtyStateChanged() {
		super.editorDirtyStateChanged();
		PDEFormEditorContributor contributor = getContributor();
		if (contributor != null)
			contributor.updateActions();
	}
	private void validateEdit(IEditorInput input) {
		final InputContext context = fInputContextManager.getContext(input);
		if (!context.validateEdit()) {
			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					doRevert(context.getInput());
					context.setValidated(false);
				}
			});
		}
	}
	private IDialogSettings getSettingsSection() {
		// store the setting in dialog settings
		IDialogSettings root = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = root.getSection("multi-page-editor"); //$NON-NLS-1$
		if (section == null)
			section = root.addNewSection("multi-page-editor"); //$NON-NLS-1$
		return section;
	}
	public void gotoMarker(IMarker marker) {
		IResource resource = marker.getResource();
		InputContext context = fInputContextManager.findContext(resource);
		if (context == null)
			return;
		IFormPage page = getActivePageInstance();
		if (!context.getId().equals(page.getId()))
			page = setActivePage(context.getId());
		IDE.gotoMarker(page, marker);
	}
	
	public void openToSourcePage(Object object, int offset, int length) {
		InputContext context = getInputContext(object);
		if (context != null) {
			PDESourcePage page = (PDESourcePage)setActivePage(context.getId());
			if (page != null)
				page.selectAndReveal(offset, length);
		}
	}

	public void setSelection(ISelection selection) {
		getSite().getSelectionProvider().setSelection(selection);
		getContributor().updateSelectableActions(selection);
	}
	public ISelection getSelection() {
		return getSite().getSelectionProvider().getSelection();
	}
	public Object getAdapter(Class key) {
		if (key.equals(IContentOutlinePage.class)) {
			return getContentOutline();
		}
		if (key.equals(IGotoMarker.class)) {
			return this;
		}
		if (key.equals(ISearchEditorAccess.class)) {
			return this;
		}
		return super.getAdapter(key);
	}
	public Menu getContextMenu() {
		return fContextMenu;
	}
	public PDEMultiPageContentOutline getContentOutline() {
		if (fContentOutline == null || fContentOutline.isDisposed()) {
			fContentOutline = new PDEMultiPageContentOutline(this);
			updateContentOutline(getActivePageInstance());
		}
		return fContentOutline;
	}

	/**
	 * 
	 * @return outline page or null
	 */
	protected ISortableContentOutlinePage getFormOutline() {
		if (fFormOutline == null) {
			fFormOutline = createContentOutline();
			if (fFormOutline != null) {
				fEditorSelectionChangedListener = new PDEFormEditorChangeListener();
				fEditorSelectionChangedListener.install(getSite()
						.getSelectionProvider());
			}
		}
		return fFormOutline;
	}
	abstract protected ISortableContentOutlinePage createContentOutline();
	
	private void updateContentOutline(IFormPage page) {
		if (fContentOutline == null)
			return;
		ISortableContentOutlinePage outline = null;
		if (page instanceof PDESourcePage) {
			outline = ((PDESourcePage) page).getContentOutline();
		} else {
			outline = getFormOutline();
			if (outline != null && outline instanceof FormOutlinePage)
				((FormOutlinePage) outline).refresh();
		}
		fContentOutline.setPageActive(outline);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#setActivePage(java.lang.String)
	 */
	public IFormPage setActivePage(String pageId) {
		IFormPage page = super.setActivePage(pageId);
		if (page != null)
			updateContentOutline(page);
		return page;
	}

	/* package */IFormPage[] getPages() {
		ArrayList formPages = new ArrayList();
		for (int i = 0; i < pages.size(); i++) {
			Object page = pages.get(i);
			if (page instanceof IFormPage)
				formPages.add(page);
		}
		return (IFormPage[]) formPages.toArray(new IFormPage[formPages.size()]);
	}
	protected void performGlobalAction(String id) {
		// preserve selection
		ISelection selection = getSelection();
		boolean handled = ((PDEFormPage) getActivePageInstance())
				.performGlobalAction(id);
		if (!handled) {
			IFormPage page = getActivePageInstance();
			if (page instanceof PDEFormPage) {
				if (id.equals(ActionFactory.UNDO.getId())) {
					fInputContextManager.undo();
					return;
				}
				if (id.equals(ActionFactory.REDO.getId())) {
					fInputContextManager.redo();
					return;
				}
				if (id.equals(ActionFactory.CUT.getId())
						|| id.equals(ActionFactory.COPY.getId())) {
					copyToClipboard(selection);
					return;
				}
			}
		}
	}
	private void copyToClipboard(ISelection selection) {
		Object[] objects = null;
		String textVersion = null;		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel == null || ssel.size() == 0)
				return;
			objects = ssel.toArray();
			StringWriter writer = new StringWriter();
			PrintWriter pwriter = new PrintWriter(writer);
			Class objClass = null;
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (objClass == null)
					objClass = obj.getClass();
				else if (objClass.equals(obj.getClass()) == false)
					return;
				if (obj instanceof IWritable) {
					((IWritable) obj).write("", pwriter); //$NON-NLS-1$
				}
			}
			pwriter.flush();
			textVersion = writer.toString();
			try {
				pwriter.close();
				writer.close();
			} catch (IOException e) {
			}
		} else if (selection instanceof ITextSelection) {
			textVersion = ((ITextSelection) selection).getText();
		}
		if ((textVersion == null || textVersion.length() == 0) && objects == null)
			return;
		// set the clipboard contents
		Object[]o = null;
		Transfer[] t = null;
		if (objects == null ) {
			o = new Object[] {textVersion};
			t = new Transfer[] {TextTransfer.getInstance()};
		} else if (textVersion == null || textVersion.length() == 0) {
			o = new Object[] {objects};
			t = new Transfer[] {ModelDataTransfer.getInstance()};
		} else {
			o = new Object[] {objects, textVersion};
			t = new Transfer[] {ModelDataTransfer.getInstance(), TextTransfer.getInstance()};
		}
		clipboard.setContents(o, t);
	}
	public boolean canPasteFromClipboard() {
		IFormPage page = getActivePageInstance();
		if (page instanceof PDEFormPage) {
			return ((PDEFormPage) page).canPaste(getClipboard());
		}
		return false;
	}
	public boolean canCopy(ISelection selection) {
		if (selection == null)
			return false;
		if (selection instanceof IStructuredSelection)
			return !selection.isEmpty();
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) selection;
			return textSelection.getLength() > 0;
		}
		return false;
	}
	void updateUndo(IAction undoAction, IAction redoAction) {
		IModelUndoManager undoManager = fInputContextManager.getUndoManager();
		if (undoManager != null)
			undoManager.setActions(undoAction, redoAction);
	}
	void synchronizeOutlinePage() {
		if (getFormOutline() != null) {
			getFormOutline().setSelection(getSelection());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.ISearchEditorAccess#getDocument(org.eclipse.search.ui.text.Match)
	 */
	public IDocument getDocument(Match match) {
		InputContext context = getInputContext(match.getElement());
		return context == null ? null : context.getDocumentProvider().getDocument(context.getInput());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.ISearchEditorAccess#getAnnotationModel(org.eclipse.search.ui.text.Match)
	 */
	public IAnnotationModel getAnnotationModel(Match match) {
		InputContext context = getInputContext(match.getElement());
		return context == null ? null : context.getDocumentProvider().getAnnotationModel(context.getInput());
	}
	
	protected abstract InputContext getInputContext(Object object);
	
	public IEditorValidationStack getValidationStack() {
		return fValidationStack;
	}
}
