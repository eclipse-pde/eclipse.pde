/*
 * Created on Oct 7, 2003
 */
package org.eclipse.pde.internal.ui.editor.standalone.extensions;

import org.eclipse.pde.internal.ui.editor.standalone.*;

/**
 * @author melhem
 */
public class ExtensionsSourceEditor extends StandaloneXMLEditor {

	/*
	 * (non-Javadoc) @see org.eclipse.pde.internal.ui.editor.standalone.AbstractXMLEditor#getOutlinePageContentProvider()
	 */
	protected XMLOutlinePageContentProvider2 getOutlinePageContentProvider() {
		return new ExtensionsContentProvider();
	}

	/*
	 * (non-Javadoc) @see org.eclipse.pde.internal.ui.editor.standalone.AbstractXMLEditor#getOutlinePageLabelProvider()
	 */
	protected XMLOutlinePageLabelProvider2 getOutlinePageLabelProvider() {
		return new ExtensionsLabelProvider();
	}
}
