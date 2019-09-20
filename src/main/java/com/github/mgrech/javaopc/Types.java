package com.github.mgrech.javaopc;

import com.github.javaparser.resolution.types.ResolvedType;

public class Types
{
	private static final String JAVA_LANG_STRING = "java.lang.String";

	public static boolean isJavaLangString(ResolvedType type)
	{
		return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(JAVA_LANG_STRING);
	}

	public static boolean isBuiltinType(ResolvedType type)
	{
		if(type.isPrimitive() || type.isArray())
			return true;

		if(!type.isReferenceType())
			return false;

		var decl = type.asReferenceType().getTypeDeclaration();

		if(!decl.getPackageName().equals("java.lang"))
			return false;

		var name = decl.getName();

		return name.equals("Boolean")
			|| name.equals("Byte")
			|| name.equals("Character")
			|| name.equals("Double")
			|| name.equals("Float")
			|| name.equals("Integer")
			|| name.equals("Long")
			|| name.equals("Short")
			|| name.equals("String");
	}

	public static boolean isFunctionalInterface(ResolvedType type)
	{
		return type.isReferenceType() && type.asReferenceType().getTypeDeclaration().isFunctionalInterface();
	}
}
