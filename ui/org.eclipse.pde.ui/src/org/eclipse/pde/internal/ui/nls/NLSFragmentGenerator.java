/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.nls;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.plugin.FragmentFieldData;
import org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * Generates the fragment projects for the list of selected plug-ins.
 * Each fragment contains a series of properties files specific to the locales
 * the user selected. For example plugin_fr.properties for the french locale
 * selection. The generated locale-specific properties files contain the same
 * key-value pairs as the properties file of the initial plug-in.
 *
 */
public class NLSFragmentGenerator {
	public static final String PLUGIN_NAME_MACRO = "${plugin_name}"; //$NON-NLS-1$
	public static final String LOCALE_NAME_MACRO = "${locale}"; //$NON-NLS-1$

	private static final String HTML_EXTENSION = ".html"; //$NON-NLS-1$
	private static final String XML_EXTENSION = ".xml"; //$NON-NLS-1$
	private static final String CLASS_EXTENSION = ".class"; //$NON-NLS-1$
	private static final String JAVA_EXTENSION = ".java"; //$NON-NLS-1$
	private static final String PROPERTIES_EXTENSION = ".properties"; //$NON-NLS-1$

	private static final String BIN = "/bin/"; //$NON-NLS-1$
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String BACKSLASH = "\\"; //$NON-NLS-1$
	private static final String SLASH = "/"; //$NON-NLS-1$
	private static final String RESOURCE_FOLDER_PARENT = "/nl"; //$NON-NLS-1$
	private static final double LATEST_ECLIPSE_VERSION = 3.4;

	private static final String ZERO = "0"; //$NON-NLS-1$
	private static final String PERIOD = "."; //$NON-NLS-1$
	private static final String MIN_MINOR = ZERO;
	private static final String MAX_MINOR = "9"; //$NON-NLS-1$
	private static final String LEFT_SQUARE_BRACKET = "["; //$NON-NLS-1$
	private static final String RIGHT_PARENTHESIS = ")"; //$NON-NLS-1$
	private static final String DEFAULT_VERSION = "1.0.0"; //$NON-NLS-1$
	private static final String VERSION_FORMAT_WITH_QUALIFIER = "\\d+\\.\\d+\\.\\d+\\..+"; //$NON-NLS-1$
	private static final String LOCALE_INFIX_SEPERATOR = "_"; //$NON-NLS-1$

	private final IWizardContainer container;
	private final String template;
	private final List plugins;
	private final List locales;
	private final boolean overwriteWithoutAsking;
	private IProgressMonitor monitor;

	private final Filters resourceFilter = new Filters(false) {
		{
			add(new AbstractFilter(false) {
				public boolean matches(Object object) {
					String resource = object.toString();
					return resource.endsWith(PROPERTIES_EXTENSION) || resource.endsWith(CLASS_EXTENSION) || resource.endsWith(JAVA_EXTENSION);
				}
			});

			add(new AbstractFilter(false) {
				public boolean matches(Object object) {
					String path = object.toString();
					return path.indexOf(BIN) != -1 || path.endsWith(SLASH) || path.endsWith(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
				}
			});

			add(new AbstractFilter(true) {
				public boolean matches(Object object) {
					String path = object.toString();
					return path.endsWith(XML_EXTENSION) || path.endsWith(HTML_EXTENSION);
				}
			});
		}
	};

	private final Filters propertiesFilter = new Filters(false) {
		{
			add(new AbstractFilter(false) {
				public boolean matches(Object object) {
					String path = object.toString();
					return path.indexOf(BIN) != -1 || path.endsWith(ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
				}
			});

			add(new AbstractFilter(true) {
				public boolean matches(Object object) {
					return object.toString().endsWith(PROPERTIES_EXTENSION);
				}
			});
		}
	};

	public NLSFragmentGenerator(String template, List plugins, List locales, IWizardContainer container, boolean overwriteWithoutAsking) {
		this.plugins = plugins;
		this.locales = locales;
		this.container = container;
		this.template = template;
		this.overwriteWithoutAsking = overwriteWithoutAsking;
	}

	private synchronized void setProgressMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	private synchronized IProgressMonitor getProgressMonitor() {
		return monitor;
	}

	public boolean generate() {
		try {
			final Map overwrites = promptForOverwrite(plugins, locales);

			container.run(false, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					setProgressMonitor(monitor);
					try {
						internationalizePlugins(plugins, locales, overwrites);
					} catch (final Exception ex) {
						Display.getDefault().syncExec(new Runnable() {
							public void run() {
								PDEPlugin.logException(ex, ex.getMessage(), PDEUIMessages.InternationalizeWizard_NLSFragmentGenerator_errorMessage);
							}
						});
					}
				}
			});
		} catch (Exception e) {
			PDEPlugin.logException(e);
		}
		return true;
	}

