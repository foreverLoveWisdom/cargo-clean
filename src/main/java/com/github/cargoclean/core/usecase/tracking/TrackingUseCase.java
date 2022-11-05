package com.github.cargoclean.core.usecase.tracking;

import com.github.cargoclean.core.GenericCargoError;
import com.github.cargoclean.core.model.cargo.Cargo;
import com.github.cargoclean.core.model.cargo.TrackingId;
import com.github.cargoclean.core.model.location.Location;
import com.github.cargoclean.core.port.operation.PersistenceGatewayOutputPort;
import com.github.cargoclean.core.port.presenter.tracking.TrackingPresenterOutputPort;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class TrackingUseCase implements TrackingInputPort {

    private final TrackingPresenterOutputPort presenter;

    private final PersistenceGatewayOutputPort gatewayOps;


    @Override
    public void initializeCargoTrackingView() {
        presenter.presentInitialViewForCargoTracking();
    }

    @Override
    public void trackCargo(String trackingIdText) {
        TrackingId trackingId;
        Cargo cargo;
        Location lastKnownLocation;
        try {
            trackingId = TrackingId.of(trackingIdText);
            cargo = gatewayOps.obtainCargoByTrackingId(trackingId);

            /*
                Here is an interesting point:
                since we have modeled "Cargo" aggregate with IDs, and not references, to other
                aggregate roots, like "Location", we need to load and provide the necessary
                information from other aggregates to the presenter. In the original, "DDDSample",
                this is different since "Cargo" aggregate directly references "Location", which
                is loaded by the ORM.
             */

            // load last known location for the cargo
            lastKnownLocation = Optional.ofNullable(cargo.getDelivery().getLastKnownLocation())
                    .map(gatewayOps::obtainLocationByUnLocode)
                    .orElse(Location.UNKNOWN);

        } catch (GenericCargoError e) {
            presenter.presentError(e);
            return;
        }

        presenter.presentCargoTrackingInformation(cargo, lastKnownLocation);
    }
}