package il.ac.technion.cs.ssdl.spartan.refactoring;

import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.countNodes;
import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.getBlockSingleStmnt;
import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.makeIfStmnt;
import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.makeInfixExpression;
import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.makeParenthesizedConditionalExp;
import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.makeParenthesizedExpression;
import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.makePrefixExpression;
import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.statementsCount;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_AND;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_OR;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER_EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.LESS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.LESS_EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.NOT_EQUALS;
import il.ac.technion.cs.ssdl.spartan.utils.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * @author Artium Nihamkin (original)
 * @author Boris van Sosin <code><boris.van.sosin [at] gmail.com></code> (v2)
 * @author Tomer Zeltzer <code><tomerr90 [at] gmail.com></code> (v3)

 * @since 2013/01/01
 */
public class ShortestBranchFirst extends Spartanization {
	/** Instantiates this class */
	public ShortestBranchFirst() {
		super("Shortester first",
				"Negate the expression of a conditional, and change the order of branches so that shortest branch occurs first");
	}
	@Override protected final void fillRewrite(final ASTRewrite r, final AST t, final CompilationUnit cu, final IMarker m) {
		cu.accept(new ASTVisitor() {
			@Override public boolean visit(final IfStatement n) {
				if (!inRange(m, n) || !longerFirst(n))
					return true;
				final IfStatement newIfStmnt = transpose(n);
				if (newIfStmnt != null)
					r.replace(n, newIfStmnt, null);
				return true;
			}
			@Override public boolean visit(final ConditionalExpression n) {
				if (!inRange(m, n) || !longerFirst(n))
					return true;
				final ParenthesizedExpression newCondExp = transpose(n);
				if (newCondExp != null)
					r.replace(n, newCondExp, null);
				return true;
			}
			private IfStatement transpose(final IfStatement n) {
				final Expression negatedOp = negate(t, r, n.getExpression());
				if (negatedOp == null)
					return null;
				final Statement elseStmnt = n.getElseStatement();
				final Statement thenStatement = n.getThenStatement();
				if (1 != statementsCount(elseStmnt) || ASTNode.IF_STATEMENT == getBlockSingleStmnt(elseStmnt).getNodeType()){
					final Block newElseBlock = t.newBlock();
					newElseBlock.statements().add(r.createCopyTarget(elseStmnt));
					return makeIfStmnt(t, r, negatedOp, newElseBlock, thenStatement);
				}
				return makeIfStmnt(t, r, negatedOp, elseStmnt, thenStatement);
			}
			private ParenthesizedExpression transpose(final ConditionalExpression n) {
				return n == null ? null : makeParenthesizedConditionalExp(t, r, negate(t, r, n.getExpression()), n.getElseExpression(),
						n.getThenExpression());
			}
		});
	}
	/**
	 * @return a prefix expression that is the negation of the provided
	 *         expression.
	 */
	static Expression negate(final AST t, final ASTRewrite r, final Expression e) {
		if (e instanceof InfixExpression)
			return tryNegateComparison(t, r, (InfixExpression) e);
		return e instanceof PrefixExpression ? tryNegatePrefix(r, (PrefixExpression) e)
				: makePrefixExpression(t, r, makeParenthesizedExpression(t, r, e), PrefixExpression.Operator.NOT);
	}
	private static Expression tryNegateComparison(final AST ast, final ASTRewrite r, final InfixExpression e) {
		final Operator op = negate(e.getOperator());
		if (op == null)
			return null;
		return op == CONDITIONAL_AND || op == CONDITIONAL_OR ?
				makeInfixExpression(ast, r, op, negateExp(ast, r, e.getLeftOperand()), negateExp(ast, r, e.getRightOperand()))
				: makeInfixExpression(ast, r, op, e.getLeftOperand(), e.getRightOperand());
	}
	private static Expression negateExp(final AST t, final ASTRewrite r, final Expression exp){
		if (exp.getNodeType() == ASTNode.INFIX_EXPRESSION)
			return makePrefixExpression(t, r, makeParenthesizedExpression(t, r, exp), PrefixExpression.Operator.NOT);
		return exp.getNodeType() == ASTNode.PREFIX_EXPRESSION
				&& ((PrefixExpression)exp).getOperator().equals(PrefixExpression.Operator.NOT) ?
						(Expression) r.createCopyTarget(((PrefixExpression)exp).getOperand())
						: makePrefixExpression(t, r, exp, PrefixExpression.Operator.NOT);
	}
	private static Operator negate(final Operator o) {
		return !negate.containsKey(o) ? null : negate.get(o);
	}
	private static Map<Operator, Operator> makeNegation() {
		final Map<Operator, Operator> $ = new HashMap<Operator, Operator>();
		$.put(EQUALS, NOT_EQUALS);
		$.put(NOT_EQUALS, EQUALS);
		$.put(LESS_EQUALS, GREATER);
		$.put(GREATER, LESS_EQUALS);
		$.put(LESS, GREATER_EQUALS);
		$.put(GREATER_EQUALS, LESS);
		$.put(CONDITIONAL_AND, CONDITIONAL_OR);
		$.put(CONDITIONAL_OR, CONDITIONAL_AND);
		return $;
	}
	private static Map<Operator, Operator> negate = makeNegation();

	private static Expression tryNegatePrefix(final ASTRewrite r, final PrefixExpression exp) {
		return !exp.getOperator().equals(PrefixExpression.Operator.NOT) ? null : (Expression) r.createCopyTarget(exp.getOperand());
	}
	private static final int threshold = 1;
	@Override protected ASTVisitor fillOpportunities(final List<Range> opportunities) {
		return new ASTVisitor() {
			@Override public boolean visit(final IfStatement n) {
				if (longerFirst(n))
					opportunities.add(new Range(n));
				return true;
			}
			@Override public boolean visit(final ConditionalExpression n) {
				if (longerFirst(n))
					opportunities.add(new Range(n));
				return true;
			}
		};
	}
	static boolean longerFirst(final IfStatement n) {
		return null != n.getElseStatement() && countNodes(n.getThenStatement()) > threshold + countNodes(n.getElseStatement());
	}
	static boolean longerFirst(final ConditionalExpression n) {
		return n.getThenExpression().getLength() > threshold + n.getElseExpression().getLength();
	}
}
