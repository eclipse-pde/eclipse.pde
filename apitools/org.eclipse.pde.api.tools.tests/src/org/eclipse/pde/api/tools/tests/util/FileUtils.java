/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * Utilities for handling files
 * 
 * @since 1.0.0
 */
public class FileUtils {
	/**
	 * Maximum of time in ms to wait in deletion operation while running JDT/Core tests.
	 * Default is 10 seconds. This number cannot exceed 1 minute (ie. 60000).
	 * <br>
	 * To avoid too many loops while waiting, the ten first ones are done waiting
	 * 10ms before repeating, the ten loops after are done waiting 100ms and
	 * the other loops are done waiting 1s...
	 */
	public static int DELETE_MAX_WAIT = 10000;

	/**
	 * Time wasted deleted resources
	 */
	private static int DELETE_MAX_TIME = 0;
	
	/**
	 * Recursively adds files from the specified directory to the provided list
	 * @param dir
	 * @param collection
	 * @throws IOException
	 */
	public static void addJavaFiles(File dir, List<File> collection) throws IOException {
		File[] files = dir.listFiles();
		List<File> subDirs = new ArrayList<File>(2);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				collection.add(files[i]);
			} else if (files[i].isDirectory()) {
				subDirs.add(files[i]);
			}
		}
		Iterator<File> iter = subDirs.iterator();
		while (iter.hasNext()) {
			File subDir = iter.next();
			addJavaFiles(subDir, collection);
		}
	}
	
	/**
	 * Imports files from the specified root directory into the specified path
	 * @param rootDir
	 * @param destPath
	 * @param monitor
	 * @throws InvocationTargetException
	 * @throws IOException
	 */
	public static void importFilesFromDirectory(File rootDir, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, IOException {
		IResource findMember = ResourcesPlugin.getWorkspace().getRoot().getFolder(destPath);
		File dest = findMember.getLocation().toFile();
		if (!dest.exists()) dest.mkdirs();
		TestSuiteHelper.copy(rootDir, dest);
		try {
			findMember.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
		}
	}
	
	/**
	 * Imports the specified file to the destination path 
	 * @param file
	 * @param destPath
	 * @param monitor
	 * @throws InvocationTargetException
	 * @throws IOException
	 */
	public static void importFileFromDirectory(File file, IPath destPath, IProgressMonitor monitor) throws CoreException, InvocationTargetException, IOException {
		IResource findMember = null;
		if (destPath.segmentCount() == 1) {
			findMember = ResourcesPlugin.getWorkspace().getRoot().getProject(destPath.lastSegment());
		} else {
			findMember = ResourcesPlugin.getWorkspace().getRoot().getFolder(destPath);
		}
		if (findMember == null) return;
		File dest = findMember.getLocation().toFile();
		if (!dest.exists()) dest.mkdirs();
		TestSuiteHelper.copy(file, dest);
		try {
			findMember.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
		}
	}

	/**
	 * Creates a new java.io.File at the given path with the given contents
	 * @param path
	 * @param contents
	 * @throws IOException
	 */
	public static void createFile(String path, String contents) throws IOException {
	    FileOutputStream output = new FileOutputStream(path);
	    try {
	        output.write(contents.getBytes());
	    } finally {
	        output.close();
	    }
	}
	
	/**
	 * Delete a file or directory and insure that the file is no longer present
	 * on file system. In case of directory, delete all the hierarchy underneath.
	 *
	 * @param resource The resource to delete
	 * @return true iff the file was really delete, false otherwise
	 */
	public static boolean delete(IResource resource) {
	    try {
	        resource.delete(true, null);
	        if (isResourceDeleted(resource)) {
	            return true;
	        }
	    }
	    catch (CoreException e) {
	        //	skip
	    }
	    return waitUntilResourceDeleted(resource);
	}
	
	/**
	 * Delete a file or directory and insure that the file is no longer present
	 * on file system. In case of directory, delete all the hierarchy underneath.
	 *
	 * @param path The path of the file or directory to delete
	 * @return true iff the file was really delete, false otherwise
	 */
	public static boolean delete(String path) {
	    return org.eclipse.pde.api.tools.internal.util.Util.delete(new File(path));
	}

	/**
	 * Flush content of a given directory (leaving it empty),
	 * no-op if not a directory.
	 */
	public static void flushDirectoryContent(File dir) {
	    File[] files = dir.listFiles();
	    if (files == null) return;
	    for (int i = 0, max = files.length; i < max; i++) {
	    	org.eclipse.pde.api.tools.internal.util.Util.delete(files[i]);
	    }
	}
	
	/**
	 * Wait until a resource is _really_ deleted on file system.
	 *
	 * @param resource Deleted resource
	 * @return true if the file was finally deleted, false otherwise
	 */
	private static boolean waitUntilResourceDeleted(IResource resource) {
	    IPath location = resource.getLocation();
	    if (location == null) {
	        System.out.println();
	        System.out.println("	!!! ERROR: "+resource+" getLocation() returned null!!!");
	        System.out.println();
	        return false;
	    }
	    File file = location.toFile();
	    int count = 0;
	    int delay = 10; // ms
	    int maxRetry = DELETE_MAX_WAIT / delay;
	    int time = 0;
	    while (count < maxRetry) {
	        try {
	            count++;
	            Thread.sleep(delay);
	            time += delay;
	            if (time > DELETE_MAX_TIME) DELETE_MAX_TIME = time;
	            if (resource.isAccessible()) {
	                try {
	                    resource.delete(true, null);
	                    if (isResourceDeleted(resource) && org.eclipse.pde.api.tools.internal.util.Util.isFileDeleted(file)) {
	                        return true;
	                    }
	                }
	                catch (CoreException e) {}
	            }
	            if (isResourceDeleted(resource) && org.eclipse.pde.api.tools.internal.util.Util.isFileDeleted(file)) {
	                return true;
	            }
	            // Increment waiting delay exponentially
	            if (count >= 10 && delay <= 100) {
	                count = 1;
	                delay *= 10;
	                maxRetry = DELETE_MAX_WAIT / delay;
	                if ((DELETE_MAX_WAIT%delay) != 0) {
	                    maxRetry++;
	                }
	            }
	        }
	        catch (InterruptedException ie) {
	            break; // end loop
	        }
	    }
	    System.out.println();
	    System.out.println("	!!! ERROR: "+resource+" was never deleted even after having waited "+DELETE_MAX_TIME+"ms!!!");
	    System.out.println();
	    return false;
	}
	
	/**
	 * Returns whether a resource is really deleted or not.
	 * Does not only rely on {@link IResource#isAccessible()} method but also
	 * look if it's not in its parent children {@link #getParentChildResource(IResource)}.
	 *
	 * @param resource The resource to test if deleted
	 * @return true if the resource is not accessible and was not found in its parent children.
	 */
	public static boolean isResourceDeleted(IResource resource) {
	    return !resource.isAccessible() && getParentChildResource(resource) == null;
	}
	
	/**
	 * Reads the content of the given source file.
	 * 
	 * Returns null if unable to read given source file.
	*/
	public static String readFromFile(String sourceFilePath) throws FileNotFoundException, IOException {
	    File sourceFile = new File(sourceFilePath);
	    if (!sourceFile.exists()) {
	        return null;
	    }
	    if (!sourceFile.isFile()) {
	        return null;
	    }
	    StringBuffer sourceContentBuffer = new StringBuffer();
	    FileInputStream input = null;
	    input = new FileInputStream(sourceFile);
	    try {
	        int read;
	        do {
	            read = input.read();
	            if (read != -1) {
	                sourceContentBuffer.append((char)read);
	            }
	        } while (read != -1);
	        input.close();
	    } finally {
	    	input.close();
	    }
	    return sourceContentBuffer.toString();
	}
	
	/**
	 * Writes the given content string to the output file, specified
	 * @param contents
	 * @param destinationFilePath
	 */
	public static void writeToFile(String contents, String destinationFilePath) {
	    File destFile = new File(destinationFilePath);
	    FileOutputStream output = null;
	    try {
	        output = new FileOutputStream(destFile);
	        PrintWriter writer = new PrintWriter(output);
	        writer.print(contents);
	        writer.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	        return;
	    } finally {
	        if (output != null) {
	            try {
	                output.close();
	            } catch (IOException e2) {
	            }
	        }
	    }
	}
	
	/**
	 * writes a new zip file from all of the files contained in the specified root directory
	 * @param rootDir
	 * @param zipPath
	 * @throws IOException
	 */
	public static void zip(File rootDir, String zipPath) throws IOException {
	    ZipOutputStream zip = null;
	    try {
	        File zipFile = new File(zipPath);
	        if (zipFile.exists()) {
	        	org.eclipse.pde.api.tools.internal.util.Util.delete(zipFile);
	        }
	        zip = new ZipOutputStream(new FileOutputStream(zipFile));
	        zip(rootDir, zip, rootDir.getPath().length()+1); // 1 for last slash
	    } finally {
	        if (zip != null) {
	            zip.close();
	        }
	    }
	}
	
	/**
	 * Writes all of the zip entries from the given directory to the specified zip output stream
	 * @param dir
	 * @param zip
	 * @param rootPathLength
	 * @throws IOException
	 */
	public static void zip(File dir, ZipOutputStream zip, int rootPathLength) throws IOException {
	    File[] files = dir.listFiles();
	    if (files != null) {
	        for (int i = 0, length = files.length; i < length; i++) {
	            File file = files[i];
	            if (file.isFile()) {
	                String path = file.getPath();
	                path = path.substring(rootPathLength);
	                ZipEntry entry = new ZipEntry(path.replace('\\', '/'));
	                zip.putNextEntry(entry);
	                zip.write(org.eclipse.jdt.internal.compiler.util.Util.getFileByteContent(file));
	                zip.closeEntry();
	            } else {
	                zip(file, zip, rootPathLength);
	            }
	        }
	    }
	}
	
	/**
	 * Returns parent's child resource matching the given resource or null if not found.
	 *
	 * @param resource The searched file in parent
	 * @return The parent's child matching the given file or null if not found.
	 */
	private static IResource getParentChildResource(IResource resource) {
	    IContainer parent = resource.getParent();
	    if (parent == null || !parent.exists()) return null;
	    try {
	        IResource[] members = parent.members();
	        int length = members ==null ? 0 : members.length;
	        if (length > 0) {
	            for (int i=0; i<length; i++) {
	                if (members[i] == resource) {
	                    return members[i];
	                } else if (members[i].equals(resource)) {
	                    return members[i];
	                } else if (members[i].getFullPath().equals(resource.getFullPath())) {
	                    return members[i];
	                }
	            }
	        }
	    }
	    catch (CoreException ce) {
	        // skip
	    }
	    return null;
	}
	
	/**
	 * Copy the given source (a file or a directory that must exists) to the given destination (a directory that must exists).
	 */
	public static void copyFile(String sourcePath, String destPath) {
	    sourcePath = org.eclipse.pde.api.tools.internal.util.Util.toNativePath(sourcePath);
	    destPath = org.eclipse.pde.api.tools.internal.util.Util.toNativePath(destPath);
	    File source = new File(sourcePath);
	    if (!source.exists()) return;
	    File dest = new File(destPath);
	    if (!dest.exists()) return;
	    if (source.isDirectory()) {
	        String[] files = source.list();
	        if (files != null) {
	            for (int i = 0; i < files.length; i++) {
	                String file = files[i];
	                File sourceFile = new File(source, file);
	                if (sourceFile.isDirectory()) {
	                    File destSubDir = new File(dest, file);
	                    destSubDir.mkdir();
	                    copyFile(sourceFile.getPath(), destSubDir.getPath());
	                } else {
	                    copyFile(sourceFile.getPath(), dest.getPath());
	                }
	            }
	        }
	    } else {
	        FileInputStream in = null;
	        FileOutputStream out = null;
	        try {
	            in = new FileInputStream(source);
	            File destFile = new File(dest, source.getName());
	            if (destFile.exists()) {
	                if (!org.eclipse.pde.api.tools.internal.util.Util.delete(destFile)) {
	                    throw new IOException(destFile + " is in use");
	                }
	            }
	             out = new FileOutputStream(destFile);
	            int bufferLength = 1024;
	            byte[] buffer = new byte[bufferLength];
	            int read = 0;
	            while (read != -1) {
	                read = in.read(buffer, 0, bufferLength);
	                if (read != -1) {
	                    out.write(buffer, 0, read);
	                }
	            }
	        } catch (IOException e) {
	            throw new Error(e.toString());
	        } finally {
	            if (in != null) {
	                try {
	                    in.close();
	                } catch (IOException e) {
	                }
	            }
	            if (out != null) {
	                try {
	                    out.close();
	                } catch (IOException e) {
	                }
	            }
	        }
	    }
	}
}
