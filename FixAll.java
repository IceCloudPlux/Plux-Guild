import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.*;

public class FixAll {
    
    public static void main(String[] args) throws IOException {
        Path srcDir = Paths.get("src/main/java");
        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    fixFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("Done!");
    }
    
    private static void fixFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        
        int offset = 0;
        if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
            offset = 3;
        }
        
        String content = new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        
        content = removeComments(content);
        
        content = content.replaceAll("\\b(p)\\s+private\\b", "private");
        content = content.replaceAll("\\b(i)\\s+instance\\b", "instance");
        
        content = content.replaceAll("\\b(private|public|protected)\\s+", "\n$1 ");
        content = content.replaceAll("\\b(@Override|@EventHandler)\\s+", "\n$1\n");
        content = content.replaceAll("(?m)^\\s+", "");
        
        content = content.replaceAll("(?m)^package\\s+", "package ");
        
        content = formatCode(content);
        
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Fixed: " + file);
    }
    
    private static String removeComments(String content) {
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
    
    private static String formatCode(String content) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.startsWith("}") || line.startsWith("};")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            
            for (int i = 0; i < indentLevel * 4; i++) {
                result.append(' ');
            }
            result.append(line);
            result.append('\n');
            
            if (line.endsWith("{") && !line.contains(";")) {
                indentLevel++;
            }
        }
        
        return result.toString();
    }
}
