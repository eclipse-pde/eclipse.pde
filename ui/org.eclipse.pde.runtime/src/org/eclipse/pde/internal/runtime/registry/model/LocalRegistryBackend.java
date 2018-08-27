/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wolfgang Schell <ws@jetztgrad.net> - bug 259348
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.runtime.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class LocalRegistryBackend implements IRegistryEventListener, BundleListener, ServiceListener, RegistryBackend {

	private BackendChangeListener listener;

	@Override
	public void setRegistryListener(BackendChangeListener listener) {
		this.listener = listener;
	}

	@Override
	public void connect(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;

		PDERuntimePlugin.getDefault().getBundleContext().addBundleListener(this);
		Platform.getExtensionRegistry().addListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().addServiceListener(this);
	}

	@Override
	public void disconnect() {
		Platform.getExtensionRegistry().removeListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().removeBundleListener(this);
		PDERuntimePlugin.getDefault().getBundleContext().removeServiceListener(this);
	}

	protected static boolean isRegisteredService(org.osgi.framework.Bundle bundle, ServiceReference<?> ref) {
		return bundle.equals(ref.getBundle());
	}

	protected static boolean isServiceInUse(org.osgi.framework.Bundle bundle, ServiceReference<?> ref) {
		org.osgi.framework.Bundle[] usingBundles = ref.getUsingBundles();
		return (usingBundles != null && Arrays.asList(usingBundles).contains(bundle));
	}

	@Override
	public void start(long id) throws BundleException {
		PDERuntimePlugin.getDefault().getBundleContext().getBundle(id).start();
	}

	@Override
	public void stop(long id) throws BundleException {
		PDERuntimePlugin.getDefault().getBundleContext().getBundle(id).stop();
	}

	@Override
	public MultiStatus diagnose(long id) {
		PlatformAdmin plaformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
		State state = plaformAdmin.getState(false);

		BundleDescription desc = state.getBundle(id);

		PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
		VersionConstraint[] unsatisfied = platformAdmin.getStateHelper().getUnsatisfiedConstraints(desc);
		ResolverError[] resolverErrors = platformAdmin.getState(false).getResolverErrors(desc);

		MultiStatus problems = new MultiStatus(PDERuntimePlugin.ID, IStatus.INFO, PDERuntimeMessages.RegistryView_found_problems, null);
		for (ResolverError error : resolverErrors) {
			if ((error.getType() & (ResolverError.MISSING_FRAGMENT_HOST | ResolverError.MISSING_GENERIC_CAPABILITY | ResolverError.MISSING_IMPORT_PACKAGE | ResolverError.MISSING_REQUIRE_BUNDLE)) != 0){
				continue;
			}
			IStatus status = new Status(IStatus.WARNING, PDERuntimePlugin.ID, error.toString());
			problems.add(status);
		}

		for (VersionConstraint constraint : unsatisfied) {
			IStatus status = new Status(IStatus.WARNING, PDERuntimePlugin.ID, MessageHelper.getResolutionFailureMessage(constraint));
			problems.add(status);
		}

		return problems;
	}

	@Override
	public void initializeBundles(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;

		org.osgi.framework.Bundle[] newBundles = PDERuntimePlugin.getDefault().getBundleContext().getBundles();
		for (org.osgi.framework.Bundle bundle : newBundles) {
			if (monitor.isCanceled()){
				return;
			}

			Bundle ba = createBundleAdapter(bundle);
			listener.addBundle(ba);
		}
	}

	@Override
	public void initializeExtensionPoints(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;

		IExtensionPoint[] extPoints = Platform.getExtensionRegistry().getExtensionPoints();
		ExtensionPoint[] extPts = new ExtensionPoint[extPoints.length];
		for (int i = 0; i < extPoints.length; i++) {
			if (monitor.isCanceled())
				return;

			extPts[i] = createExtensionPointAdapter(extPoints[i]);
		}
		listener.addExtensionPoints(extPts);
	}

	@Override
	public void initializeServices(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;

		ServiceReference<?>[] references = null;
		try {
			references = PDERuntimePlugin.getDefault().getBundleContext().getAllServiceReferences(null, null);
		} catch (InvalidSyntaxException e) { // nothing
		}

		if (references == null) {
			return;
		}

		for (ServiceReference<?> reference : references) {
			if (monitor.isCanceled()){
				return;
			}

			ServiceRegistration service = createServiceReferenceAdapter(reference);
			// The list of registered services is volatile, avoid adding unregistered services to the listener
			if (service.getBundle() != null) {
				listener.addService(service);
			}
		}
	}

	private Bundle createBundleAdapter(org.osgi.framework.Bundle bundle) {
		Bundle adapter = new Bundle();
		adapter.setSymbolicName(bundle.getSymbolicName());
		adapter.setVersion(bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION));
		adapter.setState(bundle.getState());
		adapter.setId(bundle.getBundleId());
		adapter.setEnabled(getIsEnabled(bundle));
		adapter.setLocation(createLocation(bundle));

		String fragmentHost = bundle.getHeaders().get(Constants.FRAGMENT_HOST);
		if (fragmentHost != null) {
			ManifestElement[] header;
			try {
				header = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, fragmentHost);

				if (header.length > 0) {
					ManifestElement host = header[0];
					adapter.setFragmentHost(host.getValue());
					String version = host.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
					if (version != null) {
						adapter.setFragmentHostVersion(version);
					}
				}
			} catch (BundleException e) {
				PDERuntimePlugin.log(e);
			}
		}

		BundlePrerequisite[] imports = (BundlePrerequisite[]) getManifestHeaderArray(bundle, Constants.REQUIRE_BUNDLE);
		if (imports != null)
			adapter.setImports(imports);

		BundleLibrary[] libraries = (BundleLibrary[]) getManifestHeaderArray(bundle, Constants.BUNDLE_CLASSPATH);
		if (libraries != null)
			adapter.setLibraries(libraries);

		BundlePrerequisite[] importPackages = (BundlePrerequisite[]) getManifestHeaderArray(bundle, Constants.IMPORT_PACKAGE);
		if (importPackages != null)
			adapter.setImportedPackages(importPackages);

		BundlePrerequisite[] exportPackages = (BundlePrerequisite[]) getManifestHeaderArray(bundle, Constants.EXPORT_PACKAGE);
		if (exportPackages != null)
			adapter.setExportedPackages(exportPackages);

		return adapter;
	}

	private Extension createExtensionAdapter(IExtension extension) {
		Extension adapter = new Extension();
		adapter.setNamespaceIdentifier(extension.getNamespaceIdentifier());
		adapter.setLabel(extension.getLabel());
		adapter.setExtensionPointUniqueIdentifier(extension.getExtensionPointUniqueIdentifier());
		adapter.setContributor(getBundleId(extension.getContributor().getName()));

		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length > 0) {
			ConfigurationElement[] configurationElements = new ConfigurationElement[elements.length];
			for (int i = 0; i < elements.length; i++) {
				configurationElements[i] = createConfigurationElement(elements[i]);
			}
			adapter.setConfigurationElements(configurationElements);
		}

		return adapter;
	}

	private ConfigurationElement createConfigurationElement(IConfigurationElement config) {
		ConfigurationElement element = new ConfigurationElement();
		element.setName(createName(config));
		Attribute[] attributes = createConfigurationElementAttributes(config);
		if (attributes != null)
			element.setElements(attributes);
		return element;
	}

	private static Long getBundleId(String name) {
		BundleDescription descr = PDERuntimePlugin.getDefault().getPlatformAdmin().getState(false).getBundle(name, null);
		return descr == null ? null : Long.valueOf(descr.getBundleId());
	}

	private ExtensionPoint createExtensionPointAdapter(IExtensionPoint extensionPoint) {
		ExtensionPoint adapter = new ExtensionPoint();
		adapter.setLabel(extensionPoint.getLabel());
		adapter.setUniqueIdentifier(extensionPoint.getUniqueIdentifier());
		adapter.setNamespaceIdentifier(extensionPoint.getNamespaceIdentifier());
		adapter.setContributor(getBundleId(extensionPoint.getContributor().getName()));

		Extension[] extensions = createExtensionAdapters(extensionPoint.getExtensions());
		adapter.getExtensions().addAll(Arrays.asList(extensions));
		return adapter;
	}

	/**
	 * Returns a new {@link ServiceRegistration} for the given service reference.  If the service being
	 * referenced is unregistered, the returned service registration will not have a bundle set.
	 *
	 * @param ref the service reference to get the registration for
	 * @return a new service registration containing information from the service reference
	 */
	private ServiceRegistration createServiceReferenceAdapter(ServiceReference<?> ref) {
		ServiceRegistration service = new ServiceRegistration();
		service.setId(((Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID)).longValue());
		org.osgi.framework.Bundle bundle = ref.getBundle();
		if (bundle != null) {
			service.setBundle(bundle.getSymbolicName());
		}

		org.osgi.framework.Bundle[] usingBundles = ref.getUsingBundles();
		long[] usingBundlesIds = null;
		if (usingBundles != null) {
			usingBundlesIds = new long[usingBundles.length];
			for (int i = 0; i < usingBundles.length; i++) {
				usingBundlesIds[i] = usingBundles[i].getBundleId();
			}
		}
		if (usingBundlesIds != null)
			service.setUsingBundles(usingBundlesIds);

		String[] classes = (String[]) ref.getProperty(org.osgi.framework.Constants.OBJECTCLASS);
		String[] propertyKeys = ref.getPropertyKeys();
		Property[] properties = null;
		if (propertyKeys != null) {
			properties = new Property[propertyKeys.length];
			for (int p = 0; p < propertyKeys.length; p++) {
				String key = propertyKeys[p];
				Object value = ref.getProperty(key);
				properties[p] = new Property(key, ServiceRegistration.toString(value));
			}
		}

		if (classes != null) {
			Arrays.sort(classes);
			service.setName(new ServiceName(classes, ref));
			service.setProperties(properties);
		}
		return service;
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
		String libraries = bundle.getHeaders().get(headerKey);
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(headerKey, libraries);
			if (elements == null)
				return null;
			if (headerKey.equals(Constants.BUNDLE_CLASSPATH)) {
				BundleLibrary[] array = new BundleLibrary[elements.length];
				for (int i = 0; i < elements.length; i++) {
					BundleLibrary library = new BundleLibrary();
					library.setLibrary(elements[i].getValue());
					array[i] = library;
				}
				return array;
			} else if (headerKey.equals(Constants.REQUIRE_BUNDLE) || headerKey.equals(Constants.IMPORT_PACKAGE) || headerKey.equals(Constants.EXPORT_PACKAGE)) {
				BundlePrerequisite[] array = new BundlePrerequisite[elements.length];
				for (int i = 0; i < elements.length; i++) {
					ManifestElement element = elements[i];

					BundlePrerequisite prereq = new BundlePrerequisite();
					prereq.setName(element.getValue());
					if (headerKey.equals(Constants.REQUIRE_BUNDLE)) {
						prereq.setVersion(element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE));
						String visibility = element.getDirective(Constants.VISIBILITY_DIRECTIVE);
						prereq.setExported(Constants.VISIBILITY_REEXPORT.equals(visibility));
					} else {
						prereq.setVersion(element.getAttribute(Constants.VERSION_ATTRIBUTE));
						prereq.setPackage(true);
					}

					array[i] = prereq;
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
			catts[i] = new Attribute(atts[i], config.getAttribute(atts[i]));

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

	@Override
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

	@Override
	public void serviceChanged(ServiceEvent event) {
		ServiceReference<?> ref = event.getServiceReference();
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

	@Override
	public void added(IExtension[] extensions) {
		listener.addExtensions(createExtensionAdapters(extensions));
	}

	@Override
	public void removed(IExtension[] extensions) {
		listener.removeExtensions(createExtensionAdapters(extensions));
	}

	@Override
	public void added(IExtensionPoint[] extensionPoints) {
		listener.addExtensionPoints(createExtensionPointAdapters(extensionPoints));
	}

	@Override
	public void removed(IExtensionPoint[] extensionPoints) {
		listener.removeExtensionPoints(createExtensionPointAdapters(extensionPoints));
	}

	@Override
	public void setEnabled(long id, boolean enabled) {
		State state = PDERuntimePlugin.getDefault().getState();
		BundleDescription desc = state.getBundle(id);

		if (enabled) {
			DisabledInfo[] infos = state.getDisabledInfos(desc);
			for (DisabledInfo info : infos) {
				PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
				platformAdmin.removeDisabledInfo(info);
			}
		} else {
			DisabledInfo info = new DisabledInfo("org.eclipse.pde.ui", "Disabled via PDE", desc); //$NON-NLS-1$ //$NON-NLS-2$
			PlatformAdmin platformAdmin = PDERuntimePlugin.getDefault().getPlatformAdmin();
			platformAdmin.addDisabledInfo(info);
		}

		org.osgi.framework.Bundle b = PDERuntimePlugin.getDefault().getBundleContext().getBundle(id);
		PackageAdmin packageAdmin = PDERuntimePlugin.getDefault().getPackageAdmin();
		packageAdmin.refreshPackages(new org.osgi.framework.Bundle[] {b});
	}
}
