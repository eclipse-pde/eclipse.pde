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
package org.eclipse.pde.api.tools.internal.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.ApiAnnotations;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.SystemApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl;
import org.eclipse.pde.api.tools.internal.descriptors.MethodDescriptorImpl;
import org.eclipse.pde.api.tools.internal.descriptors.PackageDescriptorImpl;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.icu.text.MessageFormat;

/**
 * Implementation of an API description.
 * <p>
 * Note, the implementation is not thread safe.
 * </p>
 * @see IApiDescription
 * @since 1.0.0
 */
public class SystemLibraryApiDescription implements IApiDescription {
	
	private static final SystemLibraryApiDescription EMPTY_DESCRIPTION = new SystemLibraryApiDescription("Default"); //$NON-NLS-1$
	// initialize the system api descriptions
	protected static IApiDescription[] ALL_SYSTEM_API_DESCRIPTIONS = new IApiDescription[3];
	
	protected static int INDEX_FOR_CDCs = 0;
	protected static int INDEX_FOR_JREs = 1;
	protected static int INDEX_FOR_OSGis = 2;

	// flag to indicate added profile should be inherited from parent node
	protected static final int ADDED_PROFILE_INHERITED = 0;
	// flag to indicate removed profile should be inherited from parent node
	protected static final int REMOVED_PROFILE_INHERITED = 0;

	/**
	 * API component identifier of the API component that owns this
	 * description. All references within a component have no restrictions.
	 * We allow this to be null for testing purposes, but in general
	 * a component description should have a component id.
	 */
	protected String fOwningComponentId = null;
	
	/**
	 * Whether this description needs saving
	 */
	private boolean fModified = false;
	
	/**
	 * Whether this description is exhaustive or compressed.
	 */
	private boolean isExhaustive = false;
	
	/**
	 * Represents a single node in the tree of mapped manifest items
	 */
	class ManifestNode implements Comparable {
		protected IElementDescriptor element = null;
		protected int visibility;
		protected int restrictions;
		protected int addedProfile;
		protected int removedProfile;
		protected ManifestNode parent = null;
		protected HashMap children = new HashMap(1);
		protected String superclass;
		protected String[] superinterfaces;
		protected boolean isInterface;
		
