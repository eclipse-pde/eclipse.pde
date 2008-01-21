/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.toc;

import java.util.HashSet;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.ui.util.XMLRootElementMatcher;

public class HelpEditorUtil {
	public static final String[] pageExtensions = {"htm","shtml","html","xhtml"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	public static final String tocExtension = "xml"; //$NON-NLS-1$
	private static HashSet pageExtensionSet = new HashSet(pageExtensions.length);

	private static void populateHashSet()
	{	for(int i = 0; i < pageExtensions.length; ++i)
		{	pageExtensionSet.add(pageExtensions[i]);
		}
	}
	
	public static boolean hasValidPageExtension(IPath path)
	{	String fileExtension = path.getFileExtension();	
		if(fileExtension != null)
		{	fileExtension = fileExtension.toLowerCase(Locale.ENGLISH);
			if(pageExtensionSet.isEmpty())
			{	populateHashSet();
			}
			
			return pageExtensionSet.contains(fileExtension);
		}

		return false;
	}

	private static boolean hasValidTocExtension(IPath path)
	{	String fileExtension = path.getFileExtension();
		return fileExtension != null && fileExtension.equals(tocExtension); 
	}

	/**
	 * @param file
	 */
	public static boolean isTOCFile(IPath path) {
		if(!hasValidTocExtension(path))
			return false;
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		IResource resource = root.findMember(path);
		if(resource != null && resource instanceof IFile)
		{	return XMLRootElementMatcher.fileMatchesElement((IFile)resource, ITocConstants.ELEMENT_TOC);
		}

		return XMLRootElementMatcher.fileMatchesElement(path.toFile(), ITocConstants.ELEMENT_TOC);
	}

	public static boolean isCurrentResource(IPath path, IBaseModel model)
	{	if(model instanceof IModel)
		{	IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
			IPath fullPath;	

			if(workspacePath.isPrefixOf(path))
			{	fullPath = ((IModel)model).getUnderlyingResource().getLocation();	
			}
			else
			{	fullPath = ((IModel)model).getUnderlyingResource().getFullPath();
			}

			return fullPath.equals(path);
		}

		return false;
	}

	public static String getPageExtensionList() {
		StringBuffer buf = new StringBuffer();

		for(int i = 0; i < pageExtensions.length; ++i)
		{	buf.append('.');
			buf.append(pageExtensions[i]);
			if(i != pageExtensions.length - 1)
			{	buf.append(", "); //$NON-NLS-1$
			}
		}

		return buf.toString();
	}
}
