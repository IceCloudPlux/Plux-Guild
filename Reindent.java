import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Reindent {
    
    private static final int INDENT_SIZE = 4;
    
    public static void main(String[] args) throws IOException {
        Path srcDir = Paths.get("src/main/java");
        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    reindentFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("Done!");
    }
    
    private static void reindentFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        
        int offset = 0;
        if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
            offset = 3;
        }
        
        String content = new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        
        content = removeBlockComments(content);
        content = removeLineComments(content);
        
        content = formatCode(content);
        
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Reindented: " + file);
    }
    
    private static String removeBlockComments(String content) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean inBlockComment = false;
        char prevChar = '\0';
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (inBlockComment) {
                if (prevChar == '*' && c == '/') {
                    inBlockComment = false;
                }
                prevChar = c;
                continue;
            }
            
            if (!inString && !inChar && prevChar == '/' && c == '*') {
                inBlockComment = true;
                result.deleteCharAt(result.length() - 1);
                prevChar = c;
                continue;
            }
            
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            } else if (c == '\'' && prevChar != '\\') {
                inChar = !inChar;
            }
            
            result.append(c);
            prevChar = c;
        }
        
        return result.toString();
    }
    
    private static String removeLineComments(String content) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        char prevChar = '\0';
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    result.append(c);
                }
                continue;
            }
            
            if (!inString && !inChar && prevChar == '/' && c == '/') {
                inLineComment = true;
                result.deleteCharAt(result.length() - 1);
                continue;
            }
            
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            } else if (c == '\'' && prevChar != '\\') {
                inChar = !inChar;
            }
            
            result.append(c);
            prevChar = c;
        }
        
        return result.toString();
    }
    
    private static String formatCode(String content) {
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inLine = false;
        boolean prevWasBrace = false;
        
        char prevChar = '\0';
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            } else if (c == '\'' && prevChar != '\\') {
                inChar = !inChar;
            }
            
            if (!inString && !inChar) {
                if (c == '{') {
                    if (!prevWasBrace && inLine) {
                        result.append('\n');
                    }
                    if (prevChar != '\n') {
                        result.append('\n');
                    }
                    appendIndent(result, indentLevel);
                    result.append('{');
                    result.append('\n');
                    indentLevel++;
                    prevWasBrace = true;
                    inLine = false;
                    prevChar = c;
                    continue;
                }
                
                if (c == '}') {
                    indentLevel--;
                    if (prevChar != '\n') {
                        result.append('\n');
                    }
                    appendIndent(result, indentLevel);
                    result.append('}');
                    prevWasBrace = true;
                    inLine = false;
                    prevChar = c;
                    continue;
                }
                
                if (c == ';') {
                    result.append(';');
                    result.append('\n');
                    prevWasBrace = false;
                    inLine = false;
                    prevChar = c;
                    continue;
                }
                
                if (c == '\n' || c == '\r') {
                    if (!prevWasBrace) {
                        result.append('\n');
                    }
                    inLine = false;
                    prevChar = '\n';
                    continue;
                }
                
                if (c == ' ' || c == '\t') {
                    if (inLine) {
                        result.append(' ');
                    }
                    prevChar = c;
                    continue;
                }
                
                if (c == ',') {
                    result.append(',');
                    result.append(' ');
                    prevChar = c;
                    continue;
                }
            }
            
            if (!inLine && !prevWasBrace) {
                appendIndent(result, indentLevel);
                inLine = true;
            }
            
            result.append(c);
            prevWasBrace = false;
            prevChar = c;
        }
        
        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
            result.append('\n');
        }
        
        return result.toString();
    }
    
    private static void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level * INDENT_SIZE; i++) {
            sb.append(' ');
        }
    }
}
