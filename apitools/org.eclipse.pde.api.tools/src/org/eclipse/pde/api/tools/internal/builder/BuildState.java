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
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * The API tools build state
 * 
 * @since 1.0.1
 */
public class BuildState {
	private static final IDelta[] EMPTY_DELTAS = new IDelta[0];
	private static final String[] NO_REEXPORTED_COMPONENTS = new String[0];
	private static final int VERSION = 0x10;
	
	private Map compatibleChanges;
	private Map breakingChanges;
	private String[] reexportedComponents;
	private Set apiToolingDependentProjects;
	
	/**
	 * Constructor
	 */
	BuildState() {
		this.compatibleChanges = new HashMap();
		this.breakingChanges = new HashMap();
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
			int numberOfCompatibleDeltas = in.readInt();
			// read all compatible deltas
			for (int i = 0; i < numberOfCompatibleDeltas; i++) {
				state.addCompatibleChange(readDelta(in));
			}
			int numberOfBreakingDeltas = in.readInt();
			// read all breaking deltas
			for (int i = 0; i < numberOfBreakingDeltas; i++) {
				state.addBreakingChange(readDelta(in));
			}
			int numberOfReexportedComponents = in.readInt();
			// read all reexported component names
			String[] components = new String[numberOfReexportedComponents];
			for (int i = 0; i < numberOfReexportedComponents; i++) {
				components[i] = in.readUTF();
			}
			state.reexportedComponents = components;
			int numberOfApiToolingDependents = in.readInt();
			for (int i = 0; i < numberOfApiToolingDependents; i++) {
				state.addApiToolingDependentProject(in.readUTF());
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
		return new Delta(componentID, elementType, kind, flags, restrictions, oldModifiers, newModifiers, typeName, key, datas);
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
		out.writeInt(delta.getRestrictions());
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
					if (ApiAnalysisBuilder.DEBUG) {
						System.out.println("Saved state thinks last build failed for " + project.getName()); //$NON-NLS-1$
					}
					in.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new CoreException(new Status(IStatus.ERROR, JavaCore.PLUGIN_ID, Platform.PLUGIN_ERROR, "Error reading last build state for project "+ project.getName(), e)); //$NON-NLS-1$
			}
		} else if (ApiAnalysisBuilder.DEBUG) {
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
		if (ApiAnalysisBuilder.DEBUG) {
			System.out.println("Saving build state for project: "+project.getName()); //$NON-NLS-1$
		}
		File file = BuildState.getSerializationFile(project);
		if (file == null) return;
		long t = 0;
		if (ApiAnalysisBuilder.DEBUG) {
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
		if (ApiAnalysisBuilder.DEBUG) {
			t = System.currentTimeMillis() - t;
			System.out.println(NLS.bind(BuilderMessages.build_saveStateComplete, String.valueOf(t))); 
		}
	}
}
