/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.pde.internal.ui.preferences.EditorPreferencePage;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
/**
 * A simple multi-page form editor that uses Eclipse Forms support.
 * Example plug-in is configured to create one instance of
 * form colors that is shared between multiple editor instances.
 */
public abstract class PDEFormEditor extends FormEditor implements IInputContextListener, IGotoMarker {
	private Clipboard clipboard;
	private Menu contextMenu;
	protected InputContextManager inputContextManager;
	private IContentOutlinePage formOutline;
	private PDEMultiPagePropertySheet propertySheet;
	private PDEMultiPageContentOutline contentOutline;
	/**
	 *  
	 */
	public PDEFormEditor() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		inputContextManager = createInputContextManager();
	}
	
	public IProject getCommonProject() {
		return inputContextManager.getCommonProject();
	}
	public IBaseModel getAggregateModel() {
		if (inputContextManager!=null)
			return inputContextManager.getAggregateModel();
		return null;
	}
	
	protected abstract InputContextManager createInputContextManager();

/**
 * Tests whether this editor has a context with
 * a provided id. The test can be used to check
 * whether to add certain pages.
 * @param contextId
 * @return <code>true</code> if provided context is
 * present, <code>false</code> otherwise.
 */	
	public boolean hasInputContext(String contextId) {
		return inputContextManager.hasContext(contextId);
	}
	public InputContextManager getContextManager() {
		return inputContextManager;
	}
	protected void createInputContexts(InputContextManager contextManager) {
		IEditorInput input = getEditorInput();
		if (input instanceof IFileEditorInput) {
			// resource - find the project
			createResourceContexts(contextManager, (IFileEditorInput)input);
		}
		else if (input instanceof SystemFileEditorInput) {
			// system file - find the file system folder
			createSystemFileContexts(contextManager, (SystemFileEditorInput)input);
		}
		else if (input instanceof IStorageEditorInput) {
			createStorageContexts(contextManager, (IStorageEditorInput)input);
		}
		else if (input instanceof ILocationProvider) {
			IPath path = ((ILocationProvider)input).getPath(input);
			File file = path.toFile();
			SystemFileEditorInput sinput = new SystemFileEditorInput(file);
			createSystemFileContexts(contextManager, sinput);
		}
	}
	
	protected abstract void createResourceContexts(InputContextManager contexts, IFileEditorInput input);
	protected abstract void createSystemFileContexts(InputContextManager contexts, SystemFileEditorInput input);
	protected abstract void createStorageContexts(InputContextManager contexts, IStorageEditorInput input);
	
	
	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#createToolkit(org.eclipse.swt.widgets.Display)
	 */
	protected FormToolkit createToolkit(Display display) {
		// Create a toolkit that shares colors between editors.
		return new FormToolkit(PDEPlugin.getDefault().getFormColors(
				display));
	}
	/*
 	* When subclassed, don't forget to call 'super'
 	**/
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
		contextMenu = manager.createContextMenu(getContainer());
		getContainer().setMenu(contextMenu);
		createInputContexts(inputContextManager);
		super.createPages();
		inputContextManager.addInputContextListener(this);		
		String pageToShow = computeInitialPageId();
		if (pageToShow!=null)
				setActivePage(pageToShow);
	}

	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		IFormPage page = getActivePageInstance();
		updateContentOutline(page);
		updatePropertySheet(page);
	}
	
	public Clipboard getClipboard() {
		return clipboard;
	}

	protected void contextMenuAboutToShow(IMenuManager manager) {
		PDEFormEditorContributor contributor = getContributor();
		IFormPage page = getActivePageInstance();
		if (page instanceof PDEFormPage)
			((PDEFormPage)page).contextMenuAboutToShow(manager);
		if (contributor != null)
			contributor.contextMenuAboutToShow(manager);
	}
	
	public PDEFormEditorContributor getContributor() {
		return (PDEFormEditorContributor)getEditorSite().getActionBarContributor();
	}

	protected String computeInitialPageId() {
		String firstPageId=null;
		String storedFirstPageId = loadDefaultPage();
		if (storedFirstPageId != null)
			firstPageId = storedFirstPageId;
		else if (EditorPreferencePage.getUseSourcePage())
			firstPageId = getSourcePageId();
		// Regardless what is the stored value,
		// use source page if model is not valid
		String invalidContextId = getFirstInvalidContextId();
		if (invalidContextId!=null)
			return invalidContextId;
		return firstPageId;
	}

	private String getSourcePageId() {
		InputContext context = inputContextManager.getPrimaryContext();
		if (context!=null)
			return context.getId();
		return null;
	}
	
	private String getFirstInvalidContextId() {
		InputContext [] invalidContexts = inputContextManager.getInvalidContexts();
		if (invalidContexts.length==0) return null;

		// If primary context is among the invalid ones, return that.
		for (int i=0; i<invalidContexts.length; i++) {
			if (invalidContexts[i].isPrimary())
				return invalidContexts[i].getId();
		}
		// Return the first one
		return invalidContexts[0].getId();
	}
	
	public String getTitle() {
		if (inputContextManager==null) return super.getTitle();
		InputContext context = inputContextManager.getPrimaryContext();
		if (context==null) return super.getTitle();
		return context.getInput().getName();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		commitFormPages(true);
		inputContextManager.save(monitor);
		editorDirtyStateChanged();
	}
	public void doRevert() {
		IFormPage [] pages = getPages();
		for (int i=0; i<pages.length; i++) {
			if (pages[i] instanceof PDESourcePage) {
				PDESourcePage page = (PDESourcePage)pages[i];
				page.doRevertToSaved();
				InputContext context = inputContextManager.findContext(page.getId());
				context.doRevert();
			}
		}
		editorDirtyStateChanged();
	}
	
	private void commitFormPages(boolean onSave) {
		IFormPage [] pages = getPages();
		for (int i=0; i<pages.length; i++) {
			IFormPage page = pages[i];
			IManagedForm mform = page.getManagedForm();
			if (mform!=null && mform.isDirty())
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
		int lastPage = getCurrentPage();
		if (lastPage== -1) return;
		String pageId = ((IFormPage)pages.get(lastPage)).getId();

		if (input instanceof IFileEditorInput) {
			// load the setting from the resource
			IFile file = ((IFileEditorInput) input).getFile();
			if (file != null) {
				//set the settings on the resouce
				try {
					file.setPersistentProperty(
						IPDEUIConstants.DEFAULT_EDITOR_PAGE_KEY_NEW,
						pageId);
				} catch (CoreException e) {
				}
			}
		} else if (input instanceof SystemFileEditorInput) {
			File file =
				(File) ((SystemFileEditorInput) input).getAdapter(File.class);
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
				return file.getPersistentProperty(
					IPDEUIConstants.DEFAULT_EDITOR_PAGE_KEY_NEW);
			} catch (CoreException e) {
				return null;
			}
		} else if (input instanceof SystemFileEditorInput) {
			File file =
				(File) ((SystemFileEditorInput) input).getAdapter(File.class);
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
		//setSelection(new StructuredSelection());
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		if (clipboard!=null) {
			clipboard.dispose();
			clipboard = null;
		}
		inputContextManager.dispose();
		inputContextManager = null;
	}

	public boolean isDirty() {
		IFormPage page = getActivePageInstance();
		if ((page!=null && page.isDirty()) || inputContextManager.isDirty())
			return true;
		return super.isDirty();
	}
	
	public void fireSaveNeeded(String contextId, boolean notify) {
		if (contextId==null) return;
		InputContext context = inputContextManager.findContext(contextId);
		if (context!=null)
			fireSaveNeeded(context.getInput(), notify);
	}
	
	public void fireSaveNeeded(IEditorInput input, boolean notify) {
		if (notify) editorDirtyStateChanged();
		validateEdit(input);
	}
	
	public void editorDirtyStateChanged() {
		super.editorDirtyStateChanged();
		PDEFormEditorContributor contributor = getContributor();
		if (contributor != null)
			contributor.updateActions();
	}

	private void validateEdit(IEditorInput input) {
		InputContext context = inputContextManager.getContext(input);
		context.validateEdit();
	}
	
	private IDialogSettings getSettingsSection() {
		// store the setting in dialog settings
		IDialogSettings root = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = root.getSection("multi-page-editor");
		if (section == null)
			section = root.addNewSection("multi-page-editor");
		return section;
	}
	
	public void gotoMarker(IMarker marker) {
		IResource resource = marker.getResource();
		InputContext context = inputContextManager.findContext(resource);
		if (context==null) return;
		IFormPage page = setActivePage(context.getId());
		IDE.gotoMarker(page, marker);
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
		if (key.equals(IPropertySheetPage.class)) {
			return getPropertySheet();
		}
		if (key.equals(IGotoMarker.class)) {
			return this;
		}
		return super.getAdapter(key);
	}
	
	public Menu getContextMenu() {
		return contextMenu;
	}
	
	public PDEMultiPageContentOutline getContentOutline() {
		if (contentOutline == null || contentOutline.isDisposed()) {
			contentOutline = new PDEMultiPageContentOutline();
			updateContentOutline(getActivePageInstance());
		}
		return contentOutline;
	}
	public PDEMultiPagePropertySheet getPropertySheet() {
		if (propertySheet == null || propertySheet.isDisposed()) {
			propertySheet = new PDEMultiPagePropertySheet();
			updatePropertySheet(getActivePageInstance());
		}
		return propertySheet;
	}

	protected IContentOutlinePage getFormOutline() {
		if (formOutline==null) {
			formOutline = createContentOutline();
		}
		return formOutline;
	}
	
	protected IContentOutlinePage createContentOutline() {
		return new FormOutlinePage(this);
	}

	private void updateContentOutline(IFormPage page) {
		if (contentOutline==null) return;
		IContentOutlinePage outline = null;
		if (page instanceof PDESourcePage) {
			outline = ((PDESourcePage)page).getContentOutline();
		}
		else {
			outline = getFormOutline();
			if (outline instanceof FormOutlinePage)
				((FormOutlinePage)outline).refresh();
		}
		contentOutline.setPageActive(outline);
	}
	
	protected IPropertySheetPage getPropertySheet(PDEFormPage page) {
		return ((PDEFormPage)page).getPropertySheetPage();
	}
	
	void updatePropertySheet(IFormPage page) {
		if (propertySheet==null) return;
		if (page instanceof PDEFormPage) {
			IPropertySheetPage propertySheetPage = getPropertySheet((PDEFormPage)page);
			if (propertySheetPage != null) {
				propertySheet.setPageActive(propertySheetPage);
			}
		} else
			propertySheet.setDefaultPageActive();
	}
	
	/* package */ IFormPage [] getPages() {
		return (IFormPage[])pages.toArray(new IFormPage[pages.size()]);
	}
	protected void performGlobalAction(String id) {
		// preserve selection
		ISelection selection = getSelection();
		boolean handled = ((PDEFormPage)getActivePageInstance()).performGlobalAction(id);
		if (!handled) {
			IFormPage page = getActivePageInstance();
			if (page instanceof PDEFormPage) {
				if (id.equals(ActionFactory.UNDO.getId())) {
					inputContextManager.undo();
					return;
				}
				if (id.equals(ActionFactory.REDO.getId())) {
					inputContextManager.redo();
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
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel == null || ssel.size() == 0)
			return;
		Object[] objects = ssel.toArray();
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
				((IWritable) obj).write("", pwriter);
			}
		}
		pwriter.flush();
		String textVersion = writer.toString();
		try {
			pwriter.close();
			writer.close();
		} catch (IOException e) {
		}
		// set the clipboard contents
		clipboard.setContents(
			new Object[] { objects, textVersion },
			new Transfer[] {
				ModelDataTransfer.getInstance(),
				TextTransfer.getInstance()});
	}

	public boolean canPasteFromClipboard() {
		IFormPage page = getActivePageInstance();
		if (page instanceof PDEFormPage) {
			return ((PDEFormPage)page).canPaste(getClipboard());
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
		IModelUndoManager undoManager = inputContextManager.getUndoManager();
		if (undoManager!=null)
			undoManager.setActions(undoAction, redoAction);
	}
}