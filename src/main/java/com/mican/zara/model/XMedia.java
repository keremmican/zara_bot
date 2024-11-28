package com.mican.zara.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class XMedia {
    private String datatype;
    private int set;
    private String type;
    private String kind;
    private String path;
    private String name;
    private int width;
    private int height;
    private String url;
}
