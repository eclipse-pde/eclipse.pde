/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public abstract class MultiSourceEditor extends PDEFormEditor {
	protected void addSourcePage(String contextId) {
		InputContext context = inputContextManager.findContext(contextId);
		if (context == null)
			return;
		PDESourcePage sourcePage;
		if (context instanceof XMLInputContext)
			sourcePage = createXMLSourcePage(this, contextId, context.getInput().getName());
		else
			sourcePage = new PDESourcePage(this, contextId, context.getInput().getName());
		sourcePage.setInputContext(context);
		try {
			addPage(sourcePage, context.getInput());
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected void removePage(String pageId) {
		IFormPage page = findPage(pageId);
		if (page == null)
			return;
		if (page.isDirty()) {
			// need to ask the user about this
		} else {
			removePage(page.getIndex());
			if (!page.isEditor())
				page.dispose();
		}
	}
	
	protected abstract PDESourcePage createXMLSourcePage(PDEFormEditor editor, String title, String name);
}