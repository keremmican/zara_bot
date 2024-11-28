package com.mican.zara.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductResponse {
    private DetailDto detail;

    @Data
    public static class DetailDto {
        private List<ColorDto> colors;
        @Data
        public static class ColorDto {
            private List<SizeDto> sizes;

            @Data
            public static class SizeDto {
                private String name;
                private String availability;
            }
        }
    }
}
