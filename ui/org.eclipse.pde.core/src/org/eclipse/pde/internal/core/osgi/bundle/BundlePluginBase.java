/*
 * Created on Oct 1, 2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class BundlePluginBase
	extends PlatformObject
	implements IBundlePluginBase {
	private IBundlePluginModelBase model;
	private ArrayList libraries;
	private ArrayList imports;

	public void reset() {
		libraries = null;
		imports = null;
	}
	
	public String getSchemaVersion() {
		return "3.0";
	}
	
	public void setSchemaVersion(String value) throws CoreException {
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == ModelChangedEvent.WORLD_CHANGED) {
			reset();
		} else if (event.getChangeType() == ModelChangedEvent.CHANGE) {
			String header = event.getChangedProperty();
			if (header.equals(IBundle.KEY_IMPORT_PACKAGE)
				|| header.equals(IBundle.KEY_REQUIRE_BUNDLE))
				imports = null;
			else if (header.equals(IBundle.KEY_CLASSPATH))
				libraries = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginBase#getBundle()
	 */
	public IBundle getBundle() {
		if (model != null) {
			IBundleModel bmodel = model.getBundleModel();
			return bmodel != null ? bmodel.getBundle() : null;
		}
		return null;
	}

	public ISharedPluginModel getModel() {
		return model;
	}

	void setModel(IBundlePluginModelBase model) {
		this.model = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.osgi.bundle.IBundlePluginBase#getExtensionsRoot()
	 */
	public IExtensions getExtensionsRoot() {
		if (model != null) {
			IExtensionsModel emodel = model.getExtensionsModel();
			return emodel != null ? emodel.getExtensions() : null;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void add(IPluginLibrary library) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle == null)
			return;

		if (libraries != null) {
			libraries.add(library);
		}
		String libName = library.getName();
		String cp = bundle.getHeader(IBundle.KEY_CLASSPATH);
		if (cp == null)
			cp = libName;
		else
			cp = cp + ", " + libName;
		bundle.setHeader(IBundle.KEY_CLASSPATH, cp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void remove(IPluginLibrary library) throws CoreException {
		throwException("Cannot remove library from BundlePlugin");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#add(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void add(IPluginImport pluginImport) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle == null)
			return;

		if (imports != null) {
			imports.add(pluginImport);
		}
		String rname = pluginImport.getId();
		String header = bundle.getHeader(IBundle.KEY_REQUIRE_BUNDLE);
		if (header == null)
			header = rname;
		else
			header = header + ", " + rname;
		bundle.setHeader(IBundle.KEY_REQUIRE_BUNDLE, header);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#remove(org.eclipse.pde.core.plugin.IPluginImport)
	 */
	public void remove(IPluginImport pluginImport) throws CoreException {
		throwException("Cannot remove import from BundlePlugin");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getLibraries()
	 */
	public IPluginLibrary[] getLibraries() {
		if (libraries == null) {
			libraries = new ArrayList();
			StringTokenizer stok =
				new StringTokenizer(getSafeHeader(IBundle.KEY_CLASSPATH), ",");
			while (stok.hasMoreTokens()) {
				String token = stok.nextToken().trim();
				try {
					IPluginLibrary library = model.createLibrary();
					library.setName(token);
					// TODO this is wrong -
					// must respect ExportPackage
					// or ProvidePackage
					library.setExported(true);
					libraries.add(library);
				} catch (CoreException e) {
					PDECore.logException(e);
				}
			}
		}
		return (IPluginLibrary[]) libraries.toArray(
			new IPluginLibrary[libraries.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getImports()
	 */
	public IPluginImport[] getImports() {
		if (imports == null) {
			imports = new ArrayList();
			Set uniqueIds = new HashSet();
			addImportsFromRequiredBundles(imports, uniqueIds);
			//if (imports.size() == 0)
				//addImportsFromImportedPackages(imports, uniqueIds);
		}
		return (IPluginImport[]) imports.toArray(
			new IPluginImport[imports.size()]);
	}

	private void addImportsFromRequiredBundles(ArrayList imports, Set uniqueIds) {
		StringTokenizer stok =
			new StringTokenizer(getSafeHeader(IBundle.KEY_REQUIRE_BUNDLE), ",");
		while (stok.hasMoreTokens()) {
			createImport(stok.nextToken(), imports, uniqueIds);
		}
	}
	
	private void createImport(String dependency, ArrayList imports,
			Set uniqueIds) {
		try {
			StringTokenizer tok = new StringTokenizer(dependency, ";");
			String id = tok.nextToken();
			if (!uniqueIds.contains(id)) {
				IPluginImport iimport = model.createImport();
				iimport.setId(id);
				while (tok.hasMoreTokens()) {
					String next = tok.nextToken().trim();
					int index = next.indexOf('=');
					if (index != -1 && index < next.length() - 1) {
						setImportAttributes(iimport, next.substring(0, index),
								next.substring(index + 1));
					}
				}
				uniqueIds.add(id);
				imports.add(iimport);
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		}
	}
	
	private void setImportAttributes(IPluginImport iimport, String key, String value) throws CoreException{
		if (value == null || value.length() == 0)
			return;
		
		if (key.equals("version")) {
			iimport.setVersion(value);
		} else if (key.equals("provide-packages")) {
			iimport.setReexported(value.equals("true"));
		} else if (key.equals("match")) {
			if (value.equalsIgnoreCase("perfect")) {
				iimport.setMatch(IMatchRules.PERFECT);
			} else if (value.equalsIgnoreCase("greaterOrEquals")) {
				iimport.setMatch(IMatchRules.GREATER_OR_EQUAL);
			} else if (value.equalsIgnoreCase("equivalent")) {
				iimport.setMatch(IMatchRules.EQUIVALENT);
			}
		} else if (key.equals("optional")) {
			iimport.setOptional(value.equals("true"));
		}
	}

	/*private void addImportsFromImportedPackages(ArrayList imports, Set uniqueIds) {
		StringTokenizer stok =
			new StringTokenizer(getSafeHeader(IBundle.KEY_IMPORT_PACKAGE), ",");

		while (stok.hasMoreTokens()) {
			String packageName = stok.nextToken().trim();
			try {
				String owningPluginId = findOwningPluginId(packageName);
				if (owningPluginId != null) {
					if (!uniqueIds.contains(owningPluginId)) {
						uniqueIds.add(owningPluginId);
						IPluginImport iimport = model.createImport();
						iimport.setId(owningPluginId);
						imports.add(iimport);
					}
				}
			} catch (CoreException e) {
				PDECore.logException(e);
			}
		}
	}*/

	/*
	 * Finds a plug-in that owns the package with a given name. Returns plug-in
	 * Id or null if not found.
	 */
	/*private String findOwningPluginId(final String packageName) {
		ISearchPattern pattern =
			SearchEngine.createSearchPattern(
				packageName,
				IJavaSearchConstants.PACKAGE,
				IJavaSearchConstants.DECLARATIONS,
				true);
		if (pattern == null)
			return null;
		PluginModelManager mmng = PDECore.getDefault().getModelManager();
		ModelEntry[] entries = mmng.getEntries();
		ArrayList projects = new ArrayList();
		for (int i = 0; i < entries.length; i++) {
			ModelEntry entry = entries[i];
			IPluginModelBase model = entry.getActiveModel();
			IResource resource = model.getUnderlyingResource();
			if (resource == null)
				continue;
			IJavaProject jproject = JavaCore.create(resource.getProject());
			if (jproject == null)
				continue;
			projects.add(jproject);
		}
		if (projects.size() == 0)
			return null;
		IJavaSearchScope scope =
			SearchEngine.createJavaSearchScope(
				(IJavaElement[]) projects.toArray(
					new IJavaProject[projects.size()]),
				false);
		final IProject[] result = new IProject[1];
		result[0] = null;
		IJavaSearchResultCollector collector =
			new IJavaSearchResultCollector() {
			public void aboutToStart() {
				//System.out.println("Looking for package: " + packageName);
			}

			public void accept(
				IResource resource,
				int start,
				int end,
				IJavaElement enclosingElement,
				int accuracy)
				throws CoreException {
				if (resource != null && result[0] == null)
					result[0] = resource.getProject();
			}

			public void done() {
			}

			public IProgressMonitor getProgressMonitor() {
				return null;
			}
		};
		SearchEngine searchEngine = new SearchEngine();
		try {
			searchEngine.search(
				PDECore.getWorkspace(),
				pattern,
				scope,
				collector);
			if (result[0] != null) {
				ModelEntry entry = mmng.findEntry(result[0]);
				if (entry != null)
					return entry.getId();
			}
		} catch (JavaModelException e) {
			PDECore.logException(e);
		}
		return null;
	}*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getProviderName()
	 */
	public String getProviderName() {
		IBundle bundle = getBundle();
		if (bundle == null)
			return null;
		return bundle.getHeader(IBundle.KEY_VENDOR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#getVersion()
	 */
	public String getVersion() {
		IBundle bundle = getBundle();
		if (bundle == null)
			return null;
		return bundle.getHeader(IBundle.KEY_VERSION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setProviderName(java.lang.String)
	 */
	public void setProviderName(String providerName) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			bundle.setHeader(IBundle.KEY_VENDOR, providerName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			bundle.setHeader(IBundle.KEY_VERSION, version);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginBase#swap(org.eclipse.pde.core.plugin.IPluginLibrary,
	 *      org.eclipse.pde.core.plugin.IPluginLibrary)
	 */
	public void swap(IPluginLibrary l1, IPluginLibrary l2)
		throws CoreException {
		throwException("Cannot swap libraries in BundlePlugin");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void add(IPluginExtension extension) throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return;
		extensions.add(extension);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#add(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void add(IPluginExtensionPoint point) throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return;
		extensions.add(point);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensionPoints()
	 */
	public IPluginExtensionPoint[] getExtensionPoints() {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return new IPluginExtensionPoint[0];
		return extensions.getExtensionPoints();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#getExtensions()
	 */
	public IPluginExtension[] getExtensions() {
		IExtensions extensions = getExtensionsRoot();
		if (extensions == null)
			return new IPluginExtension[0];
		return extensions.getExtensions();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void remove(IPluginExtension extension) throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null)
			extensions.remove(extension);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#remove(org.eclipse.pde.core.plugin.IPluginExtensionPoint)
	 */
	public void remove(IPluginExtensionPoint extensionPoint)
		throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null)
			extensions.remove(extensionPoint);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IExtensions#swap(org.eclipse.pde.core.plugin.IPluginExtension,
	 *      org.eclipse.pde.core.plugin.IPluginExtension)
	 */
	public void swap(IPluginExtension e1, IPluginExtension e2)
		throws CoreException {
		IExtensions extensions = getExtensionsRoot();
		if (extensions != null)
			extensions.swap(e1, e2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		IBundle bundle = getBundle();
		if (bundle == null)
			return null;
		String id = bundle.getHeader(IBundle.KEY_SYMBOLIC_NAME);
		if (id == null)
			id = bundle.getHeader(IBundle.KEY_NAME);
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			bundle.setHeader(IBundle.KEY_NAME, id);
			bundle.setHeader(IBundle.KEY_SYMBOLIC_NAME, id);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginModel()
	 */
	public IPluginModelBase getPluginModel() {
		return model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		IBundle bundle = getBundle();
		if (bundle == null)
			return null;
		return bundle.getHeader(IBundle.KEY_DESC);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		return model != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getTranslatedName()
	 */
	public String getTranslatedName() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getParent()
	 */
	public IPluginObject getParent() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginBase()
	 */
	public IPluginBase getPluginBase() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getResourceString(java.lang.String)
	 */
	public String getResourceString(String key) {
		return key;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null)
			bundle.setHeader(IBundle.KEY_DESC, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isValid()
	 */
	public boolean isValid() {
		IBundle bundle = getBundle();
		IExtensions extensions = getExtensionsRoot();
		return bundle != null
			&& bundle.isValid()
			&& (extensions == null || extensions.isValid());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}

	private void throwException(String message) throws CoreException {
		IStatus status =
			new Status(
				IStatus.ERROR,
				PDECore.PLUGIN_ID,
				IStatus.OK,
				message,
				null);
		throw new CoreException(status);
	}

	private String getSafeHeader(String key) {
		String value = getBundle().getHeader(key);
		return value != null ? value : "";
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
	}
}
