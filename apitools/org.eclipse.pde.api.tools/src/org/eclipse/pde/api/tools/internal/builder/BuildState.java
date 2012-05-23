/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.CRC32;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;

/**
 * The API tools build state
 * 
 * @since 1.0.1
 */
public class BuildState {
	private static final IDelta[] EMPTY_DELTAS = new IDelta[0];
	private static final String[] NO_REEXPORTED_COMPONENTS = new String[0];
	private static final int VERSION = 0x20;
	
	private Map compatibleChanges;
	private Map breakingChanges;
	/**
	 * Map of the last saved state of the manifest file
	 * @since 1.0.3
	 */
	private Map manifestChanges;
	/**
	 * Map of the last saved state of the build.properties file
	 * @since 1.0.3
	 */
	private Map buildPropChanges;
	private String[] reexportedComponents;
	private Set apiToolingDependentProjects;
	private long buildpathCRC = -1L;
	
	/**
	 * Constructor
	 */
	BuildState() {
		this.compatibleChanges = new HashMap();
		this.breakingChanges = new HashMap();
		this.manifestChanges = new HashMap();
		this.buildPropChanges = new HashMap();
	}
	
	/**
	 * Reads the build state from an input stream
	 * @param in
	 * @return the {@link BuildState} from the given input stream
	 * @throws IOException
	 */
	 public static BuildState read(DataInputStream in) throws IOException {
		String pluginID= in.readUTF();
		if (!pluginID.equals(ApiPlugin.PLUGIN_ID)) {
			throw new IOException(BuilderMessages.build_wrongFileFormat);
		}
		String kind= in.readUTF();
		if (!kind.equals("STATE")) {//$NON-NLS-1$
			throw new IOException(BuilderMessages.build_wrongFileFormat);
		}
		if (in.readInt() != VERSION) {
			// this is an old build state - a full build is required
			return null;
		}
		if (in.readBoolean()) {
			// continue to read
			BuildState state = new BuildState();
			state.buildpathCRC = in.readLong();
			int count = in.readInt();
			// read all compatible deltas
			for (int i = 0; i < count; i++) {
				state.addCompatibleChange(readDelta(in));
			}
			count = in.readInt();
			// read all breaking deltas
			for (int i = 0; i < count; i++) {
				state.addBreakingChange(readDelta(in));
			}
			count = in.readInt();
			// read all re-exported component names
			String[] components = new String[count];
			for (int i = 0; i < count; i++) {
				components[i] = in.readUTF();
			}
			state.reexportedComponents = components;
			count = in.readInt();
			for (int i = 0; i < count; i++) {
				state.addApiToolingDependentProject(in.readUTF());
			}
			if(in.available() > 0) {
				count = in.readInt();
				if(count > 0) {
					//read the saved headers
					HashMap map = new HashMap(count);
					for(int i = 0; i < count; i++) {
						String key = in.readUTF();
						String value = in.readUTF();
						map.put(key, value);
					}
					state.setManifestState(map);
				}
				count = in.readInt();
				if(count > 0) {
					//read the saved headers
					HashMap map = new HashMap(count);
					for(int i = 0; i < count; i++) {
						String key = in.readUTF();
						String value = in.readUTF();
						map.put(key, value);
					}
					state.setBuildPropertiesState(map);
				}
			}
			return state;
		}
		return null;
	}
	 
