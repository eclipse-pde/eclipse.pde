/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
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
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;

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
			String entry = writeEntry(getOutputFolders(models[i], checkExcluded));
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
                String entry = writeEntry(getOutputFolders(model, true));
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
			IPath[] paths = getOutputFolders(models[i], checkExcluded);
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
		IPath[] paths = getOutputFolders(model, false);
		String entry = writeEntry(paths);
		Hashtable map = new Hashtable(2);
		map.put("@ignoredot@", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(id, entry.length() > 0 ? entry : "bin"); //$NON-NLS-1$
		return map;		
	}

	private static IPath[] getOutputFolders(IPluginModelBase model, boolean checkExcluded) {
		ArrayList result = new ArrayList();
		IProject project = model.getUnderlyingResource().getProject();
		HashSet set = new HashSet();
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			set.add(libraries[i].getName());
		}
 		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				
				List excluded = getFoldersToExclude(project, checkExcluded);
				IPath path = jProject.getOutputLocation();
				if (path != null && !excluded.contains(path))
					addPath(result, project, path);
				
				IClasspathEntry[] entries = jProject.getRawClasspath();
				for (int i = 0; i < entries.length; i++) {
					path = null;
					if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						path = entries[i].getOutputLocation();
					} else if (entries[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
						IPath candidate = entries[i].getPath().removeFirstSegments(1);
						if (candidate.segmentCount() == 0) {
							if (set.isEmpty() || set.contains(".")) //$NON-NLS-1$
								path = entries[i].getPath();
						} else if (set.contains(candidate.toString())) {
							path = entries[i].getPath();
						}
					}
					if (path != null && !excluded.contains(path)) {
						addPath(result, project, path);
					}
				}
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
		return (IPath[])result.toArray(new IPath[result.size()]);	
	}

	private static void addPath(ArrayList result, IProject project, IPath path) {
		if (path.segmentCount() > 0 && path.segment(0).equals(project.getName())) {
			path = path.removeFirstSegments(1);
			if (path.segmentCount() == 0)
				path = new Path("."); //$NON-NLS-1$
			else {
				IResource resource = project.findMember(path);
				if (resource != null) {
					if (resource.isLinked()) {
						path = resource.getLocation();
					} 
				} else {
					path = null;
				}
			}
		}
		
		if (path != null && !result.contains(path))
			result.add(path);
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
