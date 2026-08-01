import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RestoreFiles {
    
    public static void main(String[] args) throws IOException {
        Path srcDir = Paths.get("src/main/java");
        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    restoreFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("Done!");
    }
    
    private static void restoreFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        
        int offset = 0;
        if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
            offset = 3;
        }
        
        String content = new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        
        content = removeAllComments(content);
        
        content = content.replaceAll("(?m)^\\s*package\\s+", "package ");
        
        content = content.replaceAll("\\bprivate\\s+", "\nprivate ");
        content = content.replaceAll("\\bpublic\\s+", "\npublic ");
        content = content.replaceAll("\\bprotected\\s+", "\nprotected ");
        content = content.replaceAll("\\bstatic\\s+", "static ");
        content = content.replaceAll("\\bfinal\\s+", "final ");
        content = content.replaceAll("\\bvoid\\s+", "void ");
        content = content.replaceAll("\\boverride\\s+", "@Override\n");
        content = content.replaceAll("\\bEventHandler\\s+", "@EventHandler\n");
        
        content = formatJavaCode(content);
        
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Restored: " + file);
    }
    
    private static String removeAllComments(String content) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean inBlockComment = false;
        boolean inLineComment = false;
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
            
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    result.append(c);
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
            
            if (!inString && !inChar && prevChar == '/' && c == '/') {
                inLineComment = true;
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
    
    private static String formatJavaCode(String content) {
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean newLineNeeded = true;
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
                    if (prevChar != '\n' && prevChar != '\r') {
                        result.append('\n');
                    }
                    appendIndent(result, indentLevel);
                    result.append('{');
                    result.append('\n');
                    indentLevel++;
                    newLineNeeded = true;
                    prevChar = '{';
                    continue;
                }
                
                if (c == '}') {
                    indentLevel--;
                    if (prevChar != '\n' && prevChar != '\r') {
                        result.append('\n');
                    }
                    appendIndent(result, indentLevel);
                    result.append('}');
                    prevChar = '}';
                    continue;
                }
                
                if (c == ';') {
                    result.append(';');
                    result.append('\n');
                    newLineNeeded = true;
                    prevChar = ';';
                    continue;
                }
                
                if (c == '\n' || c == '\r') {
                    newLineNeeded = true;
                    prevChar = '\n';
                    continue;
                }
                
                if (c == ' ') {
                    if (!newLineNeeded) {
                        result.append(' ');
                    }
                    prevChar = ' ';
                    continue;
                }
            }
            
            if (newLineNeeded && c != ' ' && c != '\t') {
                appendIndent(result, indentLevel);
                newLineNeeded = false;
            }
            
            result.append(c);
            prevChar = c;
        }
        
        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
            result.append('\n');
        }
        
        return result.toString();
    }
    
    private static void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level * 4; i++) {
            sb.append(' ');
        }
    }
}
