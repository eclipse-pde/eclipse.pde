/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.pde.internal.ui.editor.text.PDEPartitionScanner;
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
	public XMLInputContext(PDEFormEditor editor, IEditorInput input) {
		super(editor, input);
	}

	protected IDocumentPartitioner createDocumentPartitioner() {
		DefaultPartitioner partitioner = new DefaultPartitioner(
				new PDEPartitionScanner(), new String[]{
						PDEPartitionScanner.XML_TAG,
						PDEPartitionScanner.XML_COMMENT});
		return partitioner;
	}
}