/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.MultiStatus;
import org.osgi.framework.BundleException;

public class Bundle extends ModelObject {

	public static final int ACTIVE = org.osgi.framework.Bundle.ACTIVE;
	public static final int UNINSTALLED = org.osgi.framework.Bundle.UNINSTALLED;
	public static final int INSTALLED = org.osgi.framework.Bundle.INSTALLED;

	private String symbolicName;
	private String location;
	private boolean isEnabled;
	private BundlePrerequisite[] imports = new BundlePrerequisite[0];
	private String version;
	private int state;
	private long id;
	private BundleLibrary[] libraries = new BundleLibrary[0];
	private BundlePrerequisite[] importedPackages = new BundlePrerequisite[0];
	private BundlePrerequisite[] exportedPackages = new BundlePrerequisite[0];

	private String fragmentHost;
	private String fragmentHostVersion;

	public void setFragmentHost(String fragmentHost) {
		this.fragmentHost = fragmentHost;
	}

	public String getFragmentHost() {
		return fragmentHost;
	}

	public String getFragmentHostVersion() {
		return fragmentHostVersion;
	}

	public void setFragmentHostVersion(String fragmentHostVersion) {
		this.fragmentHostVersion = fragmentHostVersion;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setImports(BundlePrerequisite[] imports) {
		if (imports == null)
			throw new IllegalArgumentException();

		this.imports = imports;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setState(int state) {
		this.state = state;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	public void setLibraries(BundleLibrary[] libraries) {
		if (libraries == null)
			throw new IllegalArgumentException();

		this.libraries = libraries;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public BundlePrerequisite[] getImports() {
		return imports;
	}

	public BundleLibrary[] getLibraries() {
		return libraries;
	}

	public String getLocation() {
		return location;
	}

	public String getVersion() {
		return version;
	}

	public int getState() {
		return state;
	}

	public long getId() {
		return id;
	}

	public void start() throws BundleException {
		if (model == null)
			return;
		model.backend.start(id);
	}

	public void stop() throws BundleException {
		if (model == null)
			return;
		model.backend.stop(id);
	}

	public void enable() {
		if (model == null)
			return;
		model.backend.setEnabled(id, true);
	}

	public void disable() {
		if (model == null)
			return;
		model.backend.setEnabled(id, false);
	}

	public MultiStatus diagnose() {
		if (model == null)
			return null;
		return model.backend.diagnose(id);
	}

	public ExtensionPoint[] getExtensionPoints() {
		if (model == null)
			return new ExtensionPoint[0];
		ExtensionPoint[] extPoints = model.getExtensionPoints();
		List result = new ArrayList();

		for (int i = 0; i < extPoints.length; i++) {
			if (extPoints[i].getContributorId().longValue() == id)
				result.add(extPoints[i]);
		}
		return (ExtensionPoint[]) result.toArray(new ExtensionPoint[result.size()]);
	}

	public Extension[] getExtensions() {
		if (model == null)
			return new Extension[0];
		ExtensionPoint[] extPoints = model.getExtensionPoints();
		List result = new ArrayList();

		for (int i = 0; i < extPoints.length; i++) {
			for (Iterator it = extPoints[i].getExtensions().iterator(); it.hasNext();) {
				Extension a = (Extension) it.next();
				if (a.getContributorId().longValue() == id)
					result.add(a);
			}

		}
		return (Extension[]) result.toArray(new Extension[result.size()]);
	}

	public ServiceRegistration[] getRegisteredServices() {
		if (model == null)
			return new ServiceRegistration[0];
		ServiceRegistration[] services = model.getServices();
		List result = new ArrayList();

		for (int i = 0; i < services.length; i++) {
			if (symbolicName.equals(services[i].getBundle()))
				result.add(services[i]);
		}
		return (ServiceRegistration[]) result.toArray(new ServiceRegistration[result.size()]);
	}

	public ServiceRegistration[] getServicesInUse() {
		if (model == null)
			return new ServiceRegistration[0];
		ServiceRegistration[] services = model.getServices();
		List result = new ArrayList();

		for (int i = 0; i < services.length; i++) {
			long[] usingBundles = services[i].getUsingBundleIds();
			if (usingBundles != null) {
				for (int j = 0; j < usingBundles.length; j++)
					if (id == usingBundles[j])
						result.add(services[i]);
			}
		}
		return (ServiceRegistration[]) result.toArray(new ServiceRegistration[result.size()]);
	}

	public boolean equals(Object obj) {
		return (obj instanceof Bundle) && (id == ((Bundle) obj).id);
	}

	public int hashCode() {
		return (int) id;
	}

	public Bundle[] getFragments() {
		if (model == null)
			return new Bundle[0];
		return model.getFragments(this);
	}

	public void setImportedPackages(BundlePrerequisite[] importedPackages) {
		this.importedPackages = importedPackages;
	}

	public BundlePrerequisite[] getImportedPackages() {
		return importedPackages;
	}

	public void setExportedPackages(BundlePrerequisite[] exportedPackages) {
		this.exportedPackages = exportedPackages;
	}

	public BundlePrerequisite[] getExportedPackages() {
		return exportedPackages;
	}
}
