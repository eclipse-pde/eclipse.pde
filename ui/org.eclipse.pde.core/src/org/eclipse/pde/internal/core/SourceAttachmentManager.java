/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SourceAttachmentManager {
	private Hashtable entries;
	private static final String KEY_PLATFORM_PATH = "platform-path";

	public static class SourceAttachmentEntry {
		private IPath entryPath;
		private IPath attachmentPath;
		private IPath attachmentRootPath;
		
		public SourceAttachmentEntry(IPath entryPath, IPath attachmentPath, IPath attachmentRootPath) {
			this.entryPath = entryPath;
			this.attachmentPath = attachmentPath;
			this.attachmentRootPath = attachmentRootPath;
		}
		
		public IPath getEntryPath() {
			return entryPath;
		}
		public IPath getAttachmentPath() {
			return attachmentPath;
		}
		
		public IPath getAttachmentRootPath() {
			return attachmentRootPath;
		}
	}
	/**
	 * Constructor for SourceAttachementManager.
	 */
	public SourceAttachmentManager() {
		entries = new Hashtable();
		initialize();
	}
	
	public boolean isEmpty() {
		return entries.isEmpty();
	}
	
	public SourceAttachmentEntry findEntry(IPath entryPath) {
		return (SourceAttachmentEntry)entries.get(entryPath);
	}
	
	public void addEntry(IPath libraryPath, IPath attachmentPath, IPath attachmentRootPath) {
		entries.put(libraryPath, new SourceAttachmentEntry(libraryPath, attachmentPath, attachmentRootPath));
	}
	
	private String getFileName() {
		IPath stateLocation = PDECore.getDefault().getStateLocation();
		IPath stateFile = stateLocation.append("sourceAttachements.properties");
		return stateFile.toOSString();
	}

	private void initialize() {
		String fileName = getFileName();
		Properties properties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(fileName);
			properties.load(fis);
			parseProperties(properties);
			fis.close();
		}
		catch (IOException e) {
		}
	}
	
	private void parseProperties(Properties properties) {
		String platformPath = properties.getProperty(KEY_PLATFORM_PATH);
		if (platformPath==null) return;
		IPath oldPlatformPath = new Path(platformPath);
		IPath currentPlatformPath = ExternalModelManager.getEclipseHome(null);
		// If the saved entries are for a different platform path,
		// discard them.
		if (oldPlatformPath.equals(currentPlatformPath)==false) return;
		for (Enumeration enum = properties.keys(); enum.hasMoreElements();) {
			String key = (String)enum.nextElement();
			if (key.startsWith("entry."))
				parseEntryProperty(properties.getProperty(key));
		}
	}
	
	private void parseEntryProperty(String value) {
		int semi = value.indexOf(';');
		
		String library = value.substring(0, semi);
		String paths = value.substring(semi+1);
		
		semi = paths.indexOf(";");
		
		String att, attRoot=null;
		if (semi!= -1) {
			att = paths.substring(0, semi);
			attRoot = paths.substring(semi+1);
		}
		else 
			att = paths;
		addEntry(new Path(library), new Path(att), attRoot!=null?new Path(attRoot):null);
	}
	
	public void save() {
		String fileName = getFileName();
		Properties properties = new Properties();
		IPath platformPath = ExternalModelManager.getEclipseHome(null);
		properties.setProperty(KEY_PLATFORM_PATH, platformPath.toOSString());
		
		int i=0;
		for (Enumeration enum=entries.keys(); enum.hasMoreElements();) {
			IPath entryPath = (IPath)enum.nextElement();
			SourceAttachmentEntry entry = (SourceAttachmentEntry)entries.get(entryPath);
			String library = entry.getEntryPath().toOSString();
			String value;
			if (entry.getAttachmentRootPath()!=null)
				value = library+";"+entry.getAttachmentPath().toOSString()+";"+entry.getAttachmentRootPath().toOSString();
			else
				value = library+";"+entry.getAttachmentPath().toOSString();
			i++;
			properties.setProperty("entry."+i, value);
		}
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			properties.store(fos, "User-defined source attachments");
			fos.flush();
			fos.close();
		}
		catch (IOException e) {
		}
	}
}
