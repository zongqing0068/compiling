package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private SymbolTable symbolTable;
    /** token栈 */
    private final Stack<Token> tokenStack = new Stack<>();
    /** type栈 */
    private final Stack<SourceCodeType> typeStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // nothing to do
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        Token curToken;
        SourceCodeType curType;
        switch (production.index()){
            case 4:
                // S -> D id
                // 弹出id的token
                curToken = tokenStack.pop();
                // 弹出D的token
                tokenStack.pop();
                // 弹出id的type
                typeStack.pop();
                // 弹出D的type
                curType = typeStack.pop();
                // 将符号表中id的type更新为D的type
                this.symbolTable.get(curToken.getText()).setType(curType);
                // 压入S的token占位符
                tokenStack.push(null);
                // 压入S的type占位符
                typeStack.push(null);
                break;
            case 5:
                // D -> int
                // 弹出int的token
                tokenStack.pop();
                // 弹出int的type
                curType = typeStack.pop();
                // 压入D的token的空占位符
                tokenStack.push(null);
                // 压入D的type(事实上该行和弹出int的type在效果上可以抵消)
                typeStack.push(curType);
                break;
            default:
                // 弹出与产生式右部相同长度的token和type
                for(int i = 0; i < production.body().size(); i++){
                    tokenStack.pop();
                    typeStack.pop();
                }
                // 压入代表产生式左部的token和type的空占位符
                tokenStack.push(null);
                typeStack.push(null);
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作

        // 压入token
        tokenStack.push(currentToken);

        // 压入type，只需关心SourceCodeType，而无需在意其它token kind
        // SourceCodeType中仅有int一项
        if(currentToken.getKind() == TokenKind.fromString("int")){
            typeStack.push(SourceCodeType.Int);
        } else {
            typeStack.push(null);
        }

    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.symbolTable = table;
    }
}

