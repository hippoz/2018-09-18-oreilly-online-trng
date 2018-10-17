package com.mattstine.dddworkshop.pizzashop.delivery;

import java.util.HashMap;
import java.util.Map;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.adapters.InProcessEventSourcedRepository;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrderRef;

/**
 * @author Matt Stine
 */

final class InProcessEventSourcedDeliveryOrderRepository extends InProcessEventSourcedRepository<DeliveryOrderRef, DeliveryOrder, DeliveryOrder.OrderState, DeliveryOrderEvent, DeliveryOrderAddedEvent> implements DeliveryOrderRepository {

	Map<KitchenOrderRef, DeliveryOrderRef> kitchenOrderRefDeliveryOrderRefMap = new HashMap<>();

	InProcessEventSourcedDeliveryOrderRepository(EventLog eventLog, Topic topic) {
		super(eventLog,
				DeliveryOrderRef.class,
				DeliveryOrder.class,
				DeliveryOrder.OrderState.class,
				DeliveryOrderAddedEvent.class,
				topic);
		eventLog.subscribe(topic, this::handleEvent);
	}

	private void handleEvent(Event event) {
		if (event instanceof DeliveryOrderAddedEvent) {
			DeliveryOrderAddedEvent doae = (DeliveryOrderAddedEvent) event;
			kitchenOrderRefDeliveryOrderRefMap.put(doae.getState().getKitchenOrderRef(), doae.getRef());
		}
	}

	@Override
	public DeliveryOrder findByKitchenOrderRef(KitchenOrderRef kitchenOrderRef) {
		DeliveryOrderRef ref = kitchenOrderRefDeliveryOrderRefMap.get(kitchenOrderRef);
		if (ref != null) {
			return findByRef(ref);
		}
		return null;
	}
}
