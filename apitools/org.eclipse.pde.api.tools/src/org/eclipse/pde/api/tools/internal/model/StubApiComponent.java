/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
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

/**
 * An API component for a system library.
 * 
 * @since 1.0.0
 */
public class StubApiComponent extends SystemLibraryApiComponent {
	private static final String API_STUBS_FOLDER_NAME = ".api_stubs"; //$NON-NLS-1$
	private static final String VERSION_FILE_NAME = "version"; //$NON-NLS-1$
	public static Map AllSystemLibraryApiComponents;
	public static final int STUB_VERSION = 1;
	private static final int DEFAULT_READING_SIZE = 8192;
	private static final int DEFAULT_WRITING_SIZE = 8192;

	public static IApiComponent getStubApiComponent(int eeValue) {
		if (AllSystemLibraryApiComponents == null) {
			AllSystemLibraryApiComponents = new HashMap();
		}
		String name = ProfileModifiers.getName(eeValue);
		switch(eeValue) {
			case ProfileModifiers.CDC_1_0_FOUNDATION_1_0 :
			case ProfileModifiers.CDC_1_1_FOUNDATION_1_1 :
			case ProfileModifiers.OSGI_MINIMUM_1_0 :
			case ProfileModifiers.OSGI_MINIMUM_1_1 :
			case ProfileModifiers.OSGI_MINIMUM_1_2 :
				name = name.replace('/', '_');
		}
		IApiComponent component = (IApiComponent) AllSystemLibraryApiComponents.get(name);
		if (component == null) {
			File stubFolder = null;
			if (Platform.isRunning()) {
				IPath path = ApiPlugin.getDefault().getStateLocation().append(API_STUBS_FOLDER_NAME).addTrailingSeparator();
				stubFolder = path.toFile();
			} else {
				File tempFolderRoot = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
				stubFolder = new File(tempFolderRoot, API_STUBS_FOLDER_NAME);
			}
			if (!stubFolder.exists()) {
				stubFolder.mkdirs();
				// dump the stub version file
				dumpStubVersionFile(stubFolder);
			} else {
				// check stub version
				if (checkStubVersionFile(stubFolder)) {
					// delete all files
					File[] files = stubFolder.listFiles();
					for (int i = 0; i < files.length; i++) {
						File file = files[i];
						file.delete();
					}
					// save the new value
					dumpStubVersionFile(stubFolder);
				}
			}
			// search if the corresponding stub file exists
			String stubName = name + ".zip"; //$NON-NLS-1$
			File stub = new File(stubFolder, stubName);
			if (!stub.exists()) {
				dumpStubToCache(stubName, stub);
			}
			component = new StubApiComponent(ApiBaselineManager.getManager().getWorkspaceBaseline(), stub.getAbsolutePath(), name);
			AllSystemLibraryApiComponents.put(name, component);
		}
		return component;
	}

	private static void dumpStubToCache(String stubName, File stub) {
		// extract the stub file from the bundle
		BufferedInputStream inputStream = null;
		BufferedOutputStream outputStream = null;
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(stub);
			outputStream = new BufferedOutputStream(fileOutputStream, DEFAULT_WRITING_SIZE);
			inputStream = new BufferedInputStream(ApiPlugin.class.getResourceAsStream("/org/eclipse/pde/api/tools/internal/util/profiles/api_stubs/" + stubName)); //$NON-NLS-1$
			int amountRead = -1;
			byte[] buffer = new byte[DEFAULT_READING_SIZE];
			int bufferLength = DEFAULT_READING_SIZE;
			do {
				int amountRequested = Math.max(inputStream.available(), DEFAULT_READING_SIZE);  // read at least 8K

				// resize contents if needed
				if (amountRequested > bufferLength) {
					buffer = new byte[amountRequested];
					bufferLength = amountRequested;
				}

				// read as many bytes as possible
				amountRead = inputStream.read(buffer, 0, amountRequested);

				if (amountRead > 0) {
					outputStream.write(buffer, 0, amountRead);
				}
			} while (amountRead != -1);
		} catch(IOException e) {
			// report error
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
			if (outputStream != null) {
				try {
					outputStream.flush();
				} catch (IOException e) {
					// ignore
				}
				try {
					outputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	private static boolean checkStubVersionFile(File stubFolder) {
		File stubVersion = new File(stubFolder, VERSION_FILE_NAME);
		DataInputStream dataInputStream = null;
		try {
			dataInputStream = new DataInputStream(new FileInputStream(stubVersion));
			return STUB_VERSION != dataInputStream.readInt();
		} catch (IOException e) {
			// ignore
		} finally {
			if (dataInputStream != null) {
				try {
					dataInputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return true;
	}

	private static void dumpStubVersionFile(File stubFolder) {
		File stubVersion = new File(stubFolder, VERSION_FILE_NAME);
		BufferedOutputStream output = null;
		try {
			DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(stubVersion));
			output = new BufferedOutputStream(dataOutputStream);
			dataOutputStream.writeInt(STUB_VERSION);
			output.flush();
		} catch (IOException e) {
			// ignore
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	/**
	 * Constructs a system library from the given execution environment description file.
	 * 
	 * @param profile owning profile
	 * @param fileName the file name that corresponds to the stub file for the corresponding profile
	 * @param profileName the given profile name
	 * @exception CoreException if unable to read the execution environment description file
	 */
	private StubApiComponent(IApiBaseline profile, String fileName, String profileName) {
		super(profile);
		IPath path = new Path(fileName);
		fLibraries = new LibraryLocation[] { new LibraryLocation(path, null, null) };
		fExecEnv = new String[]{ profileName };
		fVersion = fExecEnv[0];
		setName(fExecEnv[0]);
		fLocation = path.toOSString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#createApiDescription()
	 */
	protected IApiDescription createApiDescription() throws CoreException {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiComponent#createApiFilterStore()
	 */
	protected IApiFilterStore createApiFilterStore() {
		//TODO
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#createClassFileContainers()
	 */
	protected List createApiTypeContainers() throws CoreException {
		List libs = new ArrayList(fLibraries.length);
		for (int i = 0; i < fLibraries.length; i++) {
			LibraryLocation lib = fLibraries[i];
			libs.add(new StubArchiveApiTypeContainer(this, lib.getSystemLibraryPath().toOSString()));
		}
		return libs;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent#isSystemComponent()
	 */
	public boolean isSystemComponent() {
		return false;
	}
	public static void disposeAllCaches() {
		if (AllSystemLibraryApiComponents != null) {
			for (Iterator iterator = AllSystemLibraryApiComponents.values().iterator(); iterator.hasNext(); ) {
				IApiComponent apiComponent = (IApiComponent) iterator.next();
				apiComponent.dispose();
			}
		}
	}
}
