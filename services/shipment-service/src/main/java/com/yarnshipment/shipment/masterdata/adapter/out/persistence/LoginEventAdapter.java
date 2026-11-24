package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.events.auth.AccountLocked;
import com.yarnshipment.events.auth.LoginFailed;
import com.yarnshipment.events.auth.LoginSucceeded;
import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginEventPort;
import com.yarnshipment.shipment.masterdata.domain.model.AuthUser;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Giriş olaylarını {@link DomainEventPublisher} ile yayınlar. Giriş transaction açmadığı için olay hemen iletilir ve
 * denetim kaydı kendi kısa transaction'ında yazılır; hatalı denemenin kaydı, giriş reddedilse de kalır. Olayın aktörü
 * hesabın kendisidir.
 */
@Component
final class LoginEventAdapter implements LoginEventPort {

    private static final String AGGREGATE_TYPE = "LoginAccount";

    private final DomainEventPublisher events;

    LoginEventAdapter(DomainEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publishSucceeded(AuthUser user, Instant now) {
        publish(user, LoginSucceeded.TYPE, new LoginSucceeded(user.id(), user.username(), now), now);
    }

    @Override
    public void publishFailed(AuthUser user, Instant now) {
        publish(user, LoginFailed.TYPE, new LoginFailed(user.id(), user.username(), now), now);
    }

    @Override
    public void publishLocked(AuthUser user, Instant lockedUntil, Instant now) {
        publish(user, AccountLocked.TYPE, new AccountLocked(user.id(), user.username(), lockedUntil, now), now);
    }

    private void publish(AuthUser user, String type, Object payload, Instant now) {
        events.publish(new DomainEvent(AGGREGATE_TYPE, user.id(), type, payload, now, user.id()));
    }
}
