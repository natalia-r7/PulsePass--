package com.pulsepass.pulsepass.mapper;

import com.pulsepass.pulsepass.dto.response.VenueResponse;
import com.pulsepass.pulsepass.entity.Venue;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    VenueResponse toResponse(Venue venue);
}
