package il.org.spartan.spartanizer.tippers;

import static org.eclipse.jdt.core.dom.Assignment.Operator.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.text.edits.*;

import static il.org.spartan.spartanizer.ast.navigate.step.*;

import il.org.spartan.spartanizer.ast.factory.*;
import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.ast.safety.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.spartanizer.engine.OldInliner.*;
import il.org.spartan.spartanizer.java.*;

/** Converts <code>int a=3;return a;</code> into <code>return 3;</code>
 * @author Yossi Gil
 * @since 2015-08-07
 * @DisableSpartan */
public final class FragmentInitializerReturnAssignment extends $FragementInitializerStatement//
    implements TipperCategory.Inlining {
  @Override public String description(final VariableDeclarationFragment ¢) {
    return "Eliminate local '" + ¢.getName() + "', inlining its value into the subsequent return statement";
  }

  @Override protected ASTRewrite go(final ASTRewrite $, final VariableDeclarationFragment f, final SimpleName n, final Expression initializer,
      final Statement nextStatement, final TextEditGroup g) {
    if (initializer == null || haz.annotation(f))
      return null;
    final Assignment a = az.assignment(step.expression(az.returnStatement(nextStatement)));
    if (a == null || !wizard.same(n, to(a)) || a.getOperator() != ASSIGN)
      return null;
    final Expression newReturnValue = copy.of(from(a));
    final InlinerWithValue i = new OldInliner(n, $, g).byValue(initializer);
    if (!i.canInlineinto(newReturnValue) || i.replacedSize(newReturnValue) - metrics.size(newReturnValue) - InliningUtilties.eliminationSaving(f) > 0)
      return null;
    $.replace(a, newReturnValue, g);
    i.inlineInto(newReturnValue);
    eliminate(f, $, g);
    return $;
  }
}
