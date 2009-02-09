/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.toc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ua.ui.IConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;

public class HelpEditorUtil {
	public static final String[] pageExtensions = {
			"htm", "shtml", "html", "xhtml" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	public static final String tocExtension = "xml"; //$NON-NLS-1$
	private static HashSet pageExtensionSet = new HashSet(pageExtensions.length);

	private static void populateHashSet() {
		for (int i = 0; i < pageExtensions.length; ++i) {
			pageExtensionSet.add(pageExtensions[i]);
		}
	}

	public static boolean hasValidPageExtension(IPath path) {
		String fileExtension = path.getFileExtension();
		if (fileExtension != null) {
			fileExtension = fileExtension.toLowerCase(Locale.ENGLISH);
			if (pageExtensionSet.isEmpty()) {
				populateHashSet();
			}

			return pageExtensionSet.contains(fileExtension);
		}

		return false;
	}

	private static boolean hasValidTocExtension(IPath path) {
		String fileExtension = path.getFileExtension();
		return fileExtension != null && fileExtension.equals(tocExtension);
	}

	/**
	 * @param file
	 */
	public static boolean isTOCFile(IPath path) {
		if (!hasValidTocExtension(path))
			return false;

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		IResource resource = root.findMember(path);
		if (resource != null && resource instanceof IFile) {
			try {
				IFile file = (IFile) resource;
				IContentDescription description = file.getContentDescription();
				IContentType type = description.getContentType();
				return type.getId().equalsIgnoreCase(
						IConstants.TABLE_OF_CONTENTS_CONTENT_ID);
			} catch (CoreException e) {
				PDEUserAssistanceUIPlugin.logException(e);
			}
		} else {
			File file = path.toFile();
			IContentTypeManager manager = Platform.getContentTypeManager();
			try {
				InputStream stream = new FileInputStream(file);
				IContentType type = manager.findContentTypeFor(stream, file
						.getName());
				return type.getId().equalsIgnoreCase(
						IConstants.TABLE_OF_CONTENTS_CONTENT_ID);
			} catch (IOException e) {
				PDEUserAssistanceUIPlugin.logException(e);
			}
		}
		return false;
	}

	public static boolean isCurrentResource(IPath path, IBaseModel model) {
		if (model instanceof IModel) {
			IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot()
					.getLocation();
			IPath fullPath;

			if (workspacePath.isPrefixOf(path)) {
				fullPath = ((IModel) model).getUnderlyingResource()
						.getLocation();
			} else {
				fullPath = ((IModel) model).getUnderlyingResource()
						.getFullPath();
			}

			return fullPath.equals(path);
		}

		return false;
	}

	public static String getPageExtensionList() {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i < pageExtensions.length; ++i) {
			buf.append('.');
			buf.append(pageExtensions[i]);
			if (i != pageExtensions.length - 1) {
				buf.append(", "); //$NON-NLS-1$
			}
		}

		return buf.toString();
	}
}
