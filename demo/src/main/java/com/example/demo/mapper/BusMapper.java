package com.example.demo.mapper;

import com.example.demo.dto.BusDto;
import com.example.demo.model.Bus;

public class BusMapper {
    
    public static BusDto toDto(Bus bus) {
        if (bus == null) {
            return null;
        }
        return new BusDto(
            bus.getId(),
            bus.getModel()
        );
    }
    
    public static Bus toEntity(BusDto dto) {
        if (dto == null) {
            return null;
        }
        Bus bus = new Bus();
        bus.setId(dto.id());
        bus.setModel(dto.model());
        return bus;
    }
}