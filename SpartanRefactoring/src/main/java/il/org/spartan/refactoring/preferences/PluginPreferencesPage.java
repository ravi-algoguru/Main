package il.org.spartan.refactoring.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.*;
import org.eclipse.ui.*;

import il.org.spartan.refactoring.builder.*;
import il.org.spartan.refactoring.preferences.PluginPreferencesResources.*;

@SuppressWarnings("javadoc") public class PluginPreferencesPage extends FieldEditorPreferencePage implements
    IWorkbenchPreferencePage {
  private final SpartanPropertyListener listener;

  @SuppressWarnings("synthetic-access") public PluginPreferencesPage() {
    super(GRID);
    listener = new SpartanPropertyListener();
  }
  /**
   * Build the preferences page by adding controls
   */
  @Override public void createFieldEditors() {
    // Add the startup behavior combo box
    addField(new ComboFieldEditor(PluginPreferencesResources.PLUGIN_STARTUP_BEHAVIOR_ID, //
        PluginPreferencesResources.PLUGIN_STARTUP_BEHAVIOR_TEXT, //
        PluginPreferencesResources.PLUGIN_STARTUP_BEHAVIOR_OPTIONS, //
        getFieldEditorParent()) //
    );
    // Add the enabled for new projects checkbox
    addField(new BooleanFieldEditor( //
        PluginPreferencesResources.NEW_PROJECTS_ENABLE_BY_DEFAULT_ID, //
        PluginPreferencesResources.NEW_PROJECTS_ENABLE_BY_DEFAULT_TEXT, //
        getFieldEditorParent()) //
    );
    // Add the "resolve bindings" checkbox
    final BooleanFieldEditor bindingsCheckbox = new BooleanFieldEditor( //
        PluginPreferencesResources.ENABLE_BINDING_RESOLUTION_ID, //
        PluginPreferencesResources.ENABLE_BINDING_RESOLUTION_TEXT, //
        getFieldEditorParent());
    addField(bindingsCheckbox);
    // Create and fill the "enabled spartanizations" group box
    final GroupFieldEditor gr = new GroupFieldEditor("Enabled spartanizations", getFieldEditorParent());
    for (final WringGroup wring : WringGroup.values())
      gr.add(new ComboFieldEditor(wring.getId(), wring.getLabel(), PluginPreferencesResources.WRING_COMBO_OPTIONS, gr
          .getFieldEditor()));
    addField(gr);
    gr.init();
  }
  @Override public void init(@SuppressWarnings("unused") final IWorkbench __) {
    setPreferenceStore(Plugin.getDefault().getPreferenceStore());
    setDescription(PluginPreferencesResources.PAGE_DESCRIPTION);
    Plugin.getDefault().getPreferenceStore().addPropertyChangeListener(listener);
  }

  /**
   * An event handler used to re-initialize the Trimmer spartanization once a
   * wring preference was modified.
   */
  private static class SpartanPropertyListener implements IPropertyChangeListener {
    @Override public void propertyChange(@SuppressWarnings("unused") final PropertyChangeEvent __) {
      // Recreate the toolbox's internal instance, adding only enabled wrings
      // TODO Ori: check if still working
      try {
        Plugin.refreshAllProjects();
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}
