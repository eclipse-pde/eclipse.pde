/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

public class ModelDataTransfer extends ByteArrayTransfer {
	/**
	 * Singleton instance.
	 */
	private static final ModelDataTransfer instance = new ModelDataTransfer();
	// Create a unique ID to make sure that different Eclipse
	// applications use different "types" of <code>ModelDataTransfer</code>

	public static final String TYPE_PREFIX = "pde-model-transfer-format"; //$NON-NLS-1$
	private static final String TYPE_NAME = TYPE_PREFIX + ":" //$NON-NLS-1$
			+ System.currentTimeMillis() + ":" //$NON-NLS-1$
			+ instance.hashCode();

	private static final int TYPEID = registerType(TYPE_NAME);

	public static ModelDataTransfer getInstance() {
		return instance;
	}

	/**
	 * Constructor for ModelDataTransfer.
	 */
	public ModelDataTransfer() {
		super();
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	@Override
	protected void javaToNative(Object data, TransferData transferData) {
		if (!(data instanceof Object[] objects)) {
			return;
		}
		int count = objects.length;

		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
			//write the number of resources
			objectOut.writeInt(count);

			//write each object
			for (Object object : objects) {
				if (object instanceof IExecutionEnvironment ee) {
					object = new EESerializationSurogate(ee.getId());
				}
				objectOut.writeObject(object);
			}

			byte[] bytes = out.toByteArray();
			super.javaToNative(bytes, transferData);
		} catch (IOException e) {
			//it's best to send nothing if there were problems
			System.out.println(e);
		}

	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
		byte[] bytes = (byte[]) super.nativeToJava(transferData);
		if (bytes == null) {
			return null;
		}
		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			int count = in.readInt();
			Object[] objects = new Object[count];
			for (int i = 0; i < count; i++) {
				Object object = in.readObject();
				if (object instanceof EESerializationSurogate ee) {
					object = VMUtil.getExecutionEnvironment(ee.eeId());
				}
				objects[i] = object;
			}
			return objects;
		} catch (ClassNotFoundException | IOException e) {
			return null;
		}
	}

	private record EESerializationSurogate(String eeId) implements Serializable {
		// JDT doesn't want to make IExecutionEnvironment serializable, so we
		// need special handling in PDE:
		// https://github.com/eclipse-jdt/eclipse.jdt.debug/pull/456
	}
}
