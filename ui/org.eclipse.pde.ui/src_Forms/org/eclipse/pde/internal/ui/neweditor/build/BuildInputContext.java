/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.build;

import java.io.File;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.pde.internal.ui.neweditor.context.InputContext;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BuildInputContext extends InputContext {
	public static final String CONTEXT_ID = "build-context";
	/**
	 * @param editor
	 * @param input
	 */
	public BuildInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
		create();
	}

	protected IDocumentProvider createDocumentProvider(IEditorInput input) {
		IDocumentProvider documentProvider = null;
		if (input instanceof IFileEditorInput)
			documentProvider = new FileDocumentProvider() {
			public IDocument createDocument(Object element)
				throws CoreException {
				IDocument document = super.createDocument(element);
				if (document != null) {
					IDocumentPartitioner partitioner =
						createDocumentPartitioner();
					if (partitioner != null) {
						partitioner.connect(document);
						document.setDocumentPartitioner(partitioner);
					}
				}
				return document;
			}
		};
		else if (input instanceof SystemFileEditorInput) {
			documentProvider =
				new SystemFileDocumentProvider(createDocumentPartitioner());
		} else if (input instanceof IStorageEditorInput) {
			documentProvider =
				new StorageDocumentProvider(createDocumentPartitioner());
		}
		return documentProvider;
	}
	protected IDocumentPartitioner createDocumentPartitioner() {
		return null;
	}

	protected IModel createModel(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			return createWorkspaceModel((IFileEditorInput)input);
		}
		if (input instanceof SystemFileEditorInput) {
			return createExternalModel((SystemFileEditorInput)input);
			
		}
		if (input instanceof IStorageEditorInput) {
			return createStorageModel((IStorageEditorInput)input);
		}
		return null;
	}
	private IModel createWorkspaceModel(IFileEditorInput input) {
		WorkspaceBuildModel model = new WorkspaceBuildModel(input.getFile());
		model.load();
		return model;
	}
	private IModel createExternalModel(SystemFileEditorInput input) {
		File file = (File)input.getAdapter(File.class);
		String location = file.getParentFile().getPath();
		ExternalBuildModel model = new ExternalBuildModel(location);
		model.load();
		return model;
	}
	private IModel createStorageModel(IStorageEditorInput input) {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}

	protected void flushModel(IDocument doc) {
	}
	
	protected boolean synchronizeModel(IDocument doc) {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
	}
}
