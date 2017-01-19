package il.org.spartan.spartanizer.research.nanos;

import static il.org.spartan.spartanizer.research.TipperFactory.*;

import org.eclipse.jdt.core.dom.*;

import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.spartanizer.research.*;
import il.org.spartan.spartanizer.research.nanos.common.*;

/** @author Ori Marcovitch
 * @year 2016 */
public final class CachingPattern extends NanoPatternTipper<Block> {
  private static final UserDefinedTipper<Block> tipper = //
      statementsPattern("if($X1 == null)$X1 = $X2;return $X1;", //
          "return $X1!=null?$X1:($X1=$X2);", //
          "Caching pattern: rewrite as return of ternary");

  @Override public boolean canTip(final Block x) {
    return tipper.canTip(x);
  }

  @Override public Tip pattern(final Block x) {
    return tipper.tip(x);
  }

  @Override public Category category() {
    return Category.Field;
  }

  @Override public String description() {
    return "A field which its value is defined by an expression which is evaluated only on the first access";
  }

  @Override public String technicalName() {
    return "IfX₁IsNullIntializeWithX₂ReturnX₁";
  }

  @Override public String example() {
    return tipper.pattern();
  }

  @Override public String symbolycReplacement() {
    return tipper.replacement();
  }
}