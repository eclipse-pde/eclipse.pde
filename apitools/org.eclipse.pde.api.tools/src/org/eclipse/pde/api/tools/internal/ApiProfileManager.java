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
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.builder.ApiAnalysisBuilder;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This manager is used to maintain (persist, restore, access, update) Api profiles.
 * This manager is lazy, in that caches are built and maintained when requests
 * are made for information, nothing is pre-loaded when the manager is initialized.
 * 
 * @since 1.0.0
 */
public final class ApiProfileManager implements IApiProfileManager, ISaveParticipant, IElementChangedListener, IPluginModelListener, IResourceChangeListener {
	
	/**
	 * Constant used for controlling tracing in the API tool builder
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the API tool builder
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
	
	/**
	 * Constant for the default API profile.
	 * Value is: <code>default_api_profile</code>
	 */
	private static final String DEFAULT_PROFILE = "default_api_profile"; //$NON-NLS-1$
	
	/**
	 * The main cache for the manager.
	 * The form of the cache is: 
	 * <pre>
	 * HashMap<String(profileid), ApiProfile>
	 * </pre>
	 */
	private HashMap profilecache = null;
	
	/**
	 * The current default {@link IApiProfile}
	 */
	private String defaultprofile = null;
	
	/**
	 * The current workspace profile
	 */
	private IApiProfile workspaceprofile = null;
	
	/**
	 * The default save location for persisting the cache from this manager.
	 */
	private IPath savelocation = null;
	
	/**
	 * If the cache of profiles needs to be saved or not.
	 */
	private boolean fNeedsSaving = false;
	
	private static ApiProfileManager fInstance = null;
	
