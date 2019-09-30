package dev.mgrech.javaopc;

import com.github.javaparser.ast.expr.*;

public interface ExprRewritingVisitor
{
	default Expression visit(ArrayAccessExpr expr)
	{
		 return null;
	}

	default Expression visit(ArrayCreationExpr expr)
	{
		 return null;
	}

	default Expression visit(ArrayInitializerExpr expr)
	{
		 return null;
	}

	default Expression visit(AssignExpr expr)
	{
		 return null;
	}

	default Expression visit(BinaryExpr expr)
	{
		 return null;
	}

	default Expression visit(BooleanLiteralExpr expr)
	{
		 return null;
	}

	default Expression visit(CastExpr expr)
	{
		 return null;
	}

	default Expression visit(CharLiteralExpr expr)
	{
		 return null;
	}

	default Expression visit(ClassExpr expr)
	{
		 return null;
	}

	default Expression visit(ConditionalExpr expr)
	{
		 return null;
	}

	default Expression visit(DoubleLiteralExpr expr)
	{
		 return null;
	}

	default Expression visit(EnclosedExpr expr)
	{
		 return null;
	}

	default Expression visit(FieldAccessExpr expr)
	{
		 return null;
	}

	default Expression visit(InstanceOfExpr expr)
	{
		 return null;
	}

	default Expression visit(IntegerLiteralExpr expr)
	{
		 return null;
	}

	default Expression visit(LambdaExpr expr)
	{
		 return null;
	}

	default Expression visit(LongLiteralExpr expr)
	{
		 return null;
	}

	default Expression visit(MethodCallExpr expr)
	{
		 return null;
	}

	default Expression visit(MethodReferenceExpr expr)
	{
		 return null;
	}

	default Expression visit(NameExpr expr)
	{
		 return null;
	}

	default Expression visit(NullLiteralExpr expr)
	{
		 return null;
	}

	default Expression visit(ObjectCreationExpr expr)
	{
		 return null;
	}

	default Expression visit(StringLiteralExpr expr)
	{
		 return null;
	}

	default Expression visit(SuperExpr expr)
	{
		 return null;
	}

	default Expression visit(SwitchExpr expr)
	{
		 return null;
	}

	default Expression visit(ThisExpr expr)
	{
		 return null;
	}

	default Expression visit(UnaryExpr expr)
	{
		 return null;
	}

	default Expression visit(VariableDeclarationExpr expr)
	{
		return null;
	}
}
