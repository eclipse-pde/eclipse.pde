package org.eclipse.pde.internal.base;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
/**
 * This interface provides data set up in the master plug-in
 * project wizard that can be used by plug-in content
 * wizards to set it up. Master wizard is only responsible
 * for collecting this information - it is the content
 * wizard that needs to act upon it.
 */
public interface IPluginStructureData {
/**
 * Returns the folder name for the Java build output.
 *
 * @return Java build output
 */
public String getJavaBuildFolderName();
/**
 * Returns the path for the JRE runtime library.
 * @return JRE default library path
 */
IPath getJREPath();
/**
 * Returns paths for JRE source annotation. This
 * information is required for being able to
 * step through the JRE source code in Java debugger.
 */
IPath [] getJRESourceAnnotation();
/**
 * Returns the JAR library name. A plug-in can contain
 * more than one JAR, so this one will be only the
 * first to use.
 *
 * @return the initial JAR library name
 */
public String getRuntimeLibraryName();
/**
 * Returns the initial source folder name.
 * Source code should be in one or more
 * source folders. Each folder will
 * be added to the Java build path.
 *
 * @return the initial source folder name
 */
public String getSourceFolderName();
}
