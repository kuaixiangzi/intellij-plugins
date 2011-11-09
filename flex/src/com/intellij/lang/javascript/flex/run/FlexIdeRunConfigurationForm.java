package com.intellij.lang.javascript.flex.run;

import com.intellij.execution.ExecutionBundle;
import com.intellij.ide.ui.ListCellRendererWrapper;
import com.intellij.lang.javascript.flex.FlexModuleType;
import com.intellij.lang.javascript.flex.build.FlexCompilerSettingsEditor;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfigurationManager;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexIdeBuildConfiguration;
import com.intellij.lang.javascript.flex.projectStructure.model.OutputType;
import com.intellij.lang.javascript.flex.projectStructure.model.TargetPlatform;
import com.intellij.lang.javascript.refactoring.ui.JSReferenceEditor;
import com.intellij.lang.javascript.ui.JSClassChooserDialog;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class FlexIdeRunConfigurationForm extends SettingsEditor<FlexIdeRunConfiguration> {

  private JPanel myMainPanel;
  private JComboBox myBCsCombo;

  private JCheckBox myOverrideMainClassCheckBox;
  private JSClassChooserDialog.PublicInheritor myMainClassFilter;
  private JSReferenceEditor myMainClassComponent;
  private JLabel myOutputFileNameLabel;
  private JTextField myOutputFileNameTextField;

  private JPanel myLaunchPanel;
  private JRadioButton myBCOutputRadioButton;
  private JLabel myBCOutputLabel;
  private JRadioButton myURLRadioButton;
  private JTextField myURLTextField;

  private JPanel myWebOptionsPanel;
  private TextFieldWithBrowseButton myLauncherParametersTextWithBrowse;
  private JCheckBox myRunTrustedCheckBox;

  private JPanel myDesktopOptionsPanel;
  private RawCommandLineEditor myAdlOptionsEditor;
  private RawCommandLineEditor myAirProgramParametersEditor;

  private JPanel myMobileRunPanel;
  private JRadioButton myOnEmulatorRadioButton;
  private JRadioButton myOnAndroidDeviceRadioButton;
  private JRadioButton myOnIOSDeviceRadioButton;

  private JComboBox myEmulatorCombo;
  private JPanel myEmulatorScreenSizePanel;
  private JTextField myScreenWidth;
  private JTextField myScreenHeight;
  private JTextField myFullScreenWidth;
  private JTextField myFullScreenHeight;

  private JPanel myMobileOptionsPanel;
  private JPanel myDebugTransportPanel;
  private JLabel myDebugOverLabel;
  private JRadioButton myDebugOverNetworkRadioButton;
  private JRadioButton myDebugOverUSBRadioButton;
  private JTextField myUsbDebugPortTextField;
  private JBLabel myAdlOptionsLabel;
  private RawCommandLineEditor myEmulatorAdlOptionsEditor;

  private final Project myProject;
  private FlexIdeBuildConfiguration[] myAllConfigs;
  private boolean mySingleModuleProject;
  private Map<FlexIdeBuildConfiguration, Module> myBCToModuleMap;

  private LauncherParameters myLauncherParameters;

  public FlexIdeRunConfigurationForm(final Project project) {
    myProject = project;

    initBCCombo();
    initMainClassRelatedControls();
    initRadioButtons();
    initLaunchWithTextWithBrowse();
    initMobileControls();
  }

  private void initBCCombo() {
    myBCToModuleMap = new THashMap<FlexIdeBuildConfiguration, Module>();

    final Module[] modules = ModuleManager.getInstance(myProject).getModules();
    mySingleModuleProject = modules.length == 1;
    for (final Module module : modules) {
      if (ModuleType.get(module) instanceof FlexModuleType) {
        for (final FlexIdeBuildConfiguration config : FlexBuildConfigurationManager.getInstance(module).getBuildConfigurations()) {
          myBCToModuleMap.put(config, module);
        }
      }
    }

    myAllConfigs = myBCToModuleMap.keySet().toArray(new FlexIdeBuildConfiguration[myBCToModuleMap.size()]);

    myBCsCombo.setRenderer(new ListCellRendererWrapper(myBCsCombo.getRenderer()) {
      @Override
      public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof Pair) {
          final String moduleName = (String)((Pair)value).first;
          final String configName = (String)((Pair)value).second;
          setIcon(PlatformIcons.ERROR_INTRODUCTION_ICON);
          setText("<html><font color='red'>" + getPresentableText(moduleName, configName, mySingleModuleProject) + "</font></html>");
        }
        else {
          assert value instanceof FlexIdeBuildConfiguration : value;
          final FlexIdeBuildConfiguration config = (FlexIdeBuildConfiguration)value;
          setIcon(config.getIcon());
          setText(getPresentableText(myBCToModuleMap.get(config).getName(), config.getName(), mySingleModuleProject));
        }
      }
    });

    myBCsCombo.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        // remove invalid entry
        final Object selectedItem = myBCsCombo.getSelectedItem();
        final Object firstItem = myBCsCombo.getItemAt(0);
        if (selectedItem instanceof FlexIdeBuildConfiguration && !(firstItem instanceof FlexIdeBuildConfiguration)) {
          myBCsCombo.setModel(new DefaultComboBoxModel(myAllConfigs));
          myBCsCombo.setSelectedItem(selectedItem);
        }

        updateMainClassField();
        updateControls();
      }
    });
  }

  private void updateMainClassField() {
    final Object selectedItem = myBCsCombo.getSelectedItem();
    if (selectedItem instanceof FlexIdeBuildConfiguration) {
      final Module module = myBCToModuleMap.get((FlexIdeBuildConfiguration)selectedItem);
      myMainClassComponent.setScope(GlobalSearchScope.moduleScope(module));
      myMainClassFilter.setModule(module);
      myMainClassComponent.setChooserBlockingMessage(null);
    }
    else {
      myMainClassComponent.setScope(GlobalSearchScope.EMPTY_SCOPE);
      myMainClassFilter.setModule(null);
      myMainClassComponent.setChooserBlockingMessage("Build configuration not selected");
    }
  }

  private void initMainClassRelatedControls() {
    myOverrideMainClassCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (myOverrideMainClassCheckBox.isSelected()) {
          FlexCompilerSettingsEditor.updateOutputFileName(myOutputFileNameTextField, false);
        }

        updateControls();

        if (myMainClassComponent.isEnabled()) {
          IdeFocusManager.getInstance(myProject).requestFocus(myMainClassComponent.getChildComponent(), true);
        }
      }
    });

    myMainClassComponent.addDocumentListener(new DocumentAdapter() {
      public void documentChanged(final DocumentEvent e) {
        final String shortName = StringUtil.getShortName(myMainClassComponent.getText().trim());
        if (!shortName.isEmpty()) {
          myOutputFileNameTextField.setText(shortName + ".swf");
        }
      }
    });

    myOutputFileNameTextField.getDocument().addDocumentListener(new com.intellij.ui.DocumentAdapter() {
      protected void textChanged(final javax.swing.event.DocumentEvent e) {
        final FlexIdeBuildConfiguration bc = getCurrentBC();
        if (bc != null && bc.getTargetPlatform() == TargetPlatform.Web) {
          updateBCOutputLabel(bc);
        }
      }
    });
  }

  private void initRadioButtons() {
    myBCOutputRadioButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateControls();
      }
    });

    myURLRadioButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateControls();

        if (myURLTextField.isEnabled()) {
          IdeFocusManager.getInstance(myProject).requestFocus(myURLTextField, true);
        }
      }
    });
  }

  private void initLaunchWithTextWithBrowse() {
    myLauncherParametersTextWithBrowse.getTextField().setEditable(false);
    myLauncherParametersTextWithBrowse.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final FlexLauncherDialog dialog = new FlexLauncherDialog(myProject, myLauncherParameters);
        dialog.show();
        if (dialog.isOK()) {
          myLauncherParameters = dialog.getLauncherParameters();
          updateControls();
        }
      }
    });
  }

  private void initMobileControls() {
    initEmulatorRelatedControls();
    myOnIOSDeviceRadioButton.setVisible(false); // until supported

    final ActionListener debugTransportListener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateDebugTransportRelatedControls();
      }
    };
    myDebugOverNetworkRadioButton.addActionListener(debugTransportListener);
    myDebugOverUSBRadioButton.addActionListener(debugTransportListener);
  }

  private void initEmulatorRelatedControls() {
    myEmulatorCombo.setModel(new DefaultComboBoxModel(AirMobileRunnerParameters.Emulator.values()));

    myEmulatorCombo.setRenderer(new ListCellRendererWrapper<AirMobileRunnerParameters.Emulator>(myEmulatorCombo.getRenderer()) {
      @Override
      public void customize(JList list, AirMobileRunnerParameters.Emulator value, int index, boolean selected, boolean hasFocus) {
        setText(value.name);
      }
    });

    myEmulatorCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateEmulatorRelatedControls();
      }
    });

    final ActionListener targetDeviceListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateControls();

        if (myOnEmulatorRadioButton.isSelected()) {
          IdeFocusManager.getInstance(myProject).requestFocus(myEmulatorCombo, true);
        }
      }
    };

    myOnEmulatorRadioButton.addActionListener(targetDeviceListener);
    myOnAndroidDeviceRadioButton.addActionListener(targetDeviceListener);
    myOnIOSDeviceRadioButton.addActionListener(targetDeviceListener);
  }

  private void updateControls() {
    final FlexIdeBuildConfiguration config = getCurrentBC();

    final boolean overrideMainClass = myOverrideMainClassCheckBox.isSelected();
    myMainClassComponent.setEnabled(overrideMainClass);
    myOutputFileNameLabel.setEnabled(overrideMainClass);
    myOutputFileNameTextField.setEnabled(overrideMainClass);

    if (!overrideMainClass && config != null) {
      myMainClassComponent.setText(config.getMainClass());
      myOutputFileNameTextField.setText(config.getOutputFileName());
    }

    final boolean web = config != null && config.getTargetPlatform() == TargetPlatform.Web;
    final boolean desktop = config != null && config.getTargetPlatform() == TargetPlatform.Desktop;
    final boolean mobile = config != null && config.getTargetPlatform() == TargetPlatform.Mobile;

    myLaunchPanel.setVisible(web);
    myWebOptionsPanel.setVisible(web);
    myDesktopOptionsPanel.setVisible(desktop);
    myMobileRunPanel.setVisible(mobile);
    myMobileOptionsPanel.setVisible(mobile);

    if (web) {
      updateBCOutputLabel(config);

      myURLTextField.setEnabled(myURLRadioButton.isSelected());

      myLauncherParametersTextWithBrowse.getTextField().setText(myLauncherParameters.getPresentableText());
      myRunTrustedCheckBox.setEnabled(!myURLRadioButton.isSelected());
    }

    if (mobile) {
      final boolean runOnEmulator = myOnEmulatorRadioButton.isSelected();
      myEmulatorCombo.setEnabled(runOnEmulator);
      UIUtil.setEnabled(myEmulatorScreenSizePanel, runOnEmulator, true);
      myAdlOptionsLabel.setEnabled(runOnEmulator);
      myEmulatorAdlOptionsEditor.setEnabled(runOnEmulator);

      if (runOnEmulator) {
        updateEmulatorRelatedControls();
      }

      updateDebugTransportRelatedControls();
    }
  }

  @Nullable
  private FlexIdeBuildConfiguration getCurrentBC() {
    final Object item = myBCsCombo.getSelectedItem();
    return item instanceof FlexIdeBuildConfiguration ? (FlexIdeBuildConfiguration)item : null;
  }

  private void updateBCOutputLabel(final FlexIdeBuildConfiguration bc) {
    if (bc.getOutputType() == OutputType.Application || myOverrideMainClassCheckBox.isSelected()) {
      String bcOutput = myOverrideMainClassCheckBox.isSelected() ? myOutputFileNameTextField.getText().trim() : bc.getOutputFileName();
      if (!bcOutput.isEmpty() && bc.isUseHtmlWrapper()) {
        bcOutput += " via HTML wrapper";
      }
      myBCOutputLabel.setText(bcOutput);
    }
    else {
      myBCOutputLabel.setText("");
    }
  }

  private void updateEmulatorRelatedControls() {
    final AirMobileRunnerParameters.Emulator emulator = (AirMobileRunnerParameters.Emulator)myEmulatorCombo.getSelectedItem();
    if (emulator.adlAlias == null) {
      myScreenWidth.setEditable(true);
      myScreenHeight.setEditable(true);
      myFullScreenWidth.setEditable(true);
      myFullScreenHeight.setEditable(true);
    }
    else {
      myScreenWidth.setEditable(false);
      myScreenHeight.setEditable(false);
      myFullScreenWidth.setEditable(false);
      myFullScreenHeight.setEditable(false);
      myScreenWidth.setText(String.valueOf(emulator.screenWidth));
      myScreenHeight.setText(String.valueOf(emulator.screenHeight));
      myFullScreenWidth.setText(String.valueOf(emulator.fullScreenWidth));
      myFullScreenHeight.setText(String.valueOf(emulator.fullScreenHeight));
    }
  }

  private void updateDebugTransportRelatedControls() {
    final boolean enabled = !myOnEmulatorRadioButton.isSelected();
    myDebugOverLabel.setEnabled(enabled);
    UIUtil.setEnabled(myDebugTransportPanel, enabled, true);
    if (enabled) {
      myUsbDebugPortTextField.setEnabled(myDebugOverUSBRadioButton.isSelected());
    }
  }

  private void createUIComponents() {
    myMainClassFilter = new JSClassChooserDialog.PublicInheritor(myProject, FlexCompilerSettingsEditor.SPRITE_CLASS_NAME, null, true);
    myMainClassComponent = JSReferenceEditor.forClassName("", myProject, null, GlobalSearchScope.EMPTY_SCOPE, null,
                                                          myMainClassFilter, ExecutionBundle.message("choose.main.class.dialog.title"));
  }


  private static String getPresentableText(String moduleName, String configName, final boolean singleModuleProject) {
    moduleName = moduleName.isEmpty() ? "[no module]" : moduleName;
    configName = configName.isEmpty() ? "[no configuration]" : configName;
    return singleModuleProject ? configName : configName + " (" + moduleName + ")";
  }

  @NotNull
  protected JComponent createEditor() {
    return myMainPanel;
  }

  protected void resetEditorFrom(final FlexIdeRunConfiguration configuration) {
    final FlexIdeRunnerParameters params = configuration.getRunnerParameters();
    myLauncherParameters = params.getLauncherParameters().clone(); // must be before myBCsCombo.setModel()

    final Module module = ModuleManager.getInstance(myProject).findModuleByName(params.getModuleName());
    final FlexIdeBuildConfiguration config =
      module != null && (ModuleType.get(module) instanceof FlexModuleType)
      ? FlexBuildConfigurationManager.getInstance(module).findConfigurationByName(params.getBCName())
      : null;

    if (config == null) {
      final Object[] model = new Object[myAllConfigs.length + 1];
      model[0] = Pair.create(params.getModuleName(), params.getBCName());
      System.arraycopy(myAllConfigs, 0, model, 1, myAllConfigs.length);
      myBCsCombo.setModel(new DefaultComboBoxModel(model));
      myBCsCombo.setSelectedIndex(0);
    }
    else {
      myBCsCombo.setModel(new DefaultComboBoxModel(myAllConfigs));
      myBCsCombo.setSelectedItem(config);
    }

    myOverrideMainClassCheckBox.setSelected(params.isOverrideMainClass());
    if (params.isOverrideMainClass()) {
      myMainClassComponent.setText(params.getOverriddenMainClass());
      myOutputFileNameTextField.setText(params.getOverriddenOutputFileName());
    }

    myBCOutputRadioButton.setSelected(!params.isLaunchUrl());
    myURLRadioButton.setSelected(params.isLaunchUrl());
    myURLTextField.setText(params.getUrl());

    myRunTrustedCheckBox.setSelected(params.isRunTrusted());

    myAdlOptionsEditor.setText(params.getAdlOptions());
    myAirProgramParametersEditor.setText(params.getAirProgramParameters());

    myOnEmulatorRadioButton.setSelected(params.getMobileRunTarget() == AirMobileRunnerParameters.AirMobileRunTarget.Emulator);
    myEmulatorCombo.setSelectedItem(params.getEmulator());
    if (params.getEmulator().adlAlias == null) {
      myScreenWidth.setText(String.valueOf(params.getScreenWidth()));
      myScreenHeight.setText(String.valueOf(params.getScreenHeight()));
      myFullScreenWidth.setText(String.valueOf(params.getFullScreenWidth()));
      myFullScreenHeight.setText(String.valueOf(params.getFullScreenHeight()));
    }

    myOnAndroidDeviceRadioButton.setSelected(params.getMobileRunTarget() == AirMobileRunnerParameters.AirMobileRunTarget.AndroidDevice);
    myOnIOSDeviceRadioButton.setSelected(params.getMobileRunTarget() == AirMobileRunnerParameters.AirMobileRunTarget.iOSDevice);

    myDebugOverNetworkRadioButton.setSelected(params.getDebugTransport() == AirMobileRunnerParameters.AirMobileDebugTransport.Network);
    myDebugOverUSBRadioButton.setSelected(params.getDebugTransport() == AirMobileRunnerParameters.AirMobileDebugTransport.USB);
    myUsbDebugPortTextField.setText(String.valueOf(params.getUsbDebugPort()));

    myEmulatorAdlOptionsEditor.setText(params.getEmulatorAdlOptions());

    updateControls();
  }

  protected void applyEditorTo(final FlexIdeRunConfiguration configuration) throws ConfigurationException {
    final FlexIdeRunnerParameters params = configuration.getRunnerParameters();

    final Object selectedItem = myBCsCombo.getSelectedItem();

    if (selectedItem instanceof Pair) {
      params.setModuleName((String)((Pair)selectedItem).first);
      params.setBCName((String)((Pair)selectedItem).second);
    }
    else {
      assert selectedItem instanceof FlexIdeBuildConfiguration : selectedItem;
      params.setModuleName(myBCToModuleMap.get(((FlexIdeBuildConfiguration)selectedItem)).getName());
      params.setBCName(((FlexIdeBuildConfiguration)selectedItem).getName());
    }

    final boolean overrideMainClass = myOverrideMainClassCheckBox.isSelected();
    params.setOverrideMainClass(overrideMainClass);
    params.setOverriddenMainClass(overrideMainClass ? myMainClassComponent.getText().trim() : "");
    params.setOverriddenOutputFileName(overrideMainClass ? myOutputFileNameTextField.getText().trim() : "");

    params.setLaunchUrl(myURLRadioButton.isSelected());
    params.setUrl(myURLTextField.getText().trim());

    params.setLauncherParameters(myLauncherParameters);
    params.setRunTrusted(myRunTrustedCheckBox.isSelected());

    params.setAdlOptions(myAdlOptionsEditor.getText().trim());
    params.setAirProgramParameters(myAirProgramParametersEditor.getText().trim());

    final AirMobileRunnerParameters.AirMobileRunTarget mobileRunTarget = myOnEmulatorRadioButton.isSelected()
                                                                         ? AirMobileRunnerParameters.AirMobileRunTarget.Emulator
                                                                         : myOnAndroidDeviceRadioButton.isSelected()
                                                                           ? AirMobileRunnerParameters.AirMobileRunTarget.AndroidDevice
                                                                           : AirMobileRunnerParameters.AirMobileRunTarget.iOSDevice;
    params.setMobileRunTarget(mobileRunTarget);

    final AirMobileRunnerParameters.Emulator emulator = (AirMobileRunnerParameters.Emulator)myEmulatorCombo.getSelectedItem();
    params.setEmulator(emulator);

    if (emulator.adlAlias == null) {
      try {
        params.setScreenWidth(Integer.parseInt(myScreenWidth.getText()));
        params.setScreenHeight(Integer.parseInt(myScreenHeight.getText()));
        params.setFullScreenWidth(Integer.parseInt(myFullScreenWidth.getText()));
        params.setFullScreenHeight(Integer.parseInt(myFullScreenHeight.getText()));
      }
      catch (NumberFormatException e) {/**/}
    }

    params.setDebugTransport(myDebugOverNetworkRadioButton.isSelected()
                             ? AirMobileRunnerParameters.AirMobileDebugTransport.Network
                             : AirMobileRunnerParameters.AirMobileDebugTransport.USB);
    try {
      final int port = Integer.parseInt(myUsbDebugPortTextField.getText().trim());
      if (port > 0 && port < 65535) {
        params.setUsbDebugPort(port);
      }
    }
    catch (NumberFormatException ignore) {/*ignore*/}

    params.setEmulatorAdlOptions(myEmulatorAdlOptionsEditor.getText().trim());
  }

  protected void disposeEditor() {
    myAllConfigs = null;
    myBCToModuleMap = null;
  }
}
