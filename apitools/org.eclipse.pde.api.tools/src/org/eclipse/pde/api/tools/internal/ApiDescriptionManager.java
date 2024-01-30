/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode;
import org.eclipse.pde.api.tools.internal.ProjectApiDescription.TypeNode;
import org.eclipse.pde.api.tools.internal.model.ApiModelCache;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.scanner.ScannerMessages;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Manages a cache of API descriptions for Java projects. Descriptions are
 * re-used between API components for the same project.
 *
 * @since 1.0
 */
public final class ApiDescriptionManager implements ISaveParticipant {

	/**
	 * Singleton
	 */
	private static ApiDescriptionManager fgDefault;

	/**
	 * Maps Java projects to API descriptions
	 */
	private final Map<IJavaProject, IApiDescription> fDescriptions = new HashMap<>();

	/**
	 * Path to the local directory where API descriptions are cached per
	 * project.
	 */
	public static final IPath API_DESCRIPTIONS_CONTAINER_PATH = ApiPlugin.getDefault().getStateLocation();

	/**
	 * Constructs an API description manager.
	 */
	private ApiDescriptionManager() {
		ApiPlugin.getDefault().addSaveParticipant(this);
	}

	/**
	 * Cleans up Java element listener
	 */
	public static void shutdown() {
		if (fgDefault != null) {
			ApiPlugin.getDefault().removeSaveParticipant(fgDefault);
		}
	}

	/**
	 * Returns the singleton API description manager.
	 *
	 * @return API description manager
	 */
	public synchronized static ApiDescriptionManager getManager() {
		if (fgDefault == null) {
			fgDefault = new ApiDescriptionManager();
		}
		return fgDefault;
	}

	/**
	 * Returns an API description for the given project component and connect it to
	 * the given bundle description.
	 *
	 * @param component Java project
	 * @param bundle
	 *
	 * @return API description
	 */
	public synchronized IApiDescription getApiDescription(ProjectComponent component, BundleDescription bundle) {
		IJavaProject project = component.getJavaProject();
		ProjectApiDescription description = (ProjectApiDescription) fDescriptions.get(project);
		if (description == null) {
			if (Util.isApiProject(project)) {
				description = new ProjectApiDescription(project);
			} else {
				description = new NonApiProjectDescription(project);
			}
			try {
				restoreDescription(project, description);
			} catch (CoreException e) {
				ApiPlugin.log(e.getStatus());
				description = new ProjectApiDescription(project);
			}
			fDescriptions.put(project, description);
		}
		return description;
	}

	/**
	 * Cleans the API description for the given project.
	 *
	 * @param delete whether to delete the file on disk
	 * @param remove whether to remove the cached API description
	 */
	public synchronized void clean(IJavaProject project, boolean delete, boolean remove) {
		ProjectApiDescription desc = null;
		if (remove) {
			desc = (ProjectApiDescription) fDescriptions.remove(project);
		} else {
			desc = (ProjectApiDescription) fDescriptions.get(project);
		}
		if (desc != null) {
			desc.clean();
		}
		if (delete) {
			File file = API_DESCRIPTIONS_CONTAINER_PATH.append(project.getElementName()).append(IApiCoreConstants.API_DESCRIPTION_XML_NAME).toFile();
			if (file.exists()) {
				file.delete();
			}
			file = API_DESCRIPTIONS_CONTAINER_PATH.append(project.getElementName()).toFile();
			if (file.exists() && file.isDirectory()) {
				file.delete();
			}
		}
	}

	/**
	 * Notifies the API description that the underlying project has changed.
	 */
	synchronized void projectChanged(IJavaProject project) {
		ProjectApiDescription desc = (ProjectApiDescription) fDescriptions.get(project);
		if (desc != null) {
			desc.projectChanged();
		}
	}

	/**
	 * Notifies the API description that the underlying project classpath has
	 * changed.
	 */
	synchronized void projectClasspathChanged(IJavaProject project) {
		ProjectApiDescription desc = (ProjectApiDescription) fDescriptions.get(project);
		if (desc != null) {
			desc.projectClasspathChanged();
		}
	}

