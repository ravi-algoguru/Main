package il.org.spartan.athenizer;

import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.*;

import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.text.undo.*;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.*;

import fluent.ly.*;
import il.org.spartan.athenizer.SingleFlater.*;
import il.org.spartan.plugin.preferences.revision.*;
import il.org.spartan.spartanizer.plugin.*;
import il.org.spartan.utils.*;

/** Listener for code inflation/deflation using mouse and CTRL key.
 * @author Ori Roth <tt>ori.rothh@gmail.com</tt>
 * @since 2017-03-30 */
public class InflaterListener implements KeyListener, Listener {
  // XXX: Ori Roth why so many fields? --yg
  // GUI class, all SWT look like this. --or
  private static final int NO_COMPUND_EDIT = 0;
  private static final int ZOOMIN_COMPUND_EDIT = -1;
  private static final int ZOOMOUT_COMPUND_EDIT = 1;
  private static final Function<Device, Color> INFLATE_COLOR = λ -> new Color(λ, 200, 200, 255);
  private static final Function<Device, Color> DEFLATE_COLOR = λ -> new Color(λ, 200, 255, 200);
  static final int CURSOR_IMAGE = SWT.CURSOR_CROSS;
  final StyledText text;
  final ITextEditor editor;
  final Cursor activeCursor;
  final Cursor inactiveCursor;
  final Selection selection;
  boolean active;
  final Bool working = new Bool();
  WindowInformation windowInformation;
  private final Color originalBackground;
  private final IDocumentUndoManager undoManager;
  private int editDirection;
  private final boolean compoundEditing; 
  @SuppressWarnings("boxing")
  private static final List<Integer> activating_keys = Arrays.asList(SWT.CTRL, SWT.ALT);
  @SuppressWarnings("boxing")
  private final List<Boolean> active_keys = activating_keys.stream().map(x -> false).collect(Collectors.toList());
  private static final List<Predicate<Event>> zoomer_keys = Arrays.asList(e -> e.keyCode == SWT.KEYPAD_ADD,
      e -> e.character == '=',
      e -> e.type == SWT.MouseWheel && e.count > 0);
  private static final List<Predicate<Event>> spartan_keys = Arrays.asList(e -> e.keyCode == SWT.KEYPAD_SUBTRACT,
      e -> e.character == '-',
      e -> e.type == SWT.MouseWheel && e.count < 0);

  public InflaterListener(final StyledText text, final ITextEditor editor, final Selection selection) {
    this.text = text;
    this.editor = editor;
    this.selection = selection;
    final Display display = PlatformUI.getWorkbench().getDisplay();
    activeCursor = new Cursor(display, CURSOR_IMAGE);
    inactiveCursor = text.getCursor();
    originalBackground = text.getSelectionBackground();
    undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(Eclipse.document(editor));
    compoundEditing = PreferencesResources.ZOOMER_REVERT_METHOD_VALUE.get();
  }

  @Override public void handleEvent(final Event ¢) {
    int t = checkEvent(¢);
    if (t == 0 || !active || !text.getBounds().contains(text.toControl(Eclipse.mouseLocation())))
      return;
    ¢.doit = false;
    ¢.type = SWT.NONE;
    ¢.count = 0;
    if (working.get())
      return;
    windowInformation = WindowInformation.of(text);
    working.set();
    if (t <= 0) {
      if (compoundEditing && editDirection != ZOOMIN_COMPUND_EDIT) {
        if (editDirection != NO_COMPUND_EDIT)
          undoManager.endCompoundChange();
        undoManager.beginCompoundChange();
      }
      editDirection = ZOOMIN_COMPUND_EDIT;
      Eclipse.runAsynchronouslyInUIThread(() -> {
        deflate();
        working.clear();
      });
    } else {
      if (compoundEditing && editDirection != ZOOMOUT_COMPUND_EDIT) {
        if (editDirection != NO_COMPUND_EDIT)
          undoManager.endCompoundChange();
        undoManager.beginCompoundChange();
      }
      editDirection = ZOOMOUT_COMPUND_EDIT;
      Eclipse.runAsynchronouslyInUIThread(() -> {
        inflate();
        working.clear();
      });
    }
  }
  
  /**
   * Returns 1 if event corresponds to a bloater shortcut, -1 if even corresponds to spartanizer shortcut and 0 otherwise. 
   */
  private static int checkEvent(final Event e) {
    return zoomer_keys.stream().anyMatch(x -> x.test(e)) ? 1 : spartan_keys.stream().anyMatch(x -> x.test(e)) ? -1 : 0;
  }

  private void inflate() {
    text.setSelectionBackground(INFLATE_COLOR.apply(Display.getCurrent()));
    final WrappedCompilationUnit wcu = the.headOf(selection.inner).build();
    SingleFlater.commitChanges(SingleFlater.in(wcu.compilationUnit).from(new InflaterProvider()).limit(windowInformation),
        ASTRewrite.create(wcu.compilationUnit.getAST()), wcu, text, editor, windowInformation, compoundEditing);
  }

  private void deflate() {
    text.setSelectionBackground(DEFLATE_COLOR.apply(Display.getCurrent()));
    final WrappedCompilationUnit wcu = the.headOf(selection.inner).build();
    SingleFlater.commitChanges(SingleFlater.in(wcu.compilationUnit).from(new DeflaterProvider()).limit(windowInformation),
        ASTRewrite.create(wcu.compilationUnit.getAST()), wcu, text, editor, windowInformation, compoundEditing);
  }

  @Override @SuppressWarnings("boxing") public void keyPressed(final KeyEvent ¢) {
    int index = activating_keys.indexOf(¢.keyCode);
    if (index >= 0)
      active_keys.set(index, true);
    if (active_keys.stream().allMatch(x -> x) && !active)
      activate();
  }

  @Override @SuppressWarnings("boxing") public void keyReleased(final KeyEvent ¢) {
    int index = activating_keys.indexOf(¢.keyCode);
    if (index < 0)
      return;
    active_keys.set(index, false);
    if (active)
      deactivate();
  }

  private void activate() {
    active = true;
    editDirection = NO_COMPUND_EDIT;
    if (text.isDisposed())
      return;
    text.setCursor(activeCursor);
    Optional.ofNullable(text.getVerticalBar()).ifPresent(λ -> λ.setEnabled(false));
  }

  private void deactivate() {
    text.setSelectionBackground(originalBackground);
    active = false;
    if (compoundEditing && editDirection != NO_COMPUND_EDIT)
      undoManager.endCompoundChange();
    editDirection = NO_COMPUND_EDIT;
    if (text.isDisposed())
      return;
    text.setCursor(inactiveCursor);
    Optional.ofNullable(text.getVerticalBar()).ifPresent(λ -> λ.setEnabled(true));
  }

  public void finilize() {
    if (active)
      deactivate();
  }

  public Listener find(final Iterable<Listener> ls) {
    TypedListener $ = null;
    for (final Listener ¢ : ls)
      if (¢ instanceof TypedListener && equals(((TypedListener) ¢).getEventListener()))
        $ = (TypedListener) ¢;
    return $;
  }
}
