/*
 * Created on Sep 12, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.standalone.manifest;

import org.eclipse.pde.internal.ui.editor.standalone.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.w3c.dom.*;

/**
 * @author melhem
 */
public abstract class BaseSourceContentProvider extends XMLOutlinePageContentProvider2 {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageContentProvider2#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		Object[] elements = getChildren(inputElement);
		for (int i = 0; i < elements.length; i++) {
			Node node = ((IDocumentNode)elements[i]).getContent();
			if (isValidTopLevelNode(node))
				return new Object[] {elements[i]};
		}
		return super.getElements(inputElement);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageContentProvider2#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof DocumentNode) {
			Node node = ((DocumentNode) parentElement).getContent();
			if (ManifestEditorUtil.isLibraryNode(node)
				|| ManifestEditorUtil.isExtensionNode(node)
				|| ManifestEditorUtil.isExtensionPointNode(node))
				return new Object[0];
		}
		return super.getChildren(parentElement);
	}
	
	abstract protected boolean isValidTopLevelNode(Node node);

}
