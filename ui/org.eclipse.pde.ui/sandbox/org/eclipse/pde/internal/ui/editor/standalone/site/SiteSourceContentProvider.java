/*
 * Created on Sep 14, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.site;

import org.eclipse.pde.internal.ui.editor.standalone.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.w3c.dom.*;

/**
 * @author melhem
 */
public class SiteSourceContentProvider extends XMLOutlinePageContentProvider2 {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.XMLOutlinePageContentProvider2#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		Object[] elements = getChildren(inputElement);
		for (int i = 0; i < elements.length; i++) {
			Node node = ((IDocumentNode)elements[i]).getContent();
			if (SiteEditorUtil.isSiteNode(node))
				return new Object[] {elements[i]};
		}
		return super.getElements(inputElement);
	}

}
