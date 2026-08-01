import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FixIndent {
    
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
        
        content = content.replaceFirst("^ackage ", "package ");
        
        content = removeBlockComments(content);
        content = removeLineComments(content);
        
        content = normalizeContent(content);
        
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Fixed: " + file);
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
    
    private static String normalizeContent(String content) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (result.length() > 0) {
                    result.append('\n');
                }
                
                int indent = 0;
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    if (c == ' ' || c == '\t') {
                        indent++;
                    } else {
                        break;
                    }
                }
                
                for (int i = 0; i < indent; i++) {
                    result.append(' ');
                }
                result.append(trimmed);
            }
        }
        
        if (result.length() > 0) {
            result.append('\n');
        }
        
        return result.toString();
    }
}
