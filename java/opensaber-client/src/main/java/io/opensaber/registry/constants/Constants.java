package io.opensaber.registry.constants;

public class Constants {

    public static class JsonldConstants {
        public static final String CONTEXT = "@context";
        public static final String ID = "@id";
        public static final String TYPE = "@type";
        public static final String VALUE = "@value";
        public static final String GRAPH = "@graph";
    }

    public static class MappingConstants {
        public static final String CONTEXT = "context";
        public static final String PREFIX = "prefix";
        public static final String TYPE = "type";
        public static final String ID = "id";
        public static final String DEFINITION = "definition";
        public static final String COLLECTION = "collection";
        public static final String ENUMERATION = "opensaber:enumeration";
        public static final String ENUMERATION_ARRAY = "opensaber:enumeration_array";
        public static final String XSD_ELEMENT = "xsd";
        public static final String SCHEMA_ELEMENT = "schema";
    }

    public static class ApiEndPoints {
        public static final String ADD = "add";
        public static final String UPDATE = "update";
        public static final String DELETE = "delete";
        public static final String READ = "read";
    }

}
