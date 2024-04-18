package com.github.cargoclean.core.usecase.booking;

import com.github.cargoclean.core.model.cargo.TrackingId;
import com.github.cargoclean.core.model.location.Location;
import com.github.cargoclean.core.port.ErrorHandlingPresenterOutputPort;

import java.util.List;

public interface BookingPresenterOutputPort extends ErrorHandlingPresenterOutputPort {

    void presentNewCargoBookingView(List<Location> locations);

    void presentResultOfNewCargoBooking(TrackingId trackingId);
}
