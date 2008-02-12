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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
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
public final class ApiProfileManager implements IApiProfileManager, ISaveParticipant, IElementChangedListener, IPluginModelListener {
	
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
	 * Constant representing the API profile node name for an API profile xml file.
	 * Value is <code>apiprofile</code>
	 */
	private static final String ELEMENT_APIPROFILE = "apiprofile";  //$NON-NLS-1$
	/**
	 * Constant representing the API component node name for an API profile xml file.
	 * Value is <code>apicomponent</code>
	 */
	private static final String ELEMENT_APICOMPONENT = "apicomponent";  //$NON-NLS-1$
	/**
	 * Constant representing the API component pool node name for an API profile xml file.
	 * Value is <code>pool</code>
	 */
	private static final String ELEMENT_POOL = "pool";  //$NON-NLS-1$	
	/**
	 * Constant representing the id attribute name for an API profile xml file.
	 * Value is <code>id</code>
	 */
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	/**
	 * Constant representing the version attribute name for an API profile xml file.
	 * Value is <code>version</code>
	 */
	private static final String ATTR_VERSION = "version"; //$NON-NLS-1$
	/**
	 * Constant representing the name attribute name for an API profile xml file.
	 * Value is <code>name</code>
	 */
	private static final String ATTR_NAME = "name"; //$NON-NLS-1$
	/**
	 * Constant representing the location attribute name for an API profile xml file.
	 * Value is <code>location</code>
	 */
	private static final String ATTR_LOCATION = "location"; //$NON-NLS-1$
	/**
	 * Constant representing the ee attribute name for an API profile xml file.
	 * Value is <code>ee</code>
	 */
	private static final String ELEMENT_EE = "ee"; //$NON-NLS-1$
	
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
	 * The saved state from the last session the manager participated in
	 */
	private ISavedState savedstate = null;
	
	/**
	 * The default save location for persisting the cache from this manager.
	 */
	private IPath savelocation = ApiPlugin.getDefault().getStateLocation().append(".api_profiles").addTrailingSeparator(); //$NON-NLS-1$
	
