/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import java.util.Enumeration;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.PartInitException;


/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class MultiSourceEditor extends PDEFormEditor {
	protected InputContext findContext(String id) {
		for (Enumeration enum=inputContexts.elements(); enum.hasMoreElements();) {
			InputContext context = (InputContext)enum.nextElement();
			if (context.getId().equals(id))
				return context;
		}
		return null;
	}
	protected InputContext getPrimaryContext() {
		for (Enumeration enum=inputContexts.elements(); enum.hasMoreElements();) {
			InputContext context = (InputContext)enum.nextElement();
			if (context.isPrimary())
				return context;
		}
		return null;
	}
	
	protected void addSourcePage(String contextId) {
		InputContext context = findContext(contextId);
		if (context == null)
			return;
		PDESourcePage sourcePage;
		if (context instanceof XMLInputContext)
			sourcePage = new XMLSourcePage(this, contextId, context.getInput()
					.getName());
		else
			sourcePage = new PDESourcePage(this, contextId, context.getInput()
					.getName());
		sourcePage.setInputContext(context);
		try {
			addPage(sourcePage, context.getInput());
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
}