	/**
	 * Creates an NL fragment project along with the locale specific properties
	 * files.
	 * @throws CoreException 
	 * @throws IOException 
	 * @throws InvocationTargetException 
	 * @throws InterruptedException 
	 */
	private void internationalizePlugins(List plugins, List locales, Map overwrites) throws CoreException, IOException, InvocationTargetException, InterruptedException {

		Set created = new HashSet();

		for (Iterator it = plugins.iterator(); it.hasNext();) {
			IPluginModelBase plugin = (IPluginModelBase) it.next();

			for (Iterator iter = locales.iterator(); iter.hasNext();) {
				Locale locale = (Locale) iter.next();

				IProject project = getNLProject(plugin, locale);
				if (created.contains(project) || overwriteWithoutAsking || !project.exists() || OVERWRITE == overwrites.get(project.getName())) {
					if (!created.contains(project) && project.exists()) {
						project.delete(true, getProgressMonitor());
					}

					if (!created.contains(project)) {
						createNLFragment(plugin, project, locale);
						created.add(project);
						project.getFolder(RESOURCE_FOLDER_PARENT).create(false, true, getProgressMonitor());
					}

					project.getFolder(RESOURCE_FOLDER_PARENT).getFolder(locale.toString()).create(true, true, getProgressMonitor());
					createLocaleSpecificPropertiesFile(project, plugin, locale);
				}
			}
		}
	}

	private Object OVERWRITE = new Object();

	private Map promptForOverwrite(List plugins, List locales) {
		Map overwrites = new HashMap();

		if (overwriteWithoutAsking)
			return overwrites;

		for (Iterator iter = plugins.iterator(); iter.hasNext();) {
			IPluginModelBase plugin = (IPluginModelBase) iter.next();
			for (Iterator it = locales.iterator(); it.hasNext();) {
				Locale locale = (Locale) it.next();
				IProject project = getNLProject(plugin, locale);

				if (project.exists() && !overwrites.containsKey(project.getName())) {
					boolean overwrite = MessageDialog.openConfirm(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.InternationalizeWizard_NLSFragmentGenerator_overwriteTitle, NLS.bind(PDEUIMessages.InternationalizeWizard_NLSFragmentGenerator_overwriteMessage, pluginName(plugin, locale)));
					overwrites.put(project.getName(), overwrite ? OVERWRITE : null);
				}
			}
		}

		return overwrites;
	}