	/**
	 * Writes the given {@link BuildState} to the given output stream
	 * @param state
	 * @param out
	 * @throws IOException
	 */
	public static void write(BuildState state, DataOutputStream out) throws IOException {
		out.writeUTF(ApiPlugin.PLUGIN_ID);
		out.writeUTF("STATE"); //$NON-NLS-1$
		out.writeInt(VERSION);
		out.writeBoolean(true);
		out.writeLong(state.buildpathCRC);
		IDelta[] compatibleChangesDeltas = state.getCompatibleChanges();
		int length = compatibleChangesDeltas.length;
		out.writeInt(length);
		for (int i = 0; i < length; i++) {
			writeDelta(compatibleChangesDeltas[i], out);
		}
		IDelta[] breakingChangesDeltas = state.getBreakingChanges();
		length = breakingChangesDeltas.length;
		out.writeInt(length);
		for (int i = 0; i < length; i++) {
			writeDelta(breakingChangesDeltas[i], out);
		}
		String[] reexportedComponents = state.getReexportedComponents();
		length = reexportedComponents.length;
		out.writeInt(length);
		for (int i = 0; i < length; i++) {
			out.writeUTF(reexportedComponents[i]);
		}
		Set apiToolingDependentsProjects = state.getApiToolingDependentProjects();
		length = apiToolingDependentsProjects.size();
		out.writeInt(length);
		for (Iterator iterator = apiToolingDependentsProjects.iterator(); iterator.hasNext(); ) {
			out.writeUTF((String) iterator.next());
		}
		Map map = state.getManifestState();
		out.writeInt(map.size());
		Entry entry = null;
		for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
			entry = (Entry) i.next();
			out.writeUTF((String) entry.getKey());
			out.writeUTF((String) entry.getValue());
		}
		map = state.getBuildPropertiesState();
		out.writeInt(map.size());
		entry = null;
		for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
			entry = (Entry) i.next();
			out.writeUTF((String) entry.getKey());
			out.writeUTF((String) entry.getValue());
		}
	}
	
	/**
	 * Read the {@link IDelta} from the build state (input stream)
	 * @param in the input stream to read the {@link IDelta} from
	 * @return a reconstructed {@link IDelta} from the build state
	 * @throws IOException
	 */
	private static IDelta readDelta(DataInputStream in) throws IOException {
		// decode the delta from the build state
		boolean hasComponentID = in.readBoolean();
		String componentID = null;
		if (hasComponentID) in.readUTF(); // delta.getComponentID()
		int elementType = in.readInt(); // delta.getElementType()
		int kind = in.readInt(); // delta.getKind()
		int flags = in.readInt(); // delta.getFlags()
		int restrictions = in.readInt(); // delta.getRestrictions()
		int modifiers = in.readInt(); // delta.getModifiers()
		String typeName = in.readUTF(); // delta.getTypeName()
		String key = in.readUTF(); // delta.getKey()
		int length = in.readInt(); // arguments.length;
		String[] datas = null;
		if (length != 0) {
			ArrayList arguments = new ArrayList();
			for (int i = 0; i < length; i++) {
				arguments.add(in.readUTF());
			}
			datas = new String[length];
			arguments.toArray(datas);
		} else {
			datas = new String[1];
			datas[0] = typeName.replace('$', '.');
		}
		int oldModifiers = modifiers & Delta.MODIFIERS_MASK;
		int newModifiers = modifiers >>> Delta.NEW_MODIFIERS_OFFSET;
		int previousRestrictions = restrictions >>> Delta.PREVIOUS_RESTRICTIONS_OFFSET;
		int currentRestrictions = restrictions & Delta.RESTRICTIONS_MASK;
		return new Delta(componentID, elementType, kind, flags, currentRestrictions, previousRestrictions, oldModifiers, newModifiers, typeName, key, datas);
	}
	
	/**
	 * Writes a given {@link IDelta} to the build state (the output stream)
	 * @param delta the delta to write
	 * @param out the stream to write to
	 * @throws IOException
	 */
	private static void writeDelta(IDelta delta, DataOutputStream out) throws IOException {
		// encode a delta into the build state
		// int elementType, int kind, int flags, int restrictions, int modifiers, String typeName, String key, Object data
		String apiComponentID = delta.getComponentVersionId();
		boolean hasComponentID = apiComponentID != null;
		out.writeBoolean(hasComponentID);
		if (hasComponentID) {
			out.writeUTF(apiComponentID);
		}
		out.writeInt(delta.getElementType());
		out.writeInt(delta.getKind());
		out.writeInt(delta.getFlags());
		out.writeInt(delta.getCurrentRestrictions());
		int modifiers = (delta.getNewModifiers() << Delta.NEW_MODIFIERS_OFFSET) | delta.getOldModifiers();
		out.writeInt(modifiers);
		out.writeUTF(delta.getTypeName());
		out.writeUTF(delta.getKey());
		String[] arguments = delta.getArguments();
		int length = arguments.length;
		out.writeInt(length);
		for (int i = 0; i < length; i++) {
			out.writeUTF(arguments[i]);
		}
	}

	/**
	 * Adds an {@link IDelta} for a compatible compatibility change to the current state
	 * 
	 * @param delta the {@link IDelta} to add to the state
	 */
	public void addCompatibleChange(IDelta delta) {
		String typeName = delta.getTypeName();
		Set object = (Set) this.compatibleChanges.get(typeName);
		if (object == null) {
			Set changes = new HashSet();
			changes.add(delta);
			this.compatibleChanges.put(typeName, changes);
		} else {
			object.add(delta);
		}
	}

	/**
	 * Add an {@link IDelta} for an incompatible compatibility change to the current state
	 * 
	 * @param delta the {@link IDelta} to add to the state
	 */
	public void addBreakingChange(IDelta delta) {
		String typeName = delta.getTypeName();
		Set object = (Set) this.breakingChanges.get(typeName);
		if (object == null) {
			Set changes = new HashSet();
			changes.add(delta);
			this.breakingChanges.put(typeName, changes);
		} else {
			object.add(delta);
		}
	}
	
	/**
	 * @return the complete list of recorded breaking changes with duplicates removed, or 
	 * an empty array, never <code>null</code>
	 */
	public IDelta[] getBreakingChanges() {
		if (this.breakingChanges == null || this.breakingChanges.size() == 0) {
			return EMPTY_DELTAS;
		}
		HashSet collector = new HashSet();
		Collection values = this.breakingChanges.values();
		for (Iterator iterator = values.iterator(); iterator.hasNext(); ) {
			collector.addAll((HashSet) iterator.next());
		}
		return (IDelta[]) collector.toArray(new IDelta[collector.size()]);
	}

	/**
	 * @return the complete list of recorded compatible changes with duplicates removed,
	 * or an empty array, never <code>null</code>
	 */
	public IDelta[] getCompatibleChanges() {
		if (this.compatibleChanges == null || this.compatibleChanges.size() == 0) {
			return EMPTY_DELTAS;
		}
		HashSet collector = new HashSet();
		Collection values = this.compatibleChanges.values();
		for (Iterator iterator = values.iterator(); iterator.hasNext(); ) {
			collector.addAll((HashSet) iterator.next());
		}
		return (IDelta[]) collector.toArray(new IDelta[collector.size()]);
	}

	/**
	 * @return the complete list of re-exported {@link IApiComponent}s
	 */
	public String[] getReexportedComponents() {
		if (this.reexportedComponents == null) {
			return NO_REEXPORTED_COMPONENTS;
		}
		return this.reexportedComponents;
	}
	
	/**
	 * Remove all entries for the given type name.
	 *
	 * @param typeName the given type name
	 */
	public void cleanup(String typeName) {
		this.breakingChanges.remove(typeName);
		this.compatibleChanges.remove(typeName);
		this.reexportedComponents = null;
	}

	/**
	 * Sets the current list if re-exported {@link IApiComponent}s for this build state
	 * @param components
	 */
	public void setReexportedComponents(IApiComponent[] components) {
		if (components == null) {
			return;
		}
		if (this.reexportedComponents == null) {
			final int length = components.length;
			String[] result = new String[length];
			for (int i = 0; i < length; i++) {
				result[i] = components[i].getSymbolicName();
			}
			this.reexportedComponents = result;
		}
	}

	/**
	 * Adds a dependent project to the listing of dependent projects
	 * @param projectName
	 */
	public void addApiToolingDependentProject(String projectName) {
		if (this.apiToolingDependentProjects == null) {
			this.apiToolingDependentProjects = new HashSet(3);
		}
		this.apiToolingDependentProjects.add(projectName);
	}
	
	/**
	 * @return the complete listing of dependent projects
	 */
	public Set getApiToolingDependentProjects() {
		return this.apiToolingDependentProjects == null ? Collections.EMPTY_SET : this.apiToolingDependentProjects;
	}
	
	/**
	 * Allows the last built state of the manifest to be saved. This method will perform compaction of the manifest,
	 * removing headers that we not need to care about.
	 * 
	 * @param state the last built state of the manifest
	 * @since 1.0.3
	 */
	public void setManifestState(Map state) {
		if(state != null) {
			Map compact = new HashMap(7);
			for (Iterator i = ApiAnalysisBuilder.IMPORTANT_HEADERS.iterator(); i.hasNext();) {
				String key = (String) i.next();
				Object val = state.get(key);
				if(val != null) {
					compact.put(key, val);
				}
			}
			this.manifestChanges = compact;
		}
		else {
			this.manifestChanges.clear();
		}
	}
	
	/**
	 * Returns the last saved state of the manifest or an empty {@link Map}, never <code>null</code>
	 * 
	 * @return the last built state of the manifest or an empty {@link Map}, never <code>null</code>
	 * @since 1.0.3
	 */
	public Map getManifestState() {
		return this.manifestChanges;
	}
	
	/**
	 * Allows the last built state of the build.properties file to be saved. This method will only save 
	 * entries that we care about, not an entire build.properties file snap-shot.
	 * <br><br>
	 * The retained entries are:
	 * <ul>
	 * <li>names that match: <code>custom</code></li>
	 * <li>names that start with: {@link IBuildEntry#JAR_PREFIX}</li>
	 * <li>names that start with: <code>extra.</code></li>
	 * </ul>
	 * 
	 * @param model the {@link IBuildModel} to save
	 * @since 1.0.3
	 */
	public void setBuildPropertiesState(IBuildModel model) {
		if(model != null) {
			IBuildEntry[] entries = model.getBuild().getBuildEntries();
			String name = null;
			for (int i = 0; i < entries.length; i++) {
				name = entries[i].getName();
				if(ProjectComponent.ENTRY_CUSTOM.equals(name)) {
					this.buildPropChanges.put(ProjectComponent.ENTRY_CUSTOM, Util.deepToString(entries[i].getTokens()));
				}
				else if(name.startsWith(IBuildEntry.JAR_PREFIX)) {
					this.buildPropChanges.put(name, Util.deepToString(entries[i].getTokens()));
				}
				else if(name.startsWith(ProjectComponent.EXTRA_PREFIX)) {
					this.buildPropChanges.put(name, Util.deepToString(entries[i].getTokens()));
				}
			}
		}
		else {
			this.buildPropChanges.clear();
		}
	}
	
	/**
	 * Allows the map to be reset to the given map, passing in <code>null</code> clears the current mapping.
	 * @param map the map to set
	 * @since 1.0.3
	 */
	void setBuildPropertiesState(Map map) {
		if(map != null) {
			this.buildPropChanges = map;
		}
		else {
			this.buildPropChanges.clear();
		}
	}
	
	/**
	 * Returns the last built state of the build.properties file or an empty {@link Map}, never <code>null</code>
	 * 
	 * @return the last built state of the build.properties file or an empty {@link Map}, never <code>null</code>
	 * @since 1.0.3
	 */
	public Map getBuildPropertiesState() {
		return this.buildPropChanges;
	}
	
	/**
	 * Returns a CRC32 code of the project's build path or -1 if unknown.
	 * 
	 * @return CRC32 code of the project's build path or -1
	 */
	public long getBuildPathCRC() {
		return buildpathCRC;
	}
	
	/**
	 * Sets the build path CRC for this project's resolved build path.
	 * 
	 * @param crc32 crc32 code
	 */
	public void setBuildPathCRC(long crc32) {
		buildpathCRC = crc32;
	}
	
	/**
	 * Return the last built state for the given project, or null if none
	 */
	public static BuildState getLastBuiltState(IProject project) throws CoreException {
		if (!Util.isApiProject(project)) {
			// should never be requested on non-Java projects
			return null;
		}
		return readState(project);
	}
	
	/**
	 * Reads the build state for the relevant project.
	 * @return the current {@link BuildState} for the given project or <code>null</code> if there is not one
	 */
	static BuildState readState(IProject project) throws CoreException {
		File file = getSerializationFile(project);
		if (file != null && file.exists()) {
			try {
				DataInputStream in= new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
				try {
					return read(in);
				} finally {
					if (ApiPlugin.DEBUG_BUILDER) {
						System.out.println("Saved state thinks last build failed for " + project.getName()); //$NON-NLS-1$
					}
					in.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, Platform.PLUGIN_ERROR, "Error reading last build state for project "+ project.getName(), e)); //$NON-NLS-1$
			}
		} else if (ApiPlugin.DEBUG_BUILDER) {
			if (file == null) {
				System.out.println("Project does not exist: " + project); //$NON-NLS-1$
			} else {
				System.out.println("Build state file " + file.getPath() + " does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return null;
	}
	
	/**
	 * Sets the last built state for the given project, or null to reset it.
	 * 
	 * @param project the project to set a state for
	 * @param state the {@link BuildState} to set as the last state
	 */
	public static void setLastBuiltState(IProject project, BuildState state) throws CoreException {
		if (Util.isApiProject(project)) {
			// should never be requested on non-Java projects
			if (state != null) {
				saveBuiltState(project, state);
			} else {
				try {
					File file = getSerializationFile(project);
					if (file != null && file.exists()) {
						file.delete();
					}
				} catch(SecurityException se) {
					// could not delete file: cannot do much more
				}
			}
		}
	}
	
	/**
	 * Returns the {@link File} to use for saving and restoring the last built state for the given project.
	 * 
	 * @param project gets the saved state file for the given project
	 * @return the {@link File} to use for saving and restoring the last built state for the given project.
	 */
	static File getSerializationFile(IProject project) {
		if (!project.exists()) {
			return null;
		}
		IPath workingLocation = project.getWorkingLocation(ApiPlugin.PLUGIN_ID);
		return workingLocation.append("state.dat").toFile(); //$NON-NLS-1$
	}
	
	/**
	 * Saves the current build state
	 * @param project
	 * @param state
	 * @throws CoreException
	 */
	static void saveBuiltState(IProject project, BuildState state) throws CoreException {
		if (ApiPlugin.DEBUG_BUILDER) {
			System.out.println("Saving build state for project: "+project.getName()); //$NON-NLS-1$
		}
		File file = BuildState.getSerializationFile(project);
		if (file == null) return;
		long t = 0;
		if (ApiPlugin.DEBUG_BUILDER) {
			t = System.currentTimeMillis();
		}
		try {
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			try {
				write(state, out);
			} finally {
				out.close();
			}
		} catch (RuntimeException e) {
			try {
				file.delete();
			} catch(SecurityException se) {
				// could not delete file: cannot do much more
			}
			throw new CoreException(
				new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, Platform.PLUGIN_ERROR,
					NLS.bind(BuilderMessages.build_cannotSaveState, project.getName()), e)); 
		} catch (IOException e) {
			try {
				file.delete();
			} catch(SecurityException se) {
				// could not delete file: cannot do much more
			}
			throw new CoreException(
				new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, Platform.PLUGIN_ERROR,
					NLS.bind(BuilderMessages.build_cannotSaveState, project.getName()), e)); 
		}
		if (ApiPlugin.DEBUG_BUILDER) {
			t = System.currentTimeMillis() - t;
			System.out.println(NLS.bind(BuilderMessages.build_saveStateComplete, String.valueOf(t))); 
		}
	}
	
	/**
	 * Computes and returns a CRC of the projects resolved build path, or -1 if unknown.
	 * 
	 * @param project project
	 * @return build path CRC or -1
	 */
	public static long computeBuildPathCRC(IProject project) {
		IJavaProject jp = JavaCore.create(project);
		try {
			IClasspathEntry[] classpath = jp.getResolvedClasspath(true);
			CRC32 crc32 = new CRC32();
			for (int i = 0; i < classpath.length; i++) {
				IClasspathEntry entry = classpath[i];
				crc32.update(entry.getPath().toPortableString().getBytes());
			}
			return crc32.getValue();
		} catch (JavaModelException e) {
		}
		return -1L;
	}
}
