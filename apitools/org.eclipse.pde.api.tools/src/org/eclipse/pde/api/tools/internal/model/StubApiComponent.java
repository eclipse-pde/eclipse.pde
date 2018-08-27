/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;

/**
 * An API component for a system library.
 *
 * @since 1.0.0
 */
public class StubApiComponent extends SystemLibraryApiComponent {
	private static final String STUB_PATH = "/org/eclipse/pde/api/tools/internal/api_stubs/"; //$NON-NLS-1$
	private static Map<String, IApiComponent> AllSystemLibraryApiComponents;

	public static IApiComponent getStubApiComponent(int eeValue) {
		if (AllSystemLibraryApiComponents == null) {
			AllSystemLibraryApiComponents = new LinkedHashMap<>();
		}
		String name = ProfileModifiers.getName(eeValue);
		IApiComponent component = AllSystemLibraryApiComponents.get(name);
		if (component == null) {
			// search if the corresponding stub file exists
			File stubFile = getFileFor(eeValue, name);
			if (stubFile == null) {
				return null;
			}
			component = new StubApiComponent(ApiBaselineManager.getManager().getWorkspaceBaseline(), stubFile.getAbsolutePath(), name);
			AllSystemLibraryApiComponents.put(name, component);
		}
		return component;
	}

	private static File getFileFor(int eeValue, String name) {
		try {
			String lname = name;
			switch (eeValue) {
				case ProfileModifiers.CDC_1_0_FOUNDATION_1_0:
				case ProfileModifiers.CDC_1_1_FOUNDATION_1_1:
				case ProfileModifiers.OSGI_MINIMUM_1_0:
				case ProfileModifiers.OSGI_MINIMUM_1_1:
				case ProfileModifiers.OSGI_MINIMUM_1_2:
					lname = lname.replace('/', '_');
					break;
				default:
					break;
			}
			String stubName = lname + ".zip"; //$NON-NLS-1$
			URL stub = null;
			if (Platform.isRunning()) {
				stub = ApiPlugin.getDefault().getBundle().getResource(STUB_PATH + stubName);
				if (stub == null) {
					return null;
				}
				stub = FileLocator.toFileURL(stub);
			} else {
				stub = ApiPlugin.class.getResource(STUB_PATH + stubName);
				if (stub == null) {
					return null;
				}
			}
			File stubFile = new File(stub.getFile());
			if (!stubFile.exists()) {
				return null;
			}
			return stubFile;
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns a listing of all of the installed meta-data or an empty array,
	 * never <code>null</code>
	 *
	 * @return list of installed meta-data or an empty list, never
	 *         <code>null</code>
	 */
	public static String[] getInstalledMetadata() {
		List<String> allEEs = new ArrayList<>();
		int[] allEEsValues = ProfileModifiers.getAllIds();
		String name = null;
		File stubFile = null;
		int eeValue = -1;
		for (int allEEsValue : allEEsValues) {
			eeValue = allEEsValue;
			name = ProfileModifiers.getName(eeValue);
			switch (eeValue) {
				case ProfileModifiers.CDC_1_0_FOUNDATION_1_0:
				case ProfileModifiers.CDC_1_1_FOUNDATION_1_1:
				case ProfileModifiers.OSGI_MINIMUM_1_0:
				case ProfileModifiers.OSGI_MINIMUM_1_1:
				case ProfileModifiers.OSGI_MINIMUM_1_2:
					name = name.replace('/', '_');
					break;
				default:
					break;
			}
			stubFile = getFileFor(eeValue, name);
			if (stubFile == null) {
				continue;
			}
			allEEs.add(ProfileModifiers.getName(eeValue));
		}
		String[] result = new String[allEEs.size()];
		allEEs.toArray(result);
		Arrays.sort(result);
		return result;
	}

	/**
	 * Constructs a system library from the given execution environment
	 * description file.
	 *
	 * @param baseline owning baseline
	 * @param fileName the file name that corresponds to the stub file for the
	 *            corresponding profile
	 * @param profileName the given profile name
	 * @exception CoreException if unable to read the execution environment
	 *                description file
	 */
	private StubApiComponent(IApiBaseline baseline, String fileName, String profileName) {
		super(baseline);
		IPath path = new Path(fileName);
		fLibraries = new LibraryLocation[] { new LibraryLocation(path, null, null) };
		fExecEnv = new String[] { profileName };
		fVersion = fExecEnv[0];
		setName(fExecEnv[0]);
		fLocation = path.toOSString();
	}

	@Override
	protected IApiDescription createApiDescription() throws CoreException {
		return null;
	}

	@Override
	protected IApiFilterStore createApiFilterStore() {
		// TODO
		return null;
	}

	@Override
	protected List<IApiTypeContainer> createApiTypeContainers() throws CoreException {
		List<IApiTypeContainer> libs = new ArrayList<>(fLibraries.length);
		for (LibraryLocation lib : fLibraries) {
			libs.add(new StubArchiveApiTypeContainer(this, lib.getSystemLibraryPath().toOSString()));
		}
		return libs;
	}

	@Override
	public boolean isSystemComponent() {
		return false;
	}

	public static void disposeAllCaches() {
		if (AllSystemLibraryApiComponents != null) {
			for (IApiComponent apiComponent : AllSystemLibraryApiComponents.values()) {
				apiComponent.dispose();
			}
		}
	}

	public static boolean isInstalled(int eeValue) {
		String name = ProfileModifiers.getName(eeValue);
		switch (eeValue) {
			case ProfileModifiers.CDC_1_0_FOUNDATION_1_0:
			case ProfileModifiers.CDC_1_1_FOUNDATION_1_1:
			case ProfileModifiers.OSGI_MINIMUM_1_0:
			case ProfileModifiers.OSGI_MINIMUM_1_1:
			case ProfileModifiers.OSGI_MINIMUM_1_2:
				name = name.replace('/', '_');
				break;
			default:
				break;
		}
		File stubFile = getFileFor(eeValue, name);
		return stubFile != null;
	}
}
