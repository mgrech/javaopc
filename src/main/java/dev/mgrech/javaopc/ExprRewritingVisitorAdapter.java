package dev.mgrech.javaopc;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;

public class ExprRewritingVisitorAdapter extends VoidVisitorAdapter<Void>
{
	private final ExprRewritingVisitor visitor;

	public ExprRewritingVisitorAdapter(ExprRewritingVisitor visitor)
	{
		this.visitor = visitor;
	}

	private void replaceIfNonNull(Expression expr, Expression replacement)
	{
		if(replacement != null)
			expr.replace(replacement);
	}

	@Override
	public void visit(BlockStmt stmt, Void arg)
	{
		// avoid ConcurrentModificationException
		var copy = new ArrayList<>(stmt.getStatements());
		copy.forEach(s -> s.accept(this, arg));

		stmt.getComment().ifPresent(c -> c.accept(this, arg));
	}

	@Override
	public void visit(ArrayAccessExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(ArrayCreationExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(ArrayInitializerExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(AssignExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(BinaryExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(BooleanLiteralExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(CastExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(CharLiteralExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(ClassExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(ConditionalExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(DoubleLiteralExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(EnclosedExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(FieldAccessExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(InstanceOfExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(IntegerLiteralExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(LambdaExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(LongLiteralExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(MethodCallExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(MethodReferenceExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(NameExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(NullLiteralExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(ObjectCreationExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(StringLiteralExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(SuperExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(ThisExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(UnaryExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(SwitchExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}

	@Override
	public void visit(VariableDeclarationExpr expr, Void arg)
	{
		super.visit(expr, arg);
		replaceIfNonNull(expr, visitor.visit(expr));
	}
}
