/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.use;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.api.tools.ui.internal.use.ApiUsePatternTab.Pattern;

/**
 * Abstract page that describes a page for creating {@link Pattern}s
 * 
 * @since 1.0.1
 */
public abstract class UsePatternPage extends WizardPage {

	boolean dirty = false;
	
	/**
	 * Constructor
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	protected UsePatternPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	protected boolean pageDirty() {
		return this.dirty;
	}
	
	protected void setDirty() {
		this.dirty = true;
	}
	
	protected void resetMessage(boolean isediting) {
		//do nothing by default
	}
	
	/**
	 * @return the kind of the pattern
	 * @see Pattern for pattern kinds
	 */
	public abstract int getKind();
	
	/**
	 * @return the pattern itself
	 */
	public abstract String getPattern();
}
