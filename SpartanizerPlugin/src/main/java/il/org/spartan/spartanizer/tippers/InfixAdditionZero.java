package il.org.spartan.spartanizer.tippers;

import static il.org.spartan.Utils.*;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.MINUS;
import static org.eclipse.jdt.core.dom.PrefixExpression.Operator.PLUS;

import static il.org.spartan.lisp.*;

import static il.org.spartan.spartanizer.ast.navigate.step.*;

import static il.org.spartan.spartanizer.ast.navigate.extract.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.text.edits.*;

import il.org.spartan.plugin.preferences.revision.PreferencesResources.*;
import il.org.spartan.spartanizer.ast.factory.*;
import il.org.spartan.spartanizer.ast.safety.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.spartanizer.engine.nominal.*;
import il.org.spartan.spartanizer.tipping.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A {@link Tipper} to convert an expression such as {@code
 * 0 + X = X
 * } or {@code
 * X + 0 = X
 * } or {@code
 * X + 0 + Y
 * } to {@code
 * X + Y
 * }
 * @author Matteo Orrù
 * @since 2016 */
public final class InfixAdditionZero extends EagerTipper<InfixExpression>//
    implements TipperCategory.NOP.onNumbers {
  private static final long serialVersionUID = 4219806359292635514L;

  @NotNull private static List<Expression> gather(final Expression x, @NotNull final List<Expression> $) {
    if (iz.infixExpression(x))
      return gather(az.infixExpression(x), $);
    $.add(x);
    return $;
  }

  @NotNull private static List<Expression> gather(final InfixExpression ¢) {
    return gather(¢, new ArrayList<>());
  }

  @NotNull private static List<Expression> gather(@Nullable final InfixExpression x, @NotNull final List<Expression> $) {
    if (x == null)
      return $;
    if (!in(operator(x), PLUS, MINUS)) {
      $.add(x);
      return $;
    }
    gather(core(left(x)), $);
    gather(core(right(x)), $);
    if (x.hasExtendedOperands())
      gather(extendedOperands(x), $);
    return $;
  }

  @NotNull private static List<Expression> gather(@NotNull final Iterable<Expression> xs, @NotNull final List<Expression> $) {
    xs.forEach(λ -> gather(λ, $));
    return $;
  }

  @Override public String description() {
    return null;
  }

  @NotNull @Override public String description(final InfixExpression ¢) {
    return "Remove noop of adding 0 in " + trivia.gist(¢);
  }

  @Override public Tip tip(@NotNull final InfixExpression x, @Nullable final ExclusionManager exclude) {
    @NotNull final List<Expression> $ = gather(x);
    if ($.size() < 2)
      return null;
    final int n = minus.level($);
    if (n == 0 || n == 1 && minus.level(first($)) == 1)
      return null;
    if (exclude != null)
      exclude.exclude(x);
    return new Tip(description(x), x, getClass()) {
      @Override public void go(@NotNull final ASTRewrite r, final TextEditGroup g) {
        @Nullable final Expression first = n % 2 == 0 ? null : first($);
        $.stream().filter(λ -> λ != first && minus.level(λ) > 0)
            .forEach(λ -> r.replace(λ, make.plant(copy.of(minus.peel(λ))).into(λ.getParent()), g));
        if (first != null)
          r.replace(first, make.plant(subject.operand(minus.peel(first)).to(PrefixExpression.Operator.MINUS)).into(first.getParent()), g);
      }
    };
  }

  @NotNull @Override public TipperGroup tipperGroup() {
    return TipperGroup.Abbreviation;
  }
}
