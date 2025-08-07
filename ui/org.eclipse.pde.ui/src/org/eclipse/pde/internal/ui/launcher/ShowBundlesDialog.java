package org.eclipse.pde.internal.ui.launcher;


import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class ShowBundlesDialog extends Dialog {
	Text fModuleArgumentsText;
	Map<IPluginModelBase, String> fModelsWithLevels = new HashMap<>();


	protected ShowBundlesDialog(Shell parentShell, Map<IPluginModelBase, String> modelsWithLevels) {
		super(parentShell);
		fModelsWithLevels = modelsWithLevels;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(PDEUIMessages.ShowBundlesDialog_LaunchBundles);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				PDEUIMessages.ShowBundlesDialog_Copy, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				PDEUIMessages.ShowBundlesDialog_Close, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		Font font = parent.getFont();

		Group group = new Group(comp, SWT.NONE);
		GridLayout topLayout = new GridLayout();
		group.setLayout(topLayout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(20);
		gd.widthHint = convertWidthInCharsToPixels(90);
		group.setLayoutData(gd);
		group.setFont(font);

		fModuleArgumentsText = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(10);
		gd.widthHint = convertWidthInCharsToPixels(60);
		fModuleArgumentsText.setLayoutData(gd);

		StringBuilder command = new StringBuilder();

		for (Map.Entry<IPluginModelBase, String> entry : fModelsWithLevels.entrySet()) {
			IPluginModelBase model = entry.getKey();
			String value = entry.getValue();
			int index = value.indexOf(':');
			String startLevel = value.substring(0, index);
			command.append(model.getBundleDescription().getSymbolicName()).append(", ") //$NON-NLS-1$
					.append(model.getBundleDescription().getVersion()).append(", ").append(model.getInstallLocation()) //$NON-NLS-1$
					.append(", ").append(startLevel).append("\n");  //$NON-NLS-1$//$NON-NLS-2$
		}

		fModuleArgumentsText.setText(command.toString());
		fModuleArgumentsText.setEditable(false);

		return comp;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			Clipboard clipboard = new Clipboard(null);
			try {
				TextTransfer textTransfer = TextTransfer.getInstance();
				Transfer[] transfers = new Transfer[] { textTransfer };
				Object[] data = new Object[] { fModuleArgumentsText.getText() };
				clipboard.setContents(data, transfers);
			} finally {
				clipboard.dispose();
			}
		}
		super.buttonPressed(buttonId);
	}

}
