/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DefaultRuntimeUISupport
	implements IAlternativeRuntimeUISupport {
	private PDELabelProvider labelProvider;
	
	public PDELabelProvider getLabelProvider() {
		if (labelProvider==null)
			labelProvider = new PDELabelProvider();
		return labelProvider;
	}
	public void shutdown() {
		if (labelProvider!=null) {
			labelProvider.dispose();
			labelProvider=null;
		}
	}

}
