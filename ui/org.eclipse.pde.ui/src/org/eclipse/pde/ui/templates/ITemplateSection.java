/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import java.net.URL;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;

/**
 * This interface represents a section of the template wizard that generates a
 * new extension or plug-in. Typically, it maps to one wizard page, but more
 * complex sections may span several pages. Also note that in the very simple
 * cases it may not contribute any wizard pages.
 * <p>
 * If a section generates extensions, it should be written in such a way to be
 * used both in the 'New Extension' wizard and as a part of a new plug-in
 * project wizard. When used as part of the new plug-in project wizard, it may
 * appear alongside other templates and therefore should not do anything that
 * prevents it.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */

public interface ITemplateSection {
	/**
	 * Returns the URL of the zip file containing template files and directories
	 * that will be created in the plug-in project. If URL protocol is 'file',
	 * and the URL ends with a trailing file separator, an attempt will be made
	 * to treat the URL as a root directory and iterate using standard Java I/O
	 * classes. If template files are stored in a ZIP or JAR archive, the name
	 * of the archive must be part of the URL.
	 * 
	 * @return a template location URL
	 */
	public URL getTemplateLocation();

	/**
	 * Returns a presentable label the section.
	 * 
	 * @return a template label
	 */
	public String getLabel();

	/**
	 * Returns a description of the section. The description should explain what
	 * extension will be used, what classes will be generated and how to test
	 * that the generated code works properly.
	 * 
	 * @return a template description
	 */
	public String getDescription();

	/**
	 * Returns a replacement string for the provided key. When a token is found
	 * in the template file with a form '$key$', the actual key is passed to
	 * this method to obtain the replacement. If replacement is provided, it is
	 * substituted for the token (including the '$' characters). Otherwise, it
	 * is transfered as-is.
	 * 
	 * @param fileName
	 *            the name of the file in which the key was found. You can use
	 *            it to return different values for different files.
	 * @param key
	 *            the replacement key found in the template file
	 * @return replacement string for the provided key, or the key itself if not
	 *         found.
	 */
	public String getReplacementString(String fileName, String key);

	/**
	 * Adds template-related pages to the wizard. A typical section
	 * implementation contributes one page, but complex sections may span
	 * several pages.
	 * 
	 * @param wizard
	 *            the host wizard to add pages into
	 */
	public void addPages(Wizard wizard);

	/**
	 * Returns a wizard page at the provided index.
	 * 
	 * @param pageIndex the index to get the page for 
	 * @return wizard page index.
	 */
	public WizardPage getPage(int pageIndex);

	/**
	 * Returns number of pages that are contributed by this template.
	 * @return the contributed page count
	 */
	public int getPageCount();

	/**
	 * Tests whether this template have had a chance to create its pages. This
	 * method returns true after 'addPages' has been called.
	 * 
	 * @return <samp>true </samp> if wizard pages have been created by this
	 *         template.
	 */

	public boolean getPagesAdded();

	/**
	 * Returns the number of work units that this template will consume during
	 * the execution. This number is used to calculate the total number of work
	 * units when initializing the progress indicator.
	 * 
	 * @return the number of work units
	 */
	public int getNumberOfWorkUnits();

	/**
	 * Provides the list of template dependencies. A template may generate a
	 * number of Java classes that reference classes and interfaces from other
	 * plug-ins. By providing this list, a template enables the template wizard
	 * to create the correct Java build path so that these classes and
	 * interfaces are correctly resolved.
	 * 
	 * @param schemaVersion
	 *            version of the target manifest, or <samp>null </samp> if older
	 *            manifest (prior to 3.0) will be created. Depending on the
	 *            manifest version, the list of dependencies may vary.
	 *            
	 * @return an array of template dependencies
	 */
	public IPluginReference[] getDependencies(String schemaVersion);

	/**
	 * Returns identifier of the extension point used in this section.
	 * 
	 * @return extension point id if this section contributes into an extension
	 *         point or <samp>null </samp> if not applicable.
	 */
	public String getUsedExtensionPoint();

	/**
	 * Executes the template. As part of the execution, template may generate
	 * resources under the provided project, and/or modify the plug-in model.
	 * 
	 * @param project
	 *            the workspace project that contains the plug-in
	 * @param model
	 *            structured representation of the plug-in manifest
	 * @param monitor
	 *            progress monitor to indicate execution progress
	 * @throws CoreException if there is a problem generating resources
	 */
	public void execute(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns an array of tokens representing new files and folders created by
	 * this template section. The information is collected for the benefit of
	 * <code>build.properties</code> file so that the generated files and
	 * folders are included in the binary build. The tokens will be added as-is
	 * to the variable <code>bin.includes</code>. For this reason, wild cards
	 * and other syntax rules applicable to this variable can be used in this
	 * method. For example:
	 * <p>
	 * 
	 * <pre>
	 * return new String[]{&quot;/icons/*.gif&quot;};
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @return an array of strings that fully describe the files and folders
	 *         created by this template section as required by <code>
	 *         bin.includes</code> variable in <code>build.properties</code>
	 *         file.
	 */
	public String[] getNewFiles();
}
