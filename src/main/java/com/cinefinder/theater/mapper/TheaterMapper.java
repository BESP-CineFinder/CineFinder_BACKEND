package com.cinefinder.theater.mapper;

import com.cinefinder.theater.data.Theater;
import com.cinefinder.theater.data.dto.SimplifiedTheaterDto;

public class TheaterMapper {

    public static SimplifiedTheaterDto toSimplifiedTheaterDto(Theater theater) {
        return new SimplifiedTheaterDto(
                theater.getId(),
                theater.getCode(),
                theater.getName()
        );
    }
}
