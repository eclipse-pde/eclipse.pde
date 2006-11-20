/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;

public class ClasspathHelper {

	public static String getDevEntriesProperties(String fileName, boolean checkExcluded) {
		File file = new File(fileName);
		if (!file.exists()) {
			File directory = file.getParentFile();
			if (directory != null && (!directory.exists() || directory.isFile())) {
				directory.mkdirs();
			}
		}
		Properties properties = new Properties();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase[] models = manager.getWorkspaceModels();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null)
				continue;
			String entry = writeEntry(getDevPaths(models[i], checkExcluded, null));
			if (entry.length() > 0)
				properties.put(id, entry);
		}
		properties.put("@ignoredot@", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(fileName);
			properties.store(stream, ""); //$NON-NLS-1$
			stream.flush();
			return new URL("file:" + fileName).toString(); //$NON-NLS-1$
		} catch (IOException e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
			}			
		}
		return getDevEntries(checkExcluded);
	}

    public static String getDevEntriesProperties(String fileName, Map map) {
        File file = new File(fileName);
        if (!file.exists()) {
            File directory = file.getParentFile();
            if (directory != null && (!directory.exists() || directory.isFile())) {
                directory.mkdirs();
            }
        }
        Properties properties = new Properties();
        Iterator iter = map.values().iterator();
        while (iter.hasNext()) {
            IPluginModelBase model = (IPluginModelBase)iter.next();
            if (model.getUnderlyingResource() != null) {
                String entry = writeEntry(getDevPaths(model, true, map));
                if (entry.length() > 0)
                    properties.put(model.getPluginBase().getId(), entry);                
            }
        }
        properties.put("@ignoredot@", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(fileName);
            properties.store(stream, ""); //$NON-NLS-1$
            stream.flush();
            return new URL("file:" + fileName).toString(); //$NON-NLS-1$
        } catch (IOException e) {
            PDECore.logException(e);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {
            }           
        }
        return getDevEntries(true);
    }

	public static String getDevEntries(boolean checkExcluded) {
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getWorkspaceModels();
		ArrayList list = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null || id.trim().length() == 0)
				continue;
			IPath[] paths = getDevPaths(models[i], checkExcluded, null);
			for (int j = 0; j < paths.length; j++) {
				list.add(paths[j]);
			}
		}
		String entry = writeEntry((IPath[])list.toArray(new IPath[list.size()]));
		return entry.length() > 0 ? entry : "bin"; //$NON-NLS-1$
	}
	
	private static String writeEntry(IPath[] paths) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < paths.length; i++) {
			buffer.append(paths[i].toString());
			if (i < paths.length - 1)
				buffer.append(","); //$NON-NLS-1$
		}
		return buffer.toString();
	}
	
	public static Dictionary getDevDictionary(IPluginModelBase model) {
		if (model.getUnderlyingResource() == null)
			return null;
		
		String id = model.getPluginBase().getId();
		if (id == null || id.trim().length() == 0)
			return null;
		IPath[] paths = getDevPaths(model, false, null);
		String entry = writeEntry(paths);
		Hashtable map = new Hashtable(2);
		map.put("@ignoredot@", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(id, entry.length() > 0 ? entry : "bin"); //$NON-NLS-1$
		return map;		
	}

	private static IPath[] getDevPaths(IPluginModelBase model, boolean checkExcluded, Map pluginsMap) {
		ArrayList result = new ArrayList();
		IProject project = model.getUnderlyingResource().getProject();
		IPluginBase base = model.getPluginBase();
		IPluginLibrary[] libraries = base.getLibraries();
		List excluded = getFoldersToExclude(project, checkExcluded);
 		try {
 			if (project.hasNature(JavaCore.NATURE_ID)) {
 				IFile file = project.getFile("build.properties"); //$NON-NLS-1$
 				if (file.exists()) {
 					WorkspaceBuildModel bModel = new WorkspaceBuildModel(file);
 					IBuild build = bModel.getBuild();
 					for (int i = 0; i < libraries.length;i++) {
 						IBuildEntry entry = build.getEntry(IBuildEntry.OUTPUT_PREFIX + libraries[i].getName());
 						if (entry != null) {
 							String [] resources = entry.getTokens();
 							for (int j = 0; j < resources.length; j++)  {
 								IResource res = project.findMember(resources[j]);
 								if (res.exists()) {
 									IPath path = res.getFullPath().makeRelative();
 									if (!excluded.contains(path)) {
 										addPath(result, project, path, false);
 									}
 								}
 							}
 						} else {
 	 						boolean found = false;
 							IResource res = project.findMember(libraries[i].getName());
 							if (res != null) {
 								IPath path = res.getProjectRelativePath();
 								if (!excluded.contains(path)) {
 									addPath(result, project, path, true);
 									found = true;
 								}
 							}
 							if (!found)
 								addLibraryFromFragments(libraries[i].getName(), model, result, checkExcluded, pluginsMap);
 						}
 					}
 				} else {
 					// if no build.properties, add all output folders
 					IJavaProject jProject = JavaCore.create(project);
 					HashSet set = new HashSet();
 					for (int i = 0; i < libraries.length; i++)
 						set.add(libraries[i].getName());

 					IPath path = jProject.getOutputLocation();
 					if (path != null && !excluded.contains(path))
 						addPath(result, project, path.makeRelative(), false);

 					IClasspathEntry[] entries = jProject.getRawClasspath();
 					for (int i = 0; i < entries.length; i++) {
 						path = null;
 						boolean addIfLinked = false;
 						if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
 							path = entries[i].getOutputLocation();
 						} else if (entries[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
 							IPath candidate = entries[i].getPath().removeFirstSegments(1);
 							if (candidate.segmentCount() == 0) {
 								if (set.isEmpty() || set.contains(".")) //$NON-NLS-1$
 									path = entries[i].getPath();
 							} else if (set.contains(candidate.toString())) {
 								addIfLinked = !base.getId().equals("org.eclipse.osgi"); //$NON-NLS-1$
 								path = entries[i].getPath();
 							}
 						}
 						if (path != null && !excluded.contains(path)) {
 							addPath(result, project, path.makeRelative(), addIfLinked);
 						}
 					}
				}
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
		return (IPath[])result.toArray(new IPath[result.size()]);	
	}
	
	private static void addLibraryFromFragments(String libName, IPluginModelBase model, ArrayList result, boolean checkExcluded, Map plugins) {
		IFragmentModel[] frags = PDEManager.findFragmentsFor(model);
		for (int i  = 0; i < frags.length; i++) {
			if (plugins != null && !plugins.containsKey(frags[i].getBundleDescription().getSymbolicName()))
				continue;
			// look in project first
			if (frags[i].getUnderlyingResource() != null) {
				IProject project = frags[i].getUnderlyingResource().getProject();
				List excluded = getFoldersToExclude(project, checkExcluded);
				IFile file = project.getFile("build.properties"); //$NON-NLS-1$
				if (file.exists()) {
					WorkspaceBuildModel bModel = new WorkspaceBuildModel(file);
					IBuild build = bModel.getBuild();
					IBuildEntry entry = build.getEntry(IBuildEntry.OUTPUT_PREFIX + libName);
					if (entry != null) {
						String [] resources = entry.getTokens();
						for (int j = 0; j < resources.length; j++)  {
							IResource res = project.findMember(resources[j]);
							if (res.exists()) {
								IPath path = res.getProjectRelativePath();
								if (!excluded.contains(path)) {
									addPath(result, project, res.getRawLocation(), false);
									return;
								}
							}
						}
					} else {
						IResource res = project.findMember(libName);
						if (res != null) {
							IPath path = res.getProjectRelativePath();
							if (!excluded.contains(path)) {
								addPath(result, project, res.getRawLocation(), true);
								return;
							}
						}
					}
				}
			// if external plugin, look in child directories for library
			} else {
				File file = new File(frags[i].getInstallLocation());
				if (file.isDirectory()) {
					file = new File(file, libName);
					if (file.exists()) {
						addPath(result, null, new Path(file.getPath()), false);
						return;
					}
				}
			}
		}
	}

	private static void addPath(ArrayList result, IProject project, IPath path, boolean onlyIfLinked) {
		IPath resultPath = null;
		if (path.isAbsolute())
			resultPath = path;
		else if (path.segmentCount() > 0 && path.segment(0).equals(project.getName())) {
			path = path.removeFirstSegments(1);
			if (path.segmentCount() == 0 && !onlyIfLinked)
				resultPath = new Path("."); //$NON-NLS-1$
			else {
				IResource resource = project.findMember(path);
				if (resource != null) {
					if (!onlyIfLinked)
						resultPath = path;
					if (resource.isLinked(IResource.CHECK_ANCESTORS)) {
						resultPath = resource.getLocation();
					} 
				} 
			}
		}
		
		if (resultPath != null && !result.contains(resultPath))
			result.add(resultPath);
	}
	
	private static List getFoldersToExclude(IProject project, boolean checkExcluded) {
		ArrayList list = new ArrayList();
		if (checkExcluded) {
			IEclipsePreferences pref = new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
			if (pref != null) {
				String binExcludes = pref.get(ICoreConstants.SELFHOSTING_BIN_EXCLUDES, ""); //$NON-NLS-1$
				StringTokenizer tokenizer = new StringTokenizer(binExcludes, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens()) {
					list.add(new Path(tokenizer.nextToken().trim()));
				}
			}
		}
		return list;
	}

}
