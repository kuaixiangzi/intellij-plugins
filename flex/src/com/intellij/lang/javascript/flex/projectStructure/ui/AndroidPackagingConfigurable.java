package com.intellij.lang.javascript.flex.projectStructure.ui;

import com.intellij.lang.javascript.flex.FlexBundle;
import com.intellij.lang.javascript.flex.projectStructure.model.ModifiableAndroidPackagingOptions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nls;

public class AndroidPackagingConfigurable extends AirPackagingConfigurableBase<ModifiableAndroidPackagingOptions> {
  public static final String TAB_NAME = FlexBundle.message("bc.tab.android.display.name");

  public AndroidPackagingConfigurable(final Module module,
                                      final ModifiableAndroidPackagingOptions model,
                                      final Computable<String> mainClassComputable,
                                      final Computable<String> airVersionComputable,
                                      final Computable<Boolean> androidEnabledComputable,
                                      final Computable<Boolean> iosEnabledComputable,
                                      final Consumer<String> createdDescriptorConsumer) {
    super(module, model, mainClassComputable, airVersionComputable, androidEnabledComputable, iosEnabledComputable,
          createdDescriptorConsumer);
  }

  @Nls
  public String getDisplayName() {
    return TAB_NAME;
  }

  public String getHelpTopic() {
    return "BuildConfigurationPage.Android";
  }
}
