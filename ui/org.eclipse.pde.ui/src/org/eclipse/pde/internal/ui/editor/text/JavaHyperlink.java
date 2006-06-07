/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;

public class JavaHyperlink implements IHyperlink {

	private IRegion fRegion;
	private IProject fProject;
	private String fClazz;

	public JavaHyperlink(IRegion region, IProject project, String clazz) {
		fRegion = region;
		fProject = project;
		fClazz = clazz;
	}

	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	public String getHyperlinkText() {
		return null;
	}

	public String getTypeLabel() {
		return null;
	}

	public void open() {
		try {
			if (fProject.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(fProject);
				IJavaElement result = javaProject.findType(fClazz);
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