	/**
	 * Flushes the changed element from the model cache
	 */
	void flushElementCache(IJavaElement element) {
		switch (element.getElementType()) {
			case IJavaElement.COMPILATION_UNIT -> {
				ICompilationUnit unit = (ICompilationUnit) element;
				IType type = unit.findPrimaryType();
				if (type != null) {
					ApiModelCache.getCache().removeElementInfo(ApiBaselineManager.WORKSPACE_API_BASELINE_ID, element.getJavaProject().getElementName(), type.getFullyQualifiedName(), IApiElement.TYPE);
				}
			}
			case IJavaElement.JAVA_PROJECT -> {
				ApiModelCache.getCache().removeElementInfo(ApiBaselineManager.WORKSPACE_API_BASELINE_ID, element.getElementName(), null, IApiElement.COMPONENT);
			}
			default -> { /**/ }
		}
	}

	@Override
	public void doneSaving(ISaveContext context) {
		//
	}

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {
		//
	}

	@Override
	public void rollback(ISaveContext context) {
		//
	}

	@Override
	public synchronized void saving(ISaveContext context) throws CoreException {
		if (context.getKind() == ISaveContext.PROJECT_SAVE && !Util.isJavaProject(context.getProject())) {
			return;
		}

		for (Entry<IJavaProject, IApiDescription> entry : fDescriptions.entrySet()) {
			IJavaProject project = entry.getKey();
			ProjectApiDescription desc = (ProjectApiDescription) entry.getValue();
			if (desc.isModified()) {
				File dir = API_DESCRIPTIONS_CONTAINER_PATH.append(project.getElementName()).toFile();
				dir.mkdirs();
				String xml = desc.getXML();
				try {
					Util.saveFile(new File(dir, IApiCoreConstants.API_DESCRIPTION_XML_NAME), xml);
					desc.setModified(false);
				} catch (IOException e) {
					abort(MessageFormat.format(ScannerMessages.ApiDescriptionManager_0, project.getElementName()), e);
				}
			}
		}
	}

	/**
	 * Restores the API description from its saved file, if any and returns true
	 * if successful.
	 *
	 * @return whether the restore succeeded
	 */
	private boolean restoreDescription(IJavaProject project, ProjectApiDescription description) throws CoreException {
		File file = API_DESCRIPTIONS_CONTAINER_PATH.append(project.getElementName()).append(IApiCoreConstants.API_DESCRIPTION_XML_NAME).toFile();
		if (file.exists()) {
			try {
				String xml = Files.readString(file.toPath());
				Element root = Util.parseDocument(xml);
				if (!root.getNodeName().equals(IApiXmlConstants.ELEMENT_COMPONENT)) {
					abort(ScannerMessages.ComponentXMLScanner_0, null);
				}
				long timestamp = getLong(root, IApiXmlConstants.ATTR_MODIFICATION_STAMP);
				String version = root.getAttribute(IApiXmlConstants.ATTR_VERSION);
				description.setEmbeddedVersion(version);
				if (IApiXmlConstants.API_DESCRIPTION_CURRENT_VERSION.equals(version)) {
					description.fPackageTimeStamp = timestamp;
					description.fManifestFile = project.getProject().getFile(JarFile.MANIFEST_NAME);
					restoreChildren(description, root, null, description.fPackageMap);
					return true;
				}
			} catch (IOException e) {
				abort(MessageFormat.format(ScannerMessages.ApiDescriptionManager_1, project.getElementName()), e);
			}
		}
		return false;
	}

