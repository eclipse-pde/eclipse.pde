package org.eclipse.pde.internal.base;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
/**
 * This interface provides data entered in the master plug-in
 * project wizard that can be used by plug-in content
 * wizards to set it up. Master wizard is only responsible
 * for collecting this information - it is the content
 * wizard that needs to act upon it.
 */
public interface IPluginStructureData {
/**
 * Returns the plug-in id as defined in the dialog
 * 
 * @return plug-in id
 */

public String getPluginId();
  
/**
 * Returns the folder name for the Java build output.
 *
 * @return Java build output folder name
 */
public String getJavaBuildFolderName();
/**
 * Returns the path for the JRE runtime library. This information 
 * will be provided by the Java plug-in depending on the current
 * selection. JRE library path must be added to the build path of
 * the plug-in project to allow Java builder to compile the project.
 * @return JRE default library path
 */
IPath getJREPath();
/**
 * Returns paths for JRE source annotation. This
 * information is required for being able to
 * step through the JRE source code in Java debugger. This
 * information is obtained from the Java plug-in.
 */
IPath [] getJRESourceAnnotation();
/**
 * Returns the JAR library name. A plug-in can contain
 * more than one JAR. This initial library name is
 * entered by the user in the master plug-in project wizard.
 *
 * @return the initial JAR library name
 */
public String getRuntimeLibraryName();
/**
 * Returns the initial source folder name.
 * Source code should be in one or more
 * source folders. Each folder will
 * be added to the Java build path. This initial source
 * folder is entered by the user in the master plug-in project
 * wizard.
 *
 * @return the initial source folder name
 */
public String getSourceFolderName();
}
