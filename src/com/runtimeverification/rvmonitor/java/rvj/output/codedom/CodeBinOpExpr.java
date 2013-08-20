package com.runtimeverification.rvmonitor.java.rvj.output.codedom;

import com.runtimeverification.rvmonitor.java.rvj.output.NotImplementedException;
import com.runtimeverification.rvmonitor.java.rvj.output.codedom.helper.ICodeFormatter;
import com.runtimeverification.rvmonitor.java.rvj.output.codedom.type.CodeType;

public class CodeBinOpExpr extends CodeExpr {
	private final Operator operator;
	private final CodeExpr lhs;
	private final CodeExpr rhs;
	
	public enum Operator {
		LAND,
		LOR,
		IDENTICAL,
		NOTIDENTICAL,
		GREATER,
		LESS,
	}
	
	private CodeBinOpExpr(CodeType type, Operator operator, CodeExpr lhs, CodeExpr rhs) {
		super(type);
		
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = rhs;
		
		this.validate();
	}
	
	public static CodeBinOpExpr logicalAnd(CodeExpr lhs, CodeExpr rhs) {
		return new CodeBinOpExpr(CodeType.bool(), Operator.LAND, lhs, rhs);
	}

	public static CodeBinOpExpr logicalOr(CodeExpr lhs, CodeExpr rhs) {
		return new CodeBinOpExpr(CodeType.bool(), Operator.LOR, lhs, rhs);
	}
	
	public static CodeBinOpExpr identical(CodeExpr lhs, CodeExpr rhs) {
		return new CodeBinOpExpr(CodeType.bool(), Operator.IDENTICAL, lhs, rhs);
	}

	public static CodeBinOpExpr notIdentical(CodeExpr lhs, CodeExpr rhs) {
		return new CodeBinOpExpr(CodeType.bool(), Operator.NOTIDENTICAL, lhs, rhs);
	}

	public static CodeBinOpExpr greater(CodeExpr lhs, CodeExpr rhs) {
		return new CodeBinOpExpr(CodeType.bool(), Operator.GREATER, lhs, rhs);
	}

	public static CodeBinOpExpr less(CodeExpr lhs, CodeExpr rhs) {
		return new CodeBinOpExpr(CodeType.bool(), Operator.LESS, lhs, rhs);
	}
	
	public static CodeBinOpExpr isNull(CodeExpr lhs) {
		return CodeBinOpExpr.identical(lhs, CodeLiteralExpr.nul());
	}

	public static CodeExpr isNotNull(CodeExpr lhs) {
		return CodeBinOpExpr.notIdentical(lhs, CodeLiteralExpr.nul());
	}
	
	private void validate() {
		if (this.lhs == null)
			throw new IllegalArgumentException();
		if (this.rhs == null)
			throw new IllegalArgumentException();
		
		// In order to catch unexpected cases thoroughly, this method checks
		// very aggressively. What Java allows can be rejected by this method, if
		// that is never meant to be used in the generated code. For example,
		// "p == q" is valid in Java even if their types are different; however,
		// this method will intentionally reject it.
		switch (this.operator) {
		case LAND:
		case LOR:
			if (!(this.lhs.type.isBool() && this.rhs.type.isBool()))
				throw new NotImplementedException();
			break;
		case IDENTICAL:
		case NOTIDENTICAL:
			if (this.lhs != CodeLiteralExpr.nul() && this.rhs != CodeLiteralExpr.nul()) {
				if (!this.lhs.type.equals(this.rhs.type))
					throw new NotImplementedException();
			}
			break;
		case GREATER:
		case LESS:
			if (!this.lhs.type.equals(this.rhs.type))
				throw new NotImplementedException();
			if (!(this.lhs.type.isInteger() || this.lhs.type.isLong()))
				throw new NotImplementedException();
			break;
		default:
			throw new NotImplementedException();
		}
	}

	@Override
	public void getCode(ICodeFormatter fmt) {
		// This method always puts a pair of parentheses around this expression.
		// As a result, the resulting code may have unnecessary parentheses, but
		// that won't do any harm.
		fmt.operator("(");
		{
			this.lhs.getCode(fmt);
	
			String op;
			switch (this.operator) {
			case LAND:
				op = "&&";
				break;
			case LOR:
				op = "||";
				break;
			case IDENTICAL:
				op = "==";
				break;
			case NOTIDENTICAL:
				op = "!=";
				break;
			case GREATER:
				op = ">";
				break;
			case LESS:
				op = "<";
				break;
			default:
				throw new NotImplementedException();
			}
			fmt.operator(op);
	
			this.rhs.getCode(fmt);
		}
		fmt.operator(")");
	}

}