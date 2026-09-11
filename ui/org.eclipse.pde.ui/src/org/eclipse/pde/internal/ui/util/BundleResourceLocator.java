/*******************************************************************************
 * Copyright (c) 2026 Lakshminarayana Nekkanti and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lakshminarayana Nekkanti - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

/**
 * Resolves and opens the value of a resource-like attribute, for example the
 * <code>icon</code> attribute of an extension element, a splash path or a
 * <code>build.properties</code> entry.
 * <p>
 * The following notations are understood, in this order:
 * </p>
 * <ul>
 * <li><code>platform:/plugin/&lt;bundle-id&gt;/&lt;path&gt;</code> and the
 * <code>platform:/fragment/...</code> and <code>platform:/base/plugin/...</code>
 * variants. The resource is looked up in the referenced bundle, which may be a
 * workspace project or a bundle of the target platform (directory or jar).</li>
 * <li>The path variables <code>$nl$</code>, <code>$os$</code> and
 * <code>$ws$</code>, which are expanded exactly like at runtime, most specific
 * location first, ending with the un-qualified location.</li>
 * <li>A path relative to the root of the bundle being edited.</li>
 * <li>A workspace absolute path of the form
 * <code>/&lt;project&gt;/&lt;path&gt;</code>.</li>
 * </ul>
 * <p>
 * The same notations are accepted by the PDE builder when it validates resource
 * attributes, so everything that does not produce a marker can be opened.
 * </p>
 */
public final class BundleResourceLocator {

	/**
	 * A resource reference parsed out of an attribute value.
	 *
	 * @param bundleId       the symbolic name of the bundle the resource belongs
	 *                       to, or <code>null</code> if the resource belongs to the
	 *                       bundle that contains the attribute
	 * @param candidatePaths the possible locations of the resource inside that
	 *                       bundle, most specific first, never empty
	 */
	public record BundleResourceRef(String bundleId, List<String> candidatePaths) {

		public BundleResourceRef {
			candidatePaths = List.copyOf(candidatePaths);
		}
	}

	private static final String PLATFORM_SCHEME = "platform:/"; //$NON-NLS-1$
	private static final String BASE_SEGMENT = "base"; //$NON-NLS-1$
	private static final String PLUGIN_SEGMENT = "plugin"; //$NON-NLS-1$
	private static final String FRAGMENT_SEGMENT = "fragment"; //$NON-NLS-1$

	private static final String NL_VARIABLE = "$nl$"; //$NON-NLS-1$
	private static final String OS_VARIABLE = "$os$"; //$NON-NLS-1$
	private static final String WS_VARIABLE = "$ws$"; //$NON-NLS-1$
	private static final String[] VARIABLES = {NL_VARIABLE, OS_VARIABLE, WS_VARIABLE};

	/** Guards against a pathological number of combined path variables. */
	private static final int MAX_CANDIDATES = 32;

	/** Directory below the plug-in state location for entries read out of jars. */
	private static final String EXTRACTED_DIR = "external-resources"; //$NON-NLS-1$

	private static final Pattern REPEATED_SEPARATORS = Pattern.compile("/{2,}"); //$NON-NLS-1$
	private static final Pattern UNSAFE_FILE_NAME_CHARS = Pattern.compile("[^a-zA-Z0-9._-]"); //$NON-NLS-1$

	private BundleResourceLocator() {
	}

	/**
	 * Parses an attribute value into the bundle it points at and the locations to
	 * look at inside that bundle.
	 *
	 * @param value the raw attribute value, may be <code>null</code>
	 * @return the parsed reference or <code>null</code> if the value is empty or
	 *         uses a notation that cannot be resolved to a bundle resource, for
	 *         example an <code>http:</code> URL
	 */
	public static BundleResourceRef parse(String value) {
		if (value == null) {
			return null;
		}
		String location = value.trim();
		if (location.isEmpty()) {
			return null;
		}
		String bundleId = null;
		if (location.regionMatches(true, 0, PLATFORM_SCHEME, 0, PLATFORM_SCHEME.length())) {
			PlatformUrl platformUrl = parsePlatformUrl(location.substring(PLATFORM_SCHEME.length()));
			if (platformUrl == null) {
				return null;
			}
			bundleId = platformUrl.bundleId();
			location = platformUrl.path();
		} else if (hasScheme(location)) {
			// some other protocol, nothing this class can resolve
			return null;
		}
		// one location per combination of path variable substitutions
		List<String> candidates = new ArrayList<>(4);
		expand(location, candidates);
		if (candidates.isEmpty()) {
			return null;
		}
		return new BundleResourceRef(bundleId, candidates);
	}

