/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.project;

import java.net.URI;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.natures.PDE;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Describes a project representing an OSGi bundle. Used to create or modify
 * artifacts associated with a bundle project. A bundle project description can be
 * created for an {@link IProject} via {@link IBundleProjectService#getDescription(IProject)}.
 * 
 * @since 3.6
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IBundleProjectDescription {
	/**
	 * Identifies bundles developed for Eclipse 3.0, value is <code>"3.0"</code>.
	 */
	public static final String VERSION_3_0 = ICoreConstants.TARGET30;
	/**
	 * Identifies bundles developed for Eclipse 3.1, value is <code>"3.1"</code>.
	 */
	public static final String VERSION_3_1 = ICoreConstants.TARGET31;
	/**
	 * Identifies bundles developed for Eclipse 3.2, value is <code>"3.2"</code>.
	 */
	public static final String VERSION_3_2 = ICoreConstants.TARGET32;
	/**
	 * Identifies bundles developed for Eclipse 3.3, value is <code>"3.3"</code>.
	 */
	public static final String VERSION_3_3 = ICoreConstants.TARGET33;
	/**
	 * Identifies bundles developed for Eclipse 3.4, value is <code>"3.4"</code>.
	 */
	public static final String VERSION_3_4 = ICoreConstants.TARGET34;
	/**
	 * Identifies bundles developed for Eclipse 3.5, value is <code>"3.5"</code>.
	 */
	public static final String VERSION_3_5 = ICoreConstants.TARGET35;
	/**
	 * Identifies bundles developed for Eclipse 3.6, value is <code>"3.6"</code>.
	 */
	public static final String VERSION_3_6 = ICoreConstants.TARGET36;
	/**
	 * Identifies bundles developed for Eclipse 3.7, value is <code>"3.7"</code>.
	 * @since 3.7
	 */
	public static final String VERSION_3_7 = ICoreConstants.TARGET37;
	/**
	 * Identifies bundles developed for Eclipse 3.8, value is <code>"3.8"</code>.
	 * @since 3.8
	 */
	public static final String VERSION_3_8 = ICoreConstants.TARGET38;

	/**
	 * Constant for the PDE Plug-in project nature, value is <code>"org.eclipse.pde.PluginNature"</code>.
	 */
	public static final String PLUGIN_NATURE = PDE.PLUGIN_NATURE;

	/**
	 * Creates or modifies a bundle project and associated artifacts based current settings.
	 * 
	 * @param monitor progress monitor or <code>null</code>
	 * @throws CoreException if project creation or modification fails
	 */
	public void apply(IProgressMonitor monitor) throws CoreException;

	/**
	 * Sets the symbolic name of the described bundle.
	 * <p>
	 * A symbolic name must be specified.
	 * </p>
	 * @param name bundle symbolic name
	 */
	public void setSymbolicName(String name);

	/**
	 * Returns the symbolic name of the described bundle or <code>null</code> if unspecified.
	 * 
	 * @return bundle symbolic name or <code>null</code>
	 */
	public String getSymbolicName();

	/**
	 * Sets the location for the described project.  
	 * If <code>null</code> is specified, the default location is used.
	 * <p>
	 * Setting the location on a description for a project which already
	 * exists has no effect; the new project location is ignored when the
	 * description is applied to the already existing project. This method is 
	 * intended for use on descriptions for new projects.
	 * </p>
	 * <p>
	 * This operation maps the root folder of the project to the exact location
	 * provided.  For example, if the location for project named "P" is set
	 * to the URI file://c:/my_plugins/Project1, the file resource at workspace path
	 * /P/index.html  would be stored in the local file system at 
	 * file://c:/my_plugins/Project1/index.html.
	 * </p>
	 *
	 * @param location the location for the described project or <code>null</code>
	 * @see #getLocationURI()
	 * @see IProjectDescription#setLocationURI(URI)
	 */
	public void setLocationURI(URI location);

	/**
	 * Returns the location URI for the described project.  <code>null</code> is
	 * returned if the default location should be used.
	 *
	 * @return the location for the described project or <code>null</code>
	 * @see #setLocationURI(URI)
	 */
	public URI getLocationURI();

	/**
	 * Sets the value of the Bundle-Name header for the described bundle.
	 * When <code>null</code>, the bundle name defaults to the bundle symbolic name.
	 * 
	 * @param name bundle name
	 */
	public void setBundleName(String name);

	/**
	 * Returns the value of the Bundle-Name header for the described bundle
	 * or <code>null</code> if unspecified.
	 * <p>
	 * For new projects, the bundle name defaults to the bundle symbolic name.
	 * </p>
	 * @return bundle name or <code>null</code>
	 */
	public String getBundleName();

	/**
	 * Sets the value of the Bundle-Vendor header for the described bundle.
	 * 
	 * @param name bundle vendor name
	 */
	public void setBundleVendor(String name);

	/**
	 * Returns the value of the Bundle-Vendor header for the described bundle
	 * or <code>null</code> if unspecified.
	 * 
	 * @return bundle vendor name or <code>null</code>
	 */
	public String getBundleVendor();

	/**
	 * Sets the value of the Bundle-Version header for the described bundle.
	 * When <code>null</code>, the bundle version defaults to <code>1.0.0.qualifier</code>.
	 * 
	 * @param version bundle version
	 */
	public void setBundleVersion(Version version);

	/**
	 * Returns the value of the Bundle-Version header for the described bundle.
	 * <p>
	 * For new projects, the bundle version is <code>1.0.0.qualifier</code> unless
	 * otherwise specified.
	 * </p>
	 * @return bundle version or <code>null</code> if unspecified
	 */
	public Version getBundleVersion();

	/**
	 * Sets whether the described bundle is a singleton.
	 * 
	 * @param singleton whether the described bundle is a singleton
	 */
	public void setSingleton(boolean singleton);

	/**
	 * Returns whether the described bundle is a singleton.
	 * <p>
	 * A bundle description for a new project is <b>not</b> a singleton, by default.
	 * </p>
	 * @return whether the described bundle is a singleton
	 */
	public boolean isSingleton();

	/**
	 * Sets the value of the Bundle-Localization header for the described bundle.
	 * 
	 * @param path bundle root relative path or <code>null</code>
	 */
	public void setLocalization(IPath path);

	/**
	 * Returns the value of the Bundle-Localization header for the described bundle
	 * or <code>null</code> if unspecified.
	 *
	 * @return bundle relative path or <code>null</code>
	 */
	public IPath getLocalization();

	/** 
	 * Returns the list of natures associated with the described project.
	 * Returns an empty array if there are no natures on this description.
	 *
	 * @return the list of natures for the described project
	 * @see #setNatureIds(String[])
	 * @see IProjectDescription#setNatureIds(String[])
	 */
	public String[] getNatureIds();

	/** 
	 * Sets the list of natures associated with the described project.
	 * A project created with this description will have these natures
	 * added to it in the given order when this description is applied.
	 * <p>
	 * When creating a new project, plug-in and Java natures ({@link #PLUGIN_NATURE}
	 * and {@link JavaCore#NATURE_ID}) will be added by default when unspecified.
	 * </p>
	 * @param natures the list of natures
	 * @see #getNatureIds()
	 * @see IProjectDescription#getNatureIds()
	 */
	public void setNatureIds(String[] natures);

	/** 
	 * Returns whether the project nature specified by the given
	 * nature extension id has been added to the described project. 
	 *
	 * @param natureId the nature extension identifier
	 * @return <code>true</code> if the described project has the given nature 
	 * @see IProjectDescription#hasNature(String)
	 */
	public boolean hasNature(String natureId);

	/**
	 * Sets the the Fragment-Host header for the described fragment.
	 * When a non-<code>null</code> value is specified, this bundle description
	 * describes a fragment. 
	 * 
	 * @param host host specification or <code>null</code>
	 */
	public void setHost(IHostDescription host);

	/**
	 * Returns the host bundle for the described fragment,
	 * or <code>null</code> if this description does not describe a fragment.
	 * 
	 * @return host specification or <code>null</code>
	 */
	public IHostDescription getHost();

	/**
	 * Sets a project relative path for the default output folder used on the Java build path
	 * for the described bundle. <code>null</code> indicates the Java project's default output
	 * location should be used.
	 * 
	 * @param output project relative path to default output location or <code>null</code>
	 */
	public void setDefaultOutputFolder(IPath output);

	/**
	 * Returns a project relative path for the described bundle's default output folder used on the Java build path,
	 * or <code>null</code> to indicate the default output location is used.
	 * 
	 * @return default project relative output folder path or <code>null</code>
	 */
	public IPath getDefaultOutputFolder();

	/**
	 * Sets the required execution environments for the described bundle, possible <code>null</code>.
	 * When more than one environment specified, the first will be used to configure compiler compliance
	 * and build path settings.
	 * 
	 * @param environments execution environment identifiers or <code>null</code>
	 */
	public void setExecutionEnvironments(String[] environments);

	/**
	 * Returns the required execution environments for the described bundle, or <code>null</code> if unspecified.
	 * When more than one environment is specified, the first will be used to configure compiler compliance
	 * and build path settings.
	 * 
	 * @return execution environment identifiers or <code>null</code>
	 */
	public String[] getExecutionEnvironments();

	/**
	 * Sets the entries for the Bundle-Classpath header of the described bundle,
	 * or <code>null</code> if unspecified. Specifies the relationship between
	 * source and/or binary folders with bundle classpath entries. When <code>null</code>
	 * is specified, no Bundle-Classpath header will be produced.
	 * 
	 * @param entries Bundle-Classpath header entries or <code>null</code>
	 * @deprecated use {@link #setBundleClasspath(IBundleClasspathEntry[])}
	 */
	public void setBundleClassath(IBundleClasspathEntry[] entries);

	/**
	 * Sets the entries for the Bundle-Classpath header of the described bundle,
	 * or <code>null</code> if unspecified. Specifies the relationship between
	 * source and/or binary folders with bundle classpath entries. When <code>null</code>
	 * is specified, no Bundle-Classpath header will be produced.
	 * 
	 * @param entries Bundle-Classpath header entries or <code>null</code>
	 * @since 3.7
	 */
	public void setBundleClasspath(IBundleClasspathEntry[] entries);

	/**
	 * Returns the entries on the Bundle-Classpath header of the described bundle,
	 * or <code>null</code> if unspecified.
	 * 
	 * @return bundle class path entries or <code>null</code> if unspecified
	 * @see #setBundleClasspath(IBundleClasspathEntry[])
	 */
	public IBundleClasspathEntry[] getBundleClasspath();

	/**
	 * Sets the value of the Bundle-Activator header for the described bundle,
	 * or <code>null</code> if none.
	 * 
	 * @param className activator class name or <code>null</code>
	 */
	public void setActivator(String className);

	/**
	 * Returns the value of the Bundle-Activator header for the described bundle,
	 * or <code>null</code> if none.
	 * 
	 * @return bundle activator class name or <code>null</code>
	 */
	public String getActivator();

	/**
	 * Sets the version of Eclipse the described bundle is to targeted for.
	 * This affects the values generated for Equinox specific headers.
	 * Has no effect when {@link #isEquinox()} is <code>false</code>.
	 * When {@link #isEquinox()} is <code>true</code>, and a target version
	 * is unspecified or set to <code>null</code>, the newest available 
	 * target version of Eclipse is used.
	 * 
	 * @param version one of the version constant values defined by this class or <code>null</code>
	 * @see #setEquinox(boolean)
	 */
	public void setTargetVersion(String version);

	/**
	 * Returns the version of Eclipse the described bundle is targeted for, or <code>null</code>
	 * if unspecified. When unspecified, the project is targeted to the newest available
	 * version of Eclipse when {@link #isEquinox()} is <code>true</code>.
	 * 
	 * @return target version or <code>null</code>
	 */
	public String getTargetVersion();

	/**
	 * Sets whether the described bundle is targeted for the Equinox OSGi framework.
	 * <p>
	 * An Equniox specific lazy-start header will be generated in the associated manifest
	 * when <code>true</code> based on the target version and activation policy. For new projects
	 * the value is <code>false</code>, by default.
	 * </p>
	 * <p>
	 * This following headers are affected when <code>true</code>, based on the {@link #getTargetVersion()}
	 * and {@link #getActivationPolicy()}. The headers are removed when {@link #getActivationPolicy()} is
	 * unspecified (<code>null</code>).
	 * <ul>
	 * <li><code>Eclipse-AutoStart</code> is set to <code>true</code> when the target version is 3.1
	 * 	and {@link #getActivationPolicy()} is {@link Constants#ACTIVATION_LAZY}</li>
	 * <li><code>Eclipse-LazyStart</code> is set to <code>true</code> when the target version is 3.2 or 3.3
	 *  and {@link #getActivationPolicy()} is {@link Constants#ACTIVATION_LAZY}</li>
	 * <li><code>Bundle-ActivationPolicy</code> is set to <code>lazy</code> when the target version is 3.4 or
	 *  greater and {@link #getActivationPolicy()} is {@link Constants#ACTIVATION_LAZY}</li>
	 * </ul>
	 * </p>
	 * @param equinox whether targeted for the Equinox OSGi framework
	 * @see #getTargetVersion()
	 */
	public void setEquinox(boolean equinox);

	/**
	 * Returns whether the described bundle is targeted for the Equinox OSGi framework.
	 * Affects the Equinox lazy-start header generated in the manifest.
	 * 
	 * @return whether the described bundle is targeted for the Equinox OSGi framework
	 */
	public boolean isEquinox();

	/**
	 * Sets this bundle's activation policy. Legal values are {@link Constants#ACTIVATION_LAZY}
	 * or <code>null</code> (unspecified). Any other values are ignored (equivalent to
	 * <code>null</code>). When unspecified, a corresponding header is not generated. By default
	 * the value is unspecified for newly created projects.
	 * <p>
	 * An Equniox specific lazy-start header will be generated in the associated manifest
	 * based on the {@link #getTargetVersion()} and the specified policy. The headers are
	 * removed when the policy is unspecified (<code>null</code>).
	 * <ul>
	 * <li><code>Eclipse-AutoStart</code> is set to <code>true</code> when the target version is 3.1
	 * 	and policy is {@link Constants#ACTIVATION_LAZY}</li>
	 * <li><code>Eclipse-LazyStart</code> is set to <code>true</code> when the target version is 3.2 or 3.3
	 *  and policy is {@link Constants#ACTIVATION_LAZY}</li>
	 * <li><code>Bundle-ActivationPolicy</code> is set to <code>lazy</code> when the target version is 3.4 or
	 *  greater and policy is {@link Constants#ACTIVATION_LAZY}</li>
	 * </ul>
	 * </p>
	 * @param policy activation policy or <code>null</code>
	 * @see #getTargetVersion()
	 */
	public void setActivationPolicy(String policy);

	/**
	 * Returns this bundle's activation policy or <code>null</code> if unspecified.
	 * 
	 * @return activation policy or <code>null</code>
	 */
	public String getActivationPolicy();

	/**
	 * Sets whether this bundle supports extension points and extensions via 
	 * {@link IExtensionRegistry} support. By default, this value is <code>false</code>
	 * for new projects.
	 * 
	 * @param supportExtensions whether extension points and extensions are supported
	 */
	public void setExtensionRegistry(boolean supportExtensions);

	/**
	 * Returns whether this bundle supports extension points and extensions via 
	 * {@link IExtensionRegistry} support. By default, this value is <code>false</code>
	 * for new projects.
	 * 
	 * @return whether extension points and extensions are supported
	 */
	public boolean isExtensionRegistry();

	/**
	 * Sets the value of the Require-Bundle header for the described bundle.
	 * 
	 * @param bundles required bundle descriptions or <code>null</code> if none
	 */
	public void setRequiredBundles(IRequiredBundleDescription[] bundles);

	/**
	 * Returns the value of the Require-Bundle header or <code>null</code> if unspecified.
	 * 
	 * @return required bundle descriptions or <code>null</code>
	 */
	public IRequiredBundleDescription[] getRequiredBundles();

	/**
	 * Sets the value of the Import-Package header for the described bundle.
	 * 
	 * @param imports package import descriptions or <code>null</code> if none
	 */
	public void setPackageImports(IPackageImportDescription[] imports);

	/**
	 * Returns the value of the Import-Package header or <code>null</code> if unspecified.
	 * 
	 * @return package import descriptions or <code>null</code>
	 */
	public IPackageImportDescription[] getPackageImports();

	/**
	 * Sets the value of the Export-Package header for the described bundle.
	 * 
	 * @param exports package export descriptions or <code>null</code> if none
	 */
	public void setPackageExports(IPackageExportDescription[] exports);

	/**
	 * Returns the value of the Export-Package header or <code>null</code> if unspecified.
	 * 
	 * @return package export descriptions or <code>null</code>
	 */
	public IPackageExportDescription[] getPackageExports();

	/**
	 * Returns the project associated with the described bundle.
	 * 
	 * @return associated project
	 */
	public IProject getProject();

	/**
	 * Sets file and folder entries on the <code>bin.includes</code> entry of
	 * the <code>build.properties</code> file of the described bundle project.
	 * <p>
	 * By default, the <code>MANIFEST/</code> folder and any entries on the
	 * Bundle-Classpath will be included. This sets any additional entries that 
	 * are to be included.
	 * </p>
	 * @param paths bundle root relative paths of files and folders to include
	 *  or <code>null</code> if none
	 */
	public void setBinIncludes(IPath[] paths);

	/**
	 * Returns the file and folder entries to be included on the <code>bin.includes</code> entry
	 * of the <code>build.properties</code> file of the described bundle project.
	 * <p>
	 * By default, the <code>MANIFEST/</code> folder and any entries on the
	 * Bundle-Classpath will be included. This returns any additional entries that 
	 * are to be included.
	 * </p>
	 * @return bundle root relative paths of files and folders on the <code>bin.includes</code>
	 * 	entry or <code>null</code>
	 */
	public IPath[] getBinIncludes();

	/**
	 * Sets the location within the project where the root of the bundle and its associated
	 * artifacts will reside, or <code>null</code> to indicate the default bundle root location
	 * should be used (project folder).
	 * <p>
	 * This has no effect on existing projects.  This method is intended for use on descriptions
	 * for new projects. To modify the bundle root of an existing project use
	 * {@link IBundleProjectService#setBundleRoot(IProject, IPath)}.
	 * </p>
	 * <p>
	 * The bundle root is the folder containing the <code>META-INF/</code> folder. When a project
	 * does not yet exist, bundle files will be created relative to the bundle root. When a project
	 * already exists and the bundle root location is modified, existing bundle artifacts at the old
	 * root are not moved or modified. Instead, the modify operation will update any existing bundle
	 * files at the new root location, or create them if not yet present.
	 * </p>
	 * @param path project relative path to bundle root artifacts in the project or <code>null</code>
	 */
	public void setBundleRoot(IPath path);

	/**
	 * Returns the location within the project that is the root of the bundle related
	 * artifacts, or <code>null</code> to indicate the default location (project folder).
	 * 
	 * @return project relative bundle root path or <code>null</code>
	 */
	public IPath getBundleRoot();

	/**
	 * Returns identifiers of <code>org.eclipse.debug.ui.launchShortcuts</code>
	 * referenced by <code>org.eclipse.pde.ui.launchShortcuts</code> extensions
	 * that will be displayed in the manifest editor for the project associated
	 * with these settings, or <code>null</code> if default shortcuts are being
	 * used.
	 * 
	 * @return identifiers of the <code>org.eclipse.debug.ui.launchShortcuts</code> extensions
	 *  or <code>null</code>
	 */
	public String[] getLaunchShortcuts();

	/**
	 * Sets the identifiers of <code>org.eclipse.debug.ui.launchShortcuts</code>
	 * referenced by <code>org.eclipse.pde.ui.launchShortcuts</code> extensions
	 * to be displayed in the manifest editor for the project associated with these settings,
	 * or <code>null</code> to indicate default shortcuts should be used.
	 * <p>
	 * When default shortcuts are used, all <code>org.eclipse.pde.ui.launchShortcuts</code> extensions
	 * are considered. When specific shortcuts are specified, the available shortcuts will be limited
	 * to those specified.
	 * </p>
	 * <p>
	 * <b>Important</b>: When specifying shortcuts, both <code>org.eclipse.debug.ui.launchShortcuts</code> and
	 * <code>org.eclipse.pde.ui.launchShortcuts</code> must exist. Labels for shortcuts in the editor
	 * are derived from the <code>org.eclipse.pde.ui.launchShortcuts</code>.
	 * </p>
	 * @param ids identifiers of <code>org.eclipse.debug.ui.launchShortcuts</code> extensions
	 *  or <code>null</code>
	 */
	public void setLaunchShortcuts(String[] ids);

	/**
	 * Returns the identifier of the <code>org.eclipse.ui.exportWizards</code> extension
	 * used in the manifest editor for exporting the project associated with these
	 * settings, or <code>null</code> if the default export wizard should be used.
	 * 
	 * @return identifier of an <code>org.eclipse.ui.exportWizards</code> extension
	 *  or <code>null</code>
	 */
	public String getExportWizardId();

	/**
	 * Sets the identifier of the <code>org.eclipse.ui.exportWizards</code> extension
	 * used in the manifest editor for exporting the project associated with these
	 * settings, or <code>null</code> if the default export wizard should be used.
	 * 
	 * @param id identifier of an <code>org.eclipse.ui.exportWizards</code> extension
	 *  or <code>null</code>
	 */
	public void setExportWizardId(String id);

	/**
	 * Sets the value of the specified header in the bundle manifest to the given value
	 * or removes the header if the value is <code>null</code>.
	 * <p>
	 * Using this method will override the value of a header generated by other methods
	 * in this interface. For example, using the method {@link #setBundleName(String)} will
	 * generate a value for the <code>Bundle-Name</code> header, but calling {@link #setHeader(String, String)}
	 * for the <code>Bundle-Name</code> header will override any value set via {@link #setBundleName(String)}.
	 * This method is intended to be used to set the value of a header that does
	 * not have explicit API.
	 * </p><p>
	 * To include an empty header <code>value.trim().length()</code> must equal zero (i.e an empty string or a string of
	 * whitespace).
	 * </p>
	 * @param header header name
	 * @param value header value or <code>null</code> to remove
	 */
	public void setHeader(String header, String value);

	/**
	 * Returns the value of the specified header from the bundle manifest, or <code>null</code>
	 * if unspecified. Note that an empty string is returned for a header that has an empty value.
	 * 
	 * @param header
	 * @return header value or <code>null</code>
	 */
	public String getHeader(String header);

}
