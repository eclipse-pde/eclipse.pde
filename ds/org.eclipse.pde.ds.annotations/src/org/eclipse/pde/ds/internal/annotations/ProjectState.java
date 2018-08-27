/*******************************************************************************
 * Copyright (c) 2012, 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;

public class ProjectState implements Serializable, Cloneable {

	private static final long serialVersionUID = 8616641822921441882L;

	// current state file format version
	public static final int FORMAT_VERSION = 1;

	// package-prefixed CU name (w/out file extension) to plugin-root-relative (portable) paths of generated DS files (deprecated)
	// note: we keep it non-null in case user downgrades to older plugin version where old logic depends on that
	private /*final*/ Map<String, Collection<String>> mappings = new HashMap<>();

	private String path;

	private DSAnnotationVersion specVersion;

	private ValidationErrorLevel errorLevel;

	private ValidationErrorLevel missingUnbindMethodLevel;

	// package fragment root-relative CU path to fully-qualified component types contained in the CU
	private Map<String, Collection<String>> types;

	// fully-qualified component type to plugin-root-relative (portable) path of corresponding generated DS file
	private Map<String, String> files;

	// (de)serialized state file format version
	private int formatVersion = FORMAT_VERSION;

	public int getFormatVersion() {
		return formatVersion;
	}

	public void setFormatVersion(int formatVersion) {
		this.formatVersion = formatVersion;
	}

	public Collection<String> getCompilationUnits() {
		if (types == null) {
			// fall back to (deprecated) mappings
			ArrayList<String> translated = new ArrayList<>(mappings.keySet());
			for (ListIterator<String> i = translated.listIterator(); i.hasNext();) {
				i.set(fromLegacyCUKey(i.next()));
			}

			return translated;
		}

		return Collections.unmodifiableCollection(types.keySet());
	}

	private String fromLegacyCUKey(String cuKey) {
		return String.format("%s.java", cuKey.replace('.', '/')); //$NON-NLS-1$
	}

	public Collection<String> removeMappings(String cuKey) {
		if (types == null) {
			// fall back to (deprecated) mappings
			return mappings.remove(toLegacyCUKey(cuKey));
		}

		Collection<String> cuTypes = types.remove(cuKey);
		if (cuTypes == null) {
			return null;
		}

		Collection<String> oldDSKeys = null;
		if (files != null) {
			oldDSKeys = new HashSet<>(cuTypes.size());
			for (String type : cuTypes) {
				String dsKey = files.remove(type);
				if (dsKey != null) {
					oldDSKeys.add(dsKey);
				}
			}
		}

		return oldDSKeys;
	}

	public Collection<String> getModelFiles(String cuKey) {
		if (types == null) {
			// fall back to (deprecated) mappings
			Collection<String> files = mappings.get(toLegacyCUKey(cuKey));
			return files == null ? null : Collections.unmodifiableCollection(files);
		}

		Collection<String> cuTypes = types.get(cuKey);
		if (cuTypes == null || files == null) {
			return null;
		}

		HashSet<String> cuFiles = new HashSet<>(cuTypes.size());
		for (String type : cuTypes) {
			String dsKey = files.get(type);
			if (dsKey != null) {
				cuFiles.add(dsKey);
			}
		}

		return Collections.unmodifiableCollection(cuFiles);
	}

	private Object toLegacyCUKey(String cuKey) {
		return JavaCore.removeJavaLikeExtension(cuKey).replace('.', '/');
	}

	public String getModelFile(String className) {
		return files == null ? null : files.get(className);
	}

	public Collection<String> updateMappings(String cuKey, HashMap<String, String> dsKeys) {
		Collection<String> oldDSKeys = removeMappings(cuKey);
		if (!dsKeys.isEmpty()) {
			getTypes().put(cuKey, new HashSet<>(dsKeys.keySet()));
			getFiles().putAll(dsKeys);
		}

		return oldDSKeys;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public DSAnnotationVersion getSpecVersion() {
		return specVersion == null ? DSAnnotationVersion.V1_3 : specVersion;
	}

	public void setSpecVersion(DSAnnotationVersion specVersion) {
		this.specVersion = specVersion;
	}

	public ValidationErrorLevel getErrorLevel() {
		return errorLevel == null ? ValidationErrorLevel.error : errorLevel;
	}

	public void setErrorLevel(ValidationErrorLevel errorLevel) {
		this.errorLevel = errorLevel;
	}

	public ValidationErrorLevel getMissingUnbindMethodLevel() {
		return missingUnbindMethodLevel == null ? getErrorLevel() : missingUnbindMethodLevel;
	}

	public void setMissingUnbindMethodLevel(ValidationErrorLevel missingUnbindMethodLevel) {
		this.missingUnbindMethodLevel = missingUnbindMethodLevel;
	}

	private Map<String, Collection<String>> getTypes() {
		if (types == null) {
			types = new HashMap<>();
		}

		return types;
	}

	private Map<String, String> getFiles() {
		if (files == null) {
			files = new HashMap<>();
		}

		return files;
	}

	@Override
	public ProjectState clone() {
		ProjectState clone;
		try {
			clone = (ProjectState) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new UnsupportedOperationException();
		}

		clone.mappings = new HashMap<>(mappings.size());
		for (Map.Entry<String, Collection<String>> entry : mappings.entrySet()) {
			clone.mappings.put(entry.getKey(), new HashSet<>(entry.getValue()));
		}

		if (types != null) {
			clone.types = new HashMap<>(types.size());
			for (Map.Entry<String, Collection<String>> entry : types.entrySet()) {
				clone.types.put(entry.getKey(), new HashSet<>(entry.getValue()));
			}
		}

		if (files != null) {
			clone.files = new HashMap<>(files);
		}

		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}

		ProjectState o = (ProjectState) obj;
		return formatVersion == o.formatVersion
				&& (path == null ? o.path == null : path.equals(o.path))
				&& specVersion == o.specVersion
				&& errorLevel == o.errorLevel
				&& missingUnbindMethodLevel == o.missingUnbindMethodLevel
				&& mappings.equals(o.mappings)
				&& (files == null ? o.files == null : files.equals(o.files))
				&& (types == null ? o.types == null : types.equals(o.types));
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("ProjectState[path="); //$NON-NLS-1$
		buf.append(path).append(";mappings="); //$NON-NLS-1$
		buf.append(mappings).append(";types="); //$NON-NLS-1$
		buf.append(types).append(";files="); //$NON-NLS-1$
		buf.append(files).append(";errorLevel="); //$NON-NLS-1$
		buf.append(specVersion).append(";specVersion="); //$NON-NLS-1$
		buf.append(errorLevel).append(";missingUnbindMethodLevel="); //$NON-NLS-1$
		buf.append(missingUnbindMethodLevel).append(";formatVersion="); //$NON-NLS-1$
		buf.append(formatVersion).append(']');
		return buf.toString();
	}
}
