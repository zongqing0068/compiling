package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String input = "";
    private final List<Token> tokens = new ArrayList<>(); // 保存词法单元的列表

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        try {
            FileInputStream fileInputStream = new FileInputStream(path);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                input = input.concat(line + " ");
            }
            fileInputStream.close();
            inputStreamReader.close();
            bufferedReader.close();
//            System.out.println("LoadFile successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.print(input);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        char ch;
        String token;
        char[] codes = input.toCharArray();
        int len = codes.length;
        int i = 0;
        int begin;
        while (i < len) {
            ch = codes[i];
            while (Character.isWhitespace(ch) && (i + 1 < len)) {
                ch = codes[++i];
            }
            begin = i;
            // 当前字符为关键字或标识符的开始
            if (isAlpha(ch) && (i + 1 < len)) {
                while (isAlNum(ch) && (i + 1 < len)) {
                    ch = codes[++i];
                }
                --i;
                String cur = input.substring(begin, i + 1);
                // 当前字符串为关键字
                if (TokenKind.isAllowed(cur)) {
                    tokens.add(Token.simple(cur));
                }
                // 当前字符串为标识符
                else {
                    tokens.add(Token.normal("id", cur));
                    // 若符号表中还没有该标识符则需要加入
                    if (!symbolTable.has(cur)) {
                        symbolTable.add(cur);
                    }
                }
            }
            // 当前字符为数字的开始
            else if (isDigit(ch)) {
                while (isDigit(ch) && (i + 1 < len)) {
                    ch = codes[++i];
                }
                --i;
                String cur = input.substring(begin, i + 1);
                tokens.add(Token.normal("IntConst", cur));
            } else {
                switch (ch) {
                    case '=':
                        tokens.add(Token.simple("="));
                        break;
                    case ',':
                        tokens.add(Token.simple(","));
                        break;
                    case ';':
                        tokens.add(Token.simple("Semicolon"));
                        break;
                    case '+':
                        tokens.add(Token.simple("+"));
                        break;
                    case '-':
                        tokens.add(Token.simple("-"));
                        break;
                    case '*':
                        tokens.add(Token.simple("*"));
                        break;
                    case '/':
                        tokens.add(Token.simple("/"));
                        break;
                    case '(':
                        tokens.add(Token.simple("("));
                        break;
                    case ')':
                        tokens.add(Token.simple(")"));
                        break;
                    default:
                }
            }
            i++;
        }
        tokens.add(Token.eof());
    }

    /** 判断当前字符是否为数字 */
    public boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    /** 判断当前字符是否为字母 */
    public boolean isAlpha(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    /** 判断当前字符是否为数字或字母 */
    public boolean isAlNum(char ch) {
        return isDigit(ch) || isAlpha(ch);
    }


    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
