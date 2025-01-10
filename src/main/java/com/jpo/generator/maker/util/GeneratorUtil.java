package com.jpo.generator.maker.util;

import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class GeneratorUtil {
    public static Map<String, String> getQueryMap(String query) {
        if (query == null) return null;
        int pos1 = query.indexOf("?");
        if (pos1 >= 0) {
            query = query.substring(pos1 + 1);
        }
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }
    public static String toCamelCase(String input) {
        StringBuilder camelCaseString = new StringBuilder();
        boolean nextUpperCase = false;
        for (char c : input.toCharArray()) {
            if (c == ' ' || c == '_' || c == '-') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    camelCaseString.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    camelCaseString.append(Character.toLowerCase(c));
                }
            }
        }
        return camelCaseString.toString();
    }
    public static boolean isDate(String str, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.parse(str);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 파일생성
     * @param fileName 파일이름
     * @param content 내용
     * @throws IOException IOException
     */
    public static void writeFile(String fileName, String content) throws IOException {
        FileWriter fileWriter = new FileWriter(fileName);
        fileWriter.write(content);
        fileWriter.close();
    }
    public static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    /**
     * 필드 타입 결정
     * @param fieldName 필드명
     * @param value 값
     * @return 필드타입
     */
    public static String determineFieldType(String fieldName, String value) {
        String fieldType = "String";
        if (fieldName.contains("Dt") && GeneratorUtil.isDate(value, "yyyyMMdd")) {
            fieldType = "String";
        } else {
            if (!value.contains("00")) {
                if (value.matches("-?\\d+(\\.\\d+)?")) {
                    fieldType = value.contains(".") ? "double" : "int";
                }
            }
        }
        return fieldType;
    }

    /**
     * Repository 기본 템플릿
     * @param className 클래스명
     * @param repositoryContent 내용
     */
    public static void defaultRepositoryStringBuilder(String className, StringBuilder repositoryContent) {
        repositoryContent.append("package com.herojoon.jpaproject.repository;\n\n");

        repositoryContent.append("import com.herojoon.jpaproject.entity.").append(className + "Entity").append(";\n");
        repositoryContent.append("import org.springframework.data.jpa.repository.JpaRepository;\n");
        repositoryContent.append("import org.springframework.stereotype.Repository;\n\n");
        repositoryContent.append("@Repository\n");
        repositoryContent.append("public interface ").append(className).append("Repository extends JpaRepository<").append(className + "Entity").append(", Long> {\n");
        repositoryContent.append("}\n");
    }
    /**
     * Entity 기본 템플릿
     * @param className 클래스명
     * @param classContent 내용
     */
    public static void defaultEntityStringBuilder(String className, StringBuilder classContent) {
        classContent.append("package com.herojoon.jpaproject.entity;\n\n");

        classContent.append("import lombok.Getter;\n");
        classContent.append("import lombok.NoArgsConstructor;\n");
        classContent.append("import lombok.Getter;\n");
        classContent.append("import lombok.Setter;\n");
        classContent.append("import lombok.experimental.SuperBuilder;\n\n");

        classContent.append("import javax.persistence.*;\n\n");

        classContent.append("@SuperBuilder\n");
        classContent.append("@Setter\n");
        classContent.append("@Getter\n");
        classContent.append("@NoArgsConstructor\n");
        classContent.append("@Entity\n");
        classContent.append("@Table(name = \"").append(className.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase()).append("\")\n");
        classContent.append("public class ").append(className+"Entity").append(" {\n\n");
    }
}
