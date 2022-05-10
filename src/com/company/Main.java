package com.company;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    static final Pattern p1 = Pattern.compile("insert into ([^ ]+) \\(([^\\)]+)\\) values \\(([^\\)]+)\\);", Pattern.CASE_INSENSITIVE);
    static final String newFile = "";
    static final StringBuilder translatedFile = new StringBuilder();
    final static String[] pKey = new String[1];

    public static void main(String[] args) throws IOException {
        Stream<Path> files = Files.walk(Paths.get("input")).filter(Files::isRegularFile);
        if(args.length >= 1) {
            pKey[0] = args[0];
            System.out.println(pKey[0]);
        } else {
            pKey[0] = null;
        }
        files.forEach(f -> {
            System.out.println("Reading file: " + f.getFileName());
            try {
                Files.readAllLines(f)
                        .forEach(line -> {
                            Matcher matcher = p1.matcher(line);
                            if (matcher.matches()) {
                                String db_name = matcher.group(1);
                                String columnsString = matcher.group(2);
                                String valuesString = matcher.group(3);

                                List<String> columns = Arrays.stream(columnsString.split(",")).map(col -> {
                                    char[] chars = col.toCharArray();
                                    boolean cutStart = false;
                                    boolean cutEnd = false;

                                    if (chars[0] == '\'') {
                                        cutStart = true;
//                                        col = col.substring(1);
                                    }
                                    if (chars[chars.length - 1] == '\'') {
//                                        col = col.substring(0, col.length() - 2);
                                        cutEnd = true;
                                    }

                                    return col.substring(
                                            cutStart ? 1                : 0,
                                            cutEnd   ? col.length() - 1 : col.length()
                                    );
                                }).collect(Collectors.toList());
                                List<String> values = Arrays.stream(valuesString.split(",")).collect(Collectors.toList());

                                StringBuilder translated = new StringBuilder("begin\n" +
                                        line + "\n" +
                                        "exception\n" +
                                        "when others then\n" +
                                        "update " + db_name + " set ");

                                if (pKey[0] == null) {
                                    for (int i = 1; i < columns.size(); ++i) {
                                        translated
                                                .append("\n")
                                                .append(columns.get(i)).append(" = ").append(values.get(i));
                                        if (i != columns.size() - 1) {
                                            translated.append(",");
                                        }
                                    }
                                    String translatedLine = translated.append(" where ").append(columns.get(0)).append(" = ").append(values.get(0))
                                            .append(";\nend;\n").toString();
                                    translatedFile.append(translatedLine);
                                } else {
                                    int pKeyIdx = -1;
                                    for (int i = 0; i < columns.size(); ++i) {
                                        if (pKey[0].equals(columns.get(i))) {
                                            pKeyIdx = i;
                                            continue;
                                        }
                                        translated
                                                .append("\n")
                                                .append(columns.get(i)).append(" = ").append(values.get(i));
                                        if (i != columns.size() - 1) {
                                            translated.append(",");
                                        }
                                    }
                                    String translatedLine = translated.append(" where ").append(pKey[0]).append(" = ").append(values.get(pKeyIdx))
                                            .append(";\nend;\n").toString();
                                    translatedFile.append(translatedLine);
                                }

                            }
                        });
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            if (!Files.exists(Paths.get("./output"))) {
                try {
                    Files.createDirectory(Paths.get("./output"));
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }

            String currentOutputFilePath = null;
            try {
                currentOutputFilePath = "./output/" + f.getFileName();
                Files.write(Paths.get(currentOutputFilePath), Collections.singleton(translatedFile.toString()));
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }

            System.out.println(translatedFile);
            System.out.println("File saved to output location:" + currentOutputFilePath);
        });
    }
}
