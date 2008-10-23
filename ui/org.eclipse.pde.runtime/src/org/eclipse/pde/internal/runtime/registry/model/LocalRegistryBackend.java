/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.runtime.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class LocalRegistryBackend implements IRegistryEventListener, BundleListener, ServiceListener, RegistryBackend {

	private BackendChangeListener listener;
	private RegistryModel model;

	public void setRegistryModel(RegistryModel model) {
		this.model = model;
	}

	public void setRegistryListener(BackendChangeListener listener) {
		this.listener = listener;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.runtime.registry.model.local.RegistryBackend#connect()
	 */
	public void connect() {
		PDERuntimePlugin.getDefault().getBundleContext().addBundleListener(this);
		Platform.getExtensionRegistry().addListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().addServiceListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.runtime.registry.model.local.RegistryBackend#disconnect()
	 */
	public void disconnect() {
		Platform.getExtensionRegistry().removeListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().removeBundleListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().removeServiceListener(this);
	}

	protected static boolean isRegisteredService(org.osgi.framework.Bundle bundle, ServiceReference ref) {
		return bundle.equals(ref.getBundle());
	}

	protected static boolean isServiceInUse(org.osgi.framework.Bundle bundle, ServiceReference ref) {
		org.osgi.framework.Bundle[] usingBundles = ref.getUsingBundles();
		return (usingBundles != null && Arrays.asList(usingBundles).contains(bundle));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.runtime.registry.model.local.RegistryBackend#start(org.osgi.framework.Bundle)
	 */
	public void start(Bundle bundle) throws BundleException {
		PDERuntimePlugin.getDefault().getBundleContext().getBundle(bundle.getId().longValue()).start();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.runtime.registry.model.local.RegistryBackend#stop(org.osgi.framework.Bundle)
	 */
	public void stop(Bundle bundle) throws BundleException {
		PDERuntimePlugin.getDefault().getBundleContext().getBundle(bundle.getId().longValue()).stop();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.runtime.registry.model.local.RegistryBackend#diagnose(org.osgi.framework.Bundle)
	 */
	public MultiStatus diagnose(Bundle bundle) {
		PlatformAdmin plaformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
		State state = plaformAdmin.getState(false);

		BundleDescription desc = state.getBundle(bundle.getId().longValue());

		PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
		VersionConstraint[] unsatisfied = platformAdmin.getStateHelper().getUnsatisfiedConstraints(desc);
		ResolverError[] resolverErrors = platformAdmin.getState(false).getResolverErrors(desc);

		MultiStatus problems = new MultiStatus(PDERuntimePlugin.ID, IStatus.INFO, PDERuntimeMessages.RegistryView_found_problems, null);
		for (int i = 0; i < resolverErrors.length; i++) {
			if ((resolverErrors[i].getType() & (ResolverError.MISSING_FRAGMENT_HOST | ResolverError.MISSING_GENERIC_CAPABILITY | ResolverError.MISSING_IMPORT_PACKAGE | ResolverError.MISSING_REQUIRE_BUNDLE)) != 0)
				continue;
			IStatus status = new Status(IStatus.WARNING, PDERuntimePlugin.ID, resolverErrors[i].toString());
			problems.add(status);
		}

		for (int i = 0; i < unsatisfied.length; i++) {
			IStatus status = new Status(IStatus.WARNING, PDERuntimePlugin.ID, MessageHelper.getResolutionFailureMessage(unsatisfied[i]));
			problems.add(status);
		}

		return problems;
	}

	public Map initializeBundles() {
		org.osgi.framework.Bundle[] newBundles = PDERuntimePlugin.getDefault().getBundleContext().getBundles();
		Map tmp = new HashMap(newBundles.length);
		for (int i = 0; i < newBundles.length; i++) {
			if (newBundles[i].getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				Bundle ba = createBundleAdapter(newBundles[i]);
				tmp.put(ba.getId(), ba);
			}
		}
		return tmp;
	}

	public Map initializeExtensionPoints() {
		IExtensionPoint[] extPoints = Platform.getExtensionRegistry().getExtensionPoints();
		Map tmp = new HashMap(extPoints.length);
		for (int i = 0; i < extPoints.length; i++) {
			ExtensionPoint epa = createExtensionPointAdapter(extPoints[i]);
			tmp.put(epa.getUniqueIdentifier(), epa);
		}
		return tmp;
	}

	public Map initializeServices() {
		Map result = new HashMap();

		ServiceReference[] references = null;
		try {
			references = PDERuntimePlugin.getDefault().getBundleContext().getAllServiceReferences(null, null);
		} catch (InvalidSyntaxException e) { // nothing
		}

		if (references == null) {
			return null;
		}

		for (int i = 0; i < references.length; i++) {
			ServiceRegistration service = createServiceReferenceAdapter(references[i]);
			result.put(service.getId(), service);
		}

		return result;
	}

	private Bundle createBundleAdapter(org.osgi.framework.Bundle bundle) {
		String symbolicName = bundle.getSymbolicName();
		String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
		int state = bundle.getState();
		Long id = new Long(bundle.getBundleId());
		String location = createLocation(bundle);
		BundlePrerequisite[] imports = (BundlePrerequisite[]) getManifestHeaderArray(bundle, Constants.REQUIRE_BUNDLE);
		BundleLibrary[] libraries = (BundleLibrary[]) getManifestHeaderArray(bundle, Constants.BUNDLE_CLASSPATH);
		boolean isEnabled = getIsEnabled(bundle);

		return new Bundle(model, symbolicName, version, state, id, location, imports, libraries, isEnabled);
	}

	private Extension createExtensionAdapter(IExtension extension) {
		String namespaceIdentifier = extension.getNamespaceIdentifier();
		String label = extension.getLabel();
		String extensionPointUniqueIdentifier = extension.getExtensionPointUniqueIdentifier();
		Long contributor = getBundleId(extension.getContributor().getName());

		IConfigurationElement[] elements = extension.getConfigurationElements();
		ConfigurationElement[] configurationElements = new ConfigurationElement[elements.length];
		for (int i = 0; i < elements.length; i++) {
			configurationElements[i] = createConfigurationElement(elements[i]);
		}
		return new Extension(model, namespaceIdentifier, label, extensionPointUniqueIdentifier, configurationElements, contributor);
	}

	private ConfigurationElement createConfigurationElement(IConfigurationElement config) {
		Attribute[] attributes = createConfigurationElementAttributes(config);
		String name = createName(config);
		return new ConfigurationElement(model, name, attributes);
	}

	private static Long getBundleId(String name) {
		BundleDescription descr = PDERuntimePlugin.getDefault().getPlatformAdmin().getState().getBundle(name, null);
		return new Long(descr.getBundleId());
	}

	private ExtensionPoint createExtensionPointAdapter(IExtensionPoint extensionPoint) {
		String label = extensionPoint.getLabel();
		String uniqueIdentifier = extensionPoint.getUniqueIdentifier();
		String namespaceIdentifier = extensionPoint.getNamespaceIdentifier();
		Long contributor = getBundleId(extensionPoint.getContributor().getName());
		Extension[] extensions = createExtensionAdapters(extensionPoint.getExtensions());
		ExtensionPoint adapter = new ExtensionPoint(model, label, uniqueIdentifier, namespaceIdentifier, contributor);
		adapter.getExtensions().addAll(Arrays.asList(extensions));
		return adapter;
	}

	private ServiceRegistration createServiceReferenceAdapter(ServiceReference ref) {
		Long id = (Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
		String bundle = ref.getBundle().getSymbolicName();
		org.osgi.framework.Bundle[] usingBundles = ref.getUsingBundles();
		Long[] usingBundlesIds = null;
		if (usingBundles != null) {
			usingBundlesIds = new Long[usingBundles.length];
			for (int i = 0; i < usingBundles.length; i++) {
				usingBundlesIds[i] = new Long(usingBundles[i].getBundleId());
			}
		}
		String[] classes = (String[]) ref.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
		return new ServiceRegistration(model, id, bundle, usingBundlesIds, classes);
	}

	private static boolean getIsEnabled(org.osgi.framework.Bundle bundle) {
		PlatformAdmin plaformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
		State state = plaformAdmin.getState(false);

		BundleDescription description = state.getBundle(bundle.getBundleId());
		return (state.getDisabledInfos(description)).length == 0;
	}

	private static String createLocation(org.osgi.framework.Bundle bundle) {
		URL bundleEntry = null;

		try {
			bundleEntry = bundle.getEntry("/"); //$NON-NLS-1$
		} catch (IllegalStateException e) {
			return null;
		}

		try {
			bundleEntry = FileLocator.resolve(bundleEntry);
		} catch (IOException e) { // do nothing
		}
		IPath path = new Path(bundleEntry.getFile());
		String pathString = path.removeTrailingSeparator().toOSString();
		if (pathString.startsWith("file:")) //$NON-NLS-1$
			pathString = pathString.substring(5);
		if (pathString.endsWith("!")) //$NON-NLS-1$
			pathString = pathString.substring(0, pathString.length() - 1);
		return pathString;
	}

	private Object[] getManifestHeaderArray(org.osgi.framework.Bundle bundle, String headerKey) {
		String libraries = (String) bundle.getHeaders().get(headerKey);
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(headerKey, libraries);
			if (elements == null)
				return null;
			if (headerKey.equals(Constants.BUNDLE_CLASSPATH)) {
				BundleLibrary[] array = new BundleLibrary[elements.length];
				for (int i = 0; i < elements.length; i++)
					array[i] = new BundleLibrary(model, elements[i].getValue());
				return array;
			} else if (headerKey.equals(Constants.REQUIRE_BUNDLE)) {
				BundlePrerequisite[] array = new BundlePrerequisite[elements.length];
				for (int i = 0; i < elements.length; i++) {
					ManifestElement element = elements[i];
					String name = element.getValue();
					String version = element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);

					String visibility = element.getDirective(Constants.VISIBILITY_DIRECTIVE);
					boolean isExported = Constants.VISIBILITY_REEXPORT.equals(visibility);
					array[i] = new BundlePrerequisite(model, name, version, isExported);
				}
				return array;
			}
		} catch (BundleException e) { // do nothing
		}
		return null;
	}

	private Attribute[] createConfigurationElementAttributes(IConfigurationElement config) {
		String[] atts = config.getAttributeNames();

		Attribute[] catts = new Attribute[atts.length];
		for (int i = 0; i < atts.length; i++)
			catts[i] = new Attribute(model, atts[i], config.getAttribute(atts[i]));

		IConfigurationElement[] children = config.getChildren();
		Attribute[] result = new Attribute[children.length + catts.length];
		for (int i = 0; i < children.length; i++) {
			IConfigurationElement child = children[i];
			result[i] = createConfigurationElement(child);
		}
		for (int i = 0; i < catts.length; i++) {
			result[children.length + i] = catts[i];
		}
		return result;
	}

	private static String createName(IConfigurationElement config) {
		String label = config.getAttribute("label"); //$NON-NLS-1$
		if (label == null)
			label = config.getName();

		if (label == null)
			label = config.getAttribute("name"); //$NON-NLS-1$

		if (label == null && config.getAttribute("id") != null) { //$NON-NLS-1$
			String[] labelSplit = config.getAttribute("id").split("\\."); //$NON-NLS-1$ //$NON-NLS-2$
			label = labelSplit.length == 0 ? null : labelSplit[labelSplit.length - 1];
		}

		return label;
	}

	private Extension[] createExtensionAdapters(IExtension[] extensions) {
		Extension[] extensionAdapters = new Extension[extensions.length];
		for (int i = 0; i < extensions.length; i++) {
			extensionAdapters[i] = createExtensionAdapter(extensions[i]);
		}
		return extensionAdapters;
	}

	public void bundleChanged(BundleEvent event) {
		Bundle adapter = createBundleAdapter(event.getBundle());

		switch (event.getType()) {
			case BundleEvent.INSTALLED :
				listener.addBundle(adapter);
				break;
			case BundleEvent.UNINSTALLED :
				listener.removeBundle(adapter);
				break;
			case BundleEvent.UPDATED :
				listener.updateBundle(adapter, ModelChangeDelta.UPDATED);
				break;
			case BundleEvent.RESOLVED :
				listener.updateBundle(adapter, ModelChangeDelta.RESOLVED);
				break;
			case BundleEvent.UNRESOLVED :
				listener.updateBundle(adapter, ModelChangeDelta.UNRESOLVED);
				break;
			case BundleEvent.STARTING :
				listener.updateBundle(adapter, ModelChangeDelta.STARTING);
				break;
			case BundleEvent.STARTED :
				listener.updateBundle(adapter, ModelChangeDelta.STARTED);
				break;
			case BundleEvent.STOPPING :
				listener.updateBundle(adapter, ModelChangeDelta.STOPPING);
				break;
			case BundleEvent.STOPPED :
				listener.updateBundle(adapter, ModelChangeDelta.STOPPED);
				break;
			default :
				listener.updateBundle(adapter, ModelChangeDelta.UPDATED);
		}
	}

	public void serviceChanged(ServiceEvent event) {
		ServiceReference ref = event.getServiceReference();
		ServiceRegistration adapter = createServiceReferenceAdapter(ref);

		switch (event.getType()) {
			case ServiceEvent.REGISTERED :
				listener.addService(adapter);
				break;
			case ServiceEvent.UNREGISTERING :
				listener.removeService(adapter);
				break;
			case ServiceEvent.MODIFIED :
			default :
				listener.updateService(adapter);
				break;
		}
	}

	private ExtensionPoint[] createExtensionPointAdapters(IExtensionPoint[] extensionPoints) {
		ExtensionPoint[] result = new ExtensionPoint[extensionPoints.length];
		for (int i = 0; i < extensionPoints.length; i++) {
			result[i] = createExtensionPointAdapter(extensionPoints[i]);
		}
		return result;
	}

	public void added(IExtension[] extensions) {
		listener.addExtensions(createExtensionAdapters(extensions));
	}

	public void removed(IExtension[] extensions) {
		listener.removeExtensions(createExtensionAdapters(extensions));
	}

	public void added(IExtensionPoint[] extensionPoints) {
		listener.addExtensionPoints(createExtensionPointAdapters(extensionPoints));
	}

	public void removed(IExtensionPoint[] extensionPoints) {
		listener.removeExtensionPoints(createExtensionPointAdapters(extensionPoints));
	}

	public void setEnabled(Bundle bundle, boolean enabled) {
		State state = PDERuntimePlugin.getDefault().getState();
		long bundleId = bundle.getId().longValue();
		BundleDescription desc = state.getBundle(bundleId);

		if (enabled) {
			DisabledInfo[] infos = state.getDisabledInfos(desc);
			for (int i = 0; i < infos.length; i++) {
				PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
				platformAdmin.removeDisabledInfo(infos[i]);
			}
		} else {
			DisabledInfo info = new DisabledInfo("org.eclipse.pde.ui", "Disabled via PDE", desc); //$NON-NLS-1$ //$NON-NLS-2$
			PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
			platformAdmin.addDisabledInfo(info);
		}

		org.osgi.framework.Bundle b = PDERuntimePlugin.getDefault().getBundleContext().getBundle(bundleId);
		PackageAdmin packageAdmin = PDERuntimePlugin.getDefault().getPackageAdmin();
		packageAdmin.refreshPackages(new org.osgi.framework.Bundle[] {b});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.runtime.registry.model.local.RegistryBackend#setEnabled(org.osgi.framework.Bundle, boolean)
	 */
	public void setEnabled(org.osgi.framework.Bundle bundle, boolean enabled) {
		PlatformAdmin plaformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
		State state = plaformAdmin.getState(false);

		BundleDescription desc = state.getBundle(bundle.getBundleId());

		if (enabled) {
			DisabledInfo[] infos = state.getDisabledInfos(desc);
			for (int i = 0; i < infos.length; i++) {
				PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
				platformAdmin.removeDisabledInfo(infos[i]);
			}
		} else {
			DisabledInfo info = new DisabledInfo("org.eclipse.pde.ui", "Disabled via PDE", desc); //$NON-NLS-1$ //$NON-NLS-2$
			PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
			platformAdmin.addDisabledInfo(info);
		}

		PackageAdmin packageAdmin = PDERuntimePlugin.getDefault().getPackageAdmin();
		packageAdmin.refreshPackages(new org.osgi.framework.Bundle[] {bundle});
	}
}
