package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.adapters.InProcessEventSourcedRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class InProcessEventSourcedPizzaRepository extends InProcessEventSourcedRepository<PizzaRef, Pizza, Pizza.PizzaState, PizzaEvent, PizzaAddedEvent> implements PizzaRepository {

    final Map<KitchenOrderRef, Set<PizzaRef>> kitchenOrderRefSetMap = new HashMap<>();

    InProcessEventSourcedPizzaRepository(EventLog eventLog, Topic pizzas) {
        super(eventLog, PizzaRef.class, Pizza.class, Pizza.PizzaState.class, PizzaAddedEvent.class, pizzas);
        eventLog.subscribe(pizzas, this::handleEvent);
    }

    private void handleEvent(Event evt) {
        if (evt instanceof PizzaAddedEvent) {
            PizzaAddedEvent pae = (PizzaAddedEvent) evt;
            Set<PizzaRef> refs = kitchenOrderRefSetMap.
                    computeIfAbsent(pae.getState().getKitchenOrderRef(),
                            r -> new HashSet<>());
            refs.add(pae.getRef());
        }
    }

    @Override
    public Set<Pizza> findPizzasByKitchenOrderRef(KitchenOrderRef kitchenOrderRef) {
        Set<PizzaRef> refs = kitchenOrderRefSetMap.get(kitchenOrderRef);
        if (refs != null) {
            return refs.stream().map(this::findByRef).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
