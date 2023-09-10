package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;
import java.util.stream.StreamSupport;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    /** 预处理后的中间代码列表 */
    private final List<Instruction> newInstructions = new ArrayList<>();

    /** 每个变量的剩余出现次数 */
    private final Map<IRVariable, Integer> counter = new HashMap<>();

    /** 空闲寄存器栈 */
    private final Stack<String> freeRegs = new Stack<>();

    /** 变量以及存储它的寄存器map */
    private final Map<IRVariable, String> IRVariableToReg = new HashMap<>();

    /** 最后的汇编代码列表 */
    private final List<AsmCode> asmCodes = new ArrayList<>();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        for (Instruction instruction : originInstructions){
            InstructionKind instructionKind = instruction.getKind();
            // 遇到Ret指令后直接舍弃后续指令
            if (instructionKind.isReturn()){
                newInstructions.add(instruction);
                break;
            }
            // 遇到Mov指令直接添加
            if (instructionKind.isUnary()){
                newInstructions.add(instruction);
            }
            // 二元指令
            else if (instructionKind.isBinary()){
                IRValue lhs = instruction.getLHS();
                IRValue rhs = instruction.getRHS();
                IRVariable result = instruction.getResult();
                // 将操作两个立即数的BinaryOp直接进行求值得到结果，然后替换成MOV指令
                if (lhs.isImmediate() && rhs.isImmediate()){
                    int resultInt = 0;
                    switch (instructionKind) {
                        case ADD -> resultInt = ((IRImmediate)lhs).getValue() + ((IRImmediate)rhs).getValue();
                        case SUB -> resultInt = ((IRImmediate)lhs).getValue() - ((IRImmediate)rhs).getValue();
                        case MUL -> resultInt = ((IRImmediate)lhs).getValue() * ((IRImmediate)rhs).getValue();
                        default -> System.err.println("instructionKind error!");
                    }
                    newInstructions.add(Instruction.createMov(result, IRImmediate.of(resultInt)));
                }
                // 左操作数是立即数
                else if (lhs.isImmediate()){
                    switch (instructionKind) {
                        // 若是加法，则交换左右两操作数位置
                        case ADD -> newInstructions.add(Instruction.createAdd(result, rhs, lhs));
                        // 若是乘法，则先插入一条mov指令，将指令调整为无立即数指令
                        case MUL -> {
                            IRVariable temp = IRVariable.temp();
                            newInstructions.add(Instruction.createMov(temp, lhs));
                            newInstructions.add(Instruction.createMul(result, temp, rhs));
                        }
                        // 若是减法，则先插入一条mov指令，将指令调整为无立即数指令
                        case SUB -> {
                            newInstructions.add(Instruction.createMov(result, lhs));
                            newInstructions.add(Instruction.createSub(result, result, rhs));
                        }
                        default -> System.err.println("instructionKind error!");
                    }
                }
                // 右操作数是立即数
                else if (rhs.isImmediate()){
                    switch (instructionKind) {
                        // 若是加法则直接添加
                        case ADD -> newInstructions.add(instruction);
                        // 若是减法同样直接添加
                        case SUB -> newInstructions.add(instruction);
                        // 若是乘法，则先插入一条mov指令，将指令调整为无立即数指令
                        case MUL -> {
                            IRVariable temp = IRVariable.temp();
                            newInstructions.add(Instruction.createMov(temp, rhs));
                            newInstructions.add(Instruction.createMul(result, lhs, temp));
                        }
                        default -> System.err.println("instructionKind error!");
                    }
                }
                else {
                    newInstructions.add(instruction);
                }
            }
        }
    }


    /** 将variable的出现次数加一 */
    private void addCounter(IRVariable variable){
        if (!counter.containsKey(variable)) {
            counter.put(variable, 1);
        }
        else{
            counter.put(variable, counter.get(variable) + 1);
        }
    }


    /** 将variable的出现次数减一，若为0说明不再被使用，释放其寄存器 */
    private void subCounter(IRVariable variable){
        int count = counter.get(variable)-1;
        if (count == 0){
            freeRegs.push(IRVariableToReg.get(variable));
            IRVariableToReg.remove(variable);
        }
        counter.put(variable, count);
    }


    /** 初始化变量出现次数的map */
    private void initCounter(){
        for (Instruction instruction : newInstructions) {
            InstructionKind instructionKind = instruction.getKind();
            // ret
            if (instructionKind.isReturn()){
                IRValue returnValue = instruction.getReturnValue();
                if (returnValue.isIRVariable()){
                    addCounter((IRVariable)returnValue);
                }
            }
            // mov
            else if (instructionKind.isUnary()){
                IRValue from = instruction.getFrom();
                if (from.isIRVariable()){
                    addCounter((IRVariable)from);
                }
            }
            // add sub mul
            else if (instructionKind.isBinary()){
                IRValue lhs = instruction.getLHS();
                IRValue rhs = instruction.getRHS();
                if (lhs.isIRVariable()){
                    addCounter((IRVariable)lhs);
                }
                if (rhs.isIRVariable()){
                    addCounter((IRVariable)rhs);
                }
            }
        }
    }


    /** 初始化空闲寄存器栈 */
    private void initFreeRegs(){
        for (int i = 0; i <= 6; i++){
            freeRegs.push("t" + i);
        }
    }


    /** 返回存储指定变量的寄存器或分配一个新寄存器 */
    private String getReg(IRVariable variable) {
        if (IRVariableToReg.containsKey(variable)) {
            return IRVariableToReg.get(variable);
        } else {
            String reg = freeRegs.pop();
            IRVariableToReg.put(variable, reg);
            return reg;
        }
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        // 初始化所有变量的出现次数
        initCounter();
        // 初始化空闲寄存器栈
        initFreeRegs();
        asmCodes.add(new AsmCode(".text"));
        for (Instruction instruction : newInstructions){
            InstructionKind instructionKind = instruction.getKind();
            // ret
            if (instructionKind.isReturn()){
                IRVariable returnValue = (IRVariable)(instruction.getReturnValue());
                asmCodes.add(AsmCode.createRet(getReg(returnValue), instruction));
                subCounter(returnValue);
            }
            // mov
            else if (instructionKind.isUnary()){
                IRVariable result = instruction.getResult();
                IRValue from = instruction.getFrom();
                // from是立即数，生成li指令
                if (from.isImmediate()){
                    asmCodes.add(AsmCode.createLi(getReg(result), from, instruction));
                }
                // from是变量，生成mv指令
                else {
                    String fromReg = IRVariableToReg.get((IRVariable)from);
                    // 先减少对from的引用，可使得若from后续不活跃，则result选择from独占的寄存器
                    subCounter((IRVariable)from);
                    asmCodes.add(AsmCode.createMov(getReg(result), fromReg, instruction));
                }
            }
            else {
                IRVariable result = instruction.getResult();
                IRValue lhs = instruction.getLHS();
                IRValue rhs = instruction.getRHS();
                switch (instructionKind) {
                    case ADD -> {
                        // add
                        if (rhs.isIRVariable()){
                            String rhsReg = IRVariableToReg.get((IRVariable)rhs);
                            String lhsReg = IRVariableToReg.get((IRVariable)lhs);
                            // 先减少对两个操作数的引用，可使得若lhs后续不活跃，则result选择lhs独占的寄存器
                            subCounter((IRVariable)rhs);
                            subCounter((IRVariable)lhs);
                            asmCodes.add(AsmCode.createAdd(getReg(result), lhsReg, rhsReg, instruction));
                        }
                        // addi
                        else {
                            String lhsReg = IRVariableToReg.get((IRVariable)lhs);
                            // 先减少对lhs操作数的引用，可使得若lhs后续不活跃，则result选择lhs独占的寄存器
                            subCounter((IRVariable)lhs);
                            asmCodes.add(AsmCode.createAddi(getReg(result), lhsReg, rhs, instruction));
                        }
                    }
                    case SUB -> {
                        // sub
                        if (rhs.isIRVariable()){
                            String rhsReg = IRVariableToReg.get((IRVariable)rhs);
                            String lhsReg = IRVariableToReg.get((IRVariable)lhs);
                            // 先减少对两个操作数的引用，可使得若lhs后续不活跃，则result选择lhs独占的寄存器
                            subCounter((IRVariable)rhs);
                            subCounter((IRVariable)lhs);
                            asmCodes.add(AsmCode.createSub(getReg(result), lhsReg, rhsReg, instruction));
                        }
                        // subi
                        else {
                            String lhsReg = IRVariableToReg.get((IRVariable)lhs);
                            // 先减少对lhs操作数的引用，可使得若lhs后续不活跃，则result选择lhs独占的寄存器
                            subCounter((IRVariable)lhs);
                            asmCodes.add(AsmCode.createSubi(getReg(result), lhsReg, rhs, instruction));
                        }
                    }
                    case MUL -> {
                        // mul
                        String rhsReg = IRVariableToReg.get((IRVariable)rhs);
                        String lhsReg = IRVariableToReg.get((IRVariable)lhs);
                        // 先减少对两个操作数的引用，可使得若lhs后续不活跃，则result选择lhs独占的寄存器
                        subCounter((IRVariable)rhs);
                        subCounter((IRVariable)lhs);
                        asmCodes.add(AsmCode.createMul(getReg(result), lhsReg, rhsReg, instruction));
                    }
                    default -> System.err.println("instructionKind error!");
                }
            }
        }

    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, asmCodes.stream().map(AsmCode::toString).toList());
    }
}

