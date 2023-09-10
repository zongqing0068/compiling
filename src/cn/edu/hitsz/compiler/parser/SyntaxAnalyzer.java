package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.*;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.*;

//TODO: 实验二: 实现 LR 语法分析驱动程序

/**
 * LR 语法分析驱动程序
 * <br>
 * 该程序接受词法单元串与 LR 分析表 (action 和 goto 表), 按表对词法单元流进行分析, 执行对应动作, 并在执行动作时通知各注册的观察者.
 * <br>
 * 你应当按照被挖空的方法的文档实现对应方法, 你可以随意为该类添加你需要的私有成员对象, 但不应该再为此类添加公有接口, 也不应该改动未被挖空的方法,
 * 除非你已经同助教充分沟通, 并能证明你的修改的合理性, 且令助教确定可能被改动的评测方法. 随意修改该类的其它部分有可能导致自动评测出错而被扣分.
 */
public class SyntaxAnalyzer {
    private final SymbolTable symbolTable;
    private final List<ActionObserver> observers = new ArrayList<>();
    /**  输入缓冲区 */
    private final Queue<Token> token_queue = new LinkedList<>();
    private LRTable lrTable;
    /**  符号栈 */
    private final Stack<Symbol> symbol_stack = new Stack<>();
    /**  状态栈 */
    private final Stack<Status> status_stack = new Stack<>();


    public SyntaxAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 注册新的观察者
     *
     * @param observer 观察者
     */
    public void registerObserver(ActionObserver observer) {
        observers.add(observer);
        observer.setSymbolTable(symbolTable);
    }

    /**
     * 在执行 shift 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param currentToken  当前词法单元
     */
    public void callWhenInShift(Status currentStatus, Token currentToken) {
        for (final var listener : observers) {
            listener.whenShift(currentStatus, currentToken);
        }
    }

    /**
     * 在执行 reduce 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     * @param production    待规约的产生式
     */
    public void callWhenInReduce(Status currentStatus, Production production) {
        for (final var listener : observers) {
            listener.whenReduce(currentStatus, production);
        }
    }

    /**
     * 在执行 accept 动作时通知各个观察者
     *
     * @param currentStatus 当前状态
     */
    public void callWhenInAccept(Status currentStatus) {
        for (final var listener : observers) {
            listener.whenAccept(currentStatus);
        }
    }

    public void loadTokens(Iterable<Token> tokens) {
        // TODO: 加载词法单元
        // 你可以自行选择要如何存储词法单元, 譬如使用迭代器, 或是栈, 或是干脆使用一个 list 全存起来
        // 需要注意的是, 在实现驱动程序的过程中, 你会需要面对只读取一个 token 而不能消耗它的情况,
        // 在自行设计的时候请加以考虑此种情况

        // 对于只读取一个 token 而不能消耗它的情况，可以使用队列和栈的peek函数
        for(Token token : tokens) {
            token_queue.offer(token);
        }

    }

    public void loadLRTable(LRTable table) {
        // TODO: 加载 LR 分析表
        // 你可以自行选择要如何使用该表格:
        // 是直接对 LRTable 调用 getAction/getGoto, 抑或是直接将 initStatus 存起来使用
        this.lrTable = table;
    }

    public void run() {
        // TODO: 实现驱动程序
        // 你需要根据上面的输入来实现 LR 语法分析的驱动程序
        // 请分别在遇到 Shift, Reduce, Accept 的时候调用上面的 callWhenInShift, callWhenInReduce, callWhenInAccept
        // 否则用于为实验二打分的产生式输出可能不会正常工作

        // 初始化符号栈
        symbol_stack.push(new Symbol(Token.eof()));

        // 初始化状态栈
        status_stack.push(lrTable.getInit());

        // flag用于判断是否循环结束
        boolean flag = true;
        do {
            Status curState = status_stack.peek();
            Token curToken = token_queue.peek();
            assert curToken != null;
            Action curAction = lrTable.getAction(curState, curToken);

            switch (curAction.getKind()) {
                case Shift : {
                    // 移入
                    callWhenInShift(curState, curToken);

                    final var shiftTo = curAction.getStatus();
                    // 压入状态
                    status_stack.push(shiftTo);
                    // 压入符号
                    symbol_stack.push(new Symbol(curToken));
                    // 弹出输入缓冲区队列中的符号
                    token_queue.poll();

                    break;
                }
                case Reduce : {
                    // 规约
                    Production curProduction = curAction.getProduction();
                    callWhenInReduce(curState, curProduction);

                    // 符号栈和状态栈中分别弹出产生式长度个的元素
                    for (int i = 0; i < curProduction.body().size(); i++){
                        status_stack.pop();
                        symbol_stack.pop();
                    }

                    // 当前的栈顶元素
                    curState = status_stack.peek();
                    // 获取当前产生式左部
                    NonTerminal head = curProduction.head();
                    // 压入状态
                    status_stack.push(lrTable.getGoto(curState, head));
                    // 压入符号
                    symbol_stack.push(new Symbol(head));

                    break;
                }
                case Accept : {
                    // 接收状态
                    callWhenInAccept(curState);

                    // 终止循环
                    flag = false;

                    break;
                }
                case Error : {
                    // 错误状态
                    System.err.println("Syntax analyzer refuses to accept!");

                    // 终止循环
                    flag = false;

                    break;
                }
                default : break;
            }

        } while (flag);

    }
}

/**  实现 Union<Token, NonTerminal> 的功能 */
class Symbol{
    Token token;
    NonTerminal nonTerminal;

    private Symbol(Token token, NonTerminal nonTerminal){
        this.token = token;
        this.nonTerminal = nonTerminal;
    }

    public Symbol(Token token){
        this(token, null);
    }

    public Symbol(NonTerminal nonTerminal){
        this(null, nonTerminal);
    }

    public boolean isToken(){
        return this.token != null;
    }

    public boolean isNonterminal(){
        return this.nonTerminal != null;
    }
}
