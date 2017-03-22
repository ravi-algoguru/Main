package il.org.spartan.spartanizer.research.classifier;

import static java.util.stream.Collectors.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;

import il.org.spartan.spartanizer.cmdline.*;
import il.org.spartan.spartanizer.research.*;
import il.org.spartan.spartanizer.research.classifier.patterns.*;
import il.org.spartan.spartanizer.research.nanos.*;
import il.org.spartan.spartanizer.tipping.*;
import il.org.spartan.utils.*;
import org.jetbrains.annotations.NotNull;

/** NOT ACTIVE RIGHT NOW. <br>
 * NEED TO ADD CATEGORIES.
 * @author Ori Marcovitch
 * @since Nov 13, 2016 */
public class Classifier extends ASTVisitor {
  private final Map<String, List<String>> forLoops = new HashMap<>();
  private final Collection<ASTNode> forLoopsList = new ArrayList<>();
  private int forLoopsAmount;
  private static final Scanner input = new Scanner(System.in);
  private static final Collection<Tipper<EnhancedForStatement>> enhancedForKnownPatterns = new ArrayList<Tipper<EnhancedForStatement>>() {
    static final long serialVersionUID = 1L;
    {
      add(new ForEach());
    }
  };
  private static final Collection<Tipper<ForStatement>> forKnownPatterns = new ArrayList<Tipper<ForStatement>>() {
    static final long serialVersionUID = 1L;
    {
      add(new CopyArray());
      add(new ForEachEnhanced());
      add(new InitArray());
    }
  };
  private Map<String, Int> patterns;

  @Override public boolean visit(final ForStatement node) {
    if (!anyTips(node))
      forLoopsList.add(node);
    return super.visit(node);
  }

  @Override public boolean visit(final EnhancedForStatement node) {
    if (!anyTips(node))
      forLoopsList.add(node);
    return super.visit(node);
  }

  public void analyze(@NotNull final ASTNode ¢) {
    ¢.accept(this);
    forLoopsAmount = forLoopsList.size();
    patterns = filterAllIntrestingPatterns();
    displayInteractive();
    classifyPatterns();
    summarize();
  }

  private void summarize() {
    for (final String k : forLoops.keySet()) {
      System.out.println("****" + k + "****");
      forLoops.get(k).forEach(λ -> System.out.println(tipperize(λ, k)));
    }
  }

  private void classifyPatterns() {
    for (@NotNull final String k : patterns.keySet()) {
      System.out.println(k);
      System.out.println("[Matched " + patterns.get(k).inner + " times]");
      if (!classify(k))
        break;
    }
  }

  private void displayInteractive() {
    System.out.println("Well we've got " + forLoopsAmount + " forLoop statements");
    System.out.println("From them " + patterns.size() + " are repetitive");
    System.out.println("Lets classify them together!");
  }

  @NotNull private Map<String, Int> filterAllIntrestingPatterns() {
    @NotNull final Map<String, Int> $ = new HashMap<>();
    for (boolean again = true; again;) {
      again = false;
      for (final ASTNode ¢ : forLoopsList) {
        @NotNull final UserDefinedTipper<ASTNode> t = TipperFactory.patternTipper(format.code(generalize.code(¢ + "")), "FOR();", "");
        @NotNull final Collection<ASTNode> toRemove = new ArrayList<>(forLoopsList.stream().filter(t::check).collect(toList()));
        if (toRemove.size() > 4) {
          $.putIfAbsent(¢ + "", Int.valueOf(toRemove.size()));
          forLoopsList.removeAll(toRemove);
          again = true;
          break;
        }
        forLoopsList.remove(¢);
        again = true;
        break;
      }
    }
    return $;
  }

  private static boolean anyTips(final EnhancedForStatement ¢) {
    return enhancedForKnownPatterns.stream().anyMatch(λ -> λ.check(¢));
  }

  private static boolean anyTips(final ForStatement ¢) {
    return forKnownPatterns.stream().anyMatch(λ -> λ.check(¢));
  }

  /** @param ¢ to classify */
  private boolean classify(@NotNull final String ¢) {
    final String code = format.code(generalize.code(¢));
    System.out.println(code);
    final String classification = input.nextLine();
    if ("q".equals(classification) || "Q".equals(classification))
      return false;
    System.out.println(tipperize(code, classification));
    forLoops.putIfAbsent(classification, new ArrayList<>());
    forLoops.get(classification).add(¢);
    return true;
  }

  @NotNull private static String tipperize(@NotNull final String code, final String classification) {
    return "add(TipperFactory.patternTipper(\"" + format.code(generalize.code(code)).replace("\n", "").replace("\r", "") + "\", \"" + classification
        + "();\", \"" + classification + "\"));";
  }
}
