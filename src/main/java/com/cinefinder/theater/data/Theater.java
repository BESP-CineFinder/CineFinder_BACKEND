package com.cinefinder.theater.data;

import com.cinefinder.brand.data.Brand;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "THEATER",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"code", "brand_id"})}
)
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(precision = 18, scale = 15)
    private BigDecimal latitude;

    @Column(precision = 18, scale = 15)
    private BigDecimal longitude;
}
