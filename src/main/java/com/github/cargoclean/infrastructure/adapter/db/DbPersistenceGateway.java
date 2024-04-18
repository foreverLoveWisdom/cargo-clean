package com.github.cargoclean.infrastructure.adapter.db;

/*
    Notice on COPYRIGHT:
    -------------------

    Some code in this file is based on, copied from, and or modified from
    the code in the original DDDSample application. Please, see the copyright
    notice in "README.md" and the copy of the original licence in
    "original-license.txt", as well.
 */

/*
    References:
    ----------

    1.  Hikari CP, connection timeout: https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby
 */

import com.github.cargoclean.core.model.cargo.Cargo;
import com.github.cargoclean.core.model.cargo.CargoInfo;
import com.github.cargoclean.core.model.cargo.TrackingId;
import com.github.cargoclean.core.model.handling.EventId;
import com.github.cargoclean.core.model.handling.HandlingEvent;
import com.github.cargoclean.core.model.handling.HandlingHistory;
import com.github.cargoclean.core.model.location.Location;
import com.github.cargoclean.core.model.location.UnLocode;
import com.github.cargoclean.core.model.report.ExpectedArrivals;
import com.github.cargoclean.core.port.persistence.PersistenceGatewayOutputPort;
import com.github.cargoclean.core.port.persistence.PersistenceOperationError;
import com.github.cargoclean.infrastructure.adapter.db.cargo.CargoDbEntity;
import com.github.cargoclean.infrastructure.adapter.db.cargo.CargoDbEntityRepository;
import com.github.cargoclean.infrastructure.adapter.db.cargo.CargoInfoRow;
import com.github.cargoclean.infrastructure.adapter.db.handling.HandlingEventEntity;
import com.github.cargoclean.infrastructure.adapter.db.handling.HandlingEventEntityRepository;
import com.github.cargoclean.infrastructure.adapter.db.location.LocationDbEntityRepository;
import com.github.cargoclean.infrastructure.adapter.db.location.LocationExistsQueryRow;
import com.github.cargoclean.infrastructure.adapter.db.map.DbEntityMapper;
import com.github.cargoclean.infrastructure.adapter.db.report.ExpectedArrivalsQueryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Default implementation of the persistence gateway. It uses one Spring Data JDBC
 * repository for each aggregate root. And it relies on MapStruct mapper to map between
 * DB entities and models.
 * <p>
 * This is a classical "Repository pattern" implementation. We have a full control
 * over the mapping between database rows and domain entities, but since we are not
 * using Hibernate, we load (eagerly) all relations for each aggregate.
 *
 * @see DbEntityMapper
 */
@Service
@RequiredArgsConstructor
public class DbPersistenceGateway implements PersistenceGatewayOutputPort {

    private final LocationDbEntityRepository locationRepository;
    private final CargoDbEntityRepository cargoRepository;

    private final HandlingEventEntityRepository handlingEventRepository;

    private final NamedParameterJdbcOperations queryTemplate;

    private final DbEntityMapper dbMapper;

    @Override
    public TrackingId nextTrackingId() {

        /*
            Based on tracking ID generator in the original:
            "se.citerus.dddsample.infrastructure.persistence.hibernate.CargoRepositoryHibernate"
         */
        final String uuid = UUID.randomUUID().toString();
        return TrackingId.of(uuid.substring(0, uuid.indexOf("-")).toUpperCase());
    }

    @Override
    public EventId nextEventId() {

        // Just for demo, should probably be based on a UUID instead.

        return EventId.of(System.currentTimeMillis());
    }

    @Override
    public List<Location> allLocations() {
        try {
            return toStream(locationRepository.findAll())
                    .map(dbMapper::convert).toList();
        } catch (Exception e) {
            throw new PersistenceOperationError("Cannot retrieve all locations", e);
        }
    }

    @Override
    public Location obtainLocationByUnLocode(UnLocode unLocode) {
        try {
            return dbMapper.convert(locationRepository.findById(unLocode.getCode()).orElseThrow());
        } catch (Exception e) {
            throw new PersistenceOperationError("Cannot obtain location with unLocode: <%s>"
                    .formatted(unLocode), e);
        }
    }

