/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class SchemaEditor extends MultiSourceEditor {
	private ShowDescriptionAction previewAction;
	
	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IFileEditorInput in = new FileEditorInput(file);
		manager.putContext(in, new SchemaInputContext(this, in, true));
	}
	
	protected InputContextManager createInputContextManager() {
		SchemaInputContextManager contextManager = new SchemaInputContextManager();
		//contextManager.setUndoManager(new SchemaUndoManager(this));
		return contextManager;
	}
	
	public boolean canCopy(ISelection selection) {
		return true;
	}	
	
	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = getClipboard().getAvailableTypes();
			Transfer[] transfers =
				new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
			for (int i = 0; i < types.length; i++) {
				for (int j = 0; j < transfers.length; j++) {
					if (transfers[j].isSupportedType(types[i]))
						return true;
				}
			}
		} catch (SWTError e) {
		}
		return false;
	}

	public void monitoredFileAdded(IFile file) {
		/*
		String name = file.getName();
		if (name.equalsIgnoreCase("site.xml")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new SiteInputContext(this, in, false));
		}
		*/
	}

	public boolean monitoredFileRemoved(IFile file) {
		/*
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		 * */
		return true;
	}

	public void contextAdded(InputContext context) {
		addSourcePage(context.getId());
	}
	public void contextRemoved(InputContext context) {
		IFormPage page = findPage(context.getId());
		if (page!=null)
			removePage(context.getId());
	}

	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		manager.putContext(input, new SchemaInputContext(this, input,
					true));
	}

	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		manager.putContext(input,
							new SchemaInputContext(this, input, true));
	}
	
	protected void contextMenuAboutToShow(IMenuManager manager) {
		super.contextMenuAboutToShow(manager);
	}
	
	void previewReferenceDocument() {
		ISchema schema = (ISchema) getAggregateModel();
		if (previewAction==null)
			previewAction = new ShowDescriptionAction(schema);
		else
			previewAction.setSchema(schema);
		previewAction.run();
	}	

	protected void addPages() {
		try {
			addPage(new SchemaFormPage(this));
			addPage(new SchemaDocPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(SchemaInputContext.CONTEXT_ID);
	}


	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = inputContextManager.getPrimaryContext();
			if (primary.getId().equals(SchemaInputContext.CONTEXT_ID))
				firstPageId = SchemaFormPage.PAGE_ID;
			if (firstPageId == null)
				firstPageId = SchemaFormPage.PAGE_ID;
		}
		return firstPageId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new SchemaSourcePage(editor, title, name);
	}
	
	protected IContentOutlinePage createContentOutline() {
		return new SchemaFormOutlinePage(this);
	}
}