		public ManifestNode(
				ManifestNode parent,
				IElementDescriptor element,
				int visibility,
				int restrictions,
				int addedProfile,
				int removedProfile) {
			this.element = element;
			this.parent = parent;
			this.visibility = visibility;
			this.restrictions = restrictions;
			this.addedProfile = addedProfile;
			this.removedProfile = removedProfile;
		}
	
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 * This method is safe to override, as the name of an element is unique within its branch of the tree
		 * and does not change over time.
		 */
		public boolean equals(Object obj) {
			if(obj instanceof ManifestNode) {
				return ((ManifestNode)obj).element.equals(element);
			}
			return false;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 * This method is safe to override, as the name of an element is unique within its branch of the tree
		 * and does not change over time.
		 */
		public int hashCode() {
			return element.hashCode();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			String type = null;
			String name = null;
			switch(element.getElementType()) {
				case IElementDescriptor.FIELD: {
					type = "Field"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.METHOD: {
					type = "Method"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.PACKAGE: {
					type = "Package"; //$NON-NLS-1$
					name = ((IPackageDescriptor) element).getName();
					break;
				}
				case IElementDescriptor.TYPE: {
					type = "Type"; //$NON-NLS-1$
					name = ((IMemberDescriptor) element).getName();
					break;
				}
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(type == null ? "Unknown" : type).append(" Node: ").append(name == null ? "Unknown Name" : name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append("\nVisibility: API");//$NON-NLS-1$
			buffer.append("\nRestrictions: None"); //$NON-NLS-1$
			String name2 = ProfileModifiers.getName(this.addedProfile);
			if (name2 != null) {
				buffer.append("\nAdded profile: ").append(name2); //$NON-NLS-1$
			}
			name2 = ProfileModifiers.getName(this.removedProfile);
			if (name2 != null) {
				buffer.append("\nRemoved profile: ").append(name2); //$NON-NLS-1$
			}
			if(parent != null) {
				String pname = parent.element.getElementType() == IElementDescriptor.PACKAGE ? 
						((IPackageDescriptor)parent.element).getName() : ((IMemberDescriptor)parent.element).getName();
				buffer.append("\nParent: ").append(parent == null ? null : pname); //$NON-NLS-1$
			}
			return buffer.toString();
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o instanceof ManifestNode) {
				ManifestNode node = (ManifestNode) o;
				return ((ElementDescriptorImpl)element).compareTo(node.element);
			}
			return -1;
		}
		
		/**
		 * Ensure this node is up to date. Default implementation does
		 * nothing. Subclasses should override as required.
		 * 
		 * Returns the resulting node if the node is valid, or <code>null</code>
		 * if the node no longer exists.
		 * 
		 * @return up to date node, or <code>null</code> if no longer exists
		 */
		protected ManifestNode refresh() {
			return this;
		}
		
		/**
		 * Persists this node as a child of the given element.
		 * 
		 * @param document XML document
		 * @param parent parent element in the document
		 * @param component component the description is for or <code>null</code>
		 */
		void persistXML(Document document, Element parent) {
			switch (element.getElementType()) {
			case IElementDescriptor.METHOD:
				IMethodDescriptor md = (IMethodDescriptor) element;
				Element method = document.createElement(IApiXmlConstants.ELEMENT_METHOD);
				method.setAttribute(IApiXmlConstants.ATTR_NAME, md.getName());
				method.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, md.getSignature());
				persistAnnotations(method);
				parent.appendChild(method);
				break;
			case IElementDescriptor.FIELD:
				IFieldDescriptor fd = (IFieldDescriptor) element;
				Element field = document.createElement(IApiXmlConstants.ELEMENT_FIELD);
				field.setAttribute(IApiXmlConstants.ATTR_NAME, fd.getName());
				persistAnnotations(field);
				parent.appendChild(field);
				break;
			}
		}
		
		/**
		 * Adds visibility and restrictions to the XML element.
		 * 
		 * @param element XML element to annotate
		 * @param component the component the description is for or <code>null</code>
		 */
		void persistAnnotations(Element element) {
			if (this.addedProfile != -1) {
				element.setAttribute(IApiXmlConstants.ATTR_ADDED_PROFILE, Integer.toString(this.addedProfile));
			}
			if (this.removedProfile != -1) {
				element.setAttribute(IApiXmlConstants.ATTR_RESTRICTIONS, Integer.toString(this.removedProfile));
			}
		}

		public int getInheritedAddedProfile() {
			int addedP = this.addedProfile;
			if (addedP != ADDED_PROFILE_INHERITED) return addedP;
			ManifestNode tmp = this;
			while (tmp != null) {
				addedP = tmp.addedProfile;
				if(tmp.addedProfile == ADDED_PROFILE_INHERITED) {
					tmp = tmp.parent;
				}
				else {
					tmp = null;
				}
			}
			return addedP;
		}
		public int getInheritedRemovedProfile() {
			int removedP = this.removedProfile;
			if (removedP != ADDED_PROFILE_INHERITED) return removedP;
			ManifestNode tmp = this;
			while (tmp != null) {
				removedP = tmp.removedProfile;
				if(tmp.removedProfile == REMOVED_PROFILE_INHERITED) {
					tmp = tmp.parent;
				}
				else {
					tmp = null;
				}
			}
			return removedP;
		}
	}
		
	/**
	 * This is a map of component names to a map of package names to package node objects represented as:
	 * <pre>
	 * HashMap<IElementDescriptor(package), ManifestNode(package)>
	 * </pre>
	 */
	protected HashMap fPackageMap = new HashMap();

	/**
	 * Constructs an API description owned by the specified component.
	 * 
	 * @param owningComponentId API component identifier or <code>null</code> if there
	 * is no specific owner.
	 */
	public static IApiDescription newSystemLibraryApiDescription(String eeID) {
		if (ProfileModifiers.isJRE(eeID)) {
			if (ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_JREs] == null) {
				ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_JREs] = initialize("JREs", "/org/eclipse/pde/api/tools/internal/util/profiles/api_descriptions/jre.api_description", false); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_JREs];
		} else if (ProfileModifiers.isOSGi(eeID)) {
			if (ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_OSGis] == null) {
				ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_OSGis] = initialize("OSGis", "/org/eclipse/pde/api/tools/internal/util/profiles/api_descriptions/osgi.api_description", true); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_OSGis];
		} else if (ProfileModifiers.isCDC_Foundation(eeID)) {
			if (ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_CDCs] == null) {
				ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_CDCs] = initialize("CDCs", "/org/eclipse/pde/api/tools/internal/util/profiles/api_descriptions/cdc.api_description", true); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_CDCs];
		}
		return SystemLibraryApiDescription.EMPTY_DESCRIPTION;
	}

	public static IApiDescription newSystemLibraryApiDescription(int eeID) {
		if (ProfileModifiers.isJRE(eeID)) {
			if (ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_JREs] == null) {
				ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_JREs] = initialize("JREs", "/org/eclipse/pde/api/tools/internal/util/profiles/api_descriptions/jre.api_description", false); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_JREs];
		} else if (ProfileModifiers.isOSGi(eeID)) {
			if (ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_OSGis] == null) {
				ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_OSGis] = initialize("OSGis", "/org/eclipse/pde/api/tools/internal/util/profiles/api_descriptions/osgi.api_description", true); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_OSGis];
		} else if (ProfileModifiers.isCDC_Foundation(eeID)) {
			if (ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_CDCs] == null) {
				ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_CDCs] = initialize("CDCs", "/org/eclipse/pde/api/tools/internal/util/profiles/api_descriptions/cdc.api_description", true); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ALL_SYSTEM_API_DESCRIPTIONS[INDEX_FOR_CDCs];
		}
		return SystemLibraryApiDescription.EMPTY_DESCRIPTION;
	}

	private static IApiDescription initialize(String eeID, String location, boolean isExhaustive) {
		IApiDescription apiDesc = new SystemLibraryApiDescription(eeID, isExhaustive);
		// first mark all packages as internal
		try {
			String xml = loadApiDescription(location);
			if (xml != null) {
				SystemApiDescriptionProcessor.annotateApiSettings(apiDesc, xml);
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
			return EMPTY_DESCRIPTION;
		} catch(CoreException e) {
			ApiPlugin.log(e);
			return EMPTY_DESCRIPTION;
		}
		return apiDesc;
	}
	public SystemLibraryApiDescription(String id) {
		this(id, false);
	}
	public SystemLibraryApiDescription(String id, boolean isExhaustive) {
		this.fOwningComponentId = id;
		this.isExhaustive = isExhaustive;
	}
	protected static String loadApiDescription(String inputLocation) throws IOException {
		BufferedInputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(ApiPlugin.class.getResourceAsStream(inputLocation));
			char[] charArray = Util.getInputStreamAsCharArray(inputStream, -1, IApiCoreConstants.UTF_8);
			return new String(charArray);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	static void closingZipFileAndStream(InputStream stream, ZipFile jarFile) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiManifest#visit(org.eclipse.pde.api.tools.ApiManifestVisitor)
	 */
	public void accept(ApiDescriptionVisitor visitor) {
		try {
			visitChildren(visitor, fPackageMap);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}
	/**
	 * Visits all children nodes in the given children map.
	 * 
	 * @param visitor visitor to visit
	 * @param childrenMap map of element name to manifest nodes
	 */
	protected void visitChildren(ApiDescriptionVisitor visitor, Map childrenMap) throws CoreException {
		List elements = new ArrayList(childrenMap.keySet());
		Collections.sort(elements);
		Iterator iterator = elements.iterator();
		while (iterator.hasNext()) {
			IElementDescriptor element = (IElementDescriptor) iterator.next();
			ManifestNode node = (ManifestNode) childrenMap.get(element);
			visitNode(visitor, node);
		}
	}
	
	/**
	 * Visits a node and its children.
	 * 
	 * @param visitor visitor to visit
	 * @param node node to visit
	 */
	private void visitNode(ApiDescriptionVisitor visitor, ManifestNode node) throws CoreException {
		IApiAnnotations desc = new ApiAnnotations(
				VisibilityModifiers.API,
				RestrictionModifiers.NO_RESTRICTIONS,
				node.getInheritedAddedProfile(),
				node.getInheritedRemovedProfile());
		boolean visitChildren = visitor.visitElement(node.element, desc);
		if (visitChildren && !node.children.isEmpty()) {
			visitChildren(visitor, node.children);
		}
		visitor.endVisitElement(node.element, desc);
	}
	
	/**
	 * Returns the node in the manifest for specified element and context, closest node, or <code>null</code>.
	 * Creates a new node with default visibility and no restrictions if insert is <code>true</code>
	 * and a node is not present. Default visibility for packages is API, and for types is inherited.
	 * 
	 * @param element element
	 * @param write <code>true</code> if setting a node, <code>false</code> if getting a node
	 * @return manifest node or <code>null</code>
	 */
	protected ManifestNode findNode(IElementDescriptor element, boolean write) {
		IElementDescriptor[] path = element.getPath();
		Map map = fPackageMap;
		ManifestNode parentNode = null;
		ManifestNode node = null;
		for (int i = 0 ; i < path.length; i++) {
			IElementDescriptor current = path[i];
			parentNode = node;
			node = (ManifestNode) map.get(current);
			if (node == null) {
				if (write || (isInsertOnResolve(current))) {
					node = createNode(parentNode, current);
					if (node != null) {
						map.put(current, node);
					} else {
						return null;
					}
				} else if (!this.isExhaustive) {
					return parentNode;
				} else {
					return null;
				}
			}
			node = node.refresh();
			if (node != null) {
				map = node.children;
			}
		}
		return node;
	}
 	
	public IApiAnnotations resolveAnnotations(IElementDescriptor element) {
		ManifestNode node = findNode(element, false);
		if (node == null) {
			// check hierarchy
			if (element.getElementType() == IElementDescriptor.METHOD) {
				MethodDescriptorImpl methodDescriptorImpl = (MethodDescriptorImpl) element;
				// no need to do a lookup for types or fields
				IElementDescriptor parent = element.getParent();
				return resolveAnnotations0(element, methodDescriptorImpl, parent);
			}
		} else {
			return resolveAnnotations(node, element);
		}
		return null;
	}
	
	/**
	 * Resolves annotations based on inheritance for the given node and element.
	 * 
	 * @param node manifest node
	 * @param element the element annotations are being resolved for
	 * @return annotations
	 */
	protected IApiAnnotations resolveAnnotations(ManifestNode node, IElementDescriptor element) {
		return new ApiAnnotations(
				VisibilityModifiers.API,
				RestrictionModifiers.NO_RESTRICTIONS,
				node.getInheritedAddedProfile(),
				node.getInheritedRemovedProfile());
	}
	
	/**
	 * Internal hook to clear the package map to remove stale data
	 */
	protected void clearPackages() {
		if(fPackageMap != null) {
			fPackageMap.clear();
		}
	}
	
	/**
	 * Creates and returns a new manifest node to be inserted into the tree
	 * or <code>null</code> if the node does not exist.
	 * 
	 * <p>
	 * Subclasses should override this method as required.
	 * </p>
	 * @param parentNode parent node
	 * @param element element the node is to be created for
	 * @return new manifest node or <code>null</code> if none
	 */
	protected ManifestNode createNode(
			ManifestNode parentNode,
			IElementDescriptor element) {
		return new ManifestNode(
				parentNode,
				element,
				VisibilityModifiers.API,
				RestrictionModifiers.NO_RESTRICTIONS,
				ADDED_PROFILE_INHERITED,
				REMOVED_PROFILE_INHERITED);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#setVisibility(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor, int)
	 */
	public IStatus setVisibility(IElementDescriptor element, int visibility) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.visibility = visibility;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API visibility: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiDescription#setRestrictions(java.lang.String, org.eclipse.pde.api.tools.model.component.IElementDescriptor, int)
	 */
	public IStatus setRestrictions(IElementDescriptor element, int restrictions) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.restrictions = restrictions;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API restriction: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Api description for component: ").append(fOwningComponentId); //$NON-NLS-1$
		return buffer.toString();
	}
	public IStatus setAddedProfile(IElementDescriptor element, int addedProfile) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.addedProfile = addedProfile;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API added profile: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}
	public IStatus setRemovedProfile(IElementDescriptor element,
			int removedProfile) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.removedProfile = removedProfile;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API removed profile: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}
	/**
	 * Returns whether a new node should be inserted into the API description
	 * when resolving the annotations for an element if a node is not already
	 * present, in the context of the given component.
	 * <p>
	 * Default implementation returns <code>false</code>. Subclasses should
	 * override this method as required.
	 * </p>
	 * @param elementDescriptor
	 * @return whether a new node should be inserted into the API description
	 * when resolving the annotations for an element if a node is not already
	 * present
	 */
	protected boolean isInsertOnResolve(IElementDescriptor elementDescriptor) {
		return false;
	}
	
	/**
	 * Marks the description as modified
	 */
	protected synchronized void modified() {
		fModified = true;
	}
	
	/**
	 * Returns whether this description has been modified.
	 * 
	 * @return
	 */
	protected synchronized boolean isModified() {
		return fModified;
	}

	public IStatus setSuperclass(IElementDescriptor element, String superclass) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.superclass = superclass;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API superclass: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}

	public IStatus setInterface(IElementDescriptor element,
			boolean interfaceFlag) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.isInterface = interfaceFlag;
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API superclass: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}

	public IStatus setSuperinterfaces(IElementDescriptor element,
			String superinterfaces) {
		ManifestNode node = findNode(element, true);
		if(node != null) {
			modified();
			node.superinterfaces = superinterfaces != null ? superinterfaces.split(",") : null; //$NON-NLS-1$
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ELEMENT_NOT_FOUND,
				MessageFormat.format("Failed to set API superclass: {0} not found in {1}", //$NON-NLS-1$
						new String[]{element.toString(), fOwningComponentId}), null);
	}

	private IApiAnnotations resolveAnnotations0(
			IElementDescriptor element,
			MethodDescriptorImpl methodDescriptorImpl,
			IElementDescriptor parent) {
		ManifestNode typeNode = findNode(parent, false);
		if (typeNode != null) {
			// should be the type node
			if (typeNode.isInterface) {
				// lookup through interfaces
				String[] superinterfaces = typeNode.superinterfaces;
				for (int i = 0, max = superinterfaces.length; i < max; i++) {
					String superinterface = superinterfaces[i];
					PackageDescriptorImpl packageDescriptorImpl = new PackageDescriptorImpl(getPackageName(superinterface));
					IReferenceTypeDescriptor typeDescriptor = packageDescriptorImpl.getType(getSimpleName(superinterface));
					IMethodDescriptor methodDescriptor = typeDescriptor.getMethod(
							methodDescriptorImpl.getName(),
							methodDescriptorImpl.getSignature());
					IApiAnnotations resolveAnnotations = resolveAnnotations(methodDescriptor);
					if (resolveAnnotations != null) {
						return resolveAnnotations;
					}
				}
			} else {
				// lookup through superclasses
				String superclass = typeNode.superclass;
				if (superclass != null) {
					PackageDescriptorImpl packageDescriptorImpl = new PackageDescriptorImpl(getPackageName(superclass));
					IReferenceTypeDescriptor typeDescriptor = packageDescriptorImpl.getType(getSimpleName(superclass));
					IMethodDescriptor methodDescriptor = typeDescriptor.getMethod(
							methodDescriptorImpl.getName(),
							methodDescriptorImpl.getSignature());
					IApiAnnotations resolveAnnotations = resolveAnnotations(methodDescriptor);
					if (resolveAnnotations != null) {
						return resolveAnnotations;
					}
				}
			}
		}
		return null;
	}

	private String getSimpleName(String typeName) {
		return typeName.substring(typeName.lastIndexOf('.') + 1);
	}

	private String getPackageName(String typeName) {
		return typeName.substring(0, typeName.lastIndexOf('.'));
	}
}
