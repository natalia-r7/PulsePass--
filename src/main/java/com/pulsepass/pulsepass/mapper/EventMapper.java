package com.pulsepass.pulsepass.mapper;

import com.pulsepass.pulsepass.dto.response.EventResponse;
import com.pulsepass.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.pulsepass.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ArtistMapper.class})
public interface EventMapper {

    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "artists",   source = "artists")
    EventResponse toResponse(Event event);

    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    EventSummaryResponse toSummary(Event event);
}
