/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.context;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.pde.internal.ui.editor.text.XMLPartitionScanner;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.ui.IEditorInput;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public abstract class XMLInputContext extends UTF8InputContext {
	/**
	 * @param editor
	 * @param input
	 */
	public XMLInputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		super(editor, input, primary);
	}

	protected IDocumentPartitioner createDocumentPartitioner() {
		DefaultPartitioner partitioner = new DefaultPartitioner(
				new XMLPartitionScanner(), new String[]{
						XMLPartitionScanner.XML_TAG,
						XMLPartitionScanner.XML_COMMENT});
		return partitioner;
	}
}