	private IProject getNLProject(final IPluginModelBase name, final Locale locale) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(pluginName(name, locale));
	}

	/**
	 * Creates a fragment project for the specified plug-in and populates
	 * the field data.
	 * @param plugin
	 * @throws CoreException 
	 * @throws InvocationTargetException 
	 * @throws InterruptedException 
	 */
	private void createNLFragment(final IPluginModelBase plugin, final IProject project, final Locale locale) throws CoreException, InvocationTargetException, InterruptedException {
		FragmentFieldData fragmentData = populateFieldData(plugin, locale);

		IProjectProvider projectProvider = new IProjectProvider() {
			public String getProjectName() {
				return project.getName();
			}

			public IProject getProject() {
				return project;
			}

			public IPath getLocationPath() {
				return project.getLocation();
			}
		};

		new NewProjectCreationOperation(fragmentData, projectProvider, null).run(getProgressMonitor());
	}

	private String pluginName(IPluginModelBase plugin, Locale locale) {
		return template.replaceAll(quote(PLUGIN_NAME_MACRO), plugin.getPluginBase().getId()).replaceAll(quote(LOCALE_NAME_MACRO), locale.toString());
	}

	/**
	 * The fields are populated based on the plug-in attributes. Some fields
	 * are set to their default values.
	 */
	private FragmentFieldData populateFieldData(IPluginModelBase plugin, Locale locale) {
		FragmentFieldData fragmentData = new FragmentFieldData();

		fragmentData.setId(pluginName(plugin, locale));
		fragmentData.setVersion(DEFAULT_VERSION);
		fragmentData.setMatch(0);

		fragmentData.setPluginId(plugin.getPluginBase().getId());
		fragmentData.setPluginVersion(incrementRelease(plugin.getPluginBase().getVersion()));
		fragmentData.setName(pluginName(plugin, locale) + " Fragment"); //$NON-NLS-1$
		fragmentData.setProvider(EMPTY_STRING);
		fragmentData.setSimple(true);

		if (!(plugin instanceof ExternalPluginModelBase)) {
			fragmentData.setSourceFolderName("src"); //$NON-NLS-1$
			fragmentData.setOutputFolderName("bin"); //$NON-NLS-1$
		}

		fragmentData.setLegacy(false);
		fragmentData.setTargetVersion(Double.toString(ensureTargetVersionCompatibility(TargetPlatformHelper.getTargetVersion())));
		fragmentData.setHasBundleStructure(true);
		fragmentData.setOSGiFramework(null);
		fragmentData.setWorkingSets(null);

		return fragmentData;
	}

	/**
	 * Adjusts the plug-in's version to reflect the required
	 * fragment-host bundle-version range. For example, 
	 * fragment-host's bundle-version range would be: "[1.0.0, 1.1.0)" 
	 * if the host's version is 1.0.0
	 * @param oldVersion
	 * @return adjusted plug-in version
	 */
	private String incrementRelease(String oldVersion) {
		if (oldVersion.matches(VERSION_FORMAT_WITH_QUALIFIER)) {
			oldVersion = oldVersion.substring(0, oldVersion.lastIndexOf(PERIOD));
		}

		String newVersion = LEFT_SQUARE_BRACKET + oldVersion + ',';
		String oldMinor = oldVersion.substring(oldVersion.indexOf(PERIOD) + 1, oldVersion.lastIndexOf(PERIOD));

		if (oldMinor.compareTo(MAX_MINOR) == 0) {
			String major = Integer.toString(Integer.parseInt(oldVersion.substring(0, oldVersion.indexOf(PERIOD))) + 1);
			newVersion += major + PERIOD + MIN_MINOR + PERIOD + ZERO + RIGHT_PARENTHESIS;
		} else {
			String major = oldVersion.substring(0, oldVersion.indexOf(PERIOD));
			String newMinor = Integer.toString(Integer.parseInt(oldMinor) + 1);
			newVersion += major + PERIOD + newMinor + PERIOD + ZERO + RIGHT_PARENTHESIS;
		}

		return newVersion;
	}

	/**
	 * Creates a locale specific properties file within the fragment project 
	 * based on the content of the host plug-in's properties file.
	 * @param fragmentProject
	 * @param locale
	 * @throws CoreException 
	 * @throws IOException 
	 */
	private void createLocaleSpecificPropertiesFile(final IProject fragmentProject, IPluginModelBase plugin, final Locale locale) throws CoreException, IOException {
		final IFolder localeResourceFolder = fragmentProject.getFolder(RESOURCE_FOLDER_PARENT).getFolder(locale.toString());

		//Case 1: External plug-in
		if (plugin instanceof ExternalPluginModelBase) {
			final String installLocation = plugin.getInstallLocation();
			//Case 1a: External plug-in is a jar file
			if (new File(installLocation).isFile()) {
				ZipFile zf = new ZipFile(installLocation);
				for (Enumeration e = zf.entries(); e.hasMoreElements();) {
					worked();

					ZipEntry zfe = (ZipEntry) e.nextElement();
					String name = zfe.getName();

					String[] segments = name.split(SLASH);
					IPath path = Path.fromPortableString(join(SLASH, segments, 0, segments.length - 1));
					String resourceName = segments[segments.length - 1];
					String localizedResourceName = localeSpecificName(resourceName, locale);
					if (propertiesFilter.include(name)) {

						createParents(fragmentProject, path);
						IFile file = fragmentProject.getFile(path.append(localizedResourceName));
						InputStream is = zf.getInputStream(zfe);
						file.create(is, false, getProgressMonitor());
					} else if (resourceFilter.include(name)) {
						IPath target = localeResourceFolder.getFullPath().append(path).append(resourceName);
						createParents(fragmentProject, target.removeLastSegments(1).removeFirstSegments(1));
						IFile file = fragmentProject.getFile(target.removeFirstSegments(1));
						file.create(zf.getInputStream(zfe), false, getProgressMonitor());
					}
				}
			}
			//Case 1b: External plug-in has a folder structure
			else {
				Visitor visitor = new Visitor() {
					public void visit(File file) throws CoreException, FileNotFoundException {
						worked();

						String relativePath = file.getAbsolutePath().substring(installLocation.length()).replaceAll(File.separator, SLASH);
						String[] segments = relativePath.split(SLASH);
						IPath path = Path.fromPortableString(join(SLASH, segments, 0, segments.length - 1));
						String resourceName = segments[segments.length - 1];
						String localizedResourceName = localeSpecificName(resourceName, locale);

						if (propertiesFilter.include(relativePath + (file.isDirectory() ? SLASH : EMPTY_STRING))) {
							createParents(fragmentProject, path);
							IFile iFile = fragmentProject.getFile(path.append(localizedResourceName));
							iFile.create(new FileInputStream(file), false, getProgressMonitor());
						} else if (resourceFilter.include(relativePath + (file.isDirectory() ? SLASH : EMPTY_STRING))) {
							IPath target = localeResourceFolder.getFullPath().append(relativePath);
							createParents(fragmentProject, target.removeLastSegments(1).removeFirstSegments(1));
							IFile iFile = fragmentProject.getFile(target.removeFirstSegments(1));
							iFile.create(new FileInputStream(file), false, getProgressMonitor());
						}

						if (file.isDirectory()) {
							File[] children = file.listFiles();
							for (int i = 0; i < children.length; i++) {
								visit(children[i]);
							}
						}
					}
				};

				visitor.visit(new File(installLocation));
			}
		}
		//Case 2: Workspace plug-in
		else {
			final IProject project = plugin.getUnderlyingResource().getProject();

			project.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					worked();

					IPath parent = resource.getFullPath().removeLastSegments(1).removeFirstSegments(1);
					if (propertiesFilter.include(resource)) {
						String segment = localeSpecificName(resource.getFullPath().lastSegment(), locale);
						IPath fragmentResource = fragmentProject.getFullPath().append(parent).append(segment);

						createParents(fragmentProject, parent);
						resource.copy(fragmentResource, true, getProgressMonitor());
					} else if (resourceFilter.include(resource)) {
						IPath target = localeResourceFolder.getFullPath().append(parent).append(resource.getFullPath().lastSegment());
						createParents(fragmentProject, target.removeLastSegments(1).removeFirstSegments(1));
						resource.copy(target, true, getProgressMonitor());
					}
					return true;
				}
			});
		}

	}

	private void worked() {
		Shell shell = container.getShell();
		Display display = shell.getDisplay();
		if (display != null && !shell.isDisposed()) {
			display.readAndDispatch();
		}
	}

	private void createParents(IProject fragmentProject, IPath parent) throws CoreException {
		String[] segments = parent.segments();
		String path = new String();

		for (int i = 0; i < segments.length; i++) {
			path += SLASH + segments[i];
			IFolder folder = fragmentProject.getFolder(path);
			if (!folder.exists()) {
				folder.create(true, true, getProgressMonitor());
			}
		}
	}

	private String join(String delimiter, String[] parts) {
		return join(delimiter, parts, 0, parts.length);
	}

	private String join(String delimiter, String[] parts, int offset, int n) {
		StringBuffer builder = new StringBuffer();
		for (int i = offset; i < n; i++) {
			builder.append(parts[i]);
			if (i < parts.length - 1) {
				builder.append(delimiter);
			}
		}
		return builder.toString();
	}

	private String localeSpecificName(String name, Locale locale) {
		String[] parts = name.split(BACKSLASH + PERIOD);
		parts[0] = parts[0] + LOCALE_INFIX_SEPERATOR + locale;
		return join(PERIOD, parts);
	}

	private static class Filters {
		private final List filters = new LinkedList();
		private final boolean default_;

		public Filters(boolean default_) {
			this.default_ = default_;
		}

		public void add(Filter filter) {
			filters.add(filter);
		}

		public boolean include(Object object) {
			if (object instanceof IResource) {
				IResource resource = (IResource) object;
				IPath path = IResource.FILE == resource.getType() ? resource.getFullPath() : resource.getFullPath().addTrailingSeparator();
				object = path.toPortableString();
			}

			for (Iterator iter = filters.iterator(); iter.hasNext();) {
				Filter filter = (Filter) iter.next();
				if (filter.matches(object)) {
					return filter.inclusive();
				}
			}
			return default_;
		}
	}

	private static abstract class AbstractFilter implements Filter {

		private final boolean inclusive;

		public AbstractFilter(boolean inclusive) {
			this.inclusive = inclusive;

		}

		public boolean inclusive() {
			return inclusive;
		}
	}

	private static interface Filter {
		boolean inclusive();

		boolean matches(Object object);
	}

	private static interface Visitor {
		void visit(File file) throws CoreException, FileNotFoundException;
	}

	private String quote(String pattern) {
		return "\\Q" + pattern + "\\E"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Ensures that the target version is compatible.
	 * @param targetVersion
	 * @return target version
	 */
	private double ensureTargetVersionCompatibility(double targetVersion) {
		if (targetVersion < 3.0) {
			return LATEST_ECLIPSE_VERSION;
		}
		return targetVersion;
	}
}
