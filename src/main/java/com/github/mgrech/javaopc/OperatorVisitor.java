package com.github.mgrech.javaopc;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

public class OperatorVisitor extends TreeVisitor
{
	private static final String JAVA_LANG_STRING = "java.lang.String";

	private final SymbolResolver resolver;

	public OperatorVisitor(SymbolResolver resolver)
	{
		this.resolver = resolver;
	}

	private void visit(BinaryExpr expr, ResolvedReferenceType leftType, ResolvedReferenceType rightType)
	{
		expr.replace(new MethodCallExpr(expr.getLeft(), "opSum", NodeList.nodeList(expr.getRight())));
	}

	@Override
	public void process(Node node)
	{
		if(node instanceof BinaryExpr)
		{
			var opnode = (BinaryExpr)node;
			var left = opnode.getLeft();
			var right = opnode.getRight();
			var leftType = resolver.calculateType(left);
			var rightType = resolver.calculateType(right);

			if(leftType.isReferenceType() && rightType.isReferenceType()
			&& !leftType.asReferenceType().getQualifiedName().equals(JAVA_LANG_STRING)
			&& !rightType.asReferenceType().getQualifiedName().equals(JAVA_LANG_STRING))
			{
				visit(opnode, leftType.asReferenceType(), rightType.asReferenceType());
			}
		}
	}
}
