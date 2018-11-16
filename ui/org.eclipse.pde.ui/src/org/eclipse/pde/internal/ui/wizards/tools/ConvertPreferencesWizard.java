/*******************************************************************************
 * Copyright (c) 2014, 2017 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;

/**
 * A wizard that can be used to convert preferences specified in an Eclipse Preferences File (.epf)
 * to a format used in a plugin_customization.ini file.
 *
 * @since 4.5
 */
public class ConvertPreferencesWizard extends Wizard {
	private ConvertPreferencesWizardPage page;
	private String fPreferencesFilePath;
	private String fPluginCustomizeFilePath;
	private boolean fOverwrite;

	/**
	 * Create a wizard to convert preferences specified in an Eclipse Preferences File (.epf)
	 * to a format used in a plugin_customization.ini file.
	 *
	 *  @param pluginCustomizationFilePath a String describing a fully qualified workspace path
	 *  to the plugin customization file that will receive the converted values. If the value does not
	 *  describe an existing file or is <code>null</code>, the wizard will open in an error state.
	 *
	 *  @param preferencesFilePath a String describing the fully qualified file system path to the Eclipse
	 *  Preferences File (.epf) whose values will be converted for use in the plugin customization
	 *  file. If the value does not describe an existing file or is <code>null</code>, the wizard will
	 *  open in an error state.
	 *
	 *  @param overwrite a boolean indicating whether the converted values should overwrite all
	 *  values in the plugin customization file or be merged with the existing values in the
	 *  plugin customization file.
	 *
	 * @since 4.5
	 */
	public ConvertPreferencesWizard(String pluginCustomizationFilePath, String preferencesFilePath, boolean overwrite) {
		this.fPreferencesFilePath = preferencesFilePath;
		this.fPluginCustomizeFilePath = pluginCustomizationFilePath;
		this.fOverwrite = overwrite;
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PRODUCT_WIZ);
		setWindowTitle(PDEUIMessages.ConvertPreferencesWizard_title);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		this.fPluginCustomizeFilePath = page.getPluginCustomizeFile().getFullPath().toString();
		File preferencesFile = page.getPreferencesFile();
		if (preferencesFile == null) {
			return false;
		}
		this.fPreferencesFilePath = preferencesFile.getAbsolutePath();
		this.fOverwrite = page.getOverwrite();
		try {
			getContainer().run(false, false, getGenerateOperation());
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getContainer().getShell(), PDEUIMessages.ConvertPreferencesWizard_errorTitle, e.getTargetException().getMessage());
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	@Override
	public void addPages() {
		page = new ConvertPreferencesWizardPage(fPluginCustomizeFilePath, fPreferencesFilePath, fOverwrite);
		addPage(page);
	}

