/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.builders;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.internal.base.model.plugin.*;

public abstract class PluginBaseParser extends AbstractParser {
	
	public PluginBaseParser(IPluginModelBase model) {
		super(model);
	}
	
	public IPluginModelBase getPluginModelBase() {
		return (IPluginModelBase)getModel();
	}

	/*
	 * @see AbstractParser#canAcceptText(int)
	 */
	protected boolean canAcceptText(int state) {
		return false;
	}

	/*
	 * @see AbstractParser#acceptText(String)
	 */
	protected void acceptText(String text) {
	}

	/*
	 * @see AbstractParser#handleErrorStatus(IStatus)
	 */
	protected void handleErrorStatus(IStatus status) {
	}

	/*
	 * @see AbstractParser#handleEndState(int, String)
	 */
	protected void handleEndState(int state, String elementName) {
	}
}
