/*
 * Created on Jan 27, 2004
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.site;
import java.io.*;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.pde.internal.ui.neweditor.context.XMLInputContext;
import org.eclipse.ui.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class SiteInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "site-context";
	private boolean storageModel=false;
	/**
	 * @param editor
	 * @param input
	 */
	public SiteInputContext(PDEFormEditor editor, IEditorInput input,
			boolean primary) {
		super(editor, input, primary);
		create();
	}

	protected IBaseModel createModel(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			return createWorkspaceModel((IFileEditorInput) input);
		}
		if (input instanceof SystemFileEditorInput) {
			return createExternalModel((SystemFileEditorInput) input);
		}
		if (input instanceof IStorageEditorInput) {
			return createStorageModel((IStorageEditorInput) input);
		}
		return null;
	}
	private IBaseModel createWorkspaceModel(IFileEditorInput input) {
		InputStream stream = null;
		IFile file = input.getFile();
		try {
			stream = file.getContents(false);
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
			return null;
		}
		ISiteModel model = new WorkspaceSiteModel(file);
		boolean cleanModel = true;
		try {
			model.load(stream, false);
		} catch (CoreException e) {
			cleanModel = false;
		}
		IPath buildPath = file.getProject().getFullPath().append(
				PDECore.SITEBUILD_DIR).append(PDECore.SITEBUILD_PROPERTIES);
		IFile buildFile = file.getWorkspace().getRoot().getFile(buildPath);
		ISiteBuildModel buildModel = new WorkspaceSiteBuildModel(buildFile);
		try {
			buildModel.load();
		} catch (CoreException e) {
		}
		model.setBuildModel(buildModel);
		try {
			stream.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		return model;
	}
	public void dispose() {
		ISiteModel model = (ISiteModel) getModel();
		ISiteBuildModel buildModel = model.getBuildModel();
		if (storageModel) {
			model.dispose();
			if (buildModel != null)
				buildModel.dispose();
		}
		super.dispose();
	}
	private IBaseModel createExternalModel(SystemFileEditorInput input) {
		return null;
	}
	private IBaseModel createStorageModel(IStorageEditorInput input) {
		return null;
	}
	protected void flushModel(IDocument doc) {
		// if model is dirty, flush its content into
		// the document so that the source editor will
		// pick up the changes.
		if (!(getModel() instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) getModel();
		if (editableModel.isDirty() == false)
			return;
		try {
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			editableModel.save(writer);
			writer.flush();
			swriter.close();
			doc.set(swriter.toString());
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected boolean synchronizeModel(IDocument doc) {
		ISiteModel model = (ISiteModel) getModel();
		boolean cleanModel = true;
		String text = doc.get();
		try {
			InputStream stream =
				new ByteArrayInputStream(text.getBytes("UTF8"));
			try {
				model.reload(stream, false);
			} catch (CoreException e) {
				cleanModel = false;
			}
			try {
				stream.close();
			} catch (IOException e) {
			}
		} catch (UnsupportedEncodingException e) {
			PDEPlugin.logException(e);
		}
		return cleanModel;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList,
	 *      org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.XMLInputContext#reorderInsertEdits(java.util.ArrayList)
	 */
	protected void reorderInsertEdits(ArrayList ops) {
	}
}