	/**
	 * Returns the workspace resource an attribute value points at.
	 *
	 * @param contextProject the project of the bundle that contains the attribute,
	 *                       may be <code>null</code>
	 * @param value          the raw attribute value, may be <code>null</code>
	 * @return the resource or <code>null</code> if the value cannot be resolved or
	 *         the resource belongs to a bundle that is not in the workspace
	 * @see #open(IProject, String)
	 */
	public static IResource findWorkspaceResource(IProject contextProject, String value) {
		BundleResourceRef ref = parse(value);
		if (ref == null) {
			return null;
		}
		if (ref.bundleId() == null) {
			return findMember(contextProject, ref.candidatePaths(), true);
		}
		IPluginModelBase model = PluginRegistry.findModel(ref.bundleId());
		IResource underlying = model != null ? model.getUnderlyingResource() : null;
		if (underlying == null) {
			return null;
		}
		return findMember(underlying.getProject(), ref.candidatePaths(), false);
	}

	/**
	 * Opens the resource an attribute value points at. Files are opened in an
	 * editor, folders are revealed in the Project Explorer. Resources of a bundle
	 * that is not in the workspace are opened read-only.
	 *
	 * @param contextProject the project of the bundle that contains the attribute,
	 *                       may be <code>null</code>
	 * @param value          the raw attribute value, may be <code>null</code>
	 * @return <code>true</code> if the resource was found and opened,
	 *         <code>false</code> if the caller should signal that there is nothing
	 *         to open, traditionally with a beep
	 */
	public static boolean open(IProject contextProject, String value) {
		BundleResourceRef ref = parse(value);
		if (ref == null) {
			return false;
		}
		if (ref.bundleId() == null) {
			return openResource(findMember(contextProject, ref.candidatePaths(), true));
		}
		IPluginModelBase model = PluginRegistry.findModel(ref.bundleId());
		if (model == null) {
			return false;
		}
		IResource underlying = model.getUnderlyingResource();
		if (underlying != null) {
			return openResource(findMember(underlying.getProject(), ref.candidatePaths(), false));
		}
		return openExternal(model, ref.candidatePaths());
	}

	/** A parsed <code>platform:/plugin/...</code> URL. */
	private record PlatformUrl(String bundleId, String path) {
	}

	/**
	 * Parses the part of a <code>platform:</code> URL that follows the scheme.
	 *
	 * @param path for example <code>plugin/org.eclipse.pde.ui/icons/x.svg</code>,
	 *             optionally prefixed with the <code>base</code> segment
	 * @return the referenced bundle and the path inside it, or <code>null</code> if
	 *         this is not a plug-in or fragment URL that names a resource
	 */
	private static PlatformUrl parsePlatformUrl(String path) {
		List<String> segments = Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toList(); //$NON-NLS-1$
		int index = 0;
		if (index < segments.size() && BASE_SEGMENT.equals(segments.get(index))) {
			index++;
		}
		if (index >= segments.size()) {
			return null;
		}
		String kind = segments.get(index++);
		if (!PLUGIN_SEGMENT.equals(kind) && !FRAGMENT_SEGMENT.equals(kind)) {
			return null;
		}
		if (index + 1 >= segments.size()) {
			// no bundle id, or no path inside the bundle
			return null;
		}
		String bundleId = segments.get(index++);
		String bundlePath = String.join("/", segments.subList(index, segments.size())); //$NON-NLS-1$
		return new PlatformUrl(bundleId, bundlePath);
	}

	/**
	 * Returns whether the value starts with a URL scheme, so that it is not
	 * mistaken for a path. A colon that follows the first separator belongs to a
	 * file name, not to a scheme.
	 */
	private static boolean hasScheme(String location) {
		int colon = location.indexOf(':');
		if (colon <= 0) {
			return false;
		}
		int slash = location.indexOf('/');
		if (slash >= 0 && slash < colon) {
			return false;
		}
		return location.substring(0, colon).chars()
				.allMatch(c -> Character.isLetterOrDigit(c) || c == '+' || c == '-' || c == '.');
	}

