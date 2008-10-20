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
	private BundlePrerequisite[] imports;
	private String version;
	private int state;
	private Long id;
	private BundleLibrary[] libraries;

	public Bundle(RegistryModel model, String symbolicName, String version, int state, Long id, String location, BundlePrerequisite[] prereq, BundleLibrary[] libs, boolean isEnabled) {
		super(model);
		this.symbolicName = symbolicName;
		this.version = version;
		this.state = state;
		this.id = id;
		this.location = location;
		this.imports = prereq;
		this.libraries = libs;
		this.isEnabled = isEnabled;
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

	public Long getId() {
		return id;
	}

	public void start() throws BundleException {
		getModel().backend.start(this);
	}

	public void stop() throws BundleException {
		getModel().backend.stop(this);
	}

	public void setEnabled(boolean enabled) {
		getModel().backend.setEnabled(this, enabled);
	}

	public MultiStatus diagnose() {
		return getModel().backend.diagnose(this);
	}

	public ExtensionPoint[] getExtensionPoints() {
		ExtensionPoint[] extPoints = getModel().getExtensionPoints();
		List result = new ArrayList();

		for (int i = 0; i < extPoints.length; i++) {
			if (id.equals(extPoints[i].getContributorId()))
				result.add(extPoints[i]);
		}
		return (ExtensionPoint[]) result.toArray(new ExtensionPoint[result.size()]);
	}

	public Extension[] getExtensions() {
		ExtensionPoint[] extPoints = getModel().getExtensionPoints();
		List result = new ArrayList();

		for (int i = 0; i < extPoints.length; i++) {
			for (Iterator it = extPoints[i].getExtensions().iterator(); it.hasNext();) {
				Extension a = (Extension) it.next();
				if (id.equals(a.getContributorId()))
					result.add(a);
			}

		}
		return (Extension[]) result.toArray(new Extension[result.size()]);
	}

	public ServiceRegistration[] getRegisteredServices() {
		ServiceRegistration[] services = getModel().getServices();
		List result = new ArrayList();

		for (int i = 0; i < services.length; i++) {
			if (symbolicName.equals(services[i].getBundle()))
				result.add(services[i]);
		}
		return (ServiceRegistration[]) result.toArray(new ServiceRegistration[result.size()]);
	}

	public ServiceRegistration[] getServicesInUse() {
		ServiceRegistration[] services = getModel().getServices();
		List result = new ArrayList();

		for (int i = 0; i < services.length; i++) {
			Long[] usingBundles = services[i].getUsingBundles();
			if (usingBundles != null) {
				for (int j = usingBundles.length; j < usingBundles.length; j++)
					if (id.equals(usingBundles[j]))
						result.add(services[i]);
			}
		}
		return (ServiceRegistration[]) result.toArray(new ServiceRegistration[result.size()]);
	}

	public boolean equals(Object obj) {
		return (obj instanceof Bundle) && (id.equals(((Bundle) obj).id));
	}

	public int hashCode() {
		return id.intValue();
	}

}
