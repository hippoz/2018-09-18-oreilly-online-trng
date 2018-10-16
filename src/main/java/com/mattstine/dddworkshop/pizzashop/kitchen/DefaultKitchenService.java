package com.mattstine.dddworkshop.pizzashop.kitchen;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrder;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderPaidEvent;
import com.mattstine.dddworkshop.pizzashop.ordering.OnlineOrderRef;
import com.mattstine.dddworkshop.pizzashop.ordering.OrderingService;
import lombok.Value;

@Value
final class DefaultKitchenService implements KitchenService {
	EventLog eventLog;
	KitchenOrderRepository kitchenOrderRepository;
	PizzaRepository pizzaRepository;
	OrderingService orderingService;

	DefaultKitchenService(EventLog eventLog, KitchenOrderRepository kitchenOrderRepository, PizzaRepository pizzaRepository, OrderingService orderingService) {
		this.kitchenOrderRepository = kitchenOrderRepository;
		this.eventLog = eventLog;
		this.pizzaRepository = pizzaRepository;
		this.orderingService = orderingService;
		eventLog.subscribe(new Topic("kitchen_orders"), this::handleKitchenOrderEvent);
		eventLog.subscribe(new Topic("pizzas"), this::handlePizzaEvent);
		eventLog.subscribe(new Topic("ordering"), this::handleOnlineOrderEvent);
	}

	private void handleOnlineOrderEvent(Event event) {
		if (event instanceof OnlineOrderPaidEvent) {
			KitchenOrder order = onlineOrderToKitchenOrder( (OnlineOrderPaidEvent) event);
			kitchenOrderRepository.add(order);
			order.startPrep();
		}
	}

	private KitchenOrder onlineOrderToKitchenOrder(OnlineOrderPaidEvent ope) {
		OnlineOrder onlineOrder = orderingService.findByRef(ope.getRef());
		List<KitchenOrder.Pizza> pizzas =
				onlineOrder.getPizzas().stream().
				map(this::onlineOrderPizza2KitchenPizza).
				collect(Collectors.toList());

		return KitchenOrder.builder().
				eventLog(eventLog).
				onlineOrderRef(ope.getRef()).
				ref(kitchenOrderRepository.nextIdentity()).
				pizzas(pizzas).
				build();
	}

	private KitchenOrder.Pizza onlineOrderPizza2KitchenPizza(com.mattstine.dddworkshop.pizzashop.ordering.Pizza pizza) {
		KitchenOrder.Pizza.Size size = KitchenOrder.Pizza.Size.valueOf(pizza.getSize().name());
		return KitchenOrder.
				Pizza.builder().
				size(size).
				build();
	}


	private void handleKitchenOrderEvent(Event event) {
		if (event instanceof KitchenOrderPrepStartedEvent) {
			KitchenOrderPrepStartedEvent kope = (KitchenOrderPrepStartedEvent)event;
			KitchenOrder order = findKitchenOrderByRef(kope.getRef());
			order.getPizzas().stream().
					map(kitchenPizza2Pizza(order)).
					forEach(p -> {
						pizzaRepository.add(p);
						p.startPrep();
					});
		}
	}

	private Function<KitchenOrder.Pizza, Pizza> kitchenPizza2Pizza(KitchenOrder order) {
		return p -> Pizza.builder().
				eventLog(eventLog).
				kitchenOrderRef(order.getRef()).
				ref(pizzaRepository.nextIdentity()).
				size(Pizza.Size.valueOf(p.getSize().name())).
				build();
	}

	private void handlePizzaEvent(Event event) {
		if (event instanceof PizzaPrepFinishedEvent) {
			PizzaPrepFinishedEvent pfe = (PizzaPrepFinishedEvent)event;
			findPizzaByRef(pfe.getRef()).startBake();
		} else if (event instanceof PizzaBakeStartedEvent) {
			PizzaBakeStartedEvent pfe = (PizzaBakeStartedEvent)event;
			Pizza pizza = findPizzaByRef(pfe.getRef());
			KitchenOrder order = findKitchenOrderByRef(pizza.getKitchenOrderRef());
			if (order.getState() == KitchenOrder.State.PREPPING) {
				order.startBake();
			}
		} else if (event instanceof PizzaBakeFinishedEvent) {
			PizzaBakeFinishedEvent pfe = (PizzaBakeFinishedEvent)event;
			Pizza pizza = findPizzaByRef(pfe.getRef());
			KitchenOrder order = findKitchenOrderByRef(pizza.getKitchenOrderRef());
			if (order.getState() == KitchenOrder.State.BAKING) {
				// First Order finished baking
				order.startAssembly();
			}
			boolean isLastPizza = findPizzasByKitchenOrderRef(order.getRef()).stream().
					allMatch(Pizza::hasFinishedBaking);
			if (isLastPizza) {
				order.finishAssembly();
			}
		}
	}

	@Override
	public void startOrderPrep(KitchenOrderRef kitchenOrderRef) {
		KitchenOrder order = findKitchenOrderByRef(kitchenOrderRef);
		if (order != null) {
			order.startPrep();
		}
	}

	@Override
	public void finishPizzaPrep(PizzaRef ref) {
		Pizza pizza = pizzaRepository.findByRef(ref);
		if (pizza != null) {
			pizza.finishPrep();
		}
	}

	@Override
	public void removePizzaFromOven(PizzaRef ref) {
		Pizza pizza = pizzaRepository.findByRef(ref);
		if (pizza != null) {
			pizza.finishBake();
		}
	}

	@Override
	public KitchenOrder findKitchenOrderByRef(KitchenOrderRef kitchenOrderRef) {
		return kitchenOrderRepository.findByRef(kitchenOrderRef);
	}

	@Override
	public KitchenOrder findKitchenOrderByOnlineOrderRef(OnlineOrderRef onlineOrderRef) {
		return kitchenOrderRepository.findByOnlineOrderRef(onlineOrderRef);
	}

	@Override
	public Pizza findPizzaByRef(PizzaRef ref) {
		return pizzaRepository.findByRef(ref);
	}

	@Override
	public Set<Pizza> findPizzasByKitchenOrderRef(KitchenOrderRef kitchenOrderRef) {
		return pizzaRepository.findPizzasByKitchenOrderRef(kitchenOrderRef);
	}

}
