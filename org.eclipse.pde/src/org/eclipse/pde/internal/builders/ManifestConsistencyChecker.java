package org.eclipse.pde.internal.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.w3c.dom.Node;

public class ManifestConsistencyChecker extends IncrementalProjectBuilder {
	public static final String BUILDERS_VERIFYING = "Builders.verifying";
	public static final String BUILDERS_FRAGMENT_BROKEN_LINK =
		"Builders.Fragment.brokenLink";
	public static final String BUILDERS_UPDATING = "Builders.updating";
	public static final String BUILDERS_VERSION_FORMAT =
		"Builders.versionFormat";

	private boolean javaDelta = false;

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
				return (PDE.hasPluginNature(project));
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
				} else if (isJavaFile(candidate)) {
					javaDelta = true;
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
		
		IResourceDelta delta = null;
		if (kind != FULL_BUILD)
			delta = getDelta(getProject());

		if (delta == null || kind == FULL_BUILD) {
			// Full build
			if (!PDE.hasPluginNature(project)) return null;
			
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
		} else {
			processDelta(delta, monitor);
		}
		return null;
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

		ManifestParser parser = new ManifestParser(reporter);
		parser.parse(file);
		if (reporter.getErrorCount() == 0) {
			if (isFragment(file)) {
				validateFragment(file, reporter);
			} else {
				validatePlugin(file, reporter);
			}
		}
		monitor.subTask(PDE.getResourceString(BUILDERS_UPDATING));
		monitor.done();
	}
	private boolean isFragment(IFile file) {
		String name = file.getName().toLowerCase();
		return name.equals("fragment.xml");
	}
	private boolean isManifestFile(IFile file) {
		if (file.getParent() instanceof IFolder) return false;
		String name = file.getName().toLowerCase();
		return name.equals("plugin.xml") || name.equals("fragment.xml");
	}

	private boolean isJavaFile(IFile file) {
		String name = file.getName().toLowerCase();
		return name.endsWith(".java");
	}

	private void reportValidationError(
		Node errorNode,
		PluginErrorReporter reporter) {
		/*
		int type = errorNode.getNodeType();
		*/
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
			validateVersion(plugin, reporter);
			//validateValues(plugin, reporter);
		}
		model.release();
	}

	private void validateFragment(IFile file, PluginErrorReporter reporter) {
		WorkspaceFragmentModel model = new WorkspaceFragmentModel(file);
		model.load();
		if (model.isLoaded()) {
			// Test the version
			// Test if plugin exists
			IFragment fragment = model.getFragment();
			validateVersion(fragment, reporter);
			String pluginId = fragment.getPluginId();
			String pluginVersion = fragment.getPluginVersion();
			int match = fragment.getRule();
			IPlugin plugin =
				PDECore.getDefault().findPlugin(pluginId, pluginVersion, match);
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
			//validateValues(fragment, reporter);
		}
		model.release();
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
		// Validate requires
		validateRequires(pluginBase, reporter);
		// Validate extensions
		validateExtensions(pluginBase, reporter);
		// Validate extension points
		validateExtensionPoints(pluginBase, reporter);
	}

	private void validateRequires(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {
		// Try to find the plug-ins
		IPluginImport[] imports = pluginBase.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			if (PDECore
				.getDefault()
				.findPlugin(
					iimport.getId(),
					iimport.getVersion(),
					iimport.getMatch())
				== null) {
				reporter.reportError(
					"Cannot resolve plug-in dependency: " + iimport.getId(),
					getLine(iimport));
			}
		}
	}

	private void validateExtensions(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {
		IPluginExtension[] extensions = pluginBase.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IPluginExtension extension = extensions[i];
			IPluginExtensionPoint point =
				PDECore.getDefault().findExtensionPoint(extension.getPoint());
			if (point == null) {
				reporter.reportError(
					"Uknown extension point: " + extension.getPoint(),
					getLine(extension));
			} else {
				ISchema schema =
					PDECore.getDefault().getSchemaRegistry().getSchema(
						extension.getPoint());
				if (schema != null)
					validateExtensionContent(extension, schema, reporter);
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

	private void validateElement(
		IPluginElement element,
		ISchema schema,
		PluginErrorReporter reporter) {
		ISchemaElement schemaElement = schema.findElement(element.getName());
		if (schemaElement == null) {
			// Invalid
			reporter.reportError(
				"Element '"
					+ element.getName()
					+ "' is not legal in the enclosing extension point.",
				getLine(element));
		} else {
			IPluginAttribute[] atts = element.getAttributes();
			validateExistingAttributes(atts, schemaElement, reporter);
			validateRequiredAttributes(element, schemaElement, reporter);
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
				reporter.reportWarning(
					"Unknown attribute '" + att.getName() + "'.");
			} else
				validateAttribute(att, attInfo, reporter);
		}
	}
	private void validateAttribute(
		IPluginAttribute att,
		ISchemaAttribute attInfo,
		PluginErrorReporter reporter) {
		String name = att.getName();
		String value = att.getValue();
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
		reporter.reportError(
			"Illegal value '"
				+ value
				+ "' for attribute '"
				+ att.getName()
				+ "'.",
			getLine(att.getParent()));
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
				reporter.reportError(
					"Class '" + value + "' cannot be found.",
					getLine(att.getParent()));
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
			reporter.reportError(
				"Referenced resource '"
					+ path
					+ "' in attribute '"
					+ att.getName()
					+ "' not found.",
				getLine(att.getParent()));
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
					reporter.reportError(
						"Required attribute '"
							+ attInfo.getName()
							+ "' not defined.",
						getLine(element));
				}
			}
		}
	}

	private void validateExtensionPoints(
		IPluginBase pluginBase,
		PluginErrorReporter reporter) {
	}

	private int getLine(IPluginObject object) {
		int line = -1;
		if (object instanceof ISourceObject) {
			line = ((ISourceObject) object).getStartLine();
		}
		return line;
	}
}