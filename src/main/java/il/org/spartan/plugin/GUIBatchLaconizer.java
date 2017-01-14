package il.org.spartan.plugin;

import java.util.*;
import java.util.function.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.engine.nominal.*;
import il.org.spartan.spartanizer.java.*;
import il.org.spartan.spartanizer.utils.*;

/** Possible events during spartanization process
 * <p>
 * Why do we need to make such a strong binding between the event generator and
 * the listener? Why should they agree on a common type of events? We should let
 * the listener take, say strings, and just record them. */
@Deprecated
enum event {
  run_start, run_finish, run_pass, run_pass_done, //
  visit_root, visit_cu, visit_node, //
}

/** An {@link Applicator} suitable for eclipse GUI.
 * @author Ori Roth
 * @since 2.6 */
public class GUIBatchLaconizer extends Applicator {
  private static final String DEFAULT_OPERATION_NAME = "Operat";
  /** Few passes for the applicator to conduct. */
  private static final int PASSES_FEW = 1;
  /** Many passes for the applicator to conduct. */
  private static final int PASSES_MANY = 20;
  /** Minimum number of files in selection initiating disabling of auto build */
  private static final int DISABLE_AUTO_BUILD_THRESHOLD = 100;

  /** Spartanization process. */
  @Override @SuppressWarnings("boxing") public void go() {
    if (selection() == null || listener() == null || runContext() == null || passes() <= 0 || selection().isEmpty())
      return;
    listener().push(message.run_start.get(operationName(), selection().name));
    if (!shouldRun())
      return;
    final boolean isAutoBuildChanged = selection().size() >= DISABLE_AUTO_BUILD_THRESHOLD && disableAutoBuild();
    final Int totalTipsInvoked = new Int();
    runContext().accept(() -> {
      for (final Integer pass : range.from(1).to(passes()).inclusive()) {
        final Int thisPassTipsInvoked = new Int();
        listener().push(message.run_pass.get(Integer.valueOf(pass)));
        if (!shouldRun())
          break;
        final List<WrappedCompilationUnit> selected = selection().inner;
        final List<WrappedCompilationUnit> alive = new ArrayList<>(selected);
        final List<WrappedCompilationUnit> done = new ArrayList<>();
        for (final WrappedCompilationUnit ¢ : alive) {
          // TODO Roth: this may throw a {@link CoreException}, be sure to catch
          // it!
          final int tipsInvoked = runAction().apply(¢.build()).intValue();
          if (tipsInvoked <= 0)
            done.add(¢);
          ¢.dispose();
          thisPassTipsInvoked.addAndGet(tipsInvoked);
          totalTipsInvoked.addAndGet(tipsInvoked);
          listener().tick(message.visit_cu.get(operationName(), Integer.valueOf(alive.indexOf(¢)), Integer.valueOf(alive.size()),
              ¢.descriptor.getElementName(), totalTipsInvoked.get(), thisPassTipsInvoked.get()));
          if (!shouldRun())
            break;
        }
        listener().pop(message.run_pass_finish.get(Integer.valueOf(pass)));
        selected.removeAll(done);
        if (selected.isEmpty() || !shouldRun())
          break;
      }
    });
    if (isAutoBuildChanged)
      enableAutoBuild();
    // TODO Roth: add metrics etc.
    listener().pop(message.run_finish.get(operationName(), selection().name, totalTipsInvoked.inner));
  }

  /** Default listener configuration of {@link GUIBatchLaconizer}. Simple
   * printing to console.
   * @return this applicator */
  @Override public GUIBatchLaconizer defaultListenerNoisy() {
    listener(os -> {
      for (final Object ¢ : os)
        System.out.print(¢ + " ");
      System.out.println();
    });
    return this;
  }

  /** Default listener configuration of {@link GUIBatchLaconizer}. Silent
   * listener.
   * @return this applicator */
  public GUIBatchLaconizer defaultListenerSilent() {
    listener((final Object... __) -> {
      //
    });
    return this;
  }

  /** Default selection configuration of {@link GUIBatchLaconizer}. Normal
   * eclipse user selection.
   * @return this applicator */
  public GUIBatchLaconizer defaultSelection() {
    selection(Selection.Util.current());
    return this;
  }

  /** Default passes configuration of {@link GUIBatchLaconizer}, with few
   * passes.
   * @return this applicator */
  public GUIBatchLaconizer defaultPassesFew() {
    passes(PASSES_FEW);
    return this;
  }