	private void restoreChildren(ProjectApiDescription apiDesc, Element element, ManifestNode parentNode, Map<IElementDescriptor, ManifestNode> childrenMap) throws CoreException {
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				restoreNode(apiDesc, (Element) child, parentNode, childrenMap);
			}
		}
	}

	private void restoreNode(ProjectApiDescription apiDesc, Element element, ManifestNode parentNode, Map<IElementDescriptor, ManifestNode> childrenMap) throws CoreException {
		ManifestNode node = null;
		IElementDescriptor elementDesc = null;
		switch (element.getTagName()) {
		case IApiXmlConstants.ELEMENT_PACKAGE:
		{
			int vis = getInt(element, IApiXmlConstants.ATTR_VISIBILITY);
			int res = getInt(element, IApiXmlConstants.ATTR_RESTRICTIONS);
			// collect fragments
			List<IJavaElement> fragments = new ArrayList<>();
			NodeList childNodes = element.getChildNodes();
			String pkgName = null;
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					if (((Element) child).getTagName().equals(IApiXmlConstants.ELEMENT_PACKAGE_FRAGMENT)) {
						Element fragment = (Element) child;
						String handle = fragment.getAttribute(IApiXmlConstants.ATTR_HANDLE);
						IJavaElement je = JavaCore.create(handle);
						if (je.getElementType() != IJavaElement.PACKAGE_FRAGMENT) {
							abort(ScannerMessages.ApiDescriptionManager_2 + handle, null);
						}
						pkgName = je.getElementName();
						fragments.add(je);
					}
				}
			}
			if (!fragments.isEmpty()) {
				elementDesc = Factory.packageDescriptor(pkgName);
				node = apiDesc.newPackageNode(fragments.toArray(new IPackageFragment[fragments.size()]), parentNode,
						elementDesc, vis, res);
			} else {
				abort(ScannerMessages.ApiDescriptionManager_2, null);
			}
			break;
		}
		case IApiXmlConstants.ELEMENT_TYPE:
		{
			String handle = element.getAttribute(IApiXmlConstants.ATTR_HANDLE);
			int vis = getInt(element, IApiXmlConstants.ATTR_VISIBILITY);
			int res = getInt(element, IApiXmlConstants.ATTR_RESTRICTIONS);
			IJavaElement je = JavaCore.create(handle);
			if (je.getElementType() != IJavaElement.TYPE) {
				abort(ScannerMessages.ApiDescriptionManager_3 + handle, null);
			}
			IType type = (IType) je;
			elementDesc = Factory.typeDescriptor(type.getFullyQualifiedName('$'));
			TypeNode tn = apiDesc.newTypeNode(type, parentNode, elementDesc, vis, res);
			node = tn;
			tn.fTimeStamp = getLong(element, IApiXmlConstants.ATTR_MODIFICATION_STAMP);
			break;
		}
		case IApiXmlConstants.ELEMENT_FIELD:
			if (parentNode.element instanceof IReferenceTypeDescriptor type) {
				int vis = getInt(element, IApiXmlConstants.ATTR_VISIBILITY);
				int res = getInt(element, IApiXmlConstants.ATTR_RESTRICTIONS);
				String name = element.getAttribute(IApiXmlConstants.ATTR_NAME);
				elementDesc = type.getField(name);
				node = apiDesc.newNode(parentNode, elementDesc, vis, res);
			}
			break;
		case IApiXmlConstants.ELEMENT_METHOD:
			if (parentNode.element instanceof IReferenceTypeDescriptor type) {
				int vis = getInt(element, IApiXmlConstants.ATTR_VISIBILITY);
				int res = getInt(element, IApiXmlConstants.ATTR_RESTRICTIONS);
				String name = element.getAttribute(IApiXmlConstants.ATTR_NAME);
				String sig = element.getAttribute(IApiXmlConstants.ATTR_SIGNATURE);
				if (sig.indexOf('.') != -1) {
					// old files might use '.' instead of '/'
					sig = sig.replace('.', '/');
				}
				elementDesc = type.getMethod(name, sig);
				node = apiDesc.newNode(parentNode, elementDesc, vis, res);
			}
			break;
		case IApiXmlConstants.ELEMENT_PACKAGE_FRAGMENT:
			return; // nothing to do
		default:
			break;
		}
		if (node != null) {
			childrenMap.put(elementDesc, node);
			restoreChildren(apiDesc, element, node, node.children);
		} else {
			abort(ScannerMessages.ApiDescriptionManager_4, null);
		}
	}

	/**
	 * Returns an integer attribute.
	 *
	 * @param element element with the integer
	 * @param attr attribute name
	 * @return attribute value as an integer
	 */
	private int getInt(Element element, String attr) {
		String attribute = element.getAttribute(attr);
		try {
			return Integer.parseInt(attribute);
		} catch (NumberFormatException e) {
			// ignore
		}
		return 0;
	}

	/**
	 * Returns a long attribute.
	 *
	 * @param element element with the long
	 * @param attr attribute name
	 * @return attribute value as an long
	 */
	private long getLong(Element element, String attr) {
		String attribute = element.getAttribute(attr);
		if (attribute != null) {
			try {
				return Long.parseLong(attribute);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return 0L;
	}

	/**
	 * Throws an exception with the given message and underlying exception.
	 *
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 */
	private static void abort(String message, Throwable exception) throws CoreException {
		IStatus status = Status.error(message, exception);
		throw new CoreException(status);
	}
}
