import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author miller
 */
public class Main {
    private static final String RESOLUTION_SCOPE_URL = "https://raw.githubusercontent.com/ctmillerlin/datadog-schema/main/service-catalog/v3/";
    private static final String ENTITY_DEFINITION_FILE = "entity.datadog.yaml";
    private static final Map<String, Schema> SCHEMAS = Stream.of("system", "service", "datastore", "queue")
        .collect(Collectors.toMap(kind -> kind, kind -> loadSchemas("schemas/" + kind + ".schema.json")));


    public static void main(String[] args) throws IOException {
        List<JsonNode> jsonNodes = parseYAML();

        ObjectMapper objectMapper = new ObjectMapper();
        StringBuilder errors = new StringBuilder();
        for (JsonNode jsonNode : jsonNodes) {
            JSONObject jsonObject = new JSONObject(objectMapper.writeValueAsString(jsonNode));
            StringBuilder errorBuilder = new StringBuilder();
            boolean valid = false;

            for (Map.Entry<String, Schema> schema : SCHEMAS.entrySet()) {
                try {
                    schema.getValue().validate(jsonObject);
                    valid = true;
                    break;
                } catch (ValidationException e) {
                    errorBuilder.append("Validation failed from: ").append(schema.getKey()).append(" schema\n");
                    for (ValidationException validationError : e.getCausingExceptions()) {
                        errorBuilder.append("Error path: ").append(validationError.getPointerToViolation()).append('\n');
                        errorBuilder.append("Error message: ").append(validationError.getMessage()).append('\n');
                    }
                    if (e.getCausingExceptions().isEmpty()) {
                        errorBuilder.append("Error path: ").append(e.getPointerToViolation()).append('\n');
                        errorBuilder.append("Error message: ").append(e.getMessage()).append('\n');
                    }
                }
            }
            if (valid) {
                System.out.println("validated " + definition(jsonNode));
            } else {
                errors.append("validate jsonNode failed, jsonNode = ").append(jsonNode.toPrettyString());
                errors.append(errorBuilder);
            }
        }
        System.err.println(errors);
    }

    private static Schema loadSchemas(String schema) {
        InputStream schemaStream = stream(schema);
        JSONObject mainSchema = new JSONObject(new JSONTokener(schemaStream));
        SchemaLoader schemaLoader = SchemaLoader.builder()
            .schemaJson(mainSchema)
            .resolutionScope(RESOLUTION_SCOPE_URL)
            .build();
        return schemaLoader.load().build();
    }

    private static List<JsonNode> parseYAML() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        YAMLParser yamlParser = new YAMLFactory().createParser(stream(ENTITY_DEFINITION_FILE));
        return objectMapper.readValues(yamlParser, JsonNode.class).readAll();
    }

    private static InputStream stream(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            Iterator<URL> urls = loader.getResources(path).asIterator();
            return openStream(urls, path);
        } catch (IOException var3) {
            throw new Error("can not load resource, path=" + path, var3);
        }
    }

    private static InputStream openStream(Iterator<URL> urls, String path) throws IOException {
        if (urls.hasNext()) {
            URL url = urls.next();
            if (urls.hasNext()) {
                throw new Error("found duplicate resources with same name, path=" + path);
            } else {
                return url.openStream();
            }
        } else {
            throw new Error("can not load resource, path=" + path);
        }
    }

    private static String definition(JsonNode jsonNode) {
        try {
            return jsonNode.get("kind").asText() + ":" + jsonNode.get("metadata").get("name").asText();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