    @Override
    public Cargo saveCargo(Cargo cargoToSave) {

        try {
            final CargoDbEntity cargoDbEntity = dbMapper.convert(cargoToSave);

            // save cargo
            cargoRepository.save(cargoDbEntity);

            // convert and load relations
            return obtainCargoByTrackingId(cargoToSave.getTrackingId());
        } catch (Exception e) {
            throw new PersistenceOperationError("Cannot save cargo with tracking ID: <%s>"
                    .formatted(cargoToSave.getTrackingId()), e);
        }

    }

    @Override
    public Cargo obtainCargoByTrackingId(TrackingId trackingId) {
        try {
            CargoDbEntity cargoDbEntity = cargoRepository.findById(trackingId.getId()).orElseThrow();
            return dbMapper.convert(cargoDbEntity);
        } catch (Exception e) {
            throw new PersistenceOperationError("Cannot obtain cargo with tracking ID: <%s>"
                    .formatted(trackingId), e);
        }

    }

    @Override
    public void deleteCargo(TrackingId trackingId) {
        try {
            cargoRepository.deleteById(trackingId.getId());
        } catch (Exception e) {
            throw new PersistenceOperationError("Cannot delete cargo with tracking ID: <%s>"
                    .formatted(trackingId), e);
        }
    }

    @Override
    public List<ExpectedArrivals> queryForExpectedArrivals() {

        try {
            List<ExpectedArrivalsQueryRow> rows = queryTemplate.query(ExpectedArrivalsQueryRow.SQL,
                    new BeanPropertyRowMapper<>(ExpectedArrivalsQueryRow.class));

            return rows.stream().map(dbMapper::convert).toList();
        } catch (DataAccessException e) {
            throw new PersistenceOperationError("Cannot execute a query for expected arrivals", e);
        }
    }

    @Override
    public void recordHandlingEvent(HandlingEvent event) {
        try {
            HandlingEventEntity eventEntity = dbMapper.convert(event);
            handlingEventRepository.save(eventEntity);
        } catch (Exception e) {
            throw new PersistenceOperationError("Cannot record handling event %s".formatted(event), e);
        }
    }

    @Override
    public HandlingHistory handlingHistory(TrackingId cargoId) {
        try {
            return HandlingHistory.builder()
                    .handlingEvents(handlingEventRepository.findAllByCargoId(cargoId.getId())
                            .stream().map(dbMapper::convert).toList())
                    .build();
        } catch (Exception e) {
            throw new PersistenceOperationError("Cannot obtain handling history for cargo with tracking ID: %s"
                    .formatted(cargoId), e);
        }
    }

    @Override
    public void rollback() {
        // roll back any transaction, if needed
        // code from: https://stackoverflow.com/a/23502214
        try {
            TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException e) {
            // do nothing if not running in a transactional context
        }
    }

    @Override
    public boolean locationExists(Location location) {

        try {
            return Optional.ofNullable(queryTemplate.queryForObject(LocationExistsQueryRow.SQL,
                            Map.of("unlocode", location.getUnlocode().getCode()),
                            new BeanPropertyRowMapper<>(LocationExistsQueryRow.class)))
                    .orElseThrow().exists();
        } catch (Exception e) {
            throw new PersistenceOperationError("Error when querying if location %s exists already"
                    .formatted(location.getUnlocode()), e);
        }

    }

    @Override
    public Location saveLocation(Location location) {
        try {
            locationRepository.save(dbMapper.convert(location));
            return obtainLocationByUnLocode(location.getUnlocode());
        } catch (Exception e) {
            throw new PersistenceOperationError("Error when saving location %s"
                    .formatted(location.getUnlocode()), e);
        }
    }

    @Override
    public List<CargoInfo> allCargoes() {

        try {
            List<CargoInfoRow> rows = queryTemplate.query(CargoInfoRow.SQL,
                    new BeanPropertyRowMapper<>(CargoInfoRow.class));

            return rows.stream().map(dbMapper::convert).toList();
        } catch (DataAccessException e) {
            throw new PersistenceOperationError("Error when querying for information about all cargoes", e);
        }
    }

    private <T> Stream<T> toStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
