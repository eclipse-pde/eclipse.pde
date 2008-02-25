/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

public class BundleHelper {
	private Bundle bundle;
	private BundleContext context;
	private static BundleHelper defaultInstance;
	private boolean debug = false;
	private ILog log = null;
	
	public static BundleHelper getDefault() {
		return defaultInstance;
	}

	static void close() {
		if (defaultInstance != null) {
			defaultInstance.context = null;
			defaultInstance.bundle = null;
			defaultInstance = null;
		}
	}

	BundleHelper(BundleContext context) throws RuntimeException {
		if (defaultInstance != null)
			throw new RuntimeException("Can not instantiate bundle helper"); //$NON-NLS-1$
		this.context = context;
		defaultInstance = this;
		bundle = context.getBundle();
		debug = "true".equalsIgnoreCase(Platform.getDebugOption(IPDEBuildConstants.PI_PDEBUILD + "/debug")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public final URL find(IPath path) {
		return FileLocator.find(bundle, path, null);
	}

	public final URL find(IPath path, Map override) {
		return FileLocator.find(bundle, path, override);
	}

	public final ILog getLog() {
		if (log == null)
			return Platform.getLog(bundle);
		return log;
	}

	public final IPath getStateLocation() throws IllegalStateException {
		return Platform.getStateLocation(getDefault().bundle);
	}

	public final InputStream openStream(IPath file) throws IOException {
		return FileLocator.openStream(bundle, file, false);
	}

	public final InputStream openStream(IPath file, boolean localized) throws IOException {
		return FileLocator.openStream(bundle, file, localized);
	}

	public String toString() {
		return bundle.getSymbolicName();
	}

	public Bundle getBundle() {
		return bundle;
	}

	public Object acquireService(String serviceName) {
		ServiceReference reference = context.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return context.getService(reference);
	}

	public boolean isDebugging() {
		return debug;
	}
	
	public Filter createFilter(String filter) {
		try {
			return context.createFilter(filter);
		} catch (InvalidSyntaxException e) {
		    //Ignore, this has been caught when resolving the state.
			return null;
		}
	}
	
	public void setLog(Object antLog) {
		if (antLog == null) {
			log = null;
			return;
		}
		
		try {
			log = new AntLogAdapter(antLog);
		} catch (NoSuchMethodException e) {
			log = null;
		}
	}
	
	public String[] getRuntimeJavaProfiles() {
		//BundleContext context = BundleHelper.getDefault().getBundle().getBundleContext();
		Bundle systemBundle = context.getBundle(0);

		URL url = systemBundle.getEntry("profile.list"); //$NON-NLS-1$
		if (url != null) {
			try {
				return getJavaProfiles(new BufferedInputStream(url.openStream()));
			} catch (IOException e) {
				//no profile.list?
			}
		}

		ArrayList results = new ArrayList(6);
		Enumeration entries = systemBundle.findEntries("/", "*.profile", false); //$NON-NLS-1$ //$NON-NLS-2$
		while (entries.hasMoreElements()) {
			URL entryUrl = (URL) entries.nextElement();
			results.add(entryUrl.getFile().substring(1));
		}

		return sortProfiles((String[]) results.toArray(new String[results.size()]));
	}
	
	public static String[] getJavaProfiles(InputStream is) throws IOException {
		Properties props = new Properties();
		props.load(is);
		return ManifestElement.getArrayFromList(props.getProperty("java.profiles"), ","); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static String[] sortProfiles(String[] profiles) {
		Arrays.sort(profiles, new Comparator() {
			public int compare(Object profile1, Object profile2) {
				// need to make sure JavaSE, J2SE profiles are sorted ahead of all other profiles
				String p1 = (String) profile1;
				String p2 = (String) profile2;
				if (p1.startsWith("JavaSE") && !p2.startsWith("JavaSE")) //$NON-NLS-1$ //$NON-NLS-2$
					return -1;
				if (!p1.startsWith("JavaSE") && p2.startsWith("JavaSE")) //$NON-NLS-1$ //$NON-NLS-2$
					return 1;
				if (p1.startsWith("J2SE") && !p2.startsWith("J2SE")) //$NON-NLS-1$ //$NON-NLS-2$
					return -1;
				if (!p1.startsWith("J2SE") && p2.startsWith("J2SE")) //$NON-NLS-1$ //$NON-NLS-2$
					return 1;
				return -p1.compareTo(p2);
			}
		});
		return profiles;
	}

}
