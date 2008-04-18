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
package org.eclipse.pde.api.tools.internal.builder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

public class BuildState {
	private IDelta[] EMPTY_DELTAS = new IDelta[0];
	private static final int VERSION = 0x01;
	
	private Map compatibleChanges;
	private Map breakingChanges;
	
	public static BuildState read(IProject project, DataInputStream in) throws IOException {
		String pluginID= in.readUTF();
		if (!pluginID.equals(ApiPlugin.PLUGIN_ID))
			throw new IOException(BuilderMessages.build_wrongFileFormat); 
		String kind= in.readUTF();
		if (!kind.equals("STATE")) //$NON-NLS-1$
			throw new IOException(BuilderMessages.build_wrongFileFormat); 
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
			return state;
		}
		return null;
	}

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
		String data = null;
		if (length == 1) {
			data = in.readUTF(); // data
		} else {
			data = Util.EMPTY_STRING;
		}
		return new Delta(componentID, elementType, kind, flags, restrictions, modifiers, typeName, key, data);
	}

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
		int length2 = breakingChangesDeltas.length;
		out.writeInt(length2);
		for (int i = 0; i < length2; i++) {
			writeDelta(breakingChangesDeltas[i], out);
		}
	}

	private static void writeDelta(IDelta delta, DataOutputStream out) throws IOException {
		// encode a delta into the build state
		// int elementType, int kind, int flags, int restrictions, int modifiers, String typeName, String key, Object data
		String apiComponentID = delta.getApiComponentID();
		boolean hasComponentID = apiComponentID != null;
		out.writeBoolean(hasComponentID);
		if (hasComponentID) {
			out.writeUTF(apiComponentID);
		}
		out.writeInt(delta.getElementType());
		out.writeInt(delta.getKind());
		out.writeInt(delta.getFlags());
		out.writeInt(delta.getRestrictions());
		out.writeInt(delta.getModifiers());
		out.writeUTF(delta.getTypeName());
		out.writeUTF(delta.getKey());
		String[] arguments = delta.getArguments();
		int length = arguments.length;
		out.writeInt(length);
		for (int i = 0; i < length; i++) {
			out.writeUTF(arguments[i]);
		}
	}

	BuildState() {
		this.compatibleChanges = new HashMap();
		this.breakingChanges = new HashMap();
	}
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
	
	public IDelta[] getBreakingChanges() {
		if (this.breakingChanges == null || this.breakingChanges.size() == 0) {
			return EMPTY_DELTAS;
		}
		ArrayList collector = new ArrayList();
		Collection values = this.breakingChanges.values();
		for (Iterator iterator = values.iterator(); iterator.hasNext(); ) {
			HashSet set = (HashSet) iterator.next();
			for (Iterator iterator2 = set.iterator(); iterator2.hasNext(); ) {
				collector.add(iterator2.next());
			}
		}
		IDelta[] result = new IDelta[collector.size()];
		collector.toArray(result);
		return result;
	}

	public IDelta[] getCompatibleChanges() {
		if (this.compatibleChanges == null || this.compatibleChanges.size() == 0) {
			return EMPTY_DELTAS;
		}
		ArrayList collector = new ArrayList();
		Collection values = this.compatibleChanges.values();
		for (Iterator iterator = values.iterator(); iterator.hasNext(); ) {
			HashSet set = (HashSet) iterator.next();
			for (Iterator iterator2 = set.iterator(); iterator2.hasNext(); ) {
				collector.add(iterator2.next());
			}
		}
		IDelta[] result = new IDelta[collector.size()];
		collector.toArray(result);
		return result;
	}

	/**
	 * Remove all entries for the given type name.
	 *
	 * @param typeName the given type name
	 */
	public void cleanup(String typeName) {
		this.breakingChanges.remove(typeName);
		this.compatibleChanges.remove(typeName);
	}
}
