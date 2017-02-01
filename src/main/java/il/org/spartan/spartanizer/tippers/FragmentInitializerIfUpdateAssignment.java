package il.org.spartan.spartanizer.tippers;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.text.edits.*;

import static il.org.spartan.spartanizer.ast.navigate.step.*;

import il.org.spartan.spartanizer.ast.factory.*;
import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.ast.safety.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.spartanizer.engine.Inliner.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** convert {@code
 * int a = 2;
 * if (b)
 *   a = 3;
 * } into {@code
 * int a = b ? 3 : 2;
 * }
 * @author Yossi Gil
 * @since 2015-08-07 */
public final class FragmentInitializerIfUpdateAssignment extends $FragementAndStatement//
    implements TipperCategory.Inlining {
  @NotNull
  @Override public String description(@NotNull final VariableDeclarationFragment ¢) {
    return "Consolidate initialization of " + ¢.getName() + " with the subsequent conditional assignment to it";
  }

  @Nullable
  @Override protected ASTRewrite go(@NotNull final ASTRewrite $, @NotNull final VariableDeclarationFragment f, final SimpleName n, @Nullable final Expression initializer,
                                    @NotNull final Statement nextStatement, final TextEditGroup g) {
    if (initializer == null)
      return null;
    final IfStatement s = az.ifStatement(nextStatement);
    if (s == null || !iz.vacuousElse(s))
      return null;
    s.setElseStatement(null);
    final Expression condition = s.getExpression();
    final Assignment a = extract.assignment(then(s));
    if (a == null || !wizard.same(to(a), n) || doesUseForbiddenSiblings(f, condition, from(a)) || a.getOperator() == Assignment.Operator.ASSIGN)
      return null;
    final ConditionalExpression newInitializer = subject.pair(make.assignmentAsExpression(a), initializer).toCondition(condition);
    final InlinerWithValue i = new Inliner(n, $, g).byValue(initializer);
    if (!i.canInlineinto(newInitializer) || i.replacedSize(newInitializer) - metrics.size(nextStatement, initializer) > 0)
      return null;
    $.replace(initializer, newInitializer, g);
    i.inlineInto(then(newInitializer), newInitializer.getExpression());
    $.remove(nextStatement, g);
    return $;
  }
}
