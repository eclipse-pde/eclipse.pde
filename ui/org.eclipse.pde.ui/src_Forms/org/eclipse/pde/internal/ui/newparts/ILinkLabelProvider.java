/*
 * Created on Feb 25, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.newparts;

import org.eclipse.jface.viewers.ILabelProvider;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface ILinkLabelProvider extends ILabelProvider {
	String getStatusText(Object object);
	String getToolTipText(Object object);
}
