package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.model.plugin.IPluginModelBase;
import org.eclipse.pde.model.plugin.IPluginReference;
import org.eclipse.core.runtime.*;
import java.net.URL;
import org.eclipse.jface.wizard.Wizard;

/**
 * This interface represents a section of the template
 * wizard that generates a new extension or plug-in.
 * Typically, it maps to one wizard page, but more
 * complex sections may span several pages. Also note
 * that in very simple cases it may not contribute any
 * wizard pages. 
 * <p>If a section generates extensions, it should be written 
 * in such a way to be used both in the 'New Extension' wizard
 * and as a part of a new plug-in project template.
 * </p>
 */

public interface ITemplateSection {
	/**
	 * Returns the URL of the zip file containing 
	 * template files and directories that will
	 * be created in the plug-in project. If URL protocol
	 * is 'file', and the URL ends with a trailing file
	 * separator, an attempt will be made to treat the
	 * URL as a root directory and iterate using standard
	 * Java IO. If template files are stored in a ZIP 
	 * or JAR archive, the name of the archive must
	 * be part of the URL.
	 * @return a template location URL
	 */
	public URL getTemplateLocation();
	
	/**
	 * Returns a description of the section. The description
	 * should explain what extension will be plugged into,
	 * what classes will be generated and how to
	 * test that the generated code works properly.
	 * @return a template description
	 */
	public String getDescription();
	
	/**
	 * Returns a replacement string for the provided key. 
	 * When a token is found in the template file with a form
	 * '$key$', the actual key is passed to this method
	 * to obtain the replacement. If replacement is
	 * provided, it is substituted for the token (including
	 * the '$' characters). Otherwise, it is transfered
	 * as-is.
	 * @param key the replacement key found in the template file
	 * @return replacement string for the provided key, or
	 * the key itself if not found. 
	 */
	public String getReplacementString(String fileName, String key);
	/**
	 * Adds template-related pages to the wizard. A typical section
	 * implementation contributes one page, but complex sections
	 * may span several pages. 
	 * @param wizard the host wizard to add pages into
	 */
	public void addPages(Wizard wizard);

	/**
	 * Returns the number of work units that this template will
	 * consume during the execution. This number is used to
	 * calculate the total number of work units when initializing
	 * the progress indicator.
	 */
	public int getNumberOfWorkUnits();

	/**
	 * Provides the list of template dependencies. A template
	 * may generate a number of Java classes that reference
	 * classes and interfaces from other plug-ins.
	 * By providing this list, a template enables the template
	 * wizard to create the correct Java build path so that
	 * these classes and interfaces are correctly resolved.
	 */
	public IPluginReference[] getDependencies();
	
	/**
	 * Returns identifier of the extension point used in
	 * this section.
	 * @return extension point id if this section contributes
	 * into an extension point or <samp>null</samp> if not
	 * applicable.
	 */
	public String getUsedExtensionPoint();

	/**
	 * Executes the template. As part of the execution, template
	 * may generate resources under the provided project, and/or
	 * modify the plug-in model.
	 * 
	 * @param project the workspace project that contains the plug-in
	 * @param model structured representation of the plug-in manifest
	 * @param monitor progress monitor to indicate execution progress 
	 */
	public void execute(
		IProject project,
		IPluginModelBase model,
		IProgressMonitor monitor) throws CoreException;
}