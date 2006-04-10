/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.xhtml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.builders.ValidatingSAXParser;
import org.eclipse.pde.internal.core.builders.XMLErrorReporter;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetUnconvertedOperation implements IRunnableWithProgress {

	private static final String F_TOC_EXTENSION = "org.eclipse.help.toc"; //$NON-NLS-1$
	private static final String F_HTML_CONTENT = "org.eclipse.help.html"; //$NON-NLS-1$
	
	private IFile fBaseFile;
	private TocReplaceTable fReplaceTable = new TocReplaceTable();
	private IContentTypeManager fContentManager = Platform.getContentTypeManager();
	
	public GetUnconvertedOperation(ISelection selection) {
		Object object = ((IStructuredSelection)selection).getFirstElement();
		if (object instanceof IProject) {
			fBaseFile = ((IProject)object).getFile("plugin.xml"); //$NON-NLS-1$
			if (!fBaseFile.exists())
				fBaseFile = ((IProject)object).getFile("fragment.xml"); //$NON-NLS-1$
		}
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		if (fBaseFile == null || !fBaseFile.exists())
			return;
		ModelModification mod = new ModelModification(fBaseFile) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				inspectModel(monitor, model);
			}
		};
		try {
			PDEModelUtility.modifyModel(mod, monitor);
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	private void inspectModel(IProgressMonitor monitor, IBaseModel baseModel) throws CoreException {
		if (!(baseModel instanceof IPluginModelBase))
			return;
		
		IPluginModelBase pluginModel = (IPluginModelBase)baseModel;
		ArrayList tocs = grabTocFiles(monitor, pluginModel);
		if (tocs.size() == 0)
			return;
		
		for (int i = 0; i < tocs.size(); i++) {
			IFile file = (IFile)tocs.get(i);
			XMLErrorReporter xml = new XMLErrorReporter(file);
			ValidatingSAXParser.parse(file, xml);
			Element root = xml.getDocumentRoot();
			if (root != null)
				checkXML(root, file);
		}
	}

	private void checkXML(Element root, IFile file) {
		inspectElement(root, file);
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) instanceof Element)
				checkXML((Element)children.item(i), file);
	}
	
	private void inspectElement(Element element, IFile file) {
		IProject project = file.getProject();
		String href = null;
		String nodeName = element.getNodeName();
		if (nodeName.equals("topic")) //$NON-NLS-1$
			href = element.getAttribute("href"); //$NON-NLS-1$
		else if (nodeName.equals("toc")) //$NON-NLS-1$
			href = element.getAttribute("topic"); //$NON-NLS-1$
		if (href == null || href.length() == 0)
			return;
		IFile htmlFile = project.getFile(href);
		inspectLocationForHTML(htmlFile, element.getAttribute("label"), file); //$NON-NLS-1$
	}
	
	private void scanFolder(IFolder parent, IResource root) {
		if (parent == null)
			return;
		try {
			IResource[] members = parent.members();
			for (int i = 0; i < members.length; i++) {
				if (members[i] instanceof IFolder)
					scanFolder((IFolder)members[i], root);
				else if (members[i] instanceof IFile) {
					IFile file = (IFile)members[i];
					String ext = file.getFileExtension();
					if (ext.equalsIgnoreCase("html") || ext.equalsIgnoreCase("htm")) //$NON-NLS-1$ //$NON-NLS-2$
						inspectLocationForHTML(file, null, root);
				}
			}
		} catch (CoreException e) {
		}
	}
	
	private void inspectLocationForHTML(IFile htmlFile, String label, IResource tocFile) {
		if (!htmlFile.exists())
			return;
		InputStream stream = null;
		try {
			stream = htmlFile.getContents();
			IContentType type = fContentManager.findContentTypeFor(stream, htmlFile.getName());
			if (type != null && type.getId().equals(F_HTML_CONTENT))
				fReplaceTable.addToTable(htmlFile.getProjectRelativePath().toString(), label, tocFile); //$NON-NLS-1$
		} catch (CoreException e) {
			PDEPlugin.log(e);
		} catch (IOException e) {
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	private ArrayList grabTocFiles(IProgressMonitor monitor, IPluginModelBase pluginModel) {
		IPluginExtension[] extensions = pluginModel.getPluginBase().getExtensions();
		ArrayList tocLocations = new ArrayList();
		for (int i = 0; i < extensions.length && !monitor.isCanceled(); i++) {
			if (extensions[i].getPoint().equals(F_TOC_EXTENSION)) {
				IPluginObject[] children = extensions[i].getChildren();
				for (int j = 0; j < children.length && !monitor.isCanceled(); j++) {
					if (children[j].getName().equals("toc") //$NON-NLS-1$
							&& children[j] instanceof IPluginElement) {
						IPluginElement element = (IPluginElement) children[j];
						IPluginAttribute fileAttrib = element.getAttribute("file"); //$NON-NLS-1$
						IProject project = fBaseFile.getProject();
						if (fileAttrib != null) {
							String location = fileAttrib.getValue();
							IFile file = project.getFile(location);
							if (file != null && file.exists()) {
								tocLocations.add(file);
								IPluginAttribute extraDir = element.getAttribute("extradir"); //$NON-NLS-1$
								if (extraDir != null) {
									IFolder folder = project.getFolder(extraDir.getValue());
									scanFolder(folder, folder);
								}
							}
						}
					}
				}
			}
		}
		return tocLocations;
	}

	public boolean needsWork() {
		return fReplaceTable.numEntries() > 0;
	}

	public TocReplaceTable getChanges() {
		return fReplaceTable;
	}
}