	/**
	 * Replaces the path variables of a location by every substitution they have,
	 * depth first, and collects the resulting locations in the order in which they
	 * must be searched: most specific first, plain location last. A location
	 * without a variable is a candidate of its own.
	 */
	private static void expand(String location, List<String> candidates) {
		if (candidates.size() >= MAX_CANDIDATES) {
			return;
		}
		for (String variable : VARIABLES) {
			int index = location.indexOf(variable);
			if (index < 0) {
				continue;
			}
			String prefix = location.substring(0, index);
			String suffix = location.substring(index + variable.length());
			for (String substitution : substitutionsFor(variable)) {
				expand(prefix + substitution + suffix, candidates);
			}
			return;
		}
		String candidate = normalize(location);
		if (candidate != null && !candidates.contains(candidate)) {
			candidates.add(candidate);
		}
	}

	/**
	 * Returns the replacements of a path variable, most specific first and always
	 * ending with the empty replacement, which yields the un-qualified location.
	 */
	private static List<String> substitutionsFor(String variable) {
		List<String> substitutions = new ArrayList<>(3);
		switch (variable) {
			case NL_VARIABLE -> {
				String[] nl = split(TargetPlatform.getNL(), "_"); //$NON-NLS-1$
				if (nl.length > 1) {
					substitutions.add("nl/" + nl[0] + '/' + nl[1]); //$NON-NLS-1$
				}
				if (nl.length > 0) {
					substitutions.add("nl/" + nl[0]); //$NON-NLS-1$
				}
			}
			case OS_VARIABLE -> {
				String os = emptyToNull(TargetPlatform.getOS());
				String arch = emptyToNull(TargetPlatform.getOSArch());
				if (os != null && arch != null) {
					substitutions.add("os/" + os + '/' + arch); //$NON-NLS-1$
				}
				if (os != null) {
					substitutions.add("os/" + os); //$NON-NLS-1$
				}
			}
			case WS_VARIABLE -> {
				String ws = emptyToNull(TargetPlatform.getWS());
				if (ws != null) {
					substitutions.add("ws/" + ws); //$NON-NLS-1$
				}
			}
			default -> { // no replacement known
			}
		}
		substitutions.add(""); //$NON-NLS-1$
		return substitutions;
	}

	/** Splits a value into its non-empty parts, tolerating a missing value. */
	private static String[] split(String value, String separator) {
		String trimmed = emptyToNull(value);
		if (trimmed == null) {
			return new String[0];
		}
		return Arrays.stream(trimmed.split(separator)).filter(s -> !s.isEmpty()).toArray(String[]::new);
	}

	private static String emptyToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * Brings a location into the form used for the lookup. Dropping a location that
	 * is empty or a bare separator matters because substituting a variable with the
	 * empty string can leave nothing but the separator behind.
	 */
	private static String normalize(String location) {
		String normalized = REPEATED_SEPARATORS.matcher(location.replace('\\', '/')).replaceAll("/"); //$NON-NLS-1$
		return normalized.isEmpty() || "/".equals(normalized) ? null : normalized; //$NON-NLS-1$
	}

	/**
	 * Looks the candidates up below the root of the given bundle project, falling
	 * back to a workspace absolute interpretation.
	 *
	 * @param project                the project of the bundle, may be
	 *                               <code>null</code>
	 * @param candidates             the locations to try, in that order
	 * @param allowWorkspaceAbsolute whether a candidate may also be read as
	 *                               <code>/&lt;project&gt;/&lt;path&gt;</code>,
	 *                               which only makes sense for a resource of the
	 *                               bundle being edited
	 * @return the first resource that exists, or <code>null</code>
	 */
	private static IResource findMember(IProject project, List<String> candidates, boolean allowWorkspaceAbsolute) {
		IContainer root = project != null && project.isAccessible() ? PDEProject.getBundleRoot(project) : null;
		for (String candidate : candidates) {
			IPath path = IPath.fromPortableString(candidate);
			if (root != null) {
				// a candidate can start with a separator, for example after $nl$
				// was substituted with the empty string
				IResource member = root.findMember(path.makeRelative());
				if (member != null) {
					return member;
				}
			}
			// "/<project>/<path>", as accepted by the plugin.xml source page
			if (allowWorkspaceAbsolute && path.isAbsolute() && path.segmentCount() > 1) {
				IResource member = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
				if (member != null) {
					return member;
				}
			}
		}
		return null;
	}

