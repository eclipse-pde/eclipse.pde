/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.osgi.framework.*;

public class ManifestConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying"; //$NON-NLS-1$
	public static final String BUILDERS_FRAGMENT_BROKEN_LINK = "Builders.Fragment.brokenLink"; //$NON-NLS-1$
	public static final String BUILDERS_UPDATING = "Builders.updating"; //$NON-NLS-1$
	public static final String BUILDERS_VERSION_FORMAT = "Builders.versionFormat"; //$NON-NLS-1$

	private boolean javaDelta = false;
	private boolean fileCompiled = false;
	private boolean ignoreJavaChanges = false;
	
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
					// That's it, but only check it if it has been added or
					// changed
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
		if (PDECore.getDefault().getBundle().getState() != Bundle.ACTIVE || monitor.isCanceled())
			return new IProject[0];

		IProject project = getProject();
		fileCompiled = false;
		javaDelta = false;
		ignoreJavaChanges = CompilerFlags
				.getFlag(CompilerFlags.P_UNKNOWN_CLASS) == CompilerFlags.IGNORE;

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
		IPluginModelBase thisModel = PDECore.getDefault().getModelManager().findModel(project);
		if (thisModel != null)
			interestingProjects = computeInterestingProjects(thisModel);
		// If not compiled already, see if there are interesting
		// changes in referenced projects that may cause us
		// to compile
		if (!fileCompiled && kind != FULL_BUILD && interestingProjects != null) {
			checkInterestingProjectDeltas(project, interestingProjects, monitor);
		}
		return interestingProjects;
	}

	private void checkThisProject(IProject project, IProgressMonitor monitor) {
		if (!PDE.hasPluginNature(project))
			return;

		IPath path = project.getFullPath().append("plugin.xml"); //$NON-NLS-1$
		IWorkspace workspace = project.getWorkspace();
		IFile file = workspace.getRoot().getFile(path);
		if (file.exists()) {
			checkFile(file, monitor);
		} else {
			path = project.getFullPath().append("fragment.xml"); //$NON-NLS-1$
			file = workspace.getRoot().getFile(path);
			if (file.exists()) {
				checkFile(file, monitor);
			}
		}
	}

	private void checkInterestingProjectDeltas(IProject project,
			IProject[] interestingProjects, IProgressMonitor monitor)
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
			IFile file = project.getFile("plugin.xml"); //$NON-NLS-1$
			if (!file.exists())
				file = project.getFile("fragment.xml"); //$NON-NLS-1$
			if (file.exists())
				checkFile(file, monitor);
		}
	}

	private void checkFile(IFile file, IProgressMonitor monitor) {
		PluginErrorReporter reporter = new PluginErrorReporter(file);
		if (WorkspaceModelManager.isBinaryPluginProject(file.getProject()) || monitor.isCanceled())
			return;
		String message = PDE.getFormattedMessage(BUILDERS_VERIFYING, file
				.getFullPath().toString());
		monitor.subTask(message);

		ValidatingSAXParser.parse(file, reporter);

		IFile bundleManifest = file.getProject()
				.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		boolean bundle = bundleManifest.exists();

		if (reporter.getErrorCount() == 0) {
			if (isFragment(file)) {
				validateFragment(file, reporter, bundle, monitor);
			} else {
				validatePlugin(file, reporter, bundle, monitor);
			}
		}
		monitor.subTask(PDE.getResourceString(BUILDERS_UPDATING));
		monitor.done();
		fileCompiled = true;
	}

	private boolean isFragment(IFile file) {
		String name = file.getName().toLowerCase();
		return name.equals("fragment.xml"); //$NON-NLS-1$
	}
	private boolean isManifestFile(IFile file) {
		if (file.getParent() instanceof IFolder)
			return false;
		String name = file.getName().toLowerCase();
		return name.equals("plugin.xml") || name.equals("fragment.xml"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private boolean isJavaFile(IFile file) {
		String name = file.getName().toLowerCase();
		return name.endsWith(".java"); //$NON-NLS-1$
	}

	protected void startupOnInitialize() {
		super.startupOnInitialize();
	}

	private void validatePlugin(IFile file, PluginErrorReporter reporter,
			boolean bundle, IProgressMonitor monitor) {
		WorkspacePluginModel model = new WorkspacePluginModel(file);
		model.load();

		if (model.isLoaded()) {
			// Test the version
			IPlugin plugin = model.getPlugin();
			if (!bundle)
				validateRequiredAttributes(plugin, reporter);
			if (reporter.getErrorCount() == 0) {
				if (!bundle)
					validateVersion(plugin, reporter);
				validateValues(plugin, reporter, bundle, monitor);
			}
		}
		model.dispose();
	}

	private void validateFragment(IFile file, PluginErrorReporter reporter, boolean bundle, IProgressMonitor monitor) {
		WorkspaceFragmentModel model = new WorkspaceFragmentModel(file);
		model.load();

		if (model.isLoaded()) {
			// Test the version
			// Test if plugin exists
			IFragment fragment = model.getFragment();
			if (!bundle) validateRequiredAttributes(fragment, reporter);
			if (reporter.getErrorCount() == 0) {
				if (!bundle) {
					validateVersion(fragment, reporter);
					String pluginId = fragment.getPluginId();
					String pluginVersion = fragment.getPluginVersion();
					int match = fragment.getRule();
					IPlugin plugin = PDECore.getDefault().findPlugin(pluginId,
						pluginVersion, match);
					if (plugin == null) {
						// broken fragment link
						String[] args = {pluginId, pluginVersion};
						String message = PDE.getFormattedMessage(
							BUILDERS_FRAGMENT_BROKEN_LINK, args);
						int line = 1;
						if (fragment instanceof ISourceObject)
							line = ((ISourceObject) fragment).getStartLine();
						reporter.reportError(message, line);
					}
				}
				validateValues(fragment, reporter, bundle, monitor);
			}
		}
		model.dispose();
	}

	private IProject[] computeInterestingProjects(IPluginModelBase model) {
		IPluginBase plugin = model.getPluginBase();
		if (plugin == null)
			return null;
		PluginModelManager modelManager = PDECore.getDefault()
				.getModelManager();
		ArrayList projects = new ArrayList();
		// Add all projects for imported plug-ins that
		// are in the workspace
		IPluginImport[] iimports = plugin.getImports();
		for (int i = 0; i < iimports.length; i++) {
			IPluginImport iimport = iimports[i];
			if (iimport.getId() == null)
				continue;
			IPluginModelBase importModel = modelManager.findPlugin(iimport
					.getId(), iimport.getVersion(), iimport.getMatch());
			addInterestingProject(iimport.getId(), importModel, projects);
		}
		// If fragment, also add the referenced plug-in
		// if in the workspace
		if (model.isFragmentModel()) {
			IFragment fragment = (IFragment) plugin;
			if (fragment.getPluginId() != null
					&& fragment.getPluginVersion() != null) {
				IPluginModelBase refPlugin = modelManager.findPlugin(fragment
						.getPluginId(), fragment.getPluginVersion(), fragment
						.getRule());
				addInterestingProject(fragment.getPluginId(), refPlugin,
						projects);
			}
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

	private void addInterestingProject(String id, IPluginModelBase model,
			ArrayList list) {
		if (model != null && model.isEnabled()) {
			if (model.getUnderlyingResource() != null)
				list.add(model.getUnderlyingResource().getProject());
		} else {
			IProject missingProject = PDECore.getWorkspace().getRoot()
					.getProject(id);
			list.add(missingProject);
		}
	}

	private void validateVersion(IPluginBase pluginBase,
			PluginErrorReporter reporter) {
		String version = pluginBase.getVersion();
		if (version == null)
			version = ""; //$NON-NLS-1$
		try {
			PluginVersionIdentifier pvi = new PluginVersionIdentifier(version);
			pvi.toString();
		} catch (Throwable e) {
			String message = PDE.getFormattedMessage(BUILDERS_VERSION_FORMAT,
					version);
			int line = 1;
			if (pluginBase instanceof ISourceObject)
				line = ((ISourceObject) pluginBase).getStartLine();
			reporter.reportError(message, line);
		}
	}

	private void validateValues(IPluginBase pluginBase,
			PluginErrorReporter reporter, boolean bundle, IProgressMonitor monitor) {

		if (!CompilerFlags.isGroupActive(CompilerFlags.PLUGIN_FLAGS))
			return;

		if (!bundle)
			// Validate requires
			validateRequires(pluginBase, reporter, CompilerFlags
					.getFlag(CompilerFlags.P_UNRESOLVED_IMPORTS));
		// Validate extensions
		validateExtensions(pluginBase, reporter, monitor);
	}

	private void validateRequiredAttributes(IPluginBase pluginBase,
			PluginErrorReporter reporter) {
		// validate name, id, version
		String rootName = "plugin"; //$NON-NLS-1$
		if (pluginBase instanceof IFragment) {
			IFragment fragment = (IFragment) pluginBase;
			rootName = "fragment"; //$NON-NLS-1$
			assertNotNull("plugin-id", rootName, getLine(fragment), fragment //$NON-NLS-1$
					.getPluginId(), reporter);
			assertNotNull("plugin-version", rootName, getLine(fragment), //$NON-NLS-1$
					fragment.getPluginVersion(), reporter);
		}

		assertNotNull("name", rootName, getLine(pluginBase), pluginBase //$NON-NLS-1$
				.getName(), reporter);
		assertNotNull("id", rootName, getLine(pluginBase), pluginBase.getId(), //$NON-NLS-1$
				reporter);
		assertNotNull("version", rootName, getLine(pluginBase), pluginBase //$NON-NLS-1$
				.getVersion(), reporter);

		// validate libraries
		IPluginLibrary[] libraries = pluginBase.getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			IPluginLibrary library = libraries[i];
			assertNotNull("name", "library", getLine(library), library //$NON-NLS-1$ //$NON-NLS-2$
					.getName(), reporter);
		}
		// validate imports
		IPluginImport[] iimports = pluginBase.getImports();
		for (int i = 0; i < iimports.length; i++) {
			IPluginImport iimport = iimports[i];
			assertNotNull("plugin", "import", getLine(iimport), //$NON-NLS-1$ //$NON-NLS-2$
					iimport.getId(), reporter);
		}
		// validate extensions
		IPluginExtension[] extensions = pluginBase.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension extension = extensions[i];
			assertNotNull("point", "extension", getLine(extension), extension //$NON-NLS-1$ //$NON-NLS-2$
					.getPoint(), reporter);
		}
		// validate extension points
		IPluginExtensionPoint[] expoints = pluginBase.getExtensionPoints();
		for (int i = 0; i < expoints.length; i++) {
			IPluginExtensionPoint expoint = expoints[i];
			assertNotNull("id", "extension-point", getLine(expoint), expoint //$NON-NLS-1$ //$NON-NLS-2$
					.getId(), reporter);
			assertNotNull("name", "extension-point", getLine(expoint), expoint //$NON-NLS-1$ //$NON-NLS-2$
					.getName(), reporter);
		}
	}

	private static void assertNotNull(String att, String el, int line,
			String value, PluginErrorReporter reporter) {
		if (value == null) {
			String message = PDE.getFormattedMessage(
					"Builders.manifest.missingRequired", new String[]{att, el}); //$NON-NLS-1$
			reporter.reportError(message, line);
		}
	}

	private void validateRequires(IPluginBase pluginBase,
			PluginErrorReporter reporter, int flag) {
		// Try to find the plug-ins
		if (flag == CompilerFlags.IGNORE)
			return;
		IPluginImport[] imports = pluginBase.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			if (!imports[i].isOptional()
					&& PDECore.getDefault().findPlugin(iimport.getId(),
							iimport.getVersion(), iimport.getMatch()) == null) {
				reporter.report(PDE.getFormattedMessage(
						"Builders.Manifest.dependency", iimport.getId()), //$NON-NLS-1$
						getLine(iimport), flag);
			}
		}
	}

	private void validateExtensions(IPluginBase pluginBase,
			PluginErrorReporter reporter, IProgressMonitor monitor) {
		
		IPluginExtension[] extensions = pluginBase.getExtensions();
		SchemaMarkerFactory factory = new SchemaMarkerFactory();

		for (int i = 0; i < extensions.length; i++) {
			if (monitor.isCanceled())
				break;
			IPluginExtension extension = extensions[i];
			IPluginExtensionPoint point = PDECore.getDefault()
					.findExtensionPoint(extension.getPoint());
			if (point == null) {
				int flag = CompilerFlags.getFlag(CompilerFlags.P_UNRESOLVED_EX_POINTS);
				if (flag != CompilerFlags.IGNORE) {
					reporter.report(PDE.getFormattedMessage(
						"Builders.Manifest.ex-point", extension.getPoint()), //$NON-NLS-1$
						getLine(extension), flag);
				}
			} else {
				ISchema schema = PDECore.getDefault().getSchemaRegistry()
						.getSchema(extension.getPoint());
				if (schema != null) {
					factory.setPoint(extension.getPoint());
					reporter.setMarkerFactory(factory);
					validateExtensionContent(extension, schema, reporter, monitor);
					reporter.setMarkerFactory(null);
				}
			}
		}
	}

	private void validateExtensionContent(IPluginExtension extension,
			ISchema schema, PluginErrorReporter reporter, IProgressMonitor monitor) {

		IPluginObject[] elements = extension.getChildren();
		for (int i = 0; i < elements.length; i++) {
			if (monitor.isCanceled())
				break;
			IPluginElement element = (IPluginElement) elements[i];
			validateElement(element, schema, reporter);
		}
	}

	private void validateContentModel(IPluginParent parent,
			ISchemaElement elementInfo, PluginErrorReporter reporter) {
		
		if (CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT) == CompilerFlags.IGNORE)
			return;
		
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
				reporter.report(PDE.getFormattedMessage(
						"Builders.Manifest.child", new String[]{ //$NON-NLS-1$
								child.getName(), parent.getName()}),
						getLine(child), CompilerFlags
								.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT));
			}
		}
	}

	private void computeAllowedElements(ISchemaType type, HashSet elementSet) {
		if (type instanceof ISchemaComplexType) {
			ISchemaComplexType complexType = (ISchemaComplexType) type;
			ISchemaCompositor compositor = complexType.getCompositor();
			if (compositor != null)
				computeAllowedElements(compositor, elementSet);

			ISchemaAttribute[] attrs = complexType.getAttributes();
			for (int i = 0; i < attrs.length; i++) {
				if (attrs[i].getKind() == ISchemaAttribute.JAVA)
					elementSet.add(attrs[i].getName());
			}
		}
	}

	private void computeAllowedElements(ISchemaCompositor compositor,
			HashSet elementSet) {
		ISchemaObject[] children = compositor.getChildren();
		for (int i = 0; i < children.length; i++) {
			ISchemaObject child = children[i];
			if (child instanceof ISchemaObjectReference) {
				ISchemaObjectReference ref = (ISchemaObjectReference) child;
				ISchemaElement refElement = (ISchemaElement) ref
						.getReferencedObject();
				if (refElement != null)
					elementSet.add(refElement.getName());
			} else if (child instanceof ISchemaCompositor) {
				computeAllowedElements((ISchemaCompositor) child, elementSet);
			}
		}
	}

	private void validateElement(IPluginElement element, ISchema schema,
			PluginErrorReporter reporter) {
		ISchemaElement schemaElement = schema.findElement(element.getName());
		boolean valid = schemaElement != null;
		boolean executableElement = false;
		ISchemaElement parentSchema = schema.findElement(element.getParent()
				.getName());
		if (schemaElement == null) {
			if (parentSchema != null) {
				ISchemaAttribute attr = parentSchema.getAttribute(element
						.getName());
				if (attr != null && attr.getKind() == ISchemaAttribute.JAVA) {
					valid = true;
					executableElement = true;
				}
			}
		}
		if (!valid) {
			int flag = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT);
			if (flag != CompilerFlags.IGNORE) {
				reporter.report(PDE.getFormattedMessage(
					"Builders.Manifest.element", element.getName()), //$NON-NLS-1$
					getLine(element), flag);
			}
		} else {
			if (executableElement) {
				validateJava(element.getAttribute("class"), parentSchema //$NON-NLS-1$
						.getAttribute(element.getName()), reporter);
				return;
			} else {
				IPluginAttribute[] atts = element.getAttributes();
				validateExistingAttributes(atts, schemaElement, reporter);
				validateRequiredAttributes(element, schemaElement, reporter);
			}
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

	private void validateExistingAttributes(IPluginAttribute[] atts,
			ISchemaElement schemaElement, PluginErrorReporter reporter) {
		
		for (int i = 0; i < atts.length; i++) {
			IPluginAttribute att = atts[i];
			ISchemaAttribute attInfo = schemaElement
					.getAttribute(att.getName());
			if (attInfo == null) {
				int flag = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ATTRIBUTE);
				if (flag != CompilerFlags.IGNORE){
					reporter.report(PDE.getFormattedMessage(
							"Builders.Manifest.attribute", att.getName()), //$NON-NLS-1$
							getLine(att.getParent()), flag);
				}
			} else
				validateAttribute(att, attInfo, reporter);
		}
	}
	private void validateAttribute(IPluginAttribute att,
			ISchemaAttribute attInfo, PluginErrorReporter reporter) {
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
		} else if (type.getName().equals("boolean")) { //$NON-NLS-1$
			validateBoolean(att, reporter);
		}
	}

	private void validateRestriction(IPluginAttribute att,
			ISchemaRestriction restriction, PluginErrorReporter reporter) {
		if (CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE) == CompilerFlags.IGNORE)
			return;
		
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
		reporter.report(PDE.getFormattedMessage("Builders.Manifest.att-value", //$NON-NLS-1$
				new String[]{value, att.getName()}), getLine(att.getParent()),
				CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE));
	}

	private void validateBoolean(IPluginAttribute att,
			PluginErrorReporter reporter) {
		if (CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE) == CompilerFlags.IGNORE)
			return;
		
		String value = att.getValue();
		if (value.equalsIgnoreCase("true")) //$NON-NLS-1$
			return;
		if (value.equalsIgnoreCase("false")) //$NON-NLS-1$
			return;
		reporter.report(PDE.getFormattedMessage("Builders.Manifest.att-value", //$NON-NLS-1$
				new String[]{value, att.getName()}), getLine(att.getParent()),
				CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE));
	}

	private void validateJava(IPluginAttribute att, ISchemaAttribute attInfo,
			PluginErrorReporter reporter) {
		
		if (CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_CLASS) == CompilerFlags.IGNORE)
			return;
		
		String qName = att.getValue();
		IProject project = att.getModel().getUnderlyingResource().getProject();
		IJavaProject javaProject = JavaCore.create(project);
		try {
			// be careful: people have the option to use the format:
			// fullqualifiedName:staticMethod
			int index = qName.indexOf(":"); //$NON-NLS-1$
			if (index != -1)
				qName = qName.substring(0, index);
			
			if (javaProject.findType(qName) == null) {
				reporter.report(PDE.getFormattedMessage(
						"Builders.Manifest.class", new String[]{qName, //$NON-NLS-1$
								att.getName()}), getLine(att.getParent()),
						CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_CLASS));
			}
		} catch (JavaModelException e) {
		}
	}
	
	private void validateResource(IPluginAttribute att,
			ISchemaAttribute attInfo, PluginErrorReporter reporter) {
		
		if (CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_RESOURCE) == CompilerFlags.IGNORE)
			return;
		
		String path = att.getValue();
		IProject project = att.getModel().getUnderlyingResource().getProject();
		IResource resource = project.findMember(new Path(path));
		if (resource == null) {
			reporter.report(PDE.getFormattedMessage(
					"Builders.Manifest.resource", new String[]{path, //$NON-NLS-1$
							att.getName()}), getLine(att.getParent()),
					CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_RESOURCE));
		}
	}

	private void validateRequiredAttributes(IPluginElement element,
			ISchemaElement schemaElement, PluginErrorReporter reporter) {
		if (CompilerFlags.getFlag(CompilerFlags.P_NO_REQUIRED_ATT) == CompilerFlags.IGNORE)
			return;
		
		ISchemaAttribute[] attInfos = schemaElement.getAttributes();
		for (int i = 0; i < attInfos.length; i++) {
			ISchemaAttribute attInfo = attInfos[i];
			if (attInfo.getUse() == ISchemaAttribute.REQUIRED) {
				boolean valid = (element.getAttribute(attInfo.getName()) != null);
				if (!valid && attInfo.getKind() == ISchemaAttribute.JAVA) {
					IPluginObject[] children = element.getChildren();
					for (int j = 0; j < children.length; j++) {
						if (attInfo.getName().equals(children[j].getName())) {
							valid = true;
							break;
						}
					}
				}
				if (!valid) {
					reporter.report(PDE.getFormattedMessage(
							"Builders.Manifest.required", attInfo.getName()), //$NON-NLS-1$
							getLine(element), CompilerFlags
									.getFlag(CompilerFlags.P_NO_REQUIRED_ATT));
				}
			}
		}
	}

	private static int getLine(IPluginObject object) {
		int line = -1;
		if (object instanceof ISourceObject) {
			line = ((ISourceObject) object).getStartLine();
		}
		return line;
	}
}