	/**
	 * Constructor
	 */
	public ApiProfileManager(ISavedState savestate) {
		ApiPlugin.getDefault().addSaveParticipant(this);
		JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
		//we must load the workspace profile as soon as the manager starts to avoid 
		//'holes' from missing workspace resource deltas
		this.savedstate = savestate;
		getWorkspaceProfile();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#getApiProfile(java.lang.String)
	 */
	public synchronized IApiProfile getApiProfile(String profileid) {
		initializeStateCache();
		return (ApiProfile) profilecache.get(profileid);
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
			profilecache.put(newprofile.getId(), newprofile);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#removeApiProfile(java.lang.String)
	 */
	public synchronized boolean removeApiProfile(String id) {
		if(id != null) {
			initializeStateCache();
			IApiProfile profile = (IApiProfile) profilecache.remove(id);
			if(profile != null) {
				profile.dispose();
				boolean success = true;
				//remove from filesystem
				File file = savelocation.append(id+".profile").toFile(); //$NON-NLS-1$
				if(file.exists()) {
					success &= file.delete();
				}
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
			profilecache = new HashMap();
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
							profilecache.put(newprofile.getId(), newprofile);
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
		if(defaultprofile != null) {
			ApiPlugin.getDefault().getPluginPreferences().setValue(DEFAULT_PROFILE, defaultprofile);
		}
		if(profilecache != null) {
			File dir = new File(savelocation.toOSString());
			if(!dir.exists()) {
				dir.mkdirs();
			}
			String xml = null;
			String id = null;
			File file = null;
			FileOutputStream fout = null;
			for(Iterator iter = profilecache.keySet().iterator(); iter.hasNext();) {
				id = (String) iter.next();
				xml = getStateAsXML(id);
				file = savelocation.append(id+".profile").toFile(); //$NON-NLS-1$
				if(!file.exists()) {
					file.createNewFile();
				}
				try {
					fout = new FileOutputStream(file);
					fout.write(xml.getBytes("UTF8")); //$NON-NLS-1$
					fout.flush();
				}
				finally {
					fout.close();
				}
			}
		}
	}
	
	/**
	 * Returns a UTF-8 string representing the contents of a profile xml file or <code>null</code>
	 * if the corresponding profile for the given id does not exist or there was an error 
	 * serializing the document
	 * @param id
	 * @return a UTF-8 xml string of the contents of a profile xml file, or <code>null</code>
	 * @throws CoreException
	 */
	private synchronized String getStateAsXML(String id) throws CoreException {
		if(profilecache != null) {
			IApiProfile profile = (IApiProfile) profilecache.get(id);
			if(profile != null) {
				return getProfileXML(profile);
			}
		}
		return null;
	}

	/**
	 * Returns an XML description of the given profile.
	 * 
	 * @param profile API profile
	 * @return XML
	 * @throws CoreException
	 */
	public static String getProfileXML(IApiProfile profile) throws CoreException {
		Document document = Util.newDocument();
		Element root = document.createElement(ELEMENT_APIPROFILE);
		document.appendChild(root);
		root.setAttribute(ATTR_ID, profile.getId());
		root.setAttribute(ATTR_NAME, profile.getName());
		root.setAttribute(ATTR_VERSION, profile.getVersion());
		root.setAttribute(ELEMENT_EE, profile.getExecutionEnvironment());
		// pool bundles by location
		Map pools = new HashMap();
		List unRooted = new ArrayList();
		IApiComponent[] components = profile.getApiComponents();
		for(int i = 0; i < components.length; i++) {
			if(!components[i].isSystemComponent()) {
				String location = components[i].getLocation();
				File file = new File(location);
				if (file.exists()) {
					File dir = file.getParentFile();
					if (dir != null) {
						List pool = (List) pools.get(dir);
						if (pool == null) {
							pool = new ArrayList();
							pools.put(dir, pool);
						}
						pool.add(components[i]);
					} else {
						unRooted.add(components[i]);
					}
				}
				
			}
		}
		Iterator iterator = pools.entrySet().iterator();
		// dump component pools
		while (iterator.hasNext()) {
			Entry entry = (Entry) iterator.next();
			File pool = (File) entry.getKey();
			Element poolElement = document.createElement(ELEMENT_POOL);
			root.appendChild(poolElement);
			poolElement.setAttribute(ATTR_LOCATION, new Path(pool.getAbsolutePath()).toPortableString());
			List comps = (List) entry.getValue();
			Iterator poolIterator = comps.iterator();
			while (poolIterator.hasNext()) {
				IApiComponent component = (IApiComponent) poolIterator.next();
				Element compElement = document.createElement(ELEMENT_APICOMPONENT);
				compElement.setAttribute(ATTR_ID, component.getId());
				compElement.setAttribute(ATTR_VERSION, component.getVersion());
				poolElement.appendChild(compElement);
			}
		}
		// dump un-pooled components
		iterator = unRooted.iterator();
		while (iterator.hasNext()) {
			IApiComponent component = (IApiComponent) iterator.next();
			Element compElement = document.createElement(ELEMENT_APICOMPONENT);
			compElement.setAttribute(ATTR_ID, component.getId());
			compElement.setAttribute(ATTR_VERSION, component.getVersion());
			compElement.setAttribute(ATTR_LOCATION, new Path(component.getLocation()).toPortableString());
			root.appendChild(compElement);
		}
		return Util.serializeDocument(document);
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
			if(root.getNodeName().equals(ELEMENT_APIPROFILE)) {
				profile = new ApiProfile(root.getAttribute(ATTR_NAME),
						root.getAttribute(ATTR_ID),
						root.getAttribute(ATTR_VERSION),
						Util.createEEFile(root.getAttribute(ELEMENT_EE)));
				// un-pooled components
				NodeList children = root.getElementsByTagName(ELEMENT_APICOMPONENT);
				List components = new ArrayList();
				for(int j = 0; j < children.getLength(); j++) {
					Element componentNode = (Element) children.item(j);
					// this also contains components in pools, so don't process them
					if (componentNode.getParentNode().equals(root)) {
						String location = componentNode.getAttribute(ATTR_LOCATION);
						IApiComponent component = profile.newApiComponent(Path.fromPortableString(location).toOSString());
						if(component != null) {
							components.add(component);
						}
					}
				}
				// pooled components
				children = root.getElementsByTagName(ELEMENT_POOL);
				for(int j = 0; j < children.getLength(); j++) {
					String location = ((Element) children.item(j)).getAttribute(ATTR_LOCATION);
					IPath poolPath = Path.fromPortableString(location);
					NodeList componentNodes = root.getElementsByTagName(ELEMENT_APICOMPONENT);
					for (int i = 0; i < componentNodes.getLength(); i++) {
						Element compElement = (Element) componentNodes.item(i);
						String id = compElement.getAttribute(ATTR_ID);
						String ver = compElement.getAttribute(ATTR_VERSION);
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
				profile.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]), true);
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
		try {
			persistStateCache();
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Cleans up the manager and persists any unsaved Api profiles
	 */
	public void stop() {
		try {
			// we should first dispose all existing profiles
			for (Iterator iterator = this.profilecache.values().iterator(); iterator.hasNext();) {
				IApiProfile profile = (IApiProfile) iterator.next();
				profile.dispose();
			}
			this.profilecache.clear();
			if(this.workspaceprofile != null) {
				this.workspaceprofile.dispose();
			}
		}
		finally {
			JavaCore.removeElementChangedListener(this);
			PDECore.getDefault().getModelManager().removePluginModelListener(this);
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
	public void setDefaultApiProfile(String id) {
		defaultprofile = id;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiProfileManager#getWorkspaceProfile()
	 */
	public IApiProfile getWorkspaceProfile() {
		if(ApiPlugin.isRunningInFramework()) {
			if(this.workspaceprofile == null) {
				this.workspaceprofile = createWorkspaceProfile();
				annotateWorkspaceProfile();
			}
			return this.workspaceprofile;
		}
		return null;
	}
	
	/**
	 * Annotates the restored workspace profile with any missing changes since the last 
	 * save cycle it participated in
	 */
	private void annotateWorkspaceProfile() {
		if(this.savedstate != null) {
			this.savedstate.processResourceChangeEvents(new IResourceChangeListener() {
				public void resourceChanged(IResourceChangeEvent event) {
					IResourceDelta delta = event.getDelta();
					if(delta != null) {
						if(DEBUG) {
							System.out.println("processing saved state resource delta..."); //$NON-NLS-1$
						}
						try {
							processResourceDeltas(delta.getAffectedChildren(), null, null);
						}
						catch (CoreException e) {
							ApiPlugin.log(e);
						}
					}
				}
			});
			//once we are done with it throw it away
			this.savedstate = null;
		}
	}
	
	/**
	 * Processes resource deltas from the last saved state of the manager
	 * @param deltas
	 * @param project
	 * @throws CoreException
	 */
	private void processResourceDeltas(IResourceDelta[] deltas, IJavaProject project, IPackageFragment fragment) throws CoreException {
		IResourceDelta delta = null;
		IResource resource = null;
		for(int i = 0; i < deltas.length; i++) {
			delta = deltas[i];
			resource = delta.getResource();
			switch(resource.getType()) {
				case IResource.PROJECT: {
					IProject proj = (IProject) resource;
					if(delta.getKind() == IResourceDelta.ADDED || 
							(delta.getFlags() & IResourceDelta.OPEN) != 0) {
						handleProjectAddition(proj);
					}
					else if(delta.getKind() == IResourceDelta.REMOVED || 
							(delta.getFlags() & IResourceDelta.OPEN) != 0) {
						handleProjectRemoval(proj);
					}
					else if(delta.getKind() == IResourceDelta.CHANGED) {
						handleProjectChanged(proj);
						if(DEBUG) {
							System.out.println("--> processing child deltas of project: ["+proj.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						processResourceDeltas(delta.getAffectedChildren(), JavaCore.create(proj), fragment);
					}
					break;
				}
				case IResource.FOLDER: {
					IFolder folder = (IFolder) resource;
					IJavaElement element = project.findPackageFragmentRoot(folder.getFullPath());
					if(element == null) {
						element = project.findPackageFragment(folder.getFullPath());
					}
					if(element == null) {
						break;
					}
					switch(element.getElementType()) {
						case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
							IPackageFragmentRoot root = (IPackageFragmentRoot) element;
							if(DEBUG) {
								System.out.println("processed package fragment root delta: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
							}
							int flags = delta.getFlags();
							if((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0 ||
									(flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) {
								if(DEBUG) {
									System.out.println("processed ADD / REMOVE package fragment root: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								handleClasspathChanged(project.getProject());
							}
							processResourceDeltas(delta.getAffectedChildren(), project, fragment);
							break;
						}
						case IJavaElement.PACKAGE_FRAGMENT: {
							IPackageFragment frag = (IPackageFragment) element;
							if(delta.getKind() == IResourceDelta.REMOVED) {
								handlePackageRemoval(project.getProject(), fragment);
							}
							else {
								if(DEBUG) {
									System.out.println("processed package fragment CHANGED delta: ["+frag.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
									System.out.println("--> processing child deltas of package fragment: ["+frag.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								processResourceDeltas(delta.getAffectedChildren(), project, frag);
							}
							break;	
						}	
					}
					break;
				}
				case IResource.FILE: {
					IFile file = (IFile) resource;
					if("java".equalsIgnoreCase(file.getFileExtension())) { //$NON-NLS-1$
						ICompilationUnit unit = fragment.getCompilationUnit(file.getName());
						if(unit == null) {
							break;
						}
						IApiComponent component = this.workspaceprofile.getApiComponent(project.getElementName());
						if(component != null) {
							if(delta.getKind() == IResourceDelta.ADDED) {
								handleCompilationUnitScan(component, unit);
							}
							else if(delta.getKind() == IResourceDelta.REMOVED) {
								if(DEBUG) {
									System.out.println("\tprocessed compilation unit REMOVE delta: ["+unit.getElementName()+"]");  //$NON-NLS-1$//$NON-NLS-2$
								}
								handleCompilationUnitRemoval(component, unit);
							}
							else if(delta.getKind() == IResourceDelta.CHANGED) {  
								if((delta.getFlags() & IResourceDelta.CONTENT) != 0 ||
										(delta.getFlags() & IResourceDelta.REPLACED) != 0) {
									handleCompilationUnitScan(component, unit);
								}
							}
						}
					}
					else if (".classpath".equalsIgnoreCase(file.getName())) { //$NON-NLS-1$
						handleClasspathChanged(project.getProject());
					}
					break;
				}
			}
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
			profile = Factory.newApiProfile(ApiPlugin.WORKSPACE_API_PROFILE_ID, ApiPlugin.WORKSPACE_API_PROFILE_ID, "CURRENT", Util.createDefaultEEFile()); //$NON-NLS-1$
			// populate it with only projects that are api aware
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
			IApiComponent[] components = new IApiComponent[componentsList.size()];
			componentsList.toArray(components);
			profile.addApiComponents(components, true);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		} catch(IOException e) {
			ApiPlugin.log(e);
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
						//process a project addition / opening, only if the project is an API aware project
						if(acceptProject(pj) && (delta.getKind() == IJavaElementDelta.ADDED ||
								(delta.getFlags() & IJavaElementDelta.F_OPENED) != 0)) {
							handleProjectAddition(pj);
							return;
						}
						//process a project removal /closure. 
						//we cannot tell if it is API aware as the project is no longer accessible at this point;
						//so we cannot ask if we accept it, we just have to try and remove it from the description
						else if(delta.getKind() == IJavaElementDelta.REMOVED ||
								(delta.getFlags() & IJavaElementDelta.F_CLOSED) != 0) {
							handleProjectRemoval(pj);
							return;
						}
						//process the project changed only if the project is API aware
						else if(acceptProject(pj) && delta.getKind() == IJavaElementDelta.CHANGED) {
							handleProjectChanged(pj);
							int flags = delta.getFlags();
							if((flags & IJavaElementDelta.F_CHILDREN) != 0) {
								if(DEBUG) {
									System.out.println("--> processing child deltas of project: ["+proj.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								processJavaElementDeltas(delta.getAffectedChildren(), proj);
							}
							if((flags & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED) != 0 &&
								(flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0 &&
								(flags & IJavaElementDelta.F_CONTENT) != 0) {
								handleClasspathChanged(pj);
							}
						}
						break;
					}
					case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
						//fragment roots do not appear in an API description anywhere, only process the children, if any
						IPackageFragmentRoot root = (IPackageFragmentRoot) delta.getElement();
						if(DEBUG) {
							System.out.println("processed package fragment root delta: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						int flags = delta.getFlags();
						switch(delta.getKind()) {
							case IJavaElementDelta.ADDED:
							case IJavaElementDelta.REMOVED: {
								if(root.getKind() == IPackageFragmentRoot.K_BINARY) {
									if(DEBUG) {
										System.out.println("processed ADD / REMOVE binary package fragment root: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
									}
									handleClasspathChanged(project.getProject());
								}
								break;
							}
							case IJavaElementDelta.CHANGED: {
								if((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0 ||
									(flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) {
									if(DEBUG) {
										System.out.println("processed CHANGED package fragment root: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
									}
									handleClasspathChanged(project.getProject());
								}
								break;
							}
						}
						if(DEBUG) {
							System.out.println("--> processing child deltas of package fragment root: ["+root.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						processJavaElementDeltas(delta.getAffectedChildren(), project);
						break;
					}
					case IJavaElement.PACKAGE_FRAGMENT: {
						IPackageFragment fragment = (IPackageFragment) delta.getElement();
						//we do not want to process an add delta, for the sake of keeping the API description sparse,
						//as we would only add the package as inherited visibility anyway
						if(delta.getKind() == IJavaElementDelta.REMOVED) {
							handlePackageRemoval(project.getProject(), fragment);
						}
						else {
							if(DEBUG) {
								System.out.println("processed package fragment CHANGED delta: ["+fragment.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								System.out.println("--> processing child deltas of package fragment: ["+fragment.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
							}
							processJavaElementDeltas(delta.getAffectedChildren(), project);
						}
						break;
					}
					case IJavaElement.COMPILATION_UNIT: {
						ICompilationUnit unit = (ICompilationUnit) delta.getElement();
						IApiComponent component = getWorkspaceProfile().getApiComponent(project.getElementName());
						if(component != null) {
							if(delta.getKind() == IJavaElementDelta.ADDED) {
								if(DEBUG) {
									System.out.println("\tprocessed compilation unit ADD delta: ["+unit.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
								}
								ICompilationUnit moved = (ICompilationUnit) delta.getMovedFromElement();
								if(moved != null) {
									handleCompilationUnitRemoval(component, moved);
								}
								handlePotentialNewPackage((IPackageFragment)unit.getParent());
								handleCompilationUnitScan(component, unit);
							}
							else if(delta.getKind() == IJavaElementDelta.REMOVED /*&& 
									(delta.getFlags() & IJavaElementDelta.F_PRIMARY_WORKING_COPY) == 0*/) {
								//if an editor is open we will not get just a plain removed delta, it will always be a 
								//primary working copy delta, otherwise we could use the additional condition to prune some deltas
								if(DEBUG) {
									System.out.println("\tprocessed compilation unit REMOVE delta: ["+unit.getElementName()+"]");  //$NON-NLS-1$//$NON-NLS-2$
								}
								if(delta.getMovedToElement() == null) {
									handleCompilationUnitRemoval(component, unit);
								}
							}
							else if(delta.getKind() == IJavaElementDelta.CHANGED) {
								if((delta.getFlags() & IJavaElementDelta.F_CONTENT) != 0 ||
										(delta.getFlags() & IJavaElementDelta.F_PRIMARY_RESOURCE) != 0 ) {
									if(DEBUG) {
										System.out.println("\tprocessed compilation unit CONTENT & CHANGED delta: ["+unit.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
									}
									handleCompilationUnitScan(component, unit);
								}
							}
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
	 * Handles the specified {@link IProject} being added / opened
	 * @param project
	 */
	private void handleProjectAddition(IProject project) throws CoreException {
		//the project has been added, create a new IApiComponent for it
		if(project.exists() && project.isOpen() && project.hasNature(ApiPlugin.NATURE_ID)) {
			//do no work for non-API tooling projects
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			if(model != null) {
				IApiProfile profile = getWorkspaceProfile();
				IApiComponent component = profile.newApiComponent(model);
				profile.addApiComponents(new IApiComponent[] {component}, true);
				if(DEBUG) {
					System.out.println("process project ADD/OPENED delta: ["+project.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			return;
		}
	}
	
	/**
	 * Handles a change to the classpath
	 * @param root
	 */
	private void handleClasspathChanged(IProject project) {
		if(DEBUG) {
			System.out.println("processed CLASSPATH change for project: ["+project.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		reset(project.getName());
	}
	
	/**
	 * Resets the component with the given id.
	 * 
	 * @param componentId
	 */
	private void reset(String componentId) {
		IApiComponent component = getWorkspaceProfile().getApiComponent(componentId);
		if(component instanceof AbstractApiComponent) {
			try {
				((AbstractApiComponent) component).reset();
				if(DEBUG) {
					System.out.println("reset component: ["+componentId+"]"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (CoreException e) {
				ApiPlugin.log(e.getStatus());
			}
		}
	}
	
	/**
	 * Handles the specified project being removed / closed
	 * @param project
	 */
	private void handleProjectRemoval(IProject project) {
		//the project has been removed remove the IApiComponent for it
		IApiComponent component = this.workspaceprofile.getApiComponent(project.getName());
		if(component != null) {
			getWorkspaceProfile().removeApiComponents(new IApiComponent[] {component});
			if(DEBUG) {
				System.out.println("processed project CLOSED/REMOVED delta: ["+project.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return;
		}	
	}
	
	/**
	 * Handles the specified project being changed
	 * @param project
	 * @throws CoreException
	 */
	private void handleProjectChanged(IProject project) throws CoreException {
		if(DEBUG) {
			System.out.println("processed project CHANGED delta: ["+project.getName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		IApiProfile profile = getWorkspaceProfile();
		IApiComponent component = profile.getApiComponent(project.getName());
		if(component == null) {
			//create a new component
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			if(model != null) {
				IApiComponent newcomponent = profile.newApiComponent(model);
				if(newcomponent != null) {
					profile.addApiComponents(new IApiComponent[] {newcomponent}, true);
				}
			}
		}
	}
	
	/**
	 * Handles the removal of the specified {@link ICompilationUnit}
	 * @param component
	 * @param unit
	 * @throws CoreException
	 */
	private void handleCompilationUnitRemoval(IApiComponent component, ICompilationUnit unit) throws CoreException {
		if(component.getApiDescription().removeElement(Factory.typeDescriptor(createFullyQualifiedName(unit)))) {
			if(DEBUG) {
				System.out.println("\tremoved compilation unit from API description: ["+unit.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	/**
	 * Handles the addition or change of the specified {@link ICompilationUnit}
	 * @param component
	 * @param unit
	 * @throws CoreException
	 */
	private void handleCompilationUnitScan(IApiComponent component, ICompilationUnit unit) throws CoreException {
		if(DEBUG) {
			System.out.println("\tprocessed compilation unit SCAN: ["+unit.getElementName()+"]");  //$NON-NLS-1$//$NON-NLS-2$
		}
		if(unit.exists() && unit.isConsistent() && unit.getUnderlyingResource().exists()) {
			//the scanner does not remove changes, only annotates with additions.
			//we need to remove the unit from its owning description and add back
			//annotations.
			//since profiles are sparse this is an insignificant amount of work
			handleCompilationUnitRemoval(component, unit);
			scanCompilationUnit(unit, component);
		}
	}
	
	/**
	 * Handles the specified {@link IPackageFragment} being removed.
	 * When a packaged is removed, we:
	 * <ol>
	 * <li>Remove the package from its API description</li>
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
		IApiComponent component = getWorkspaceProfile().getApiComponent(project.getName());
		if(component != null) {
			component.getApiDescription().removeElement(Factory.packageDescriptor(fragment.getElementName()));
			((ApiProfile)getWorkspaceProfile()).clearPackage(fragment.getElementName());
		}
	}
	
	/**
	 * A type has been added to a package fragment, see if the package
	 * is in the API description yet. We add packages when a type is
	 * added to avoid adding empty packages to the API description.
	 * When a packaged is added, we:
	 * <ol>
	 * <li>Add the package to its API description with default (private
	 *  visibility)</li>
	 * </ol>
	 * @param fragment package fragment
	 */
	private void handlePotentialNewPackage(IPackageFragment fragment) {
		IApiComponent component = getWorkspaceProfile().getApiComponent(fragment.getJavaProject().getElementName());
		if (component != null) {
			IPackageDescriptor descriptor = Factory.packageDescriptor(fragment.getElementName());
			try {
				IApiDescription apiDescription = component.getApiDescription();
				IApiAnnotations annotations = apiDescription.resolveAnnotations(null, descriptor);
				if (annotations == null) {
					// add default private visibility
					apiDescription.setVisibility(null, descriptor, VisibilityModifiers.PRIVATE);
				}
			} catch (CoreException e) {
				ApiPlugin.log(e.getStatus());
			}
		}
	}
	
	/**
	 * Returns if we should care about the specified project
	 * @param project
	 * @return true if the project is an 'api aware' project, false otherwise
	 */
	private boolean acceptProject(IProject project) {
		try {
			return project.exists() && project.hasNature(ApiPlugin.NATURE_ID);
		}
		catch(CoreException e) {
			return false;
		}
	}
	
	/**
	 * Creates a fully qualified name from a compilation unit. This method can be used when the compilation
	 * unit no longer exists in the java model
	 * @param unit
	 * @return
	 * @throws JavaModelException
	 */
	private String createFullyQualifiedName(ICompilationUnit unit) throws JavaModelException {
		StringBuffer name = new StringBuffer();
		IJavaElement parent = unit.getParent();
		if(parent != null) {
			while(parent != null) {
				name.insert(0, "."); //$NON-NLS-1$
				name.insert(0, parent.getElementName());
				parent = parent.getParent();
				if(parent.getElementType() != IJavaElement.JAVA_PROJECT ||
						parent.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT) {
					parent = null;
				}
			}
		}
		String typename = unit.getElementName();
		//peel off the file extension
		typename = typename.substring(0, typename.lastIndexOf(".")); //$NON-NLS-1$
		name.append(typename);
		return name.toString();
	}
	
	/**
	 * Scans the specified {@link ICompilationUnit}
	 * @param unit
	 * @param component
	 * @throws CoreException
	 */
	private void scanCompilationUnit(ICompilationUnit unit, IApiComponent component) throws CoreException {
		TagScanner scanner = TagScanner.newScanner();
		try {
			scanner.scan(new CompilationUnit(unit.getResource().getLocation().toOSString()), component.getApiDescription(), component);
			if(DEBUG) {
				System.out.println("\tscanned compilation unit: ["+unit.getElementName()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (FileNotFoundException e) {
			abort("Unable to initialize from Javadoc tags", e); //$NON-NLS-1$
		} catch (IOException e) {
			abort("Unable to initialize from Javadoc tags", e); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.IPluginModelListener#modelsChanged(org.eclipse.pde.internal.core.PluginModelDelta)
	 */
	public void modelsChanged(PluginModelDelta delta) {
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
					try {
						handleBundleDefinitionChanged(model);
					}
					catch(CoreException e) {
						ApiPlugin.log(e);
					}
				}
			}
		}
	}
	
	/**
	 * Handles updating when a bundle definition has changed.
	 * This causes the bundle to be reset/reinitialized the state
	 * with a new bundle description. Happens whenever the manifest
	 * or build.properties changes.
	 *  
	 * @param model
	 * @throws CoreException
	 */
	private void handleBundleDefinitionChanged(IPluginModelBase model) throws CoreException {
		reset(model.getBundleDescription().getName());
	}
}
