/*
 * Created on Sep 20, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.site;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;

/**
 * @author melhem
 */
public class SiteMultiPageEditor extends MultiPageEditorPart {
	
	protected SiteSourcePage2 fSourcePage;
	protected EditorPart fFirstPage;
	protected DocumentModel fModel;
	
	class Site extends MultiPageEditorSite {
		public Site(MultiPageEditorPart multiPageEditor, IEditorPart editor) {
			super(multiPageEditor, editor);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.ui.part.MultiPageEditorSite#getActionBarContributor()
		 */
		public IEditorActionBarContributor getActionBarContributor() {
			return getEditorSite().getActionBarContributor();
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
	 */
	protected void createPages() {
		createFirstPage();
		createSourcePage();
	}
	
	private void createFirstPage() {
		try {
			fFirstPage = new SiteFirstPage(fModel);
			int index = addPage(fFirstPage, getEditorInput());
			setPageText(index, "First");
		} catch (PartInitException e) {
		}		
	}

	protected void createSourcePage() {
		try {
			fSourcePage = new SiteSourcePage2(fModel);
			int index = addPage(fSourcePage, getEditorInput());
			setPageText(index, "Source");
		} catch (PartInitException e) {
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		fSourcePage.doSave(monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {
		fSourcePage.doSaveAs();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorPart#gotoMarker(org.eclipse.core.resources.IMarker)
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(1);
		fSourcePage.gotoMarker(marker);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		try {
			createModel(getInputObject(input));
		} catch (Exception e) {
		}
	}
	
	private Object getInputObject(IEditorInput input) throws Exception {
		Object inputObject = null;
		if (input instanceof SystemFileEditorInput) {
			inputObject = input.getAdapter(File.class);
		} else if (input instanceof IFileEditorInput) {
			inputObject = input.getAdapter(IFile.class);
		} else if (input instanceof IStorageEditorInput) {
			inputObject = ((IStorageEditorInput) input).getStorage();
		}
		return inputObject;		
	}
	
	protected void createModel(Object input) throws Exception {
		fModel = new DocumentModel();
		if (input instanceof IFile) {
			fModel.load(((IFile)input).getContents());
		} else if (input instanceof IStorage) {
			fModel.load(((IStorage) input).getContents());
		} else if (input instanceof File) {
			fModel.load(new FileInputStream((File)input));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return fSourcePage.isSaveAsAllowed();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return fSourcePage.getAdapter(adapter);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createSite(org.eclipse.ui.IEditorPart)
	 */
	protected IEditorSite createSite(IEditorPart editor) {
		return new Site(this, editor);
	}
	
}
