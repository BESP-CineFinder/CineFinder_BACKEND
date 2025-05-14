package com.cinefinder.theater.data.dto;

import com.cinefinder.theater.data.Theater;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimplifiedTheaterDto {
    private long id;
    private String code;
    private String name;

    public static SimplifiedTheaterDto from(Theater theater) {
        return new SimplifiedTheaterDto(
                theater.getId(),
                theater.getCode(),
                theater.getName()
        );
    }
}