	private IRunnableWithProgress getGenerateOperation() {
		return monitor -> {
			monitor.beginTask(PDEUIMessages.ConvertPreferencesWizard_progress, 100);
			File prefsFile = page.getPreferencesFile();
			if (prefsFile != null) {
				String sourceFilePath = prefsFile.getAbsolutePath();
				String errorFilePath = page.getPluginCustomizeFile().getFullPath().toString();
				if (prefsFile.exists()) {
					try {
						String lineSeparator = getLineSeparatorFromFile(errorFilePath);
						// Build properties from the EPF file, ignoring any scope strings.
						LinkedHashMap<String, String> properties = new LinkedHashMap<>();
						HashMap<String, String> mapKeyCommentPreference = new HashMap<>();
						try (BufferedReader in = new BufferedReader(new FileReader(sourceFilePath))) {
							String comment = null;
							String line1;
							while ((line1 = in.readLine()) != null) {
								if (line1.startsWith("#")) {//$NON-NLS-1$
									comment = (comment == null) ? line1 : comment.concat(line1);
									comment = comment.concat(lineSeparator);
									continue;
								}
								if (line1.equals("")) {//$NON-NLS-1$
									comment = (comment != null) ? comment.concat(lineSeparator)
											: "".concat(lineSeparator);//$NON-NLS-1$
									continue;
								}
								// ignore any lines that don't start with a separator
								if (line1.length() > 0 && line1.charAt(0) == IPath.SEPARATOR) {
									// find next separator so that we are skipping a scope such as /instance/ or /configuration/
									int index1 = line1.indexOf(IPath.SEPARATOR, 1);
									if (index1 > -1) {
										int keyIndex = line1.indexOf('=', index1);
										if (keyIndex > index1 + 1) {
											String key1 = line1.substring(index1 + 1, keyIndex).trim();
											String value1 = line1.substring(keyIndex + 1).trim();
											properties.put(key1, value1);
											mapKeyCommentPreference.put(key1, comment);
											comment = null;
										}
									}
								}
							}
						}
						monitor.worked(50);

						// If there are preferences to save, look for a plugin_customization.ini file and either
						// overwrite it or merge it.
						if (!properties.isEmpty()) {
							// Ensure file exists in case it was deleted since we validated
							IFile customizationFile = page.getPluginCustomizeFile();
							errorFilePath = customizationFile.getFullPath().toString();
							if (!customizationFile.exists()) {
								byte[] bytes = "".getBytes(); //$NON-NLS-1$
								InputStream source = new ByteArrayInputStream(bytes);
								customizationFile.create(source, IResource.NONE, null);
							}

							StringBuilder out = new StringBuilder();

							// merging old and new content
							if (!fOverwrite) {
								try (BufferedReader existingFile = new BufferedReader(
										new InputStreamReader(customizationFile.getContents()))) {
									String line;
									String comment = null;
									while ((line = existingFile.readLine()) != null) {
										if (line.startsWith("#")) {//$NON-NLS-1$
											comment = (comment == null) ? line : comment.concat(line);
											comment = comment.concat(lineSeparator);
											continue;
										}
										if (line.equals("")) {//$NON-NLS-1$
											comment = (comment != null) ? comment.concat(lineSeparator)
													: "".concat(lineSeparator);//$NON-NLS-1$
											continue;
										}
										int index = line.indexOf('=');
										if (index > 1) {
											String key = line.substring(0, index).trim();
											// If this key is not in the new preferences, then we want to preserve it.
											// If it is in the preferences, we'll be picking up the new value.
											if (!properties.containsKey(key)) {
												if (comment != null)
													out.append(comment);
												out.append(line);
												out.append(lineSeparator);
												comment = null;
											}
											else {
												// put current comment
												if (comment != null)
													out.append(comment);
												// put comment from epf file
												String comment2 = null;
												comment2 = mapKeyCommentPreference.get(key);
												if (comment2 != null)
													out.append(comment2);
												// get the value from epf file
												Object value = properties.get(key);
												out.append(key + "=" + value); //$NON-NLS-1$
												out.append(lineSeparator);
												properties.remove(key);
												comment = null;
											}
										} else {
											PDEPlugin.log(NLS.bind(PDEUIMessages.ConvertPreferencesWizard_skippedInvalidLine, fPreferencesFilePath));
											PDEPlugin.log("    " + line); //$NON-NLS-1$
										}
									}
								}
							}
							monitor.worked(20);
							for (Entry<String, String> entry : properties.entrySet()) {
								String key = entry.getKey();
								String comment = null;
								comment = mapKeyCommentPreference.get(key);
								if (comment != null)
									out.append(comment);
								String value = entry.getValue();
								out.append(key + "=" + value); //$NON-NLS-1$
								out.append(lineSeparator);
							}
							// now write the (possibly merged) values from the string builder to the file
							InputStream resultStream = new ByteArrayInputStream(out.toString().getBytes());
							customizationFile.setContents(resultStream, true, true, null);
							monitor.worked(30);
						} else {
							page.setMessage(PDEUIMessages.ConvertPreferencesWizard_errorNoPrefs, IMessageProvider.WARNING);
						}
					} catch (FileNotFoundException e1) {
						page.setErrorMessage(NLS.bind(PDEUIMessages.ConvertPreferencesWizard_errorFile, errorFilePath));
						PDEPlugin.logException(e1);
						return;
					} catch (CoreException e2) {
						page.setErrorMessage(NLS.bind(PDEUIMessages.ConvertPreferencesWizard_errorFile, errorFilePath));
						PDEPlugin.logException(e2);
						return;
					} catch (IOException e3) {
						page.setErrorMessage(NLS.bind(PDEUIMessages.ConvertPreferencesWizard_errorFile, errorFilePath));
						PDEPlugin.logException(e3);
						return;
					} finally {
						monitor.done();
					}
				} else {
					page.setErrorMessage(PDEUIMessages.ConvertPreferencesWizard_errorFileNotFound);
				}
			} else {
				page.setErrorMessage(PDEUIMessages.ConvertPreferencesWizard_errorNoFileSpecified);
			}
		};
	}

	private String getLineSeparatorFromFile(String filePath) throws CoreException {
		IPath path = Path.fromOSString(filePath);
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		ITextFileBuffer buffer = manager.getTextFileBuffer(path, LocationKind.LOCATION);

		Boolean pathNeededConnection = buffer == null;
		if (pathNeededConnection) {
			manager.connect(path, LocationKind.LOCATION, null);
			buffer = manager.getTextFileBuffer(path, LocationKind.LOCATION);
		}

		String lineSeparator;
		if (buffer != null) {
			lineSeparator = TextUtilities.getDefaultLineDelimiter(buffer.getDocument());
		} else {
			lineSeparator = System.lineSeparator();
		}

		if (pathNeededConnection) {
			manager.disconnect(path, LocationKind.LOCATION, null);
		}

		return lineSeparator;
	}

	public String getPreferencesFilePath() {
		return fPreferencesFilePath;
	}

	public String getPluginCustomizationFilePath() {
		return fPluginCustomizeFilePath;
	}

	public boolean getOverwrite() {
		return fOverwrite;
	}
}
