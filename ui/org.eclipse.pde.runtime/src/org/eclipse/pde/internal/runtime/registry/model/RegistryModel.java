/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * Model entry point for Eclipse runtime. Provides information about runtime bundles, services and extension points.
 */
public class RegistryModel {

	private BackendChangeListener backendListener = new BackendChangeListener() {
		public void addBundle(Bundle adapter) {
			adapter.setModel(RegistryModel.this);
			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.ADDED);

			bundles.put(new Long(adapter.getId()), adapter);

			if (adapter.getFragmentHost() != null) {
				addFragment(adapter);

				Bundle host = getBundle(adapter.getFragmentHost(), adapter.getFragmentHostVersion());
				if (host != null) {
					ModelChangeDelta d2 = new ModelChangeDelta(host, ModelChangeDelta.UPDATED);
					fireModelChangeEvent(new ModelChangeDelta[] {delta, d2});
					return;
				}
			}

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void removeBundle(Bundle adapter) {
			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.REMOVED);

			bundles.remove(new Long(adapter.getId()));

			if (adapter.getFragmentHost() != null) {
				removeFragment(adapter);

				Bundle host = getBundle(adapter.getFragmentHost(), adapter.getFragmentHostVersion());
				if (host != null) {
					ModelChangeDelta d2 = new ModelChangeDelta(host, ModelChangeDelta.UPDATED);
					fireModelChangeEvent(new ModelChangeDelta[] {delta, d2});
					return;
				}
			}

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
			adapter.setModel(null);
		}

		public void updateBundle(Bundle adapter, int updated) {
			adapter.setModel(RegistryModel.this);
			ModelChangeDelta delta = new ModelChangeDelta(adapter, updated);

			bundles.put(new Long(adapter.getId()), adapter); // replace old with new one

			if (adapter.getFragmentHost() != null) {
				addFragment(adapter);
			}

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void addService(ServiceRegistration adapter) {
			ModelChangeDelta serviceNameDelta = null;
			if (!serviceNames.contains(adapter.getName())) {
				ServiceName name = adapter.getName();
				name.setModel(RegistryModel.this);

				serviceNames.add(name);

				serviceNameDelta = new ModelChangeDelta(name, ModelChangeDelta.ADDED);
			}

			adapter.setModel(RegistryModel.this);
			services.put(new Long(adapter.getId()), adapter);

			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.ADDED);

			if (serviceNameDelta != null) {
				fireModelChangeEvent(new ModelChangeDelta[] {serviceNameDelta, delta});
			} else {
				fireModelChangeEvent(new ModelChangeDelta[] {delta});
			}
		}

		public void removeService(ServiceRegistration adapter) {
			ModelChangeDelta serviceNameDelta = null;
			if (getServices(adapter.getName().getClasses()).length == 0) {
				serviceNames.remove(adapter.getName());
				serviceNameDelta = new ModelChangeDelta(adapter.getName(), ModelChangeDelta.REMOVED);
			}

			services.remove(new Long(adapter.getId()));

			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.REMOVED);

