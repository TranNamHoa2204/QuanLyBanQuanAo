package common.protocol;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Mapper dùng chung sau khi cấu hình; không bật default typing. */
public final class JsonCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper(JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).maxStringLength(16384)
                    .maxNumberLength(128).build())
            .build()).registerModule(new JavaTimeModule()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            // JsonNode cũng phải giữ độ chính xác DECIMAL(18,2), không đi qua double.
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonCodec() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
