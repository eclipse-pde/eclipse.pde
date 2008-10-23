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

import java.util.*;

/**
 * Model entry point for Eclipse runtime. Provides information about runtime bundles, services and extension points.
 */
public class RegistryModel {

	private BackendChangeListener backendListener = new BackendChangeListener() {
		public void addBundle(Bundle adapter) {
			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.ADDED);

			bundles.put(adapter.getId(), adapter);

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void removeBundle(Bundle adapter) {
			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.REMOVED);

			bundles.remove(adapter.getId());

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void updateBundle(Bundle adapter, int updated) {
			ModelChangeDelta delta = new ModelChangeDelta(adapter, updated);

			bundles.put(adapter.getId(), adapter); // replace old with new one

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void addService(ServiceRegistration adapter) {
			services.put(adapter.getId(), adapter);

			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.ADDED);

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void removeService(ServiceRegistration adapter) {
			services.remove(adapter.getId());

			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.REMOVED);

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void updateService(ServiceRegistration adapter) {
			services.put(adapter.getId(), adapter);

			ModelChangeDelta delta = new ModelChangeDelta(adapter, ModelChangeDelta.UPDATED);

			fireModelChangeEvent(new ModelChangeDelta[] {delta});
		}

		public void addExtensions(Extension[] extensionAdapters) {
			for (int i = 0; i < extensionAdapters.length; i++) {
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
		}

		public void addExtensionPoints(ExtensionPoint[] extensionPointAdapters) {
			for (int i = 0; i < extensionPointAdapters.length; i++) {
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
		}
	};

	private List listeners = new ArrayList();
	private Map bundles;
	private Map services;
	private Map extensionPoints;
	protected RegistryBackend backend;

	public RegistryModel(RegistryBackend backend) {
		this.backend = backend;
		backend.setRegistryListener(backendListener);
		backend.setRegistryModel(this);
	}

	public void connect() {
		backend.connect();

		bundles = backend.initializeBundles();
		services = backend.initializeServices();
		extensionPoints = backend.initializeExtensionPoints();
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

	public ExtensionPoint getExtensionPoint(String extensionPointUniqueIdentifier) {
		return (ExtensionPoint) extensionPoints.get(extensionPointUniqueIdentifier);
	}

	/*	void setEnabled(Bundle bundle, boolean enabled);

		void start(Bundle bundle) throws BundleException; // XXX Create custom Exception

		void stop(Bundle bundle) throws BundleException;

		MultiStatus diagnose(Bundle bundle);*/

}