	/**
	 * Opens a file in an editor and reveals any other resource in the Project
	 * Explorer.
	 */
	private static boolean openResource(IResource resource) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		if (page == null || resource == null || !resource.exists()) {
			return false;
		}
		try {
			if (resource instanceof IFile file) {
				IDE.openEditor(page, file, true);
				return true;
			}
			IViewPart view = page.showView(IPageLayout.ID_PROJECT_EXPLORER);
			if (view instanceof ISetSelectionTarget target) {
				target.selectReveal(new StructuredSelection(resource));
				return true;
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return false;
	}

	/**
	 * Opens a resource of a bundle that is not in the workspace, from the install
	 * location of that bundle. Such a resource is always opened read-only.
	 */
	private static boolean openExternal(IPluginModelBase model, List<String> candidates) {
		String location = model.getInstallLocation();
		if (location == null) {
			return false;
		}
		File bundle = new File(location);
		if (bundle.isDirectory()) {
			for (String candidate : candidates) {
				String entryName = toEntryName(candidate);
				File file = entryName != null ? new File(bundle, entryName) : null;
				if (file != null && file.isFile()) {
					return openFileStore(file);
				}
			}
			return false;
		}
		if (!bundle.isFile()) {
			return false;
		}
		try (ZipFile jar = new ZipFile(bundle)) {
			for (String candidate : candidates) {
				String entryName = toEntryName(candidate);
				ZipEntry entry = entryName != null ? jar.getEntry(entryName) : null;
				if (entry == null || entry.isDirectory()) {
					continue;
				}
				return openFileStore(extract(jar, entry, entryName, model));
			}
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		return false;
	}

	/**
	 * Returns the bundle relative path as a jar entry name, or <code>null</code> if
	 * it escapes the bundle.
	 */
	private static String toEntryName(String candidate) {
		IPath path = IPath.fromPortableString(candidate).makeRelative();
		for (String segment : path.segments()) {
			if ("..".equals(segment)) { //$NON-NLS-1$
				return null;
			}
		}
		return path.segmentCount() > 0 ? path.toPortableString() : null;
	}

	/**
	 * Copies an entry of a jarred bundle to the state location so that it can be
	 * opened by any editor. The copy is read-only and is reused as long as it
	 * matches the entry.
	 */
	private static File extract(ZipFile jar, ZipEntry entry, String entryName, IPluginModelBase model)
			throws IOException {
		// the validated name, never the name of the entry itself, which could
		// escape the folder the copy belongs in
		IPath target = PDEPlugin.getDefault().getStateLocation().append(EXTRACTED_DIR).append(bundleFolder(model))
				.append(entryName);
		File file = target.toFile();
		if (file.isFile() && file.length() == entry.getSize() && file.lastModified() == entry.getTime()) {
			return file;
		}
		Files.createDirectories(file.toPath().getParent());
		// a copy left behind by an earlier open is read-only
		file.setWritable(true);
		try (InputStream contents = jar.getInputStream(entry)) {
			Files.copy(contents, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		if (entry.getTime() >= 0) {
			file.setLastModified(entry.getTime());
		}
		file.setReadOnly();
		return file;
	}

	/** Returns a file name safe folder name that is unique per bundle version. */
	private static String bundleFolder(IPluginModelBase model) {
		IPluginBase bundle = model.getPluginBase();
		String id = bundle != null ? bundle.getId() : null;
		String version = bundle != null ? bundle.getVersion() : null;
		String folder = (id == null ? "bundle" : id) + '_' + (version == null ? "0.0.0" : version); //$NON-NLS-1$ //$NON-NLS-2$
		return UNSAFE_FILE_NAME_CHARS.matcher(folder).replaceAll("_"); //$NON-NLS-1$
	}

	/** Opens a file that is outside of the workspace, by its location. */
	private static boolean openFileStore(File file) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		if (page == null || file == null) {
			return false;
		}
		try {
			IDE.openEditorOnFileStore(page, EFS.getStore(file.toURI()));
			return true;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return false;
		}
	}
}
