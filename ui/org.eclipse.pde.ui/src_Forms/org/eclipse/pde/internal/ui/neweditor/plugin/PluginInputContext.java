/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.model.plugin.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;

public class PluginInputContext extends XMLInputContext {
	public static final String CONTEXT_ID = "plugin-context";
	private boolean fIsFragment;
	private HashMap fOperationTable = new HashMap();
	/**
	 * @param editor
	 * @param input
	 */
	public PluginInputContext(PDEFormEditor editor, IEditorInput input, boolean primary, boolean isFragment) {
		super(editor, input, primary);
		fIsFragment = isFragment;
		create();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IModel createModel(IEditorInput input) throws CoreException {
		//boolean hasBundle = getEditor().getContextManager().hasContext(BundleInputContext.CONTEXT_ID);
		PluginModelBase model = null;
		if (input instanceof IStorageEditorInput) {
			boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);
			if (fIsFragment) {
				model = new FragmentModel(document, isReconciling);
			} else {
				model = new PluginModel(document, isReconciling);
			}
			if (input instanceof IFileEditorInput) {
				model.setUnderlyingResource(((IFileEditorInput)input).getFile());
			} else {
				model.setInstallLocation(((File)input.getAdapter(File.class)).getParent());
			}
			model.load();
		}
		return model;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}
	public boolean isFragment() {
		return fIsFragment;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects != null && objects.length > 0) {
			Object object = objects[0];
			if (object instanceof IDocumentAttribute) {
				addEditAttributeOperation(ops, (IDocumentAttribute) object, event);
			}
		}
	}
	
	protected void addEditAttributeOperation(ArrayList ops, IDocumentAttribute attr, IModelChangedEvent event) {
		int offset = attr.getValueOffset();
		int length = attr.getValueLength();
		Object newValue = event.getNewValue();
		TextEdit op = null;
		if (offset > -1) {
			if (newValue == null || newValue.toString().length() == 0) {
				length = attr.getValueOffset() + attr.getValueLength() + 1 - attr.getNameOffset();
				op = getDeleteEditOperation(attr.getNameOffset(), length);
			} else {
				op = new ReplaceEdit(offset, length, getWritableString(event.getNewValue().toString()));
			}
		} else {
			IDocumentNode node = attr.getEnclosingElement();
			offset = node.getOffset() + node.getXMLTagName().length() + 1;
			op = new InsertEdit(offset, " " + attr.getAttributeName() + "=\"" + getWritableString(attr.getAttributeValue()) + "\"");			
		}
		if (op != null) {
			TextEdit oldOp = (TextEdit)fOperationTable.get(attr);
			if (oldOp != null)
				ops.remove(oldOp);
			ops.add(op);
			fOperationTable.put(attr, op);
		}
	}
	
	private DeleteEdit getDeleteEditOperation(int offset, int length) {
		try {
			IDocument doc = getDocumentProvider().getDocument(getInput());
			for (;;) {
				char ch = doc.get(offset + length, 1).toCharArray()[0];
				if (!Character.isWhitespace(ch))
					break;
				length += 1;
			}
		} catch (BadLocationException e) {
		}
		return new DeleteEdit(offset, length);		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.context.InputContext#flushModel(org.eclipse.jface.text.IDocument)
	 */
	protected void flushModel(IDocument doc) {
		super.flushModel(doc);
		fOperationTable.clear();
	}
	
	public String getWritableString(String source) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}


}