package com.github.cargoclean.core.port.operation;

import com.github.cargoclean.core.model.cargo.Cargo;
import com.github.cargoclean.core.model.location.Location;

import java.util.List;

public interface PersistenceGatewayOutputPort {

    List<Location> allLocations();

    /**
     * Persist {@code Cargo} instance, and return instance with {@code id} generated by the database
     * @param cargoToSave {@code Cargo}
     * @return
     */
    Cargo save(Cargo cargoToSave);
}