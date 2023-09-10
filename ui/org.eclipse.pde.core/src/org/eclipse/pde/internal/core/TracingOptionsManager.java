/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Alena Laskavaia - Bug 453392 - No debug options help
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class TracingOptionsManager {
	private Map<String, String> template;

	public TracingOptionsManager() {
		super();
	}

	public Map<String, String> getTemplateTable(String pluginId) {
		Map<String, String> tracingTemplate = getTracingTemplate();
		Map<String, String> defaults = new HashMap<>();
		tracingTemplate.forEach((key, value) -> {
			if (belongsTo(key, pluginId)) {
				defaults.put(key, value);
			}
		});
		return defaults;
	}

	private boolean belongsTo(String option, String pluginId) {
		String firstSegment = IPath.fromOSString(option).segment(0);
		return pluginId.equalsIgnoreCase(firstSegment);
	}

	public Map<String, String> getTracingOptions(Map<String, String> storedOptions) {
		// Start with the fresh template from plugins
		Map<String, String> defaults = getTracingTemplateCopy();
		if (storedOptions != null) {
			// Load stored values, but only for existing keys
			storedOptions.forEach((key, value) -> {
				if (defaults.containsKey(key)) {
					defaults.put(key, value);
				}
			});
		}
		return defaults;
	}

	public Map<String, String> getTracingTemplateCopy() {
		return new HashMap<>(getTracingTemplate());
	}

	private synchronized Map<String, String> getTracingTemplate() {
		if (template == null) {
			Map<String, String> temp = new HashMap<>();
			IPluginModelBase[] models = PluginRegistry.getAllModels();
			Arrays.stream(models).map(TracingOptionsManager::getOptions).filter(Objects::nonNull).forEach(p -> {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Map<String, String> entries = (Map) p;
				temp.putAll(entries); // All entries are of String/String
			});
			template = temp;
		}
		return template;
	}

	public static boolean isTraceable(IPluginModelBase model) {
		String location = model.getInstallLocation();
		if (location == null) {
			return false;
		}

		File pluginLocation = new File(location);
		if (pluginLocation.isDirectory()) {
			return new File(pluginLocation, ICoreConstants.OPTIONS_FILENAME).exists();
		}
		try (ZipFile jarFile = new ZipFile(pluginLocation, ZipFile.OPEN_READ)) {
			ZipEntry manifestEntry = jarFile.getEntry(ICoreConstants.OPTIONS_FILENAME);
			if (manifestEntry != null) {
				try (InputStream stream = jarFile.getInputStream(manifestEntry)) {
					return stream != null;
				}
			}
		} catch (IOException e) {
		}
		return false;
	}

	public synchronized void reset() {
		template = null;
	}

	private void saveOptions(Path file, Map<String, String> entries) {
		Properties properties = new Properties();
		properties.putAll(entries);
		try (OutputStream stream = Files.newOutputStream(file);) {
			properties.store(stream, "Master Tracing Options"); //$NON-NLS-1$
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public void save(Path file, Map<String, String> map, Set<String> selected) {
		Map<String, String> properties = getTracingOptions(map);
		properties.keySet().removeIf(key -> {
			IPath path = IPath.fromOSString(key);
			return path.segmentCount() < 1 || !selected.contains(path.segment(0));
		});
		saveOptions(file, properties);
	}

	public void save(Path file, Map<String, String> map) {
		saveOptions(file, getTracingOptions(map));
	}

	private static Properties getOptions(IPluginModelBase model) {
		String location = model.getInstallLocation();
		if (location == null) {
			return null;
		}
		try {
			File pluginLocation = new File(location);
			Properties modelOptions = new Properties();
			if (pluginLocation.isDirectory()) {
				File file = new File(pluginLocation, ICoreConstants.OPTIONS_FILENAME);
				if (file.exists()) {
					try (InputStream stream = new FileInputStream(file)) {
						modelOptions.load(stream);
					}
					try (InputStream stream = new FileInputStream(file)) {
						loadComments(stream, modelOptions);
					}
					return modelOptions;
				}
			} else {
				try (ZipFile jarFile = new ZipFile(pluginLocation, ZipFile.OPEN_READ)) {
					ZipEntry manifestEntry = jarFile.getEntry(ICoreConstants.OPTIONS_FILENAME);
					if (manifestEntry != null) {
						try (InputStream stream = jarFile.getInputStream(manifestEntry)) {
							modelOptions.load(stream);
						}
						try (InputStream stream = jarFile.getInputStream(manifestEntry)) {
							loadComments(stream, modelOptions);
						}
						return modelOptions;
					}
				}
			}
		} catch (IOException e) {
			PDECore.logException(e);
		}
		return null;
	}

	/**
	 * Loads the comments from the properties files. This is simple version
	 * which won't cover 100% of the cases but hopefully cover 99%. It will find
	 * single or multiline comments starting with # (not !) and attach to the
	 * following property key by creating fake property with the key #key and
	 * value of the comment. Properties object which will receive these comments
	 * cannot be saved back properly without some special handling. It won't
	 * support cases when: key is split in multiple lines; key use escape
	 * characters; comment uses ! start char
	 */
	private static void loadComments(InputStream stream, Properties modelOptions) throws IOException {
		String prevComment = ""; //$NON-NLS-1$
		try (BufferedReader bufferedReader = new BufferedReader(
				// Properties.store() always uses ISO_8859_1 encoding
				new InputStreamReader(stream, StandardCharsets.ISO_8859_1))) {
			for (String line; (line = bufferedReader.readLine()) != null;) {
				if (line.startsWith("#") || line.trim().isEmpty()) { //$NON-NLS-1$
					prevComment += "\n" + line.trim(); //$NON-NLS-1$
				} else {
					if (prevComment.isBlank()) {
						continue;
					}
					int eq = line.indexOf('=');
					if (eq >= 0) {
						String key = line.substring(0, eq).trim();
						modelOptions.put("#" + key, prevComment.strip()); //$NON-NLS-1$
					}
					prevComment = ""; //$NON-NLS-1$
				}
			}
		}
	}
}
