package il.org.spartan.spartanizer.ast.navigate;

import java.util.*;
import java.util.stream.*;

import org.eclipse.jdt.core.dom.*;

import static il.org.spartan.spartanizer.ast.navigate.step.*;

import static java.util.stream.Collectors.*;

import static il.org.spartan.lisp.*;

import il.org.spartan.spartanizer.ast.factory.*;
import il.org.spartan.spartanizer.ast.safety.*;

/** TODO: YuvalSimon <yuvaltechnion@gmail.com> , this class is duplicate also
 * please add a description
 * @author YuvalSimon <yuvaltechnion@gmail.com>
 * @since Jan 15, 2017 */
public class switchBranch {
  public final List<SwitchCase> cases;
  public final List<Statement> statements;
  private int hasDefault;
  private int numOfStatements;
  private int numOfNodes;
  private int depth;
  private int sequencerLevel;
  public static final int MAX_CASES_FOR_SPARTANIZATION = 10;

  public switchBranch(final List<SwitchCase> cases, final List<Statement> statements) {
    this.cases = cases;
    this.statements = statements;
    hasDefault = numOfNodes = numOfStatements = depth = sequencerLevel = -1;
  }

  @SuppressWarnings("boxing") public boolean hasDefault() {
    if (hasDefault == -1)
      hasDefault = cases.stream().filter(SwitchCase::isDefault).map(λ -> 1).findFirst().orElse(hasDefault);
    return hasDefault == 1;
  }

  public int depth() {
    if (depth < 0)
      depth = metrics.height(statements, 0);
    return depth;
  }

  public int statementsNum() {
    if (numOfStatements < 0)
      numOfStatements = metrics.countStatements(statements);
    return numOfStatements;
  }

  public int nodesNum() {
    if (numOfNodes < 0)
      numOfNodes = metrics.nodes(statements);
    return numOfNodes;
  }

  public int casesNum() {
    return cases.size();
  }

  public int sequencerLevel() {
    if (sequencerLevel < 0) {
      final int th = metrics.countStatementsOfType(statements, ASTNode.THROW_STATEMENT),
          re = metrics.countStatementsOfType(statements, ASTNode.RETURN_STATEMENT),
          br = metrics.countStatementsOfType(statements, ASTNode.BREAK_STATEMENT),
          co = metrics.countStatementsOfType(statements, ASTNode.CONTINUE_STATEMENT), sum = th + re + br + co;
      assert sum > 0;
      sequencerLevel = sum > th && sum > re && sum > br && sum > co ? 0 : th > 0 ? 1 : re > 0 ? 2 : br > 0 ? 3 : 4;
    }
    return sequencerLevel;
  }

  /** @param ¢
   * @return returns true if _this_ has better metrics than the parameter (i.e
   *         should come before the parameter in the switch), false otherwise */
  private boolean before_(final switchBranch ¢) {
    if (hasDefault())
      return false;
    if (¢.hasDefault())
      return true;
    if (sequencerLevel() > 0 && ¢.sequencerLevel() > 0) {
      if (sequencerLevel() > ¢.sequencerLevel())
        return false;
      if (sequencerLevel() < ¢.sequencerLevel())
        return true;
    }
    return depth() < ¢.depth() || statementsNum() < ¢.statementsNum() || nodesNum() < ¢.nodesNum() || casesNum() < ¢.casesNum();
  }

  public boolean before(final switchBranch ¢) {
    final boolean $ = before_(¢);
    return $ != ¢.before_(this) ? $ : (first(cases) + "").compareTo(first(¢.cases) + "") < 0;
  }

  private void addAll(final Collection<Statement> ¢) {
    ¢.addAll(cases.stream().map(copy::of).collect(toList()));
    ¢.addAll(statements.stream().map(copy::of).collect(toList()));
  }

  private static void addAll(final Collection<Statement> ss, final Iterable<switchBranch> bs) {
    bs.forEach(λ -> λ.addAll(ss));
  }

  public static SwitchStatement makeSwitchStatement(final Iterable<switchBranch> bs, final Expression x, final AST t) {
    final SwitchStatement $ = t.newSwitchStatement();
    $.setExpression(copy.of(x));
    addAll(statements($), bs);
    return $;
  }

  @SuppressWarnings("null") public static List<switchBranch> intoBranches(final SwitchStatement n) {
    final List<Statement> l = statements(n);
    assert iz.switchCase(first(l));
    List<SwitchCase> c = null;
    List<Statement> s = null;
    final List<switchBranch> $ = new ArrayList<>();
    boolean nextBranch = true;
    for (int ¢ = 0; ¢ < l.size() - 1; ++¢) {
      if (nextBranch) {
        c = new ArrayList<>();
        s = new ArrayList<>();
        $.add(new switchBranch(c, s));
        nextBranch = false;
        for (; iz.switchCase(l.get(¢)) && ¢ < l.size() - 1; ++¢)
          c.add(az.switchCase(l.get(¢)));
        if (¢ >= l.size() - 1)
          break;
      }
      if (iz.switchCase(l.get(¢ + 1)) && iz.sequencerComplex(l.get(¢)))
        nextBranch = true;
      s.add(l.get(¢));
    }
    if (!iz.switchCase(last(l))) {
      s.add(last(l));
      if (!iz.sequencerComplex(last(l)))
        s.add(n.getAST().newBreakStatement());
    } else {
      if (!s.isEmpty())
        $.add(new switchBranch(new ArrayList<>(), new ArrayList<>()));
      c.add(az.switchCase(last(l)));
      s.add(n.getAST().newBreakStatement());
    }
    return $;
  }

  public boolean hasSameBody(final switchBranch ¢) {
    return wizard.same(functionalCommands(), ¢.functionalCommands());
  }

  private List<Statement> functionalCommands() {
    final List<Statement> $ = IntStream.range(0, statements.size() - 1).mapToObj(statements::get).collect(toList());
    if (!iz.breakStatement(last(statements)))
      $.add(last(statements));
    return $;
  }

  public boolean hasFallThrough() {
    return statements.stream().anyMatch(iz::switchCase);
  }

  public static Statement removeBreakSequencer(final Statement s) {
    if (!iz.sequencerComplex(s, ASTNode.BREAK_STATEMENT))
      return copy.of(s);
    final AST $ = s.getAST();
    if (iz.ifStatement(s)) {
      final IfStatement t = az.ifStatement(s), f = $.newIfStatement();
      f.setExpression(copy.of(expression(t)));
      f.setThenStatement(removeBreakSequencer(then(t)));
      f.setElseStatement(removeBreakSequencer(elze(t)));
      return f;
    }
    if (!iz.block(s))
      return !iz.breakStatement(s) || !iz.block(s.getParent()) ? null : $.newEmptyStatement();
    final Block b = $.newBlock();
    statements(b).addAll(removeBreakSequencer(statements(az.block(s))));
    return b;
  }

  public static Collection<Statement> removeBreakSequencer(final Iterable<Statement> ss) {
    final Collection<Statement> $ = new ArrayList<>();
    for (final Statement ¢ : ss) {
      final Statement s = removeBreakSequencer(¢);
      if (s != null)
        $.add(s);
    }
    return $;
  }

  public boolean hasStatements() {
    return !statements.isEmpty() && !iz.breakStatement(first(statements));
  }
}