	/**
	 * Constructor
	 */
	private ApiProfileManager(boolean framework) {
		if(framework) {
			ApiPlugin.getDefault().addSaveParticipant(this);
			JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_BUILD);
			PDECore.getDefault().getModelManager().addPluginModelListener(this);
			savelocation = ApiPlugin.getDefault().getStateLocation().append(".api_profiles").addTrailingSeparator(); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns the singleton instance of the manager
	 * @return the singleton instance of the manager
	 */
	public static synchronized ApiProfileManager getManager() {
		if(fInstance == null) {
			fInstance = new ApiProfileManager(ApiPlugin.isRunningInFramework());
		}
		return fInstance;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#getApiProfile(java.lang.String)
	 */
	public synchronized IApiProfile getApiProfile(String name) {
		initializeStateCache();
		return (ApiProfile) profilecache.get(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#getApiProfiles()
	 */
	public synchronized IApiProfile[] getApiProfiles() {
		initializeStateCache();
		return (IApiProfile[]) profilecache.values().toArray(new IApiProfile[profilecache.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#addApiProfile(org.eclipse.pde.api.tools.model.component.IApiProfile)
	 */
	public synchronized void addApiProfile(IApiProfile newprofile) {
		if(newprofile != null) {
			initializeStateCache();
			profilecache.put(newprofile.getName(), newprofile);
			fNeedsSaving = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#removeApiProfile(java.lang.String)
	 */
	public synchronized boolean removeApiProfile(String name) {
		if(name != null) {
			initializeStateCache();
			IApiProfile profile = (IApiProfile) profilecache.remove(name);
			if(profile != null) {
				profile.dispose();
				boolean success = true;
				if(savelocation == null) {
					return success;
				}
				//remove from filesystem
				File file = savelocation.append(name+".profile").toFile(); //$NON-NLS-1$
				if(file.exists()) {
					success &= file.delete();
				}
				fNeedsSaving = true;
				return success;
			}
		}
		return false;
	}
	
	/**
	 * Initializes the profile cache lazily. Only performs work
	 * if the current cache has not been created yet
	 * @throws FactoryConfigurationError 
	 * @throws ParserConfigurationException 
	 */
	private synchronized void initializeStateCache() {
		long time = System.currentTimeMillis();
		if(profilecache == null) {
			profilecache = new HashMap(3);
			if(!ApiPlugin.isRunningInFramework()) {
				return;
			}
			File[] profiles = savelocation.toFile().listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".profile"); //$NON-NLS-1$
				}
			});
			if(profiles != null) {
				InputStream fin = null;
				IApiProfile newprofile = null;
				for(int i = 0; i < profiles.length; i++) {
					File profile = profiles[i];
					if(profile.exists()) {
						try {
							fin = new FileInputStream(profile);
							newprofile = restoreProfile(fin);
							profilecache.put(newprofile.getName(), newprofile);
						}
						catch (IOException e) {
							ApiPlugin.log(e);
						}
						catch(CoreException e) {
							ApiPlugin.log(e.getStatus());
						}
					}
				}
			}
			String def = ApiPlugin.getDefault().getPluginPreferences().getString(DEFAULT_PROFILE);
			IApiProfile profile = (IApiProfile) profilecache.get(def);
			defaultprofile = (profile != null ? def : null);
			if(DEBUG) {
				System.out.println("Time to initialize state cache: " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	/**
	 * Persists all of the cached elements to individual xml files named 
	 * with the id of the API profile
	 * @throws IOException 
	 */
	private void persistStateCache() throws CoreException, IOException {
		if(savelocation == null) {
			return;
		}
		if(defaultprofile != null) {
			ApiPlugin.getDefault().getPluginPreferences().setValue(DEFAULT_PROFILE, defaultprofile);
		}
		if(profilecache != null) {
			File dir = new File(savelocation.toOSString());
			if(!dir.exists()) {
				dir.mkdirs();
			}
			String id = null;
			File file = null;
			FileOutputStream fout = null;
			IApiProfile profile = null;
			for(Iterator iter = profilecache.keySet().iterator(); iter.hasNext();) {
				id = (String) iter.next();
				profile = (IApiProfile) profilecache.get(id);
				file = savelocation.append(id+".profile").toFile(); //$NON-NLS-1$
				if(!file.exists()) {
					file.createNewFile();
				}
				try {
					fout = new FileOutputStream(file);
					profile.writeProfileDescription(fout);
					fout.flush();
				}
				finally {
					fout.close();
				}
			}
		}
	}	
	
	/**
	 * Throws a core exception with the given message and underlying exception,
	 * if any.
	 * 
	 * @param message error message
	 * @param e underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	private static void abort(String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, message, e));
	}	
	
	/**
	 * Constructs and returns a profile from the given input stream (persisted profile).
	 * 
	 * @param stream input stream
	 * @return API profile
	 * @throws CoreException if unable to restore the profile
	 */
	public static IApiProfile restoreProfile(InputStream stream) throws CoreException {
		long start = System.currentTimeMillis();
		DocumentBuilder parser = null;
		try {
			parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
		} catch (ParserConfigurationException e) {
			abort("Error restoring API profile", e); //$NON-NLS-1$
		} catch (FactoryConfigurationError e) {
			abort("Error restoring API profile", e); //$NON-NLS-1$
		}
		IApiProfile profile = null;
		try {
			Document document = parser.parse(stream);
			Element root = document.getDocumentElement();
			if(root.getNodeName().equals(IApiXmlConstants.ELEMENT_APIPROFILE)) {
				profile = new ApiProfile(root.getAttribute(IApiXmlConstants.ATTR_NAME));
				// un-pooled components
				NodeList children = root.getElementsByTagName(IApiXmlConstants.ELEMENT_APICOMPONENT);
				List components = new ArrayList();
				for(int j = 0; j < children.getLength(); j++) {
					Element componentNode = (Element) children.item(j);
					// this also contains components in pools, so don't process them
					if (componentNode.getParentNode().equals(root)) {
						String location = componentNode.getAttribute(IApiXmlConstants.ATTR_LOCATION);
						IApiComponent component = profile.newApiComponent(Path.fromPortableString(location).toOSString());
						if(component != null) {
							components.add(component);
						}
					}
				}
				// pooled components
				children = root.getElementsByTagName(IApiXmlConstants.ELEMENT_POOL);
				for(int j = 0; j < children.getLength(); j++) {
					String location = ((Element) children.item(j)).getAttribute(IApiXmlConstants.ATTR_LOCATION);
					IPath poolPath = Path.fromPortableString(location);
					NodeList componentNodes = root.getElementsByTagName(IApiXmlConstants.ELEMENT_APICOMPONENT);
					for (int i = 0; i < componentNodes.getLength(); i++) {
						Element compElement = (Element) componentNodes.item(i);
						String id = compElement.getAttribute(IApiXmlConstants.ATTR_ID);
						String ver = compElement.getAttribute(IApiXmlConstants.ATTR_VERSION);
						StringBuffer name = new StringBuffer();
						name.append(id);
						name.append('_');
						name.append(ver);
						File file = poolPath.append(name.toString()).toFile();
						if (!file.exists()) {
							name.append(".jar"); //$NON-NLS-1$
							file = poolPath.append(name.toString()).toFile();
						}
						IApiComponent component = profile.newApiComponent(file.getAbsolutePath());
						if(component != null) {
							components.add(component);
						}
					}
					
				}
				profile.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
			}
		} catch (IOException e) {
			abort("Error restoring API profile", e); //$NON-NLS-1$
		} catch(SAXException e) {
			abort("Error restoring API profile", e); //$NON-NLS-1$
		} finally {
			try {
				stream.close();
			} catch (IOException io) {
				ApiPlugin.log(io);
			}
		}
		if (profile == null) {
			abort("Invalid profile description", null); //$NON-NLS-1$
		}
		if(DEBUG) {
			System.out.println("Time to restore a persisted profile : " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return profile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) throws CoreException {
		if(!fNeedsSaving) {
			return;
		}
		try {
			persistStateCache();
			fNeedsSaving = false;
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Returns if the given name is an existing profile name
	 * @param name
	 * @return true if the given name is an existing profile name, false otherwise
	 */
	public boolean isExistingProfileName(String name) {
		if(profilecache == null) {
			return false;
		}
		return profilecache.keySet().contains(name);
	}
	
	/**
	 * Cleans up the manager
	 */
	public void stop() {
		try {
			if(profilecache != null) {
				// we should first dispose all existing profiles
				for (Iterator iterator = this.profilecache.values().iterator(); iterator.hasNext();) {
					IApiProfile profile = (IApiProfile) iterator.next();
					profile.dispose();
				}
				this.profilecache.clear();
			}
			if(this.workspaceprofile != null) {
				this.workspaceprofile.dispose();
			}
		}
		finally {
			if(ApiPlugin.isRunningInFramework()) {
				ApiPlugin.getDefault().removeSaveParticipant(this);
				JavaCore.removeElementChangedListener(this);
				PDECore.getDefault().getModelManager().removePluginModelListener(this);
				ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse.core.resources.ISaveContext)
	 */
	public void doneSaving(ISaveContext context) {}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	public void prepareToSave(ISaveContext context) throws CoreException {	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	public void rollback(ISaveContext context) {}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#getDefaultApiProfile()
	 */
	public synchronized IApiProfile getDefaultApiProfile() {
		initializeStateCache();
		return (IApiProfile) profilecache.get(defaultprofile);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#setDefaultApiProfile(java.lang.String)
	 */
	public void setDefaultApiProfile(String name) {
		fNeedsSaving = true;
		defaultprofile = name;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager#getWorkspaceProfile()
	 */
	public synchronized IApiProfile getWorkspaceProfile() {
		if(ApiPlugin.isRunningInFramework()) {
			if(this.workspaceprofile == null) {
				this.workspaceprofile = createWorkspaceProfile();
			}
			return this.workspaceprofile;
		}
		return null;
	}	
	
	/**
	 * Disposes the workspace profile such that a new one will be created
	 * on the next request.
	 */
	private synchronized void disposeWorkspaceProfile() {
		if (workspaceprofile != null) {
			workspaceprofile.dispose();
			workspaceprofile = null;
		}
	}
		
	/**
	 * Creates a workspace {@link IApiProfile}
	 * @return a new workspace {@link IApiProfile} or <code>null</code>
	 */
	private IApiProfile createWorkspaceProfile() {
		long time = System.currentTimeMillis();
		IApiProfile profile = null; 
		try {
			profile = Factory.newApiProfile(ApiPlugin.WORKSPACE_API_PROFILE_ID);
			// populate it with only projects that are API aware
			IPluginModelBase[] models = PluginRegistry.getActiveModels();
			List componentsList = new ArrayList(models.length);
			IApiComponent apiComponent = null;
			for (int i = 0, length = models.length; i < length; i++) {
				try {
					apiComponent = profile.newApiComponent(models[i]);
					if (apiComponent != null) {
						componentsList.add(apiComponent);
					}
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
			profile.addApiComponents((IApiComponent[]) componentsList.toArray(new IApiComponent[componentsList.size()]));
		} finally {
			if (DEBUG) {
				System.out.println("Time to create a workspace profile : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return profile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		if(!ApiPlugin.isRunningInFramework()) {
			return;
		}
		Object obj = event.getSource();
		if(obj instanceof IJavaElementDelta) {
			processJavaElementDeltas(((IJavaElementDelta)obj).getAffectedChildren(), null);
		}
	}
	
	/**
	 * Processes the java element deltas of interest
	 * @param deltas
	 */
	private synchronized void processJavaElementDeltas(IJavaElementDelta[] deltas, IJavaProject project) {
		try {
			IJavaElementDelta delta = null;
			for(int i = 0; i < deltas.length; i++) {
				delta = deltas[i];
				switch(delta.getElement().getElementType()) {
					case IJavaElement.JAVA_PROJECT: {
						IJavaProject proj = (IJavaProject) delta.getElement();
						IProject pj = proj.getProject();
						if (acceptProject(pj)) {
							switch (delta.getKind()) {
								//process the project changed only if the project is API aware
							case IJavaElementDelta.CHANGED:
								int flags = delta.getFlags();
								if( (flags & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0 ||
									(flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0 ||
									(flags & IJavaElementDelta.F_CLOSED) != 0 ||
									(flags & IJavaElementDelta.F_OPENED) != 0) {
										if(DEBUG) {
											System.out.println("--> processing CLASSPATH CHANGE/CLOSE/OPEN project: ["+proj.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
										}
										disposeWorkspaceProfile();
								} else if((flags & IJavaElementDelta.F_CHILDREN) != 0) {
									if(DEBUG) {
										System.out.println("--> processing child deltas of project: ["+proj.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
									}
									processJavaElementDeltas(delta.getAffectedChildren(), proj);
								}
								break;
							}
						}
						break;
					}
					case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
						IPackageFragmentRoot root = (IPackageFragmentRoot) delta.getElement();
						if(DEBUG) {
							System.out.println("processed package fragment root delta: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						switch(delta.getKind()) {
							case IJavaElementDelta.CHANGED: {
								if(DEBUG) {
									System.out.println("processed children of CHANGED package fragment root: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								processJavaElementDeltas(delta.getAffectedChildren(), project);
								break;
							}
						}
						break;
					}
					case IJavaElement.PACKAGE_FRAGMENT: {
						IPackageFragment fragment = (IPackageFragment) delta.getElement();
						if(delta.getKind() == IJavaElementDelta.REMOVED) {
							handlePackageRemoval(project.getProject(), fragment);
						}
						break;
					}
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}
		
	/**
	 * Handles the specified {@link IPackageFragment} being removed.
	 * When a packaged is removed, we:
	 * <ol>
	 * <li>Remove the package from the cache of resolved providers
	 * 	of that package (in the API profile)</li>
	 * </ol>
	 * @param project
	 * @param fragment
	 * @throws CoreException
	 */
	private void handlePackageRemoval(IProject project, IPackageFragment fragment) throws CoreException {
		if(DEBUG) {
			System.out.println("processed package fragment REMOVE delta: ["+fragment.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		((ApiProfile)getWorkspaceProfile()).clearPackage(fragment.getElementName());
	}
	
	/**
	 * Returns if we should care about the specified project
	 * @param project
	 * @return true if the project is an 'API aware' project, false otherwise
	 */
	private boolean acceptProject(IProject project) {
		try {
			if (!project.isOpen()) {
				return true;
			}
			return project.exists() && project.hasNature(ApiPlugin.NATURE_ID);
		}
		catch(CoreException e) {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * 
	 * Whenever a bundle definition changes (add/removed/changed), the 
	 * workspace profile becomes potentially invalid as the bundle description
	 * may have changed in some way to invalidate our underlying OSGi state.
	 * 
	 * @see org.eclipse.pde.internal.core.IPluginModelListener#modelsChanged(org.eclipse.pde.internal.core.PluginModelDelta)
	 */
	public void modelsChanged(PluginModelDelta delta) {
		if(!ApiPlugin.isRunningInFramework()) {
			return;
		}
		ModelEntry[] entries = null;
		switch(delta.getKind()) {
			case PluginModelDelta.ADDED: {
				entries = delta.getAddedEntries();
				break;
			}
			case PluginModelDelta.REMOVED: {
				entries = delta.getRemovedEntries();
				break;
			}
			case PluginModelDelta.CHANGED: {
				entries = delta.getChangedEntries();
				break;
			}
		}
		if(entries != null) {
			IPluginModelBase model = null;
			for(int i = 0; i < entries.length; i++) {
				model = entries[i].getModel();
				if(model != null) {
					disposeWorkspaceProfile();
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		if(!ApiPlugin.isRunningInFramework()) {
			return;
		}
		// clean all API errors when a project description changes
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
			IResourceDelta[] children = delta.getAffectedChildren(IResourceDelta.CHANGED);
			for (int i = 0; i < children.length; i++) {
				IResourceDelta d = children[i];
				IResource resource = d.getResource();
				if (resource.getType() == IResource.PROJECT) {
					if ((d.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
						IProject project = (IProject)resource;
						if (project.isAccessible()) {
							try {
								if (!project.getDescription().hasNature(ApiPlugin.NATURE_ID)) {
									IJavaProject jp = JavaCore.create(project);
									if (jp.exists()) {
										ApiDescriptionManager.getDefault().clean(jp, true, true);
										ApiAnalysisBuilder.cleanupMarkers(resource);
									}
								}
							} catch (CoreException e) {
								ApiPlugin.log(e.getStatus());
							}
						}
						disposeWorkspaceProfile();
					}
				}
			}
		}
	}

}
