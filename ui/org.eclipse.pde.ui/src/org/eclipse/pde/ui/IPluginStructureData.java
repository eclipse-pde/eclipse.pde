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
package org.eclipse.pde.ui;

import org.eclipse.core.runtime.*;
/**
 * This interface provides data entered in the master plug-in
 * project wizard that can be used by plug-in content
 * wizards to set it up. Master wizard is only responsible
 * for collecting this information - it is the content
 * wizard that needs to act upon it.
 * <p>
 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IPluginStructureData {
	/**
	 * Returns the plug-in id as defined in the dialog
	 * 
	 * @return plug-in id
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getPluginId();

	/**
	 * Returns the folder name for the Java build output.
	 *
	 * @return Java build output folder name
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getJavaBuildFolderName();
	/**
	 * Returns the path for the JRE runtime library. This information 
	 * will be provided by the Java plug-in depending on the current
	 * selection. JRE library path must be added to the build path of
	 * the plug-in project to allow Java builder to compile the project.
	 * @return JRE default library path
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IPath getJREPath();
	/**
	 * Returns paths for JRE source annotation. This
	 * information is required for being able to
	 * step through the JRE source code in Java debugger. This
	 * information is obtained from the Java plug-in.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IPath[] getJRESourceAnnotation();
	/**
	 * Returns the JAR library name. A plug-in can contain
	 * more than one JAR. This initial library name is
	 * entered by the user in the master plug-in project wizard.
	 *
	 * @return the initial JAR library name
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
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
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getSourceFolderName();
}
