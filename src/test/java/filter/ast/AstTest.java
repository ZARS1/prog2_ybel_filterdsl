package filter.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.List;
import org.junit.jupiter.api.Test;

class AstTest {

  @Test
  void visitorBuildsStringComparison() {
    Expr ast = AstBuilders.fromQuery("artist == \"Beatles\"", new AstBuilderVisitor()::translate);

    Expr expected = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));

    assertEquals(expected, ast);
  }

  @Test
  void visitorBuildsNumberComparison() {
    Expr ast = AstBuilders.fromQuery("year == 1965", new AstBuilderVisitor()::translate);

    Expr expected = new Expr.Comparison("year", CompOp.EQ, new Value.Num(1965));

    assertEquals(expected, ast);
  }

  @Test
  void visitorBuildsAndExpression() {
    Expr ast =
        AstBuilders.fromQuery(
            "artist == \"Beatles\" and year == 1965", new AstBuilderVisitor()::translate);

    Expr expected =
        new Expr.And(
            new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")),
            new Expr.Comparison("year", CompOp.EQ, new Value.Num(1965)));

    assertEquals(expected, ast);
  }

  @Test
  void visitorBuildsNotExpression() {
    Expr ast =
        AstBuilders.fromQuery("not artist == \"Beatles\"", new AstBuilderVisitor()::translate);

    Expr expected =
        new Expr.Not(new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles")));

    assertEquals(expected, ast);
  }

  @Test
  void visitorBuildsInListExpression() {
    Expr ast =
        AstBuilders.fromQuery("genre in (\"rock\", \"jazz\")", new AstBuilderVisitor()::translate);

    Expr expected = new Expr.InList("genre", List.of(new Value.Str("rock"), new Value.Str("jazz")));

    assertEquals(expected, ast);
  }

  @Test
  void patternBuilderBuildsSameAstAsVisitor() {
    String query = "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"";

    Expr visitorAst = AstBuilders.fromQuery(query, new AstBuilderVisitor()::translate);
    Expr patternAst = AstBuilders.fromQuery(query, new AstBuilderPattern()::translate);

    assertEquals(visitorAst, patternAst);
  }

  @Test
  void simplifyRemovesDoubleNegation() {
    Expr comparison = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));
    Expr doubleNot = new Expr.Not(new Expr.Not(comparison));

    Expr simplified = AstBuilders.simplify(doubleNot);

    assertEquals(comparison, simplified);
  }
}
