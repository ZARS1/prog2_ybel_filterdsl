package filter.ast.builder;

import filter.FilterBaseVisitor;
import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AstBuilderVisitor extends FilterBaseVisitor<Void> {

  private final Deque<Expr> exprStack = new ArrayDeque<>();
  private final Deque<Value> valueStack = new ArrayDeque<>();
  private final Deque<List<Value>> valueListStack = new ArrayDeque<>();

  // Public entry point
  public Expr translate(FilterParser.QueryContext ctx) {
    exprStack.clear();
    valueStack.clear();
    valueListStack.clear();

    visit(ctx);

    return exprStack.pop();
  }

  // query : expr EOF
  @Override
  public Void visitQuery(FilterParser.QueryContext ctx) {
    visit(ctx.getRuleContext(FilterParser.ExprContext.class, 0));
    return null;
  }

  // expr : orExpr
  @Override
  public Void visitExpr(FilterParser.ExprContext ctx) {
    visit(ctx.getRuleContext(FilterParser.OrExprContext.class, 0));
    return null;
  }

  // orExpr : andExpr (OR andExpr)*
  @Override
  public Void visitOrExpr(FilterParser.OrExprContext ctx) {
    List<FilterParser.AndExprContext> parts =
        ctx.getRuleContexts(FilterParser.AndExprContext.class);

    List<Expr> expressions = new ArrayList<>();

    for (FilterParser.AndExprContext part : parts) {
      visit(part);
      expressions.add(exprStack.pop());
    }

    Expr result = expressions.get(0);

    for (int i = 1; i < expressions.size(); i++) {
      result = new Expr.Or(result, expressions.get(i));
    }

    exprStack.push(result);
    return null;
  }

  // andExpr : notExpr (AND notExpr)*
  @Override
  public Void visitAndExpr(FilterParser.AndExprContext ctx) {
    List<FilterParser.NotExprContext> parts =
        ctx.getRuleContexts(FilterParser.NotExprContext.class);

    List<Expr> expressions = new ArrayList<>();

    for (FilterParser.NotExprContext part : parts) {
      visit(part);
      expressions.add(exprStack.pop());
    }

    Expr result = expressions.get(0);

    for (int i = 1; i < expressions.size(); i++) {
      result = new Expr.And(result, expressions.get(i));
    }

    exprStack.push(result);
    return null;
  }

  // notExpr : NOT notExpr | primary
  @Override
  public Void visitNotExpr(FilterParser.NotExprContext ctx) {
    if ("not".equalsIgnoreCase(ctx.getChild(0).getText())) {
      visit(ctx.getRuleContext(FilterParser.NotExprContext.class, 0));

      Expr inner = exprStack.pop();
      exprStack.push(new Expr.Not(inner));
    } else {
      visit(ctx.getRuleContext(FilterParser.PrimaryContext.class, 0));
    }

    return null;
  }

  // primary : comparison | '(' expr ')'
  @Override
  public Void visitPrimary(FilterParser.PrimaryContext ctx) {
    FilterParser.ComparisonContext comparison =
        ctx.getRuleContext(FilterParser.ComparisonContext.class, 0);

    if (comparison != null) {
      visit(comparison);
    } else {
      visit(ctx.getRuleContext(FilterParser.ExprContext.class, 0));
    }

    return null;
  }

  // comparison
  //    : IDENTIFIER op=COMPOP value=literal
  //    | IDENTIFIER IN '(' literalList ')'
  @Override
  public Void visitComparison(FilterParser.ComparisonContext ctx) {
    String field = ctx.getChild(0).getText();
    String operatorOrIn = ctx.getChild(1).getText();

    if ("in".equalsIgnoreCase(operatorOrIn)) {
      visit(ctx.getRuleContext(FilterParser.LiteralListContext.class, 0));

      List<Value> values = valueListStack.pop();
      exprStack.push(new Expr.InList(field, values));
    } else {
      FilterParser.LiteralContext literal =
          ctx.getRuleContext(FilterParser.LiteralContext.class, 0);

      visit(literal);

      Value value = valueStack.pop();
      CompOp op = toCompOp(operatorOrIn);

      exprStack.push(new Expr.Comparison(field, op, value));
    }

    return null;
  }

  // literalList : literal (',' literal)*
  @Override
  public Void visitLiteralList(FilterParser.LiteralListContext ctx) {
    List<Value> values = new ArrayList<>();

    for (FilterParser.LiteralContext literal :
        ctx.getRuleContexts(FilterParser.LiteralContext.class)) {
      visit(literal);
      values.add(valueStack.pop());
    }

    valueListStack.push(values);
    return null;
  }

  // literal : STRING | NUMBER
  @Override
  public Void visitLiteral(FilterParser.LiteralContext ctx) {
    valueStack.push(toValue(ctx.getText()));
    return null;
  }

  private static Value toValue(String text) {
    if (text.startsWith("\"") && text.endsWith("\"")) {
      return new Value.Str(unquote(text));
    }

    return new Value.Num(Integer.parseInt(text));
  }

  private static String unquote(String text) {
    return text.substring(1, text.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
  }

  private static CompOp toCompOp(String text) {
    return switch (text) {
      case "==" -> CompOp.EQ;
      case "!=" -> CompOp.NE;
      case "<" -> CompOp.LT;
      case "<=" -> CompOp.LE;
      case ">" -> CompOp.GT;
      case ">=" -> CompOp.GE;
      default -> throw new IllegalArgumentException("Unknown comparison operator: " + text);
    };
  }
}
