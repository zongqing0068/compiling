package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private SymbolTable symbolTable;
    /** 中间代码列表 */
    private List<Instruction> IRList = new ArrayList<>();
//    /** token栈 */
//    private final Stack<Token> tokenStack = new Stack<>();
    /** value栈 */
    private final Stack<IRValue> valueStack = new Stack<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
//        // 压入token
//        tokenStack.push(currentToken);

        // 压入value，需要判断是立即数还是变量
        String toMatch = "^[0-9]+$";
        String text = currentToken.getText();
        if(text.matches(toMatch)){
            valueStack.push(IRImmediate.of(Integer.parseInt(text)));
        } else {
            valueStack.push(IRVariable.named(text));
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        IRVariable result;
        IRValue lhs;
        IRValue rhs;
        IRValue from;
        IRValue returnValue;
        IRVariable variableTemp;
        IRValue curValue;
        switch (production.index()){
            case 6:
                // S -> id = E
                // 弹出E的value
                from = valueStack.pop();
                // 弹出=的value占位符
                valueStack.pop();
                // 弹出id的value
                result = (IRVariable)valueStack.pop();
                // 生成赋值指令的三地址指令code
                IRList.add(Instruction.createMov(result, from));
                // 压入S的value占位符
                valueStack.push(null);
                break;
            case 7:
                // S -> return E
                // 弹出E的value
                returnValue = valueStack.pop();
                // 弹出return的value占位符
                valueStack.pop();
                // 生成返回指令的三地址指令code
                IRList.add(Instruction.createRet(returnValue));
                // 压入S的value占位符
                valueStack.push(null);
                break;
            case 8:
                // E -> E + A
                // 弹出A的value
                rhs = valueStack.pop();
                // 弹出+的value占位符
                valueStack.pop();
                // 弹出E的value
                lhs = valueStack.pop();
                // 产生一个临时变量
                variableTemp = IRVariable.temp();
                // 生成赋值指令的三地址指令code
                IRList.add(Instruction.createAdd(variableTemp, lhs, rhs));
                // 压入S的value占位符
                valueStack.push(variableTemp);
                break;
            case 9:
                // E -> E - A
                // 弹出A的value
                rhs = valueStack.pop();
                // 弹出-的value占位符
                valueStack.pop();
                // 弹出E的value
                lhs = valueStack.pop();
                // 产生一个临时变量
                variableTemp = IRVariable.temp();
                // 生成赋值指令的三地址指令code
                IRList.add(Instruction.createSub(variableTemp, lhs, rhs));
                // 压入S的value占位符
                valueStack.push(variableTemp);
                break;
            case 11:
                // A -> A * B
                // 弹出B的value
                rhs = valueStack.pop();
                // 弹出*的value占位符
                valueStack.pop();
                // 弹出A的value
                lhs = valueStack.pop();
                // 产生一个临时变量
                variableTemp = IRVariable.temp();
                // 生成赋值指令的三地址指令code
                IRList.add(Instruction.createMul(variableTemp, lhs, rhs));
                // 压入A的value占位符
                valueStack.push(variableTemp);
                break;
            case 13:
                // B -> ( E )
                // 弹出)的value占位符
                valueStack.pop();
                // 弹出E的value
                curValue = valueStack.pop();
                // 弹出(的value占位符
                valueStack.pop();
                // 压入B的value
                valueStack.push(curValue);
                break;
            case 10:
                // E -> A，操作与case 15一致
            case 12:
                // A -> B，操作与case 15一致
            case 14:
                // B -> id，操作与case 15一致
            case 15:
                // B -> IntConst
                // 弹出IntConst的value
                curValue = valueStack.pop();
                // 压入B的value(事实上这两句的效果可以抵消)
                valueStack.push(curValue);
                break;
            default:
                // 弹出与产生式右部相同长度的token和type
                for(int i = 0; i < production.body().size(); i++){
                    valueStack.pop();
                }
                // 压入代表产生式左部的token和type的空占位符
                valueStack.push(null);
        }

    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // nothing to do
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return IRList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