  /** Default passes configuration of {@link GUIBatchLaconizer}, with many
   * passes.
   * @return this applicator */
  public GUIBatchLaconizer defaultPassesMany() {
    passes(PASSES_MANY);
    return this;
  }

  /** Default run context configuration of {@link GUIBatchLaconizer}. Simply
   * runs the {@link Runnable} in the current thread.
   * @return this applicator */
  public GUIBatchLaconizer defaultRunContext() {
    runContext(r -> r.run());
    return this;
  }

  // TODO Roth: use Policy / replacement for Trimmer.
  /** Default run action configuration of {@link GUIBatchLaconizer}. Spartanize
   * the {@link ICompilationUnit} using received {@link AbstractGUIApplicator}.
   * @param a JD
   * @return this applicator */
  public GUIBatchLaconizer defaultRunAction(final AbstractGUIApplicator a) {
    setRunAction(¢ -> Integer.valueOf(a.apply(¢, selection())));
    name(a.getName());
    return this;
  }

  /** Default operation name.
   * @return this applicator */
  public GUIBatchLaconizer defaultOperationName() {
    operationName(Linguistic.Activity.simple(DEFAULT_OPERATION_NAME));
    return this;
  }

  /** Default settings for all {@link Applicator} components.
   * @return this applicator */
  public GUIBatchLaconizer defaultSettings() {
    return defaultListenerSilent().defaultPassesFew().defaultRunContext().defaultSelection().defaultRunAction(new Trimmer()).defaultOperationName();
  }

  /** Factory method.
   * @return default event applicator */
  public static GUIBatchLaconizer defaultApplicator() {
    return new GUIBatchLaconizer().defaultSettings();
  }

  /** Printing definition of events that occur during spartanization.
   * @author Ori Roth
   * @since 2.6 */
  private enum message {
    run_start(2, inp -> printableAt(inp, 0, x -> ((Linguistic.Activity) x).getIng()) + " " + printableAt(inp, 1)), //
    run_pass(1, inp -> "Pass #" + printableAt(inp, 0)), //
    run_pass_finish(1, inp -> "Pass #" + printableAt(inp, 0) + " finished"), //
    visit_cu(6,
        inp -> printableAt(inp, 1) + "/" + printableAt(inp, 2) + "\t" + printableAt(inp, 0, x -> ((Linguistic.Activity) x).getIng()) + " "
            + printableAt(inp, 3) + "\nTips: total = " + printableAt(inp, 4) + "\tthis pass = " + printableAt(inp, 5)), //
    run_finish(3, inp -> "Done " + printableAt(inp, 0, x -> ((Linguistic.Activity) x).getIng()) + " " + printableAt(inp, 1) + "\nTips accepted: "
        + printableAt(inp, 2));
    private final int inputCount;
    private final Function<Object[], String> printing;

    message(final int inputCount, final Function<Object[], String> printing) {
      this.inputCount = inputCount;
      this.printing = printing;
    }

    public String get(final Object... ¢) {
      assert ¢.length == inputCount;
      return printing.apply(¢);
    }

    private static String printableAt(final Object[] os, final int index) {
      return Linguistic.unknownIfNull(os, xs -> xs[index]);
    }

    private static String printableAt(final Object[] os, final int index, final Function<Object, String> operation) {
      return Linguistic.unknownIfNull(os, xs -> operation.apply(xs[index]));
    }
  }

  private static boolean disableAutoBuild() {
    final IWorkspace w = ResourcesPlugin.getWorkspace();
    if (w == null)
      return false;
    final IWorkspaceDescription d = w.getDescription();
    if (d == null || !d.isAutoBuilding())
      return false;
    d.setAutoBuilding(false);
    try {
      w.setDescription(d);
    } catch (final CoreException ¢) {
      monitor.log(¢);
      return false;
    }
    return true;
  }

  private static void enableAutoBuild() {
    final IWorkspace w = ResourcesPlugin.getWorkspace();
    if (w == null)
      return;
    final IWorkspaceDescription d = w.getDescription();
    if (d == null || d.isAutoBuilding())
      return;
    d.setAutoBuilding(true);
    try {
      w.setDescription(d);
    } catch (final CoreException ¢) {
      monitor.log(¢);
    }
  }
}