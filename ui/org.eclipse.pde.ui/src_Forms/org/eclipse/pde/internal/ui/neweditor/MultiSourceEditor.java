/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import java.util.Enumeration;


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
}