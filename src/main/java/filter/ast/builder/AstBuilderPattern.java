package filter.ast.builder;

import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayList;
import java.util.List;

public class AstBuilderPattern {

  // Public entry point
  // query : expr EOF
  public Expr translate(FilterParser.QueryContext ctx) {
    return buildExpr(ctx.getRuleContext(FilterParser.ExprContext.class, 0));
  }

  // expr : orExpr
  private Expr buildExpr(FilterParser.ExprContext ctx) {
    return buildOrExpr(ctx.getRuleContext(FilterParser.OrExprContext.class, 0));
  }

  // orExpr : andExpr (OR andExpr)*
  private Expr buildOrExpr(FilterParser.OrExprContext ctx) {
    List<FilterParser.AndExprContext> parts =
        ctx.getRuleContexts(FilterParser.AndExprContext.class);

    Expr result = buildAndExpr(parts.get(0));

    for (int i = 1; i < parts.size(); i++) {
      result = new Expr.Or(result, buildAndExpr(parts.get(i)));
    }

    return result;
  }

  // andExpr : notExpr (AND notExpr)*
  private Expr buildAndExpr(FilterParser.AndExprContext ctx) {
    List<FilterParser.NotExprContext> parts =
        ctx.getRuleContexts(FilterParser.NotExprContext.class);

    Expr result = buildNotExpr(parts.get(0));

    for (int i = 1; i < parts.size(); i++) {
      result = new Expr.And(result, buildNotExpr(parts.get(i)));
    }

    return result;
  }

  // notExpr : NOT notExpr | primary
  private Expr buildNotExpr(FilterParser.NotExprContext ctx) {
    if ("not".equalsIgnoreCase(ctx.getChild(0).getText())) {
      FilterParser.NotExprContext inner = ctx.getRuleContext(FilterParser.NotExprContext.class, 0);

      return new Expr.Not(buildNotExpr(inner));
    }

    return buildPrimary(ctx.getRuleContext(FilterParser.PrimaryContext.class, 0));
  }

  // primary : comparison | '(' expr ')'
  private Expr buildPrimary(FilterParser.PrimaryContext ctx) {
    FilterParser.ComparisonContext comparison =
        ctx.getRuleContext(FilterParser.ComparisonContext.class, 0);

    if (comparison != null) {
      return buildComparison(comparison);
    }

    return buildExpr(ctx.getRuleContext(FilterParser.ExprContext.class, 0));
  }

  // comparison
  //    : IDENTIFIER op=COMPOP value=literal
  //    | IDENTIFIER IN '(' literalList ')'
  private Expr buildComparison(FilterParser.ComparisonContext ctx) {
    String field = ctx.getChild(0).getText();
    String operatorOrIn = ctx.getChild(1).getText();

    if ("in".equalsIgnoreCase(operatorOrIn)) {
      List<Value> values =
          buildLiteralList(ctx.getRuleContext(FilterParser.LiteralListContext.class, 0));

      return new Expr.InList(field, values);
    }

    FilterParser.LiteralContext literal = ctx.getRuleContext(FilterParser.LiteralContext.class, 0);

    return new Expr.Comparison(field, toCompOp(operatorOrIn), buildLiteral(literal));
  }

  // literalList : literal (',' literal)*
  private List<Value> buildLiteralList(FilterParser.LiteralListContext ctx) {
    List<Value> values = new ArrayList<>();

    for (FilterParser.LiteralContext literal :
        ctx.getRuleContexts(FilterParser.LiteralContext.class)) {
      values.add(buildLiteral(literal));
    }

    return values;
  }

  // literal : STRING | NUMBER
  private Value buildLiteral(FilterParser.LiteralContext ctx) {
    return toValue(ctx.getText());
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
