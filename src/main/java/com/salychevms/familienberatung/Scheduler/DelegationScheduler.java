package com.salychevms.familienberatung.Scheduler;

import com.salychevms.familienberatung.service.DelegationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DelegationScheduler {
    private final DelegationService delegationService;

    @Scheduled(cron="0 0 * * * *")
    public void autoAbortExpiredDelegations(){
        log.debug("DelegationScheduler started");
        delegationService.autoAbortDelegation();
    }
}
