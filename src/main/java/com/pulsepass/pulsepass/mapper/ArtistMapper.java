package com.pulsepass.pulsepass.mapper;

import com.pulsepass.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.pulsepass.entity.Artist;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtistMapper {

    ArtistResponse toResponse(Artist artist);
}
