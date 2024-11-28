package com.mican.zara.model;

import com.mican.zara.model.enums.Availability;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Availability availability;
}