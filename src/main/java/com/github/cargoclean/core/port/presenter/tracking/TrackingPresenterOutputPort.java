package com.github.cargoclean.core.port.presenter.tracking;

import com.github.cargoclean.core.model.cargo.Cargo;
import com.github.cargoclean.core.model.location.Location;
import com.github.cargoclean.core.port.error.ErrorHandlingPresenterOutputPort;

public interface TrackingPresenterOutputPort extends ErrorHandlingPresenterOutputPort {
    void presentInitialViewForCargoTracking();

    void presentCargoTrackingInformation(Cargo cargo, Location lastKnownLocation);
}