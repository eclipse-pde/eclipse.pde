/*
 * Created on Oct 7, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.extensions;

import org.eclipse.pde.internal.ui.editor.standalone.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.w3c.dom.*;

/**
 * @author melhem
 */
public class ExtensionsContentProvider extends XMLOutlinePageContentProvider2 {
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.standalone.XMLOutlinePageContentProvider2#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		Object[] elements = getChildren(inputElement);
		for (int i = 0; i < elements.length; i++) {
			Node node = ((IDocumentNode)elements[i]).getContent();
			if (ExtensionsEditorUtil.isTopLevelNode(node))
				return new Object[] {elements[i]};
		}
		return super.getElements(inputElement);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.standalone.XMLOutlinePageContentProvider2#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof DocumentNode) {
			Node node = ((DocumentNode) parentElement).getContent();
			if (ExtensionsEditorUtil.isExtensionNode(node)
				|| ExtensionsEditorUtil.isExtensionPointNode(node))
				return new Object[0];
		}
		return super.getChildren(parentElement);
	}

}
