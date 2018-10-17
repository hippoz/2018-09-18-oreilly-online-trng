package com.mattstine.dddworkshop.pizzashop.delivery;

import java.util.List;
import java.util.stream.Collectors;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrder;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrderAssemblyFinishedEvent;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenOrderRef;
import com.mattstine.dddworkshop.pizzashop.kitchen.KitchenService;
import com.mattstine.dddworkshop.pizzashop.ordering.OrderingService;

/**
 * @author Matt Stine
 */
final class DeliveryService {
	private final EventLog eventLog;
	private final DeliveryOrderRepository deliveryOrderRepository;
	private final OrderingService orderingService;
	private final KitchenService kitchenService;

	DeliveryService(EventLog eventLog, DeliveryOrderRepository deliveryOrderRepository, OrderingService orderingService, KitchenService kitchenService) {
		this.eventLog = eventLog;
		this.deliveryOrderRepository = deliveryOrderRepository;
		this.orderingService = orderingService;
		this.kitchenService = kitchenService;
		eventLog.subscribe(new Topic("kitchen_orders"), this::handleKitchenOrderEvent);
	}

	private void handleKitchenOrderEvent(Event event) {
		if (event instanceof KitchenOrderAssemblyFinishedEvent) {
			DeliveryOrder newOrder = makeNewOrder((KitchenOrderAssemblyFinishedEvent) event);
			deliveryOrderRepository.add(newOrder);
		}
	}

	private DeliveryOrder makeNewOrder(KitchenOrderAssemblyFinishedEvent evt) {
		KitchenOrder kOrder = kitchenService.findKitchenOrderByRef(evt.getRef());

		List<DeliveryOrder.Pizza> pizzas = kOrder.getPizzas().
				stream().
				map(this::makeDeliverPizza).
				collect(Collectors.toList());

		return DeliveryOrder.builder().
			onlineOrderRef(kOrder.getOnlineOrderRef()).
			eventLog(InProcessEventLog.instance()).
			kitchenOrderRef(kOrder.getRef()).
			pizzas(pizzas).
			ref(deliveryOrderRepository.nextIdentity()).
			build();
	}

	private DeliveryOrder.Pizza makeDeliverPizza(KitchenOrder.Pizza pizza) {
		return DeliveryOrder.Pizza.builder().
				size(DeliveryOrder.Pizza.Size.valueOf(pizza.getSize().name())).
				build();
	}

	DeliveryOrder findDeliveryOrderByKitchenOrderRef(KitchenOrderRef kitchenOrderRef) {
		return deliveryOrderRepository.findByKitchenOrderRef(kitchenOrderRef);
	}
}
