package com.github.cargoclean.core.usecase.editlocation;

import com.github.cargoclean.core.model.location.Location;
import com.github.cargoclean.core.port.ErrorHandlingPresenterOutputPort;

public interface EditLocationsPresenterOutputPort extends ErrorHandlingPresenterOutputPort {

    void presentAddNewLocationForm();

    void presentResultOfSuccessfulRegistrationOfNewLocation(Location location);
}
