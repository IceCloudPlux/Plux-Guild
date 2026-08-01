import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class CleanFix {
    
    public static void main(String[] args) throws IOException {
        Path srcDir = Paths.get("src/main/java");
        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    cleanFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("Done!");
    }
    
    private static void cleanFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        
        int offset = 0;
        if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
            offset = 3;
        }
        
        String content = new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        
        content = removeBlockComments(content);
        content = removeLineComments(content);
        
        content = content.replaceAll("p\\s+private", "private");
        content = content.replaceAll("i\\s+instance", "instance");
        
        content = content.replaceAll("(?m)^\\s+", "");
        
        content = content.replaceAll("\\s+", " ");
        
        content = content.replaceAll("\\b(package|import|public|private|protected|static|final|void|class|extends|implements|return|if|else|for|while|new|this|super|true|false|null)\\b", "\n$1");
        
        content = content.replaceAll("\\b(@Override|@EventHandler)\\b", "\n$1\n");
        
        content = content.replaceAll("\\{\\s*\\{", "{\n{");
        content = content.replaceAll("\\}\\s*\\}", "}\n}");
        
        content = formatCode(content);
        
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Cleaned: " + file);
    }
    
    private static String removeBlockComments(String content) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean inBlock = false;
        char prev = '\0';
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inBlock) {
                if (prev == '*' && c == '/') inBlock = false;
                prev = c;
                continue;
            }
            if (!inString && !inChar && prev == '/' && c == '*') {
                inBlock = true;
                result.deleteCharAt(result.length() - 1);
                prev = c;
                continue;
            }
            if (c == '"' && prev != '\\') inString = !inString;
            else if (c == '\'' && prev != '\\') inChar = !inChar;
            result.append(c);
            prev = c;
        }
        return result.toString();
    }
    
    private static String removeLineComments(String content) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean inLine = false;
        char prev = '\0';
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inLine) {
                if (c == '\n') {
                    inLine = false;
                    result.append(c);
                }
                prev = c;
                continue;
            }
            if (!inString && !inChar && prev == '/' && c == '/') {
                inLine = true;
                result.deleteCharAt(result.length() - 1);
                prev = c;
                continue;
            }
            if (c == '"' && prev != '\\') inString = !inString;
            else if (c == '\'' && prev != '\\') inChar = !inChar;
            result.append(c);
            prev = c;
        }
        return result.toString();
    }
    
    private static String formatCode(String content) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        boolean newLine = true;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                if (!newLine) result.append('\n');
                appendIndent(result, indent);
                result.append('{');
                result.append('\n');
                indent++;
                newLine = true;
                continue;
            }
            if (c == '}') {
                indent--;
                if (!newLine) result.append('\n');
                appendIndent(result, indent);
                result.append('}');
                newLine = false;
                continue;
            }
            if (c == ';') {
                result.append(';');
                result.append('\n');
                newLine = true;
                continue;
            }
            if (c == '\n') {
                newLine = true;
                continue;
            }
            if (c == ' ') {
                if (!newLine) result.append(' ');
                continue;
            }
            if (newLine) {
                appendIndent(result, indent);
                newLine = false;
            }
            result.append(c);
        }
        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
            result.append('\n');
        }
        return result.toString();
    }
    
    private static void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level * 4; i++) sb.append(' ');
    }
}
