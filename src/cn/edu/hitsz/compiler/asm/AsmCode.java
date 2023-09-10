package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.*;

import java.util.List;

public class AsmCode {

    private String instructionKind = null;
    private String result = null;
    private String lhs = null;
    private String rhs = null;
    private String ir = null;
    private String text = null;

    public AsmCode(String instructionKind, String result, String lhs, String rhs, String ir){
        this.instructionKind = instructionKind;
        this.result = result;
        this.lhs = lhs;
        this.rhs = rhs;
        this.ir = ir;
    }

    public AsmCode(String text){
        this.text = text;
    }

    public static AsmCode createAdd(String result, String lhs, String rhs, Instruction ir) {
        return new AsmCode("add", result, lhs, rhs, ir.toString());
    }

    public static AsmCode createAddi(String result, String lhs, IRValue rhs, Instruction ir) {
        return new AsmCode("addi", result, lhs, Integer.toString(((IRImmediate)rhs).getValue()), ir.toString());
    }

    public static AsmCode createSub(String result, String lhs, String rhs, Instruction ir) {
        return new AsmCode("sub", result, lhs, rhs, ir.toString());
    }

    public static AsmCode createSubi(String result, String lhs, IRValue rhs, Instruction ir) {
        return new AsmCode("subi", result, lhs, Integer.toString(((IRImmediate)rhs).getValue()), ir.toString());
    }

    public static AsmCode createMul(String result, String lhs, String rhs, Instruction ir) {
        return new AsmCode("mul", result, lhs, rhs, ir.toString());
    }

    public static AsmCode createMov(String result, String lhs, Instruction ir) {
        return new AsmCode("mv", result, lhs, null, ir.toString());
    }

    public static AsmCode createRet(String lhs, Instruction ir) {
        return new AsmCode("mv", "a0", lhs, null, ir.toString());
    }

    public static AsmCode createLi(String result, IRValue lhs, Instruction ir) {
        return new AsmCode("li", result, Integer.toString(((IRImmediate)lhs).getValue()), null, ir.toString());
    }

    @Override
    public String toString() {
        if (".text".equals(text)) {
            return text;
        }
        String line = "\t" + instructionKind + " " + result;
        if (lhs != null) {
            line += ", " + lhs;
        }
        if (rhs != null) {
            line += ", " + rhs;
        }
        if (ir != null) {
            line += "\t\t#  " + ir;
        }
        return line;
    }

}
