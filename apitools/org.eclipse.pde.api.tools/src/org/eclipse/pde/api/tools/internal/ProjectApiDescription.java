/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.api.tools.internal.builder.BuildStamps;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of an API description for a Java project.
 * 
 * @since 1.0
 */
public class ProjectApiDescription extends ApiDescription {
	
	/**
	 * Associated Java project
	 */
	private IJavaProject fProject;
	
	/**
	 * Time stamp at which package information was created
	 */
	public long fPackageTimeStamp = 0L;
	
	/** 
	 * Whether a package refresh is in progress
	 */
	private boolean fRefreshingInProgress = false;
	
	/**
	 * Associated manifest file
	 */
	public IFile fManifestFile;
	
	/**
	 * Whether this API description is in synch with its project. Becomes
	 * false if anything in a project changes. When true, visiting can
	 * be performed by traversing the cached nodes, rather than traversing
	 * the java model elements (effectively building the cache).
	 */
	private boolean fInSynch = false;
			
	/**
	 * A node for a package.
	 */
	class PackageNode extends ManifestNode {

		IPackageFragment[] fFragments;
		/**
		 * Constructs a new node.
		 * 
		 * @param parent
		 * @param element
		 * @param visibility
		 * @param restrictions
		 */
		public PackageNode(IPackageFragment fragments[], ManifestNode parent, IElementDescriptor element, int visibility, int restrictions) {
			super(parent, element, visibility, restrictions);
			fFragments = fragments;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#refresh()
		 */
		protected ManifestNode refresh() {
			refreshPackages();
			for (int i = 0; i < fFragments.length; i++) {
				if (!fFragments[i].exists()) {
					modified();
					return null;
				}
			}
			return this;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#persistXML(org.w3c.dom.Document, org.w3c.dom.Element, java.lang.String)
		 */
		void persistXML(Document document, Element parent) {
			if (hasApiVisibility(this)) {
				Element pkg = document.createElement(IApiXmlConstants.ELEMENT_PACKAGE);
				for (int i = 0; i < fFragments.length; i++) {
					Element fragment = document.createElement(IApiXmlConstants.ELEMENT_PACKAGE_FRAGMENT);
					fragment.setAttribute(IApiXmlConstants.ATTR_HANDLE, fFragments[i].getHandleIdentifier());
					pkg.appendChild(fragment);
				}
				pkg.setAttribute(IApiXmlConstants.ATTR_VISIBILITY, Integer.toString(this.visibility));
				persistChildren(document, pkg, children);
				parent.appendChild(pkg);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			String name = ((IPackageDescriptor)element).getName();
			buffer.append("Package Node: ").append(name.equals("") ? "<default package>" : name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append("\nVisibility: ").append(VisibilityModifiers.getVisibilityName(visibility)); //$NON-NLS-1$
			buffer.append("\nRestrictions: ").append(RestrictionModifiers.getRestrictionText(restrictions)); //$NON-NLS-1$
			if(fFragments != null) {
				buffer.append("\nFragments:"); //$NON-NLS-1$
				IPackageFragment fragment = null;
				for(int i = 0; i < fFragments.length; i++) {
					fragment = fFragments[i];
					buffer.append("\n\t").append(fragment.getElementName()); //$NON-NLS-1$
					buffer.append(" ["); //$NON-NLS-1$
					buffer.append(fragment.getParent().getElementName());
					buffer.append("]"); //$NON-NLS-1$
				}
			}
			return buffer.toString();
		}
	}
	
	/**
	 * Node for a reference type.
	 */
	class TypeNode extends ManifestNode {
		
		long fTimeStamp = -1L;
		long fBuildStamp = -1L;
		private boolean fRefreshing = false;
		
		IType fType;

		/**
		 * Constructs a node for a reference type.
		 * 
		 * @param type
		 * @param parent
		 * @param element
		 * @param visibility
		 * @param restrictions
		 */
		public TypeNode(IType type, ManifestNode parent, IElementDescriptor element, int visibility, int restrictions) {
			super(parent, element, visibility, restrictions);
			fType = type;
			if (parent instanceof TypeNode) {
				fTimeStamp = ((TypeNode)parent).fTimeStamp;
				fBuildStamp = ((TypeNode)parent).fBuildStamp;
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#refresh()
		 */
		protected synchronized ManifestNode refresh() {
			if (fRefreshing) {
				if(ApiPlugin.DEBUG_API_DESCRIPTION) {
					StringBuffer buffer = new StringBuffer();
					buffer.append("Refreshing manifest node: "); //$NON-NLS-1$
					buffer.append(this);
					buffer.append(" aborted because a refresh is already in progress"); //$NON-NLS-1$
					System.out.println(buffer.toString());
				}
				return this;
			}
			try {
				fRefreshing = true;
				int parentVis = resolveVisibility(parent);
				if (VisibilityModifiers.isAPI(parentVis)) {
					ICompilationUnit unit = fType.getCompilationUnit();
					if (unit != null) {
						IResource resource = null;
						try {
							resource = unit.getUnderlyingResource();
						} catch (JavaModelException e) {
							if(ApiPlugin.DEBUG_API_DESCRIPTION) {
								StringBuffer buffer = new StringBuffer();
								buffer.append("Failed to get underlying resource for compilation unit: "); //$NON-NLS-1$
								buffer.append(unit);
								System.out.println(buffer.toString());
							}
							// exception if the resource does not exist
							if (!e.getJavaModelStatus().isDoesNotExist()) {
								ApiPlugin.log(e.getStatus());
								return this;
							}
						}
						if (resource != null && resource.exists()) {
							long stamp = resource.getModificationStamp();
							if (stamp != fTimeStamp) {
								// compute current CRC
								CRCVisitor visitor = new CRCVisitor();
								visitType(this, visitor);
								long crc = visitor.getValue();
								if(ApiPlugin.DEBUG_API_DESCRIPTION) {
									StringBuffer buffer = new StringBuffer();
									buffer.append("Resource has changed for type manifest node: "); //$NON-NLS-1$
									buffer.append(this);
									buffer.append(" tag scanning the new type"); //$NON-NLS-1$
									buffer.append(" (CRC "); //$NON-NLS-1$
									buffer.append(crc);
									buffer.append(')');
									System.out.println(buffer.toString());
								}
								modified();
								children.clear();
								restrictions = RestrictionModifiers.NO_RESTRICTIONS;
								fTimeStamp = resource.getModificationStamp();
								try {
									TagScanner.newScanner().scan(unit, ProjectApiDescription.this,
										getApiTypeContainer((IPackageFragmentRoot) fType.getPackageFragment().getParent()), null);
								} catch (CoreException e) {
									ApiPlugin.log(e.getStatus());
								}
								// see if the description changed
								visitor = new CRCVisitor();
								visitType(this, visitor);
								long crc2 = visitor.getValue();
								if (crc != crc2) {
									// update relative build time stamp
									fBuildStamp = BuildStamps.getBuildStamp(resource.getProject());
									if(ApiPlugin.DEBUG_API_DESCRIPTION) {
										StringBuffer buffer = new StringBuffer();
										buffer.append("CRC changed for type manifest node: "); //$NON-NLS-1$
										buffer.append(this);
										buffer.append(" (CRC "); //$NON-NLS-1$
										buffer.append(crc2);
										buffer.append(')');
										System.out.println(buffer.toString());
									}
								}
							}
						} else {
							if(ApiPlugin.DEBUG_API_DESCRIPTION) {
								StringBuffer buffer = new StringBuffer();
								buffer.append("Underlying resource for the type manifest node: "); //$NON-NLS-1$
								buffer.append(this);
								buffer.append(" does not exist or is null"); //$NON-NLS-1$
								System.out.println(buffer.toString());
							}
							// element has been removed
							modified();
							parent.children.remove(element);
							return null;
						}
					} else {
						if(ApiPlugin.DEBUG_API_DESCRIPTION) {
							StringBuffer buffer = new StringBuffer();
							buffer.append("Failed to look up compilation unit for "); //$NON-NLS-1$
							buffer.append(fType);
							buffer.append(" refreshing type manifest node: "); //$NON-NLS-1$
							buffer.append(this);
							System.out.println(buffer.toString());
						}
						// TODO: binary type
					}
				} else {
					// don't scan internal types
				}
			} finally {
				fRefreshing = false;
			}
			return this;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#persistXML(org.w3c.dom.Document, org.w3c.dom.Element, java.lang.String)
		 */
		void persistXML(Document document, Element parent) {
			if(hasApiVisibility(this)) {
				Element type = document.createElement(IApiXmlConstants.ELEMENT_TYPE);
				type.setAttribute(IApiXmlConstants.ATTR_HANDLE, fType.getHandleIdentifier());
				persistAnnotations(type);
				type.setAttribute(IApiXmlConstants.ATTR_MODIFICATION_STAMP, Long.toString(fTimeStamp));
				persistChildren(document, type, children);
				parent.appendChild(type);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Type Node: ").append(fType.getFullyQualifiedName()); //$NON-NLS-1$
			buffer.append("\nVisibility: ").append(VisibilityModifiers.getVisibilityName(visibility)); //$NON-NLS-1$
			buffer.append("\nRestrictions: ").append(RestrictionModifiers.getRestrictionText(restrictions)); //$NON-NLS-1$
			if(parent != null) {
				String pname = parent.element.getElementType() == IElementDescriptor.PACKAGE ?
						((IPackageDescriptor)parent.element).getName() : ((IReferenceTypeDescriptor)parent.element).getQualifiedName();
				buffer.append("\nParent: ").append(pname); //$NON-NLS-1$
			}
			return buffer.toString();
		}
	}
	
	/**
	 * Constructs a new API description for the given project API component.
	 * 
	 * @param component
	 */
	public ProjectApiDescription(IJavaProject project) {
		super(project.getElementName());
		fProject = project;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiDescription#accept(org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor)
	 */
	public synchronized void accept(ApiDescriptionVisitor visitor, IProgressMonitor monitor) {
		boolean completeVisit = true;
		if (fInSynch) {
			super.accept(visitor, monitor);
		} else {
			try {
				IPackageFragment[] fragments = getLocalPackageFragments();
				IJavaElement[] children = null;
				IJavaElement child = null;
				ICompilationUnit unit = null;
				for (int j = 0; j < fragments.length; j++) {
					if (ApiPlugin.DEBUG_API_DESCRIPTION) {
						System.out.println("\t" + fragments[j].getElementName()); //$NON-NLS-1$
					}
					IPackageDescriptor packageDescriptor = Factory.packageDescriptor(fragments[j].getElementName());
					// visit package
					ManifestNode pkgNode = findNode(packageDescriptor, false);
					if (pkgNode != null) {
						IApiAnnotations annotations = resolveAnnotations(pkgNode, packageDescriptor);
						if (visitor.visitElement(packageDescriptor, annotations)) {
							children = fragments[j].getChildren();
							for (int k = 0; k < children.length; k++) {
								child = children[k];
								if (child instanceof ICompilationUnit) {
									unit = (ICompilationUnit) child;
									String cuName = unit.getElementName(); 
									String tName = cuName.substring(0, cuName.length() - ".java".length()); //$NON-NLS-1$
									visit(visitor, unit.getType(tName));
								} else if (child instanceof IClassFile) {
									visit(visitor, ((IClassFile)child).getType());
								}
							}
						} else {
							completeVisit = false;
						}
						visitor.endVisitElement(packageDescriptor, annotations);
					}
				}
			} catch (JavaModelException e) {
				completeVisit = false;
				ApiPlugin.log(e.getStatus());
			} finally {
				if (completeVisit) {
					fInSynch = true;
				}
			}
		}
	}
	
	/**
	 * Visits a type.
	 * 
	 * @param visitor
	 * @param owningComponent
	 * @param type
	 */
	private void visit(ApiDescriptionVisitor visitor, IType type) {
		IElementDescriptor element = getElementDescriptor(type);
		ManifestNode typeNode = findNode(element, false);
		if (typeNode != null) {
			visitType(typeNode, visitor);
		}
	}
	
	void visitType(ManifestNode node, ApiDescriptionVisitor visitor) {
		IApiAnnotations annotations = resolveAnnotations(node, node.element);
		if (visitor.visitElement(node.element, annotations)) {
			// children
			if (node.children != null) {
				visitChildren(visitor, node.children, null);
			}
		}
		visitor.endVisitElement(node.element, annotations);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.ApiDescription#isInsertOnResolve(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor)
	 */
	protected boolean isInsertOnResolve(IElementDescriptor elementDescriptor) {
		switch (elementDescriptor.getElementType()) {
			case IElementDescriptor.METHOD:
			case IElementDescriptor.FIELD:
				return false;
			case IElementDescriptor.TYPE:
				// no need to insert member types
				return ((IReferenceTypeDescriptor) elementDescriptor).getEnclosingType() == null;
			default:
				return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.ApiDescription#createNode(org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode, org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor)
	 */
	protected ManifestNode createNode(ManifestNode parentNode, IElementDescriptor element) {
		switch (element.getElementType()) {
			case IElementDescriptor.PACKAGE:
				try {
					IPackageDescriptor pkg = (IPackageDescriptor) element;
					IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
					List fragments = new ArrayList(1);
					for (int i = 0; i < roots.length; i++) {
						IPackageFragmentRoot root = roots[i];
						IClasspathEntry entry = root.getRawClasspathEntry();
						switch (entry.getEntryKind()) {
						case IClasspathEntry.CPE_SOURCE:
						case IClasspathEntry.CPE_LIBRARY:
							IPackageFragment fragment = root.getPackageFragment(pkg.getName());
							if (fragment.exists()) {
								fragments.add(fragment);
							}
							break;
						default:
							if (!root.isArchive()
									&& root.getKind() == IPackageFragmentRoot.K_BINARY) {
								// class file folder
								fragment = root.getPackageFragment(pkg.getName());
								if (fragment.exists()) {
									fragments.add(fragment);
								}
							}
						}
					}
					if (fragments.isEmpty()) {
						return null;
					} else {
						return newPackageNode((IPackageFragment[])fragments.toArray(new IPackageFragment[fragments.size()]), parentNode, element, VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
					}
					
				} catch (CoreException e) {
					return null;
				}
			case IElementDescriptor.TYPE:
				IReferenceTypeDescriptor descriptor = (IReferenceTypeDescriptor) element;
				try {
					IType type = null;
					String name = descriptor.getName();
					if (parentNode instanceof PackageNode) {
						IPackageFragment[] fragments = ((PackageNode) parentNode).fFragments; 
						for (int i = 0; i < fragments.length; i++) {
							IPackageFragment fragment = fragments[i];
							if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
								ICompilationUnit unit = fragment.getCompilationUnit(name + ".java"); //$NON-NLS-1$
								try {
									IResource resource = unit.getUnderlyingResource();
									if (resource != null) {
										type = unit.getType(name);
									}
								} catch (JavaModelException jme) {
									// exception if the resource does not exist
									if (!jme.getJavaModelStatus().isDoesNotExist()) {
										throw jme;
									}
								}
							} else {
								IClassFile file = fragment.getClassFile(name + ".class"); //$NON-NLS-1$
								if (file.exists()) {
									type = file.getType();
								}
							}
						}
					} else if (parentNode instanceof TypeNode) {
						type = ((TypeNode)parentNode).fType.getType(name);
					}
					if (type != null) {
						return newTypeNode(type, parentNode, element, VISIBILITY_INHERITED, RestrictionModifiers.NO_RESTRICTIONS);
					}
				} catch (CoreException e ) {
					return null;
				}
				return null;
		}
		return super.createNode(parentNode, element);
	}

	/** 
	 * Constructs and returns a new node for the given package fragment.
	 * 
	 * @param fragment
	 * @param parent
	 * @param descriptor
	 * @param vis
	 * @param res
	 * @return
	 */
	public PackageNode newPackageNode(IPackageFragment[] fragments, ManifestNode parent, IElementDescriptor descriptor, int vis, int res) {
		return new PackageNode(fragments, parent, descriptor, vis, res);
	}

	/**
	 * Constructs and returns a new node for the given type.
	 * 
	 * @param type
	 * @param parent
	 * @param descriptor
	 * @param vis
	 * @param res
	 * @return
	 */
	TypeNode newTypeNode(IType type, ManifestNode parent, IElementDescriptor descriptor, int vis, int res) {
		return new TypeNode(type, parent, descriptor, vis, res);
	}
	
	/**
	 * Constructs a new manifest node.
	 * 
	 * @param parent
	 * @param element
	 * @param vis
	 * @param res
	 * @return
	 */
	ManifestNode newNode(ManifestNode parent, IElementDescriptor element, int vis, int res) {
		return new ManifestNode(parent, element, vis, res);
	}

	/**
	 * Refreshes package nodes if required.
	 */
	synchronized void refreshPackages() {
		if (fRefreshingInProgress) {
			if(ApiPlugin.DEBUG_API_DESCRIPTION) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("Refreshing manifest node: "); //$NON-NLS-1$
				buffer.append(this);
				buffer.append(" aborted because a refresh is already in progress"); //$NON-NLS-1$
				System.out.println(buffer.toString());
			}
			return;
		}
		// check if in synch
		if (fManifestFile == null || (fManifestFile.getModificationStamp() != fPackageTimeStamp)) {
			try {
				modified();
				fRefreshingInProgress = true;
				// set all existing packages to PRIVATE (could clear
				// the map, but it would be less efficient)
				Iterator iterator = fPackageMap.values().iterator();
				while (iterator.hasNext()) {
					PackageNode node = (PackageNode) iterator.next();
					node.visibility = VisibilityModifiers.PRIVATE;
				}
				fManifestFile = getJavaProject().getProject().getFile(JarFile.MANIFEST_NAME);
				if (fManifestFile.exists()) {
					try {
						IPackageFragment[] fragments = getLocalPackageFragments();
						Set names = new HashSet();
						for (int i = 0; i < fragments.length; i++) {
							names.add(fragments[i].getElementName());
						}
						ProjectComponent component = getApiComponent();
						BundleComponent.initializeApiDescription(this, component.getBundleDescription(), names);
						fPackageTimeStamp = fManifestFile.getModificationStamp();
					} catch (CoreException e) {
						ApiPlugin.log(e.getStatus());
					}
				}
			} finally {
				fRefreshingInProgress = false;
			}
		}
	}

	private IElementDescriptor getElementDescriptor(IJavaElement element) {
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				return Factory.packageDescriptor(element.getElementName());
			case IJavaElement.TYPE:
				return Factory.typeDescriptor(((IType)element).getFullyQualifiedName('$'));
			default:
				return null;
		}
	}
	
	/**
	 * Returns the Java project associated with this component.
	 * 
	 * @return associated Java project
	 */
	private IJavaProject getJavaProject() {
		return fProject;
	}

	/**
	 * Returns a class file container for the given package fragment root.
	 * 
	 * @param root package fragment root
	 * @return class file container
	 * @exception CoreException if container cannot be located
	 */
	synchronized IApiTypeContainer getApiTypeContainer(IPackageFragmentRoot root) throws CoreException {
		IApiTypeContainer container  = getApiComponent().getTypeContainer(root);
		if (container == null) {
			throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, "Unable to resolve type conatiner for package fragment root"));  //$NON-NLS-1$
		}
		return container;
	}
	
	/**
	 * Returns all package fragments that originate from this project.
	 * 
	 * @return all package fragments that originate from this project
	 */
	private IPackageFragment[] getLocalPackageFragments() {
		List local = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root = roots[i];
				// only care about roots originating from this project (binary or source)
				IResource resource = root.getCorrespondingResource();
				if (resource != null && resource.getProject().equals(getJavaProject().getProject())) {
					IJavaElement[] children = root.getChildren();
					for (int j = 0; j < children.length; j++) {
						local.add(children[j]);
					}
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return (IPackageFragment[]) local.toArray(new IPackageFragment[local.size()]);
	}
	
	/**
	 * Returns this API description as XML.
	 * 
	 * @throws CoreException
	 */
	public synchronized String getXML() throws CoreException {
		Document document = Util.newDocument();	
		Element component = document.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
		component.setAttribute(IApiXmlConstants.ATTR_ID, getJavaProject().getElementName());
		component.setAttribute(IApiXmlConstants.ATTR_MODIFICATION_STAMP, Long.toString(fPackageTimeStamp));
		component.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_DESCRIPTION_CURRENT_VERSION);
		document.appendChild(component);
		persistChildren(document, component, fPackageMap);
		return Util.serializeDocument(document);
	}

	/**
	 * Persists the elements in the given map as XML elements, appended
	 * to the given xmlElement.
	 *  
	 * @param document XML document
	 * @param xmlElement node to append children no
	 * @param elementMap elements to persist
	 */
	void persistChildren(Document document, Element xmlElement, Map elementMap) {
		Iterator iterator = elementMap.values().iterator();
		while (iterator.hasNext()) {
			ManifestNode node = (ManifestNode) iterator.next();
			node.persistXML(document, xmlElement);
		}
	}
	
	/**
	 * Cleans this API description so it will be re-populated with fresh data.
	 */
	public synchronized void clean() {
		fPackageMap.clear();
		fPackageTimeStamp = -1L;
		fInSynch = false;
		modified();
	}
	
	/**
	 * Notes that the underlying project has changed in some way and that the
	 * description cache is no longer in synch with the project.
	 */
	public synchronized void projectChanged() {
		fInSynch = false;
	}

	/**
	 * Notes that the underlying project classpath has changed in some way and that the
	 * description cache is no longer in synch with the project.
	 */
	public synchronized void projectClasspathChanged() {
		fInSynch = false;
		// we want to flush the packages cache to "reload" all packages using the new package fragment roots
		fPackageTimeStamp = -1L;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.ApiDescription#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Project API description for: ").append(getJavaProject().getElementName()); //$NON-NLS-1$
		return buffer.toString();
	}
	
	/**
	 * Returns the API component associated with this API description
	 * 
	 * @return API component
	 * @exception CoreException if the API component cannot be located
	 */
	private ProjectComponent getApiComponent() throws CoreException {
		IApiBaseline baseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
		ProjectComponent component = (ProjectComponent) baseline.getApiComponent(getJavaProject().getProject());
		if (component == null) {
			throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, "Unable to resolve project API component for API description"));  //$NON-NLS-1$
		}
		return component;
	}
	
	/**
	 * Resolves annotations based on inheritance for the given node and element.
	 * 
	 * @param node manifest node
	 * @param element the element annotations are being resolved for
	 * @return annotations
	 */
	protected IApiAnnotations resolveAnnotations(ManifestNode node, IElementDescriptor element) {
		IApiAnnotations ann = super.resolveAnnotations(node, element);
		if (node instanceof TypeNode) {
			return new TypeAnnotations(ann, ((TypeNode)node).fBuildStamp);
		}
		return ann;
		
	}	
}
