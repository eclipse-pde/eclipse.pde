/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

public class JavaHyperlink extends AbstractHyperlink {

	private IResource fResource;

	public JavaHyperlink(IRegion region, String clazz, IResource res) {
		super(region, clazz);
		fResource = res;
	}

	public void open() {
		try {
			if (fResource == null)
				return;
			if (fResource.getProject().hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(fResource.getProject());
				IJavaElement result = javaProject.findType(fElement);
				if (result != null)
					JavaUI.openInEditor(result);
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		} catch (JavaModelException e) {
			Display.getCurrent().beep(); // just for Dejan
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

}
