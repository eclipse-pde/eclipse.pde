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
package org.eclipse.pde.internal.builders;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;

public class ManifestConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying";
	public static final String BUILDERS_FRAGMENT_BROKEN_LINK =
		"Builders.Fragment.brokenLink";
	public static final String BUILDERS_UPDATING = "Builders.updating";
	public static final String BUILDERS_VERSION_FORMAT =
		"Builders.versionFormat";

	private boolean javaDelta = false;
	private boolean fileCompiled = false;
	private boolean ignoreJavaChanges=false;

	class DeltaVisitor implements IResourceDeltaVisitor {
		private IProgressMonitor monitor;
		public DeltaVisitor(IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject) {
				// Only check projects with plugin nature
				IProject project = (IProject) resource;
				return (project.isOpen() && PDE.hasPluginNature(project));
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isManifestFile(candidate)) {
					// That's it, but only check it if it has been added or changed
					if (delta.getKind() != IResourceDelta.REMOVED) {
						checkFile(candidate, monitor);
						return false;
					}
				} else if (!ignoreJavaChanges && isJavaFile(candidate)) {
					javaDelta = true;
					return false;
				}
			}
			return true;
		}
	}

	class ReferenceDeltaVisitor implements IResourceDeltaVisitor {
		private boolean interestingChange;
		public ReferenceDeltaVisitor() {
		}

		public boolean isInterestingChange() {
			return interestingChange;
		}

		public boolean visit(IResourceDelta delta) {
			IResource resource = delta.getResource();

			if (resource instanceof IProject) {
				// Only check projects with plugin nature
				IProject project = (IProject) resource;
				return (project.isOpen() && PDE.hasPluginNature(project));
			}
			if (resource instanceof IFile) {
				// see if this is it
				IFile candidate = (IFile) resource;
				if (isManifestFile(candidate)) {
					interestingChange = true;
					return false;
				}
			}
			return true;
		}
	}

	public ManifestConsistencyChecker() {
		super();
	}
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
		throws CoreException {

		IProject project = getProject();
		fileCompiled = false;
		javaDelta = false;
		ignoreJavaChanges = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_CLASS)==CompilerFlags.IGNORE;

		// Ignore binary plug-in projects
		if (WorkspaceModelManager.isBinaryPluginProject(project))
			return null;

		IResourceDelta delta = null;
		if (kind != FULL_BUILD)
			delta = getDelta(getProject());

		if (delta == null || kind == FULL_BUILD) {
			checkThisProject(project, monitor);
		} else {
			processDelta(delta, monitor);
		}
		IProject[] interestingProjects = null;

		// Compute interesting projects
		WorkspaceModelManager wmanager =
			PDECore.getDefault().getWorkspaceModelManager();
		IModel thisModel = wmanager.getWorkspaceModel(project);
		if (thisModel != null && thisModel instanceof IPluginModelBase)
			interestingProjects =
				computeInterestingProjects((IPluginModelBase) thisModel);
		// If not compiled already, see if there are interesting
		// changes in referenced projects that may cause us
		// to compile
		if (!fileCompiled
			&& kind != FULL_BUILD
			&& interestingProjects != null) {
			checkInterestingProjectDeltas(
				project,
				interestingProjects,
				monitor);
		}
		return interestingProjects;
	}

	private void checkThisProject(IProject project, IProgressMonitor monitor) {
		if (!PDE.hasPluginNature(project))
			return;

		IPath path = project.getFullPath().append("plugin.xml");
		IWorkspace workspace = project.getWorkspace();
		IFile file = workspace.getRoot().getFile(path);
		if (file.exists()) {
			checkFile(file, monitor);
		} else {
			path = project.getFullPath().append("fragment.xml");
			file = workspace.getRoot().getFile(path);
			if (file.exists()) {
				checkFile(file, monitor);
			}
		}
	}

	private void checkInterestingProjectDeltas(
		IProject project,
		IProject[] interestingProjects,
		IProgressMonitor monitor)
		throws CoreException {
		// although we didn't have any changes we care about in this project,
		// there may be changes in referenced projects that affect us
		ReferenceDeltaVisitor rvisitor = new ReferenceDeltaVisitor();

		for (int i = 0; i < interestingProjects.length; i++) {
			IProject interestingProject = interestingProjects[i];
			IResourceDelta delta = getDelta(interestingProject);
			if (delta != null) {
				// there is a delta here
				delta.accept(rvisitor);
				if (rvisitor.isInterestingChange())
					break;
			}
		}
		if (rvisitor.isInterestingChange()) {
			// At least one interesting project has a change
			// Need to check the file.
			checkThisProject(project, monitor);
		}
	}

	private void processDelta(IResourceDelta delta, IProgressMonitor monitor)
		throws CoreException {
		javaDelta = false;
		delta.accept(new DeltaVisitor(monitor));
		if (javaDelta) {
			IProject project = getProject();
			IFile file = project.getFile("plugin.xml");
			if (!file.exists())
				file = project.getFile("fragment.xml");
			if (file.exists())
				checkFile(file, monitor);
		}
	}

	private void checkFile(IFile file, IProgressMonitor monitor) {
		PluginErrorReporter reporter = new PluginErrorReporter(file);
		if (WorkspaceModelManager.isBinaryPluginProject(file.getProject()))
			return;
		String message =
			PDE.getFormattedMessage(
				BUILDERS_VERIFYING,
				file.getFullPath().toString());
		monitor.subTask(message);

		ValidatingSAXParser.parse(file, reporter);

		if (reporter.getErrorCount() == 0) {
			if (isFragment(file)) {
				validateFragment(file, reporter);
			} else {
				validatePlugin(file, reporter);
			}
		}
		monitor.subTask(PDE.getResourceString(BUILDERS_UPDATING));
		monitor.done();
		fileCompiled = true;
	}	

	private boolean isFragment(IFile file) {
		String name = file.getName().toLowerCase();
		return name.equals("fragment.xml");
	}
	private boolean isManifestFile(IFile file) {
		if (file.getParent() instanceof IFolder)
			return false;
		String name = file.getName().toLowerCase();
		return name.equals("plugin.xml") || name.equals("fragment.xml");
	}

	private boolean isJavaFile(IFile file) {
		String name = file.getName().toLowerCase();
		return name.endsWith(".java");
	}

	protected void startupOnInitialize() {
		super.startupOnInitialize();
	}

	private void validatePlugin(IFile file, PluginErrorReporter reporter) {
		WorkspacePluginModel model = new WorkspacePluginModel(file);
		model.load();

		if (model.isLoaded()) {
			// Test the version
			IPlugin plugin = model.getPlugin();
			validateRequiredAttributes(plugin, reporter);
			if (reporter.getErrorCount() == 0) {
				validateVersion(plugin, reporter);
				validateValues(plugin, reporter);
			}
		}
		model.dispose();
	}

	private void validateFragment(IFile file, PluginErrorReporter reporter) {
		WorkspaceFragmentModel model = new WorkspaceFragmentModel(file);
		model.load();

		if (model.isLoaded()) {
			// Test the version
			// Test if plugin exists
			IFragment fragment = model.getFragment();
			validateRequiredAttributes(fragment, reporter);
			if (reporter.getErrorCount() == 0) {
				validateVersion(fragment, reporter);
				String pluginId = fragment.getPluginId();
				String pluginVersion = fragment.getPluginVersion();
				int match = fragment.getRule();
				IPlugin plugin =
					PDECore.getDefault().findPlugin(
						pluginId,
						pluginVersion,
						match);
				if (plugin == null) {
					// broken fragment link
					String[] args = { pluginId, pluginVersion };
					String message =
						PDE.getFormattedMessage(
							BUILDERS_FRAGMENT_BROKEN_LINK,
							args);
					int line = 1;
					if (fragment instanceof ISourceObject)
						line = ((ISourceObject) fragment).getStartLine();
					reporter.reportError(message, line);
				}
				validateValues(fragment, reporter);
			}
		}
		model.dispose();
	}

	private IProject[] computeInterestingProjects(IPluginModelBase model) {
		IPluginBase plugin = model.getPluginBase();
		if (plugin == null)
			return null;
		PluginModelManager modelManager =
			PDECore.getDefault().getModelManager();
		ArrayList projects = new ArrayList();
		// Add all projects for imported plug-ins that
		// are in the workspace
		IPluginImport[] iimports = plugin.getImports();
		for (int i = 0; i < iimports.length; i++) {
			IPluginImport iimport = iimports[i];
			if (iimport.getId() == null)
				continue;
			IPluginModelBase importModel =
				modelManager.findPlugin(
					iimport.getId(),
					iimport.getVersion(),
					iimport.getMatch());
			addInterestingProject(iimport.getId(), importModel, projects);
		}
		// If fragment, also add the referenced plug-in
		// if in the workspace 
		if (model.isFragmentModel()) {
			IFragment fragment = (IFragment) plugin;
			if (fragment.getPluginId() != null
				&& fragment.getPluginVersion() != null) {
				IPluginModelBase refPlugin =
					modelManager.findPlugin(
						fragment.getPluginId(),
						fragment.getPluginVersion(),
						fragment.getRule());
				addInterestingProject(
					fragment.getPluginId(),
					refPlugin,
					projects);
			}
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

	private void addInterestingProject(
		String id,
		IPluginModelBase model,
		ArrayList list) {
		if (model != null && model.isEnabled()) {
			if (model.getUnderlyingResource() != null)
				list.add(model.getUnderlyingResource().getProject());
		} else {
			IProject missingProject =
				PDECore.getWorkspace().getRoot().getProject(id);
			list.add(missingProject);
		}
	}

	private void validateVersion(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {
		String version = pluginBase.getVersion();
		if (version == null)
			version = "";
		try {
			PluginVersionIdentifier pvi = new PluginVersionIdentifier(version);
			pvi.toString();
		} catch (Throwable e) {
			String message =
				PDE.getFormattedMessage(BUILDERS_VERSION_FORMAT, version);
			int line = 1;
			if (pluginBase instanceof ISourceObject)
				line = ((ISourceObject) pluginBase).getStartLine();
			reporter.reportError(message, line);
		}
	}

	private void validateValues(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {

		if (!CompilerFlags.isGroupActive(CompilerFlags.PLUGIN_FLAGS))
			return;

		// Validate requires
		validateRequires(
			pluginBase,
			reporter,
			CompilerFlags.getFlag(CompilerFlags.P_UNRESOLVED_IMPORTS));
		// Validate extensions
		validateExtensions(pluginBase, reporter);
		// Validate extension points
		validateExtensionPoints(pluginBase, reporter);
	}

	private void validateRequiredAttributes(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {
		// validate name, id, version
		String rootName = "plugin";
		if (pluginBase instanceof IFragment) {
			IFragment fragment = (IFragment) pluginBase;
			rootName = "fragment";
			assertNotNull(
				"plugin-id",
				rootName,
				getLine(fragment),
				fragment.getPluginId(),
				reporter);
			assertNotNull(
				"plugin-version",
				rootName,
				getLine(fragment),
				fragment.getPluginVersion(),
				reporter);
		}

		assertNotNull(
			"name",
			rootName,
			getLine(pluginBase),
			pluginBase.getName(),
			reporter);
		assertNotNull(
			"id",
			rootName,
			getLine(pluginBase),
			pluginBase.getId(),
			reporter);
		assertNotNull(
			"version",
			rootName,
			getLine(pluginBase),
			pluginBase.getVersion(),
			reporter);

		// validate libraries
		IPluginLibrary[] libraries = pluginBase.getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			assertNotNull(
				"name",
				"library",
				getLine(library),
				library.getName(),
				reporter);
		}
		// validate imports
		IPluginImport[] iimports = pluginBase.getImports();
		for (int i = 0; i < iimports.length; i++) {
			IPluginImport iimport = iimports[i];
			assertNotNull(
				"plugin",
				"import",
				getLine(iimport),
				iimport.getId(),
				reporter);
		}
		// validate extensions
		IPluginExtension[] extensions = pluginBase.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension extension = extensions[i];
			assertNotNull(
				"point",
				"extension",
				getLine(extension),
				extension.getPoint(),
				reporter);
		}
		// validate extension points
		IPluginExtensionPoint[] expoints = pluginBase.getExtensionPoints();
		for (int i = 0; i < expoints.length; i++) {
			IPluginExtensionPoint expoint = expoints[i];
			assertNotNull(
				"id",
				"extension-point",
				getLine(expoint),
				expoint.getId(),
				reporter);
			assertNotNull(
					"name",
					"extension-point",
					getLine(expoint),
					expoint.getName(),
					reporter);
		}
	}

	private static void assertNotNull(
		String att,
		String el,
		int line,
		String value,
		PluginErrorReporter reporter) {
		if (value == null) {
			String message =
				PDE.getFormattedMessage(
					"Builders.manifest.missingRequired",
					new String[] { att, el });
			reporter.reportError(message, line);
		}
	}

	private void validateRequires(
		IPluginBase pluginBase,
		PluginErrorReporter reporter,
		int flag) {
		// Try to find the plug-ins
		if (flag == CompilerFlags.IGNORE)
			return;
		IPluginImport[] imports = pluginBase.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			if (!imports[i].isOptional() && PDECore
				.getDefault()
				.findPlugin(
					iimport.getId(),
					iimport.getVersion(),
					iimport.getMatch())
				== null) {
				reporter.report(
					PDE.getFormattedMessage(
						"Builders.Manifest.dependency",
						iimport.getId()),
					getLine(iimport),
					flag);
			}
		}
	}

	private void validateExtensions(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {
		IPluginExtension[] extensions = pluginBase.getExtensions();
		SchemaMarkerFactory factory = new SchemaMarkerFactory();

		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension extension = extensions[i];
			IPluginExtensionPoint point =
				PDECore.getDefault().findExtensionPoint(extension.getPoint());
			if (point == null) {
				reporter.report(
					PDE.getFormattedMessage(
						"Builders.Manifest.ex-point",
						extension.getPoint()),
					getLine(extension),
					CompilerFlags.getFlag(
						CompilerFlags.P_UNRESOLVED_EX_POINTS));
			} else {
				ISchema schema =
					PDECore.getDefault().getSchemaRegistry().getSchema(
						extension.getPoint());
				if (schema != null) {
					factory.setPoint(extension.getPoint());
					reporter.setMarkerFactory(factory);
					validateExtensionContent(extension, schema, reporter);
					reporter.setMarkerFactory(null);
				}
			}
		}
	}

	private void validateExtensionContent(
		IPluginExtension extension,
		ISchema schema,
		PluginErrorReporter reporter) {

		IPluginObject[] elements = extension.getChildren();
		for (int i = 0; i < elements.length; i++) {
			IPluginElement element = (IPluginElement) elements[i];
			validateElement(element, schema, reporter);
		}
	}

	private void validateContentModel(
		IPluginParent parent,
		ISchemaElement elementInfo,
		PluginErrorReporter reporter) {
		IPluginObject[] children = parent.getChildren();
		ISchemaType type = elementInfo.getType();

		// Compare the content model defined in the 'type' 
		// to the actual content of this parent.
		// Errors should be:
		//   - Elements that should not appear according to the content model 
		//   - Elements that appear too few or too many times
		//   - No elements when the type requires some
		//   - Elements in the wrong order
		HashSet allowedElements = new HashSet();
		computeAllowedElements(type, allowedElements);

		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement) children[i];
			String name = child.getName();
			if (!allowedElements.contains(name)) {
				// Invalid
				reporter.report(
					PDE.getFormattedMessage(
						"Builders.Manifest.child",
						new String[] { child.getName(), parent.getName()}),
					getLine(child),
					CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT));
			}
		}
	}

	private void computeAllowedElements(ISchemaType type, HashSet elementSet) {
		if (type instanceof ISchemaComplexType) {
			ISchemaComplexType complexType = (ISchemaComplexType) type;
			ISchemaCompositor compositor = complexType.getCompositor();
			if (compositor != null)
				computeAllowedElements(compositor, elementSet);
		}
	}

	private void computeAllowedElements(
		ISchemaCompositor compositor,
		HashSet elementSet) {
		ISchemaObject[] children = compositor.getChildren();
		for (int i = 0; i < children.length; i++) {
			ISchemaObject child = children[i];
			if (child instanceof ISchemaObjectReference) {
				ISchemaObjectReference ref = (ISchemaObjectReference) child;
				ISchemaElement refElement =
					(ISchemaElement) ref.getReferencedObject();
				if (refElement != null)
					elementSet.add(refElement.getName());
			} else if (child instanceof ISchemaCompositor) {
				computeAllowedElements((ISchemaCompositor) child, elementSet);
			}
		}
	}

	private void validateElement(
		IPluginElement element,
		ISchema schema,
		PluginErrorReporter reporter) {
		ISchemaElement schemaElement = schema.findElement(element.getName());
		if (schemaElement == null) {
			// Invalid
			reporter.report(
				PDE.getFormattedMessage(
					"Builders.Manifest.element",
					element.getName()),
				getLine(element),
				CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT));
		} else {
			IPluginAttribute[] atts = element.getAttributes();
			validateExistingAttributes(atts, schemaElement, reporter);
			validateRequiredAttributes(element, schemaElement, reporter);
		}

		if (schemaElement != null)
			validateContentModel(element, schemaElement, reporter);

		IPluginObject[] children = element.getChildren();

		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement) children[i];
			// need to validate if this child can appear here
			// according to the parent type.
			validateElement(child, schema, reporter);
		}
	}

	private void validateExistingAttributes(
		IPluginAttribute[] atts,
		ISchemaElement schemaElement,
		PluginErrorReporter reporter) {
		for (int i = 0; i < atts.length; i++) {
			IPluginAttribute att = atts[i];
			ISchemaAttribute attInfo =
				schemaElement.getAttribute(att.getName());
			if (attInfo == null) {
				reporter.report(
					PDE.getFormattedMessage(
						"Builders.Manifest.attribute",
						att.getName()),
					getLine(att.getParent()),
					CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ATTRIBUTE));
			} else
				validateAttribute(att, attInfo, reporter);
		}
	}
	private void validateAttribute(
		IPluginAttribute att,
		ISchemaAttribute attInfo,
		PluginErrorReporter reporter) {
		ISchemaSimpleType type = attInfo.getType();
		ISchemaRestriction restriction = type.getRestriction();
		if (restriction != null) {
			validateRestriction(att, restriction, reporter);
		}
		int kind = attInfo.getKind();
		if (kind == ISchemaAttribute.JAVA) {
			validateJava(att, attInfo, reporter);
		} else if (kind == ISchemaAttribute.RESOURCE) {
			validateResource(att, attInfo, reporter);
		} else if (type.getName().equals("boolean")) {
			validateBoolean(att, reporter);
		}
	}

	private void validateRestriction(
		IPluginAttribute att,
		ISchemaRestriction restriction,
		PluginErrorReporter reporter) {

		Object[] children = restriction.getChildren();
		String value = att.getValue();
		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			if (child instanceof ISchemaEnumeration) {
				ISchemaEnumeration enum = (ISchemaEnumeration) child;
				if (enum.getName().equals(value)) {
					return;
				}
			}
		}
		reporter.report(
			PDE.getFormattedMessage(
				"Builders.Manifest.att-value",
				new String[] { value, att.getName()}),
			getLine(att.getParent()),
			CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE));
	}

	private void validateBoolean(
		IPluginAttribute att,
		PluginErrorReporter reporter) {
		String value = att.getValue();
		if (value.equalsIgnoreCase("true"))
			return;
		if (value.equalsIgnoreCase("false"))
			return;
		reporter.report(
			PDE.getFormattedMessage(
				"Builders.Manifest.att-value",
				new String[] { value, att.getName()}),
			getLine(att.getParent()),
			CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE));
	}

	private void validateJava(
		IPluginAttribute att,
		ISchemaAttribute attInfo,
		PluginErrorReporter reporter) {
		String value = att.getValue();
		String basedOn = attInfo.getBasedOn();
		IProject project = att.getModel().getUnderlyingResource().getProject();
		IJavaProject javaProject = JavaCore.create(project);
		try {
			IType element = javaProject.findType(value);
			if (element == null) {
				reporter.report(
					PDE.getFormattedMessage(
						"Builders.Manifest.class",
						new String[] { value, att.getName()}),
					getLine(att.getParent()),
					CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_CLASS));
			} else if (basedOn != null) {
				// Test the type conditions
				String baseType;
				String baseInterface = null;
				int sep = basedOn.indexOf(":");
				if (sep != -1) {
					baseType = basedOn.substring(0, sep);
					baseInterface = basedOn.substring(sep + 1);
				} else {
					baseType = basedOn;
				}
				IType baseTypeElement = javaProject.findType(baseType);
				if (baseTypeElement != null) {
				}
				if (baseInterface != null) {
					IJavaElement baseInterfaceElement =
						javaProject.findType(baseInterface);
					if (baseInterfaceElement != null) {
					}
				}
			}
		} catch (JavaModelException e) {
		}
	}

	private void validateResource(
		IPluginAttribute att,
		ISchemaAttribute attInfo,
		PluginErrorReporter reporter) {
		String path = att.getValue();
		IProject project = att.getModel().getUnderlyingResource().getProject();
		IResource resource = project.findMember(new Path(path));
		if (resource == null) {
			reporter.report(
				PDE.getFormattedMessage(
					"Builders.Manifest.resource",
					new String[] { path, att.getName()}),
				getLine(att.getParent()),
				CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_RESOURCE));
		}
	}

	private void validateRequiredAttributes(
		IPluginElement element,
		ISchemaElement schemaElement,
		PluginErrorReporter reporter) {
		ISchemaAttribute[] attInfos = schemaElement.getAttributes();
		for (int i = 0; i < attInfos.length; i++) {
			ISchemaAttribute attInfo = attInfos[i];
			if (attInfo.getUse() == ISchemaAttribute.REQUIRED) {
				if (element.getAttribute(attInfo.getName()) == null) {
					reporter.report(
						PDE.getFormattedMessage(
							"Builders.Manifest.required",
							attInfo.getName()),
						getLine(element),
						CompilerFlags.getFlag(CompilerFlags.P_NO_REQUIRED_ATT));
				}
			}
		}
	}

	private void validateExtensionPoints(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {
	}

	private static int getLine(IPluginObject object) {
		int line = -1;
		if (object instanceof ISourceObject) {
			line = ((ISourceObject) object).getStartLine();
		}
		return line;
	}
}
