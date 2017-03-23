package il.org.spartan.spartanizer.tippers;

import static il.org.spartan.spartanizer.ast.navigate.step.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.text.edits.*;
import org.jetbrains.annotations.*;

import il.org.spartan.spartanizer.ast.factory.*;
import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.ast.safety.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.spartanizer.java.*;
import il.org.spartan.spartanizer.tipping.*;

/** TODO: Yossi Gil {@code Yossi.Gil@GMail.COM} please add a description
 * @author Yossi Gil {@code Yossi.Gil@GMail.COM}
 * @since Sep 25, 2016 */
public abstract class $FragmentAndStatement extends GoToNextStatement<VariableDeclarationFragment> {
  private static final long serialVersionUID = 7723281513517888L;

  @Override public boolean prerequisite(@Nullable final VariableDeclarationFragment ¢) {
    return super.prerequisite(¢) && ¢ != null;
  }

  @Override public abstract String description(VariableDeclarationFragment f);

  static boolean doesUseForbiddenSiblings(@NotNull final VariableDeclarationFragment f, final ASTNode... ns) {
    return forbiddenSiblings(f).stream().anyMatch(λ -> collect.BOTH_SEMANTIC.of(λ).existIn(ns));
  }

  static int eliminationSaving(@NotNull final VariableDeclarationFragment f) {
    @NotNull final VariableDeclarationStatement parent = (VariableDeclarationStatement) f.getParent();
    @NotNull final List<VariableDeclarationFragment> live = wizard.live(f, fragments(parent));
    final int $ = metrics.size(parent);
    if (live.isEmpty())
      return $;
    final VariableDeclarationStatement newParent = copy.of(parent);
    fragments(newParent).clear();
    fragments(newParent).addAll(live);
    return $ - metrics.size(newParent);
  }

  protected static boolean forbidden(@NotNull final VariableDeclarationFragment f, @Nullable final Expression initializer) {
    return initializer == null || haz.annotation(f);
  }

  @NotNull private static Collection<VariableDeclarationFragment> forbiddenSiblings(@NotNull final VariableDeclarationFragment f) {
    @NotNull final Collection<VariableDeclarationFragment> $ = new ArrayList<>();
    boolean collecting = false;
    for (final VariableDeclarationFragment brother : fragments((VariableDeclarationStatement) f.getParent())) {
      if (brother == f) {
        collecting = true;
        continue;
      }
      if (collecting)
        $.add(brother);
    }
    return $;
  }

  public static int removalSaving(@NotNull final VariableDeclarationFragment f) {
    @NotNull final VariableDeclarationStatement parent = (VariableDeclarationStatement) f.getParent();
    final int $ = metrics.size(parent);
    if (parent.fragments().size() <= 1)
      return $;
    final VariableDeclarationStatement newParent = copy.of(parent);
    newParent.fragments().remove(parent.fragments().indexOf(f));
    return $ - metrics.size(newParent);
  }

  /** Removes a {@link VariableDeclarationFragment}, leaving intact any other
   * fragment fragments in the containing {@link VariabelDeclarationStatement} .
   * Still, if the containing node is left empty, it is removed as well.
   * @param f
   * @param r
   * @param g */
  static void remove(@NotNull final VariableDeclarationFragment f, @NotNull final ASTRewrite r, final TextEditGroup g) {
    @NotNull final VariableDeclarationStatement parent = (VariableDeclarationStatement) f.getParent();
    r.remove(parent.fragments().size() > 1 ? f : parent, g);
  }

  static boolean usedInSubsequentInitializers(@NotNull final VariableDeclarationFragment f, final SimpleName n) {
    boolean searching = true;
    for (@NotNull final VariableDeclarationFragment ff : fragments(az.variableDeclrationStatement(f.getParent())))
      if (searching)
        searching = ff != f;
      else if (!collect.usesOf(n).in(ff.getInitializer()).isEmpty())
        return true;
    return false;
  }

  @Nullable protected abstract ASTRewrite go(ASTRewrite r, VariableDeclarationFragment f, SimpleName n, Expression initializer,
      Statement nextStatement, TextEditGroup g);

  @Override protected final ASTRewrite go(final ASTRewrite r, @NotNull final VariableDeclarationFragment f, final Statement nextStatement,
      final TextEditGroup g) {
    if (!iz.variableDeclarationStatement(f.getParent()))
      return null;
    final SimpleName $ = f.getName();
    return $ == null ? null : go(r, f, $, f.getInitializer(), nextStatement, g);
  }

  @Override public final Tip tip(@NotNull final VariableDeclarationFragment f, @Nullable final ExclusionManager exclude) {
    @Nullable final Tip $ = super.tip(f, exclude);
    if ($ != null && exclude != null)
      exclude.exclude(f.getParent());
    return $;
  }
}