			if (serviceNameDelta != null) {
				fireModelChangeEvent(new ModelChangeDelta[] {serviceNameDelta, delta});
				adapter.getName().setModel(null);
				adapter.setModel(null);
			} else {
				fireModelChangeEvent(new ModelChangeDelta[] {delta});
				adapter.setModel(null);
			}
		}

		public void updateService(ServiceRegistration adapter) {
			adapter.setModel(RegistryModel.this);
			services.put(new Long(adapter.getId()), adapter);

			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.UPDATED);

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void addExtensions(Extension[] extensionAdapters) {
			for (int i = 0; i < extensionAdapters.length; i++) {
				extensionAdapters[i].setModel(RegistryModel.this);
				String id = extensionAdapters[i].getExtensionPointUniqueIdentifier();
				ExtensionPoint extPoint = (ExtensionPoint) extensionPoints.get(id);
				extPoint.getExtensions().add(extensionAdapters[i]);
			}

			ModelChangeDelta[] delta = new ModelChangeDelta[extensionAdapters.length];
			for (int i = 0; i < delta.length; i++) {
				delta[i] = new ModelChangeDelta(extensionAdapters[i], ModelChangeDelta.ADDED);
			}
			fireModelChangeEvent(delta);
		}

		public void removeExtensions(Extension[] extensionAdapters) {
			for (int i = 0; i < extensionAdapters.length; i++) {
				String id = extensionAdapters[i].getExtensionPointUniqueIdentifier();
				ExtensionPoint extPoint = (ExtensionPoint) extensionPoints.get(id);
				extPoint.getExtensions().remove(extensionAdapters[i]);
			}

			ModelChangeDelta[] delta = new ModelChangeDelta[extensionAdapters.length];
			for (int i = 0; i < delta.length; i++) {
				delta[i] = new ModelChangeDelta(extensionAdapters[i], ModelChangeDelta.REMOVED);
			}
			fireModelChangeEvent(delta);

			for (int i = 0; i < extensionAdapters.length; i++) {
				extensionAdapters[i].setModel(null);
			}
		}

		public void addExtensionPoints(ExtensionPoint[] extensionPointAdapters) {
			for (int i = 0; i < extensionPointAdapters.length; i++) {
				extensionPointAdapters[i].setModel(RegistryModel.this);
				extensionPoints.put(extensionPointAdapters[i].getUniqueIdentifier(), extensionPointAdapters[i]);
			}

			ModelChangeDelta[] delta = new ModelChangeDelta[extensionPointAdapters.length];
			for (int i = 0; i < delta.length; i++) {
				delta[i] = new ModelChangeDelta(extensionPointAdapters[i], ModelChangeDelta.ADDED);
			}
			fireModelChangeEvent(delta);
		}

		public void removeExtensionPoints(ExtensionPoint[] extensionPointAdapters) {
			for (int i = 0; i < extensionPointAdapters.length; i++) {
				extensionPoints.remove(extensionPointAdapters[i].getUniqueIdentifier());
			}

			ModelChangeDelta[] delta = new ModelChangeDelta[extensionPointAdapters.length];
			for (int i = 0; i < delta.length; i++) {
				delta[i] = new ModelChangeDelta(extensionPointAdapters[i], ModelChangeDelta.REMOVED);
			}
			fireModelChangeEvent(delta);

			for (int i = 0; i < extensionPointAdapters.length; i++) {
				extensionPointAdapters[i].setModel(null);
			}
		}
	};

	private List listeners = new ArrayList();
	private Map bundles;
	private Map services;
	private Map extensionPoints;
	private Set serviceNames;
	private Map fragments;

	protected RegistryBackend backend;

	public RegistryModel(RegistryBackend backend) {
		bundles = Collections.synchronizedMap(new HashMap());
		services = Collections.synchronizedMap(new HashMap());
		extensionPoints = Collections.synchronizedMap(new HashMap());
		serviceNames = Collections.synchronizedSet(new HashSet());
		fragments = Collections.synchronizedMap(new HashMap());

		this.backend = backend;
		backend.setRegistryListener(backendListener);
	}

	protected void addFragment(Bundle fragment) {
		Set hostFragments = (Set) fragments.get(fragment.getFragmentHost());
		if (hostFragments == null) {
			hostFragments = Collections.synchronizedSet(new HashSet());
			fragments.put(fragment.getFragmentHost(), hostFragments);
		}

		if (!hostFragments.add(fragment)) {
			// not added if element already exists. So remove old and add it again.
			hostFragments.remove(fragment);
			hostFragments.add(fragment);
		}
	}

	protected void removeFragment(Bundle fragment) {
		Set hostFragments = (Set) fragments.get(fragment.getFragmentHost());
		if (hostFragments == null) {
			return;
		}

		hostFragments.remove(fragment);
	}

	public void connect(IProgressMonitor monitor, boolean forceInit) {
		backend.connect(monitor);

		if (forceInit) {
			initialize(monitor);
		}
	}

	public void initialize(IProgressMonitor monitor) {
		backend.initializeBundles(monitor);
		backend.initializeServices(monitor);
		backend.initializeExtensionPoints(monitor);
	}

	public void disconnect() {
		backend.disconnect();
	}

	public Bundle[] getBundles() {
		return (Bundle[]) bundles.values().toArray(new Bundle[bundles.values().size()]);
	}

	public ExtensionPoint[] getExtensionPoints() {
		return (ExtensionPoint[]) extensionPoints.values().toArray(new ExtensionPoint[extensionPoints.values().size()]);
	}

	public ServiceRegistration[] getServices() {
		return (ServiceRegistration[]) services.values().toArray(new ServiceRegistration[services.values().size()]);
	}

	public ServiceName[] getServiceNames() {
		return (ServiceName[]) serviceNames.toArray(new ServiceName[serviceNames.size()]);
	}

	public ServiceRegistration[] getServices(String[] classes) {
		List result = new ArrayList();

		for (Iterator i = services.values().iterator(); i.hasNext();) {
			ServiceRegistration sr = (ServiceRegistration) i.next();
			if (Arrays.equals(classes, sr.getName().getClasses()))
				result.add(sr);
		}

		return (ServiceRegistration[]) result.toArray(new ServiceRegistration[result.size()]);
	}

	public void addModelChangeListener(ModelChangeListener listener) {
		listeners.add(listener);
	}

	public void removeModelChangeListener(ModelChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * For received domain types: Bundle, IExtension, IExtensionPoint, ServiceReference,
	 * generates delta with model types: IBundle, IExtensionAdapter, IExtensionPointAdapter, IService
	 *  
	 * @param objects
	 */
	protected void fireModelChangeEvent(ModelChangeDelta[] delta) {
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			ModelChangeListener listener = (ModelChangeListener) i.next();
			listener.modelChanged(delta);
		}
	}

	public Bundle getBundle(Long id) {
		return (Bundle) bundles.get(id);
	}

	public Bundle getBundle(String symbolicName, String versionRange) {
		for (Iterator i = bundles.values().iterator(); i.hasNext();) {
			Bundle bundle = (Bundle) i.next();

			if (bundle.getSymbolicName().equals(symbolicName)) {
				if (versionMatches(bundle.getVersion(), versionRange))
					return bundle;
			}
		}

		return null;
	}

	public ExtensionPoint getExtensionPoint(String extensionPointUniqueIdentifier) {
		return (ExtensionPoint) extensionPoints.get(extensionPointUniqueIdentifier);
	}

	public Bundle[] getFragments(Bundle bundle) {
		Set set = (Set) fragments.get(bundle.getSymbolicName());
		if (set == null)
			return new Bundle[0];

		List result = new ArrayList(set.size());
		Version hostVersion = Version.parseVersion(bundle.getVersion());
		for (Iterator i = set.iterator(); i.hasNext();) {
			Bundle fragment = (Bundle) i.next();
			String fragmentVersionOrRange = fragment.getFragmentHostVersion();

			if (versionMatches(hostVersion, fragmentVersionOrRange))
				result.add(fragment);
		}

		return (Bundle[]) result.toArray(new Bundle[result.size()]);
	}

	private boolean versionMatches(String hostVersion, String versionOrRange) {
		try {
			Version version = Version.parseVersion(hostVersion);
			return versionMatches(version, versionOrRange);

		} catch (IllegalArgumentException e) {
			// ignore
		}

		return false;
	}

	/**
	 * Check if hostVersion is greater or equal fragmentVersion, or is included in fragment version range
	 * @param hostVersion Version
	 * @param versionOrRange Version or VersionRange
	 * @return true if matches, false otherwise
	 */
	private boolean versionMatches(Version hostVersion, String versionOrRange) {
		if (versionOrRange == null) {
			return true;
		}

		try {
			Version version = Version.parseVersion(versionOrRange);
			if (hostVersion.compareTo(version) >= 0)
				return true;

		} catch (IllegalArgumentException e) {
			// wrong formatting, try VersionRange
		}

		try {
			VersionRange range = new VersionRange(versionOrRange);
			if (range.isIncluded(hostVersion))
				return true;

		} catch (IllegalArgumentException e2) {
			// wrong range formatting
		}

		return false;
	}

	/*	void setEnabled(Bundle bundle, boolean enabled);

		void start(Bundle bundle) throws BundleException; // XXX Create custom Exception

		void stop(Bundle bundle) throws BundleException;

		MultiStatus diagnose(Bundle bundle);*/

}
