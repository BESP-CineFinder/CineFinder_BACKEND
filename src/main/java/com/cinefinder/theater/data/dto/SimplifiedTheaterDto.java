package com.cinefinder.theater.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimplifiedTheaterDto {
    private long id;
    private String code;
    private String name;
}
