/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.*;
public class PackageLabelProvider extends JavaElementLabelProvider {
	private boolean showParentName;
	/**
	 * 
	 * @param dialogFlags the initial options; a bitwise OR of <code>SHOW_* </code> constants
	 * @param showParentName true if source folder name should be used to identify package
	 */
	public PackageLabelProvider(int dialogFlags, boolean showParentName) {
		super(dialogFlags);
		this.showParentName = showParentName;
	}
	public String getText(Object element) {
		StringBuffer buffer = new StringBuffer(super.getText(element));
		if (element instanceof IPackageFragment && showParentName){
			buffer.append(" (");
			buffer.append(((IPackageFragment) element).getParent()
					.getElementName());
			buffer.append("/");
			buffer.append(")");
		}
		return buffer.toString();
	}
}