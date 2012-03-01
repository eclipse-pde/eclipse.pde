/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.builder.BuildState;
import org.eclipse.pde.api.tools.internal.model.ApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Standard delta processor for us to track element state changes in the workspace
 * using {@link IJavaElementDelta}s and {@link IResourceDelta}s
 * 
 * @since 1.1
 */
public class WorkspaceDeltaProcessor implements IElementChangedListener, IResourceChangeListener {

	ApiBaselineManager bmanager = ApiBaselineManager.getManager();
	ApiDescriptionManager dmanager = ApiDescriptionManager.getManager();
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		processJavaElementDeltas(event.getDelta().getAffectedChildren(), null);
	}

	/**
	 * Processes the java element deltas of interest
	 * @param deltas
	 */
	void processJavaElementDeltas(IJavaElementDelta[] deltas, IJavaProject project) {
		for(int i = 0; i < deltas.length; i++) {
			IJavaElementDelta delta = deltas[i];
			switch(delta.getElement().getElementType()) {
				case IJavaElement.JAVA_PROJECT: {
					IJavaProject proj = (IJavaProject) delta.getElement();
					int flags = delta.getFlags();
					switch (delta.getKind()) {
						case IJavaElementDelta.CHANGED: {
							if(!Util.isApiProject(proj)) {
								if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
									System.out.println("--> skipped processing CHANGED delta for project: "+proj.getElementName()); //$NON-NLS-1$
								}
								continue;
							}
							if((flags & IJavaElementDelta.F_OPENED) != 0) {
								if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
									System.out.println("--> processing OPEN project: ["+proj.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								bmanager.disposeWorkspaceBaseline();
							}
							else if( (flags & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0 ||
								(flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
									if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
										System.out.println("--> processing CLASSPATH CHANGE project: ["+proj.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
									}
									bmanager.disposeWorkspaceBaseline();
									dmanager.projectClasspathChanged(proj);
									try {
										BuildState.setLastBuiltState(proj.getProject(), null);
									} catch (CoreException e) {}
									dmanager.flushElementCache(delta.getElement());
							}
							else if((flags & IJavaElementDelta.F_CHILDREN) != 0) {
								if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
									System.out.println("--> processing CHILDREN delta of project: ["+proj.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								processJavaElementDeltas(delta.getAffectedChildren(), proj);
							} 
							else if((flags & IJavaElementDelta.F_CONTENT) != 0) {
								if (proj != null) {
									if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
										System.out.println("--> processing child CONTENT of project: ["+proj.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
									}
									IResourceDelta[] resourcedeltas = delta.getResourceDeltas();
									if(resourcedeltas != null) {
										IResourceDelta rdelta = null;
										for (int j = 0; j < resourcedeltas.length; j++) {
											rdelta = resourcedeltas[j].findMember(new Path(Util.MANIFEST_NAME));
											if(rdelta!= null && rdelta.getKind() == IResourceDelta.CHANGED && (rdelta.getFlags() & IResourceDelta.CONTENT) > 0) {
												if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
													System.out.println("--> processing manifest delta"); //$NON-NLS-1$
												}
												bmanager.disposeWorkspaceBaseline();
												break;
											}
										}
									}
								}
								else {
									if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
										System.out.println("--> ignoring child CONTENT project context is null");  //$NON-NLS-1$
									}
								}
							}
							break;
						}
						case IJavaElementDelta.ADDED: {
							if(!Util.isApiProject(proj)) {
								if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
									System.out.println("--> skipped processing ADDED delta for project: "+proj.getElementName()); //$NON-NLS-1$
								}
								continue;
							}
							if((flags & IJavaElementDelta.F_MOVED_FROM) != 0) {
								if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
									System.out.println("--> processing PROJECT RENAME from: ["+delta.getMovedFromElement().getJavaProject().getElementName()+"] to: ["+proj.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}
								bmanager.disposeWorkspaceBaseline();
							}
							break;
						}
					}
					break;
				}
				case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
					IPackageFragmentRoot root = (IPackageFragmentRoot) delta.getElement();
					int flags = delta.getFlags();
					if ((flags & (IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED
							| IJavaElementDelta.F_ADDED_TO_CLASSPATH
							| IJavaElementDelta.F_REMOVED_FROM_CLASSPATH)) != 0) {
						if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
							System.out.println("processed CLASSPATH CHANGED for package fragment root: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						dmanager.projectClasspathChanged(project);
					} 
					if ((flags & IJavaElementDelta.F_CHILDREN) != 0) {
						if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
							System.out.println("processed CHILDREN for package fragment root: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						processJavaElementDeltas(delta.getAffectedChildren(), project);
					}
					break;
				}
				case IJavaElement.PACKAGE_FRAGMENT: {
					IPackageFragment fragment = (IPackageFragment) delta.getElement();
					if(delta.getKind() == IJavaElementDelta.REMOVED) {
						if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
							System.out.println("processed REMOVED delta for package fragment: ["+fragment.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						((ApiBaseline)bmanager.getWorkspaceBaseline()).clearPackage(fragment.getElementName());
					}
					int flags = delta.getFlags();
					if ((flags & IJavaElementDelta.F_CHILDREN) != 0) {
						if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
							System.out.println("processed CHILDREN delta for package fragment: ["+fragment.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						processJavaElementDeltas(delta.getAffectedChildren(), project);
					}
					break;
				}
				case IJavaElement.COMPILATION_UNIT : {
					int flags = delta.getFlags();
					switch (delta.getKind()) {
						case IJavaElementDelta.CHANGED: {
							if ((flags & (IJavaElementDelta.F_CONTENT | 
											IJavaElementDelta.F_FINE_GRAINED | 
											IJavaElementDelta.F_PRIMARY_RESOURCE)) != 0){
								if (project != null) {
									if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
										System.out.println("processed CONTENT / FINE_GRAINED / PRIMARY_RESOURCE delta for: ["+delta.getElement().getElementName()+"]");  //$NON-NLS-1$//$NON-NLS-2$
									}
									dmanager.projectChanged(project);
									dmanager.flushElementCache(delta.getElement());
									continue;
								}
							}
							break;
						}
						case IJavaElementDelta.ADDED :
						case IJavaElementDelta.REMOVED : {
							if (project != null) {
								if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
									if(delta.getKind() == IJavaElementDelta.ADDED) {
										System.out.println("processed ADDED delta for: ["+delta.getElement().getElementName()+"]");  //$NON-NLS-1$//$NON-NLS-2$
									}
									else {
										System.out.println("processed REMOVED delta for: ["+delta.getElement().getElementName()+"]");  //$NON-NLS-1$//$NON-NLS-2$
									}
								}
								dmanager.projectChanged(project);
								dmanager.flushElementCache(delta.getElement());
								continue;
							}
						}
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResource resource = event.getResource();
		switch(event.getType()) {
			case IResourceChangeEvent.PRE_BUILD: {
				if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
					if (resource == null) {
						System.out.println("processed PRE_BUILD delta for workspace."); //$NON-NLS-1$
					} else {
						System.out.println("processed PRE_BUILD delta for project: ["+resource.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				IResourceDelta delta = event.getDelta();
				if (delta != null) {
					IResourceDelta[] children = delta.getAffectedChildren(IResourceDelta.CHANGED);
					for (int i = 0; i < children.length; i++) {
						resource = children[i].getResource();
						if (children[i].getResource().getType() == IResource.PROJECT) {
							IProject project = (IProject) resource;
							if (Util.isApiProject(project) || Util.isJavaProject(project)) {
								if ((children[i].getFlags() & IResourceDelta.DESCRIPTION) != 0) {
									IJavaProject jp = (IJavaProject) JavaCore.create(resource);
									dmanager.clean(jp, true, true);
									bmanager.disposeWorkspaceBaseline();
									break;
								}
							}
						}
					}
				}
				break;
			}
			case IResourceChangeEvent.PRE_CLOSE: 
			case IResourceChangeEvent.PRE_DELETE:{
				if(resource.getType() == IResource.PROJECT) {
					IProject project = (IProject) resource;
					if (Util.isApiProject(project) || Util.isJavaProject(project)) {
						if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
							if(event.getType() == IResourceChangeEvent.PRE_CLOSE) {
								System.out.println("processed PRE_CLOSE delta for project: ["+resource.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
							}
							else {
								if(ApiPlugin.DEBUG_WORKSPACE_DELTA_PROCESSOR) {
									System.out.println("processed PRE_DELETE delta for project: ["+resource.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
						}
						bmanager.disposeWorkspaceBaseline();
						IJavaProject javaProject = (IJavaProject) JavaCore.create(resource);
						dmanager.clean(javaProject, false, true);
						dmanager.flushElementCache(javaProject);
					}
				}
				break;
			}
		}
